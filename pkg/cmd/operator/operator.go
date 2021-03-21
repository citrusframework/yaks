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

	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/tools/record"
	"math/rand"
	"os"
	"runtime"
	k8sclient "sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/log/zap"
	"time"

	"github.com/citrusframework/yaks/pkg/apis"
	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/controller"
	"github.com/citrusframework/yaks/pkg/event"
	"github.com/citrusframework/yaks/pkg/install"
	"github.com/citrusframework/yaks/pkg/util/defaults"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"

	"github.com/operator-framework/operator-lib/leader"
	corev1 "k8s.io/api/core/v1"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client/config"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager/signals"
)

const (
	OperatorLockName = "yaks-lock"
	WatchNamespaceEnvVar = "WATCH_NAMESPACE"
	NamespaceEnvVar = "NAMESPACE"
	PodNameEnvVar = "POD_NAME"
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
func Run() {
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
	if err != nil {
		log.Error(err, "failed to get watch namespace")
		os.Exit(1)
	}

	// Get a config to talk to the API server
	cfg, err := config.GetConfig()
	if err != nil {
		log.Error(err, "")
		os.Exit(1)
	}

	// Become the leader before proceeding
	err = leader.Become(context.TODO(), OperatorLockName)
	if err != nil {
		if err == leader.ErrNoNamespace {
			log.Info("Local run detected, leader election is disabled")
		} else {
			log.Error(err, "")
			os.Exit(1)
		}
	}

	// Configure an event broadcaster
	c, err := client.NewClient(false)
	if err != nil {
		log.Error(err, "cannot initialize client")
		os.Exit(1)
	}

	// We do not rely on the event broadcaster managed by controller runtime,
	// so that we can check the operator has been granted permission to create
	// Events. This is required for the operator to be installable by standard
	// admin users, that are not granted create permission on Events by default.
	broadcaster := record.NewBroadcaster()
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

	// Create a new Cmd to provide shared dependencies and start components
	mgr, err := ctrl.NewManager(cfg, ctrl.Options{
		Namespace:        watchNamespace,
		EventBroadcaster: broadcaster,
	})
	if err != nil {
		log.Error(err, "")
		os.Exit(1)
	}

	log.Info("Registering Components.")

	// Setup Scheme for all resources
	if err := apis.AddToScheme(mgr.GetScheme()); err != nil {
		log.Error(err, "")
		os.Exit(1)
	}

	// Try to register the OpenShift CLI Download link if possible
	installCtx, installCancel := context.WithTimeout(context.TODO(), 1*time.Minute)
	defer installCancel()
	install.OperatorStartupOptionalTools(installCtx, c, log)

	if err:= installInstance(installCtx, c, watchNamespace == "", defaults.Version); err != nil {
		log.Error(err, "failed to install yaks custom resource")
		os.Exit(1)
	}

	// Setup all Controllers
	if err := controller.AddToManager(mgr); err != nil {
		log.Error(err, "")
		os.Exit(1)
	}

	log.Info("Starting the Cmd.")

	if err := mgr.Start(signals.SetupSignalHandler()); err != nil {
		log.Error(err, "manager exited non-zero")
		os.Exit(1)
	}

	broadcaster.Shutdown()
}

func installInstance(ctx context.Context, c client.Client, global bool, version string) error {
	operatorNamespace, err := getOperatorNamespace()
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
				Global: global,
				Pod: operatorPodName,
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
func getOperatorPodName() (string) {
	podName, _ := os.LookupEnv(PodNameEnvVar)
	return podName
}

// getOperatorNamespace returns the Namespace the operator is installed
func getOperatorNamespace() (string, error) {
	ns, found := os.LookupEnv(NamespaceEnvVar)
	if !found {
		return "", fmt.Errorf("%s env must be set", NamespaceEnvVar)
	}
	return ns, nil
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
