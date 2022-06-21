/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package operator

import (
	"context"
	"flag"
	"fmt"

	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/selection"
	"k8s.io/client-go/tools/leaderelection/resourcelock"
	"sigs.k8s.io/controller-runtime/pkg/cache"
	"sigs.k8s.io/controller-runtime/pkg/manager"

	"math/rand"
	"os"
	"runtime"
	"time"

	batchv1 "k8s.io/api/batch/v1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/tools/record"
	k8sclient "sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/log/zap"

	"github.com/citrusframework/yaks/pkg/apis"
	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/controller"
	"github.com/citrusframework/yaks/pkg/event"
	"github.com/citrusframework/yaks/pkg/install"
	"github.com/citrusframework/yaks/pkg/util/defaults"
	"github.com/citrusframework/yaks/pkg/util/envvar"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"

	coordination "k8s.io/api/coordination/v1"
	corev1 "k8s.io/api/core/v1"
	"sigs.k8s.io/controller-runtime/pkg/client/config"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager/signals"
)

const (
	OperatorLockName     = "yaks-lock"
	WatchNamespaceEnvVar = "WATCH_NAMESPACE"
	PodNameEnvVar        = "POD_NAME"
)

var log = logf.Log.WithName("cmd")

// GitCommit --
var GitCommit string

func printVersion() {
	log.Info(fmt.Sprintf("Go Version: %s", runtime.Version()))
	log.Info(fmt.Sprintf("Go OS/Arch: %s/%s", runtime.GOOS, runtime.GOARCH))
	log.Info(fmt.Sprintf("YAKS Operator Version: %v", defaults.Version))
	log.Info(fmt.Sprintf("YAKS Git Commit: %v", GitCommit))
}

// Run starts the YAKS operator
func Run(leaderElection bool, leaderElectionID string) {
	rand.Seed(time.Now().UTC().UnixNano())

	flag.Parse()

	// The logger instantiated here can be changed to any logger
	// implementing the logr.Logger interface. This logger will
	// be propagated through the whole operator, generating
	// uniform and structured logs.
	logf.SetLogger(zap.New(func(o *zap.Options) {
		o.Development = false
	}))

	printVersion()

	watchNamespace, err := getWatchNamespace()
	exitOnError(err, "failed to get watch namespace")

	// Get a config to talk to the API server
	cfg, err := config.GetConfig()
	exitOnError(err, "")

	// Configure an event broadcaster
	c, err := client.NewClientWithConfig(false, cfg)
	exitOnError(err, "cannot initialize client")

	// We do not rely on the event broadcaster managed by controller runtime,
	// so that we can check the operator has been granted permission to create
	// Events. This is required for the operator to be installable by standard
	// admin users, that are not granted create permission on Events by default.
	broadcaster := record.NewBroadcaster()
	defer broadcaster.Shutdown()

	if ok, err := kubernetes.CheckPermission(context.TODO(), c, corev1.GroupName, "events", watchNamespace, "", "create"); err != nil || !ok {
		// Do not sink Events to the server as they'll be rejected
		broadcaster = event.NewSinkLessBroadcaster(broadcaster)
		exitOnError(err, "cannot check permissions for creating Events")
		log.Info("Event broadcasting is disabled because of missing permissions to create Events")
	}

	operatorNamespace, err := envvar.GetOperatorNamespace()
	exitOnError(err, "failed to determine operator namespace")
	if operatorNamespace == "" {
		// Fallback to using the watch namespace when the operator is not in-cluster.
		// It does not support local (off-cluster) operator watching resources globally,
		// in which case it's not possible to determine a namespace.
		operatorNamespace = watchNamespace
		if operatorNamespace == "" {
			leaderElection = false
			log.Info("unable to determine namespace for leader election")
		}
	}

	// nolint: gocritic
	if ok, err := kubernetes.CheckPermission(context.TODO(), c, corev1.GroupName, "events", watchNamespace, "", "create"); err != nil || !ok {
		// Do not sink Events to the server as they'll be rejected
		broadcaster = event.NewSinkLessBroadcaster(broadcaster)
		if err != nil {
			log.Error(err, "cannot check permissions for configuring event broadcaster")
		} else if !ok {
			log.Info("Event broadcasting to Kubernetes is disabled because of missing permissions to create events")
		}
	}

	if ok, err := kubernetes.CheckPermission(context.TODO(), c, coordination.GroupName, "leases", operatorNamespace, "", "create"); err != nil || !ok {
		leaderElection = false
		exitOnError(err, "cannot check permissions for creating Leases")
		log.Info("The operator is not granted permissions to create Leases")
	}

	if !leaderElection {
		log.Info("Leader election is disabled!")
	}

	hasTestLabel, err := labels.NewRequirement(v1alpha1.TestLabel, selection.Exists, []string{})
	exitOnError(err, "cannot create test label selector")
	selector := labels.NewSelector().Add(*hasTestLabel)

	// Create a new Cmd to provide shared dependencies and start components
	mgr, err := manager.New(c.GetConfig(), manager.Options{
		Namespace:                     watchNamespace,
		EventBroadcaster:              broadcaster,
		LeaderElection:                leaderElection,
		LeaderElectionNamespace:       operatorNamespace,
		LeaderElectionID:              leaderElectionID,
		LeaderElectionResourceLock:    resourcelock.LeasesResourceLock,
		LeaderElectionReleaseOnCancel: true,
		NewCache: cache.BuilderWithOptions(
			cache.Options{
				SelectorsByObject: cache.SelectorsByObject{
					&corev1.Pod{}:  {Label: selector},
					&batchv1.Job{}: {Label: selector},
				},
			},
		),
	})
	exitOnError(err, "")

	exitOnError(
		mgr.GetFieldIndexer().IndexField(context.Background(), &corev1.Pod{}, "status.phase",
			func(obj k8sclient.Object) []string {
				return []string{string(obj.(*corev1.Pod).Status.Phase)}
			}),
		"unable to set up field indexer for status.phase: %v",
	)

	log.Info("Registering Components.")

	// Setup Scheme for all resources
	exitOnError(apis.AddToScheme(mgr.GetScheme()), "")

	// Try to register the OpenShift CLI Download link if possible
	installCtx, installCancel := context.WithTimeout(context.TODO(), 1*time.Minute)
	defer installCancel()
	install.OperatorStartupOptionalTools(installCtx, c, log)

	err = installInstance(installCtx, c, watchNamespace == "", defaults.Version)
	exitOnError(err, "failed to install yaks custom resource")

	// Setup all Controllers
	exitOnError(controller.AddToManager(mgr), "")

	log.Info("Starting the Cmd.")

	err = mgr.Start(signals.SetupSignalHandler())
	exitOnError(err, "manager exited non-zero")
}

func installInstance(ctx context.Context, c client.Client, global bool, version string) error {
	operatorNamespace, err := envvar.GetOperatorNamespace()
	if err != nil {
		return err
	}

	operatorPodName := getOperatorPodName()

	yaks := v1alpha1.Instance{
		TypeMeta: metav1.TypeMeta{
			Kind:       v1alpha1.InstanceKind,
			APIVersion: v1alpha1.SchemeGroupVersion.String(),
		},
		ObjectMeta: metav1.ObjectMeta{
			Namespace: operatorNamespace,
			Name:      "yaks",
		},
		Spec: v1alpha1.InstanceSpec{
			Operator: v1alpha1.OperatorSpec{
				Global:    global,
				Pod:       operatorPodName,
				Namespace: operatorNamespace,
			},
		},
	}

	if err := c.Create(ctx, &yaks); err != nil && k8serrors.IsAlreadyExists(err) {
		clone := yaks.DeepCopy()
		var key k8sclient.ObjectKey
		key = k8sclient.ObjectKeyFromObject(clone)

		err = c.Get(ctx, key, clone)
		if err != nil {
			return err
		}
		// Update the custom resource
		yaks.ResourceVersion = clone.ResourceVersion
		err = c.Update(ctx, &yaks)
		if err != nil {
			return err
		}
	} else if err != nil {
		return err
	}

	yaks.Status = v1alpha1.InstanceStatus{
		Version: version,
	}

	//Update the status
	err = c.Status().Update(ctx, &yaks)
	if err != nil {
		return err
	}

	return nil
}

// getOperatorPodName returns the name the operator pod
func getOperatorPodName() string {
	podName, _ := os.LookupEnv(PodNameEnvVar)
	return podName
}

// getWatchNamespace returns the Namespace the operator should be watching for changes
func getWatchNamespace() (string, error) {
	// WatchNamespaceEnvVar is the constant for env variable WATCH_NAMESPACE
	// which specifies the Namespace to watch.
	// An empty value means the operator is running with cluster scope.
	ns, found := os.LookupEnv(WatchNamespaceEnvVar)
	if !found {
		return "", fmt.Errorf("%s must be set", WatchNamespaceEnvVar)
	}
	return ns, nil
}

func exitOnError(err error, msg string) {
	if err != nil {
		log.Error(err, msg)
		os.Exit(1)
	}
}
