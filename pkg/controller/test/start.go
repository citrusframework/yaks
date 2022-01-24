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

package test

import (
	"context"
	"fmt"
	"github.com/citrusframework/yaks/pkg/util/envvar"
	"github.com/citrusframework/yaks/pkg/util/openshift"
	"k8s.io/apimachinery/pkg/api/resource"
	"strings"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/config"
	"github.com/citrusframework/yaks/pkg/install"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"
	snap "github.com/container-tools/snap/pkg/api"
	batchv1 "k8s.io/api/batch/v1"
	v1 "k8s.io/api/core/v1"
	rbacv1 "k8s.io/api/rbac/v1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	ctrl "sigs.k8s.io/controller-runtime/pkg/client"
)

// NewStartAction creates a new start action
func NewStartAction() Action {
	return &startAction{}
}

type startAction struct {
	baseAction
}

// Name returns a common name of the action
func (action *startAction) Name() string {
	return "start"
}

// CanHandle tells whether this action can handle the test
func (action *startAction) CanHandle(test *v1alpha1.Test) bool {
	return test.Status.Phase == v1alpha1.TestPhasePending
}

// Handle handles the test
func (action *startAction) Handle(ctx context.Context, test *v1alpha1.Test) (*v1alpha1.Test, error) {
	// Create the viewer service account
	if err := action.ensureServiceAccountRoles(ctx, test.Namespace); err != nil {
		return nil, err
	}

	configMap := action.newTestConfigMap(ctx, test)
	job, err := action.newTestJob(ctx, test, configMap)
	if err != nil {
		test.Status.Phase = v1alpha1.TestPhaseError
		test.Status.Errors = err.Error()
		return nil, err
	}
	resources := []ctrl.Object{configMap, job}
	if err := kubernetes.ReplaceResources(ctx, action.client, resources); err != nil {
		test.Status.Phase = v1alpha1.TestPhaseError
		test.Status.Errors = err.Error()
		return nil, err
	}

	test.Status.Phase = v1alpha1.TestPhaseRunning

	if operatorNamespace, err := envvar.GetOperatorNamespace(); err == nil {
		test.Labels[config.OperatorLabel] = operatorNamespace
	} else {
		return nil, err
	}

	return test, nil
}

func (action *startAction) newTestJob(ctx context.Context, test *v1alpha1.Test, configMap *v1.ConfigMap) (*batchv1.Job, error) {
	controller := true
	blockOwnerDeletion := true
	retries := int32(0)
	job := batchv1.Job{
		TypeMeta: metav1.TypeMeta{
			Kind:       "Job",
			APIVersion: v1.SchemeGroupVersion.String(),
		},
		ObjectMeta: metav1.ObjectMeta{
			Namespace: test.Namespace,
			Name:      TestJobNameFor(test),
			Labels: map[string]string{
				"app":                "yaks",
				v1alpha1.TestLabel:   test.Name,
				v1alpha1.TestIdLabel: test.Status.TestID,
			},
			OwnerReferences: []metav1.OwnerReference{
				{
					APIVersion:         test.APIVersion,
					Kind:               test.Kind,
					Name:               test.Name,
					UID:                test.UID,
					Controller:         &controller,
					BlockOwnerDeletion: &blockOwnerDeletion,
				},
			},
		},
		Spec: batchv1.JobSpec{
			BackoffLimit: &retries,
			Template: v1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: test.Namespace,
					Labels: map[string]string{
						"app":                       "yaks",
						v1alpha1.TestLabel:          test.Name,
						v1alpha1.TestIdLabel:        test.Status.TestID,
						"app.kubernetes.io/part-of": "yaks-tests",
					},
				},
				Spec: v1.PodSpec{
					ServiceAccountName: config.ViewerServiceAccount,
					Containers: []v1.Container{
						{
							Name:                     "test",
							Image:                    config.GetTestBaseImage(),
							Command:                  getMavenArgLine(),
							TerminationMessagePolicy: "FallbackToLogsOnError",
							TerminationMessagePath:   "/dev/termination-log",
							ImagePullPolicy:          v1.PullIfNotPresent,
							TTY:                      true,
							VolumeMounts: []v1.VolumeMount{
								{
									Name:      "tests",
									MountPath: "/etc/yaks/tests",
								},
								{
									Name:      "secrets",
									MountPath: "/etc/yaks/secrets",
								},
							},
							Env: []v1.EnvVar{
								{
									Name:  "YAKS_TERMINATION_LOG",
									Value: "/dev/termination-log",
								},
								{
									Name:  "YAKS_TESTS_PATH",
									Value: "/etc/yaks/tests",
								},
								{
									Name:  "YAKS_SECRETS_PATH",
									Value: "/etc/yaks/secrets",
								},
							},
						},
					},
					RestartPolicy: v1.RestartPolicyNever,
					Volumes: []v1.Volume{
						{
							Name: "tests",
							VolumeSource: v1.VolumeSource{
								ConfigMap: &v1.ConfigMapVolumeSource{
									LocalObjectReference: v1.LocalObjectReference{
										Name: configMap.Name,
									},
								},
							},
						},
					},
				},
			},
		},
	}

	for _, value := range test.Spec.Env {
		pair := strings.SplitN(value, "=", 2)
		if len(pair) == 2 {
			k := strings.TrimSpace(pair[0])
			v := strings.TrimSpace(pair[1])

			if len(k) > 0 && len(v) > 0 {
				job.Spec.Template.Spec.Containers[0].Env = append(job.Spec.Template.Spec.Containers[0].Env, v1.EnvVar{
					Name:  k,
					Value: v,
				})
			}
		}
	}

	if test.Spec.Settings.Name != "" {
		job.Spec.Template.Spec.Containers[0].Env = append(job.Spec.Template.Spec.Containers[0].Env, v1.EnvVar{
			Name:  "YAKS_SETTINGS_FILE",
			Value: "/etc/yaks/tests/" + test.Spec.Settings.Name,
		})
	}

	var clusterType v1alpha1.ClusterType
	if isOpenshift, err := openshift.IsOpenShift(action.client); err == nil && isOpenshift {
		clusterType = v1alpha1.ClusterTypeOpenShift
	} else {
		clusterType = v1alpha1.ClusterTypeKubernetes
	}

	job.Spec.Template.Spec.Containers[0].Env = append(job.Spec.Template.Spec.Containers[0].Env, v1.EnvVar{
		Name:  "YAKS_CLUSTER_TYPE",
		Value: strings.ToUpper(string(clusterType)),
	}, v1.EnvVar{
		Name:  "YAKS_TEST_NAME",
		Value: test.Name,
	}, v1.EnvVar{
		Name:  "YAKS_TEST_ID",
		Value: test.Status.TestID,
	})

	if err := action.bindSecrets(ctx, test, &job); err != nil {
		return nil, err
	}

	if err := action.injectSnap(ctx, &job); err != nil {
		return nil, err
	}

	action.addSelenium(test, &job)
	action.addKubeDock(test, &job)

	return &job, nil
}

func getMavenArgLine() []string {
	argLine := make([]string, 0)

	// add base flags
	argLine = append(argLine, "mvn", "-B", "-q")

	// add pom file path
	argLine = append(argLine, "-f", "/deployments/data/yaks-runtime-maven")

	// add settings file
	argLine = append(argLine, "-s", "/deployments/artifacts/settings.xml")

	// add test goal
	argLine = append(argLine, "verify")

	// add system property settings
	argLine = append(argLine, "-Dremoteresources.skip=true", "-Dmaven.repo.local=/deployments/artifacts/m2")

	return argLine
}

func (action *startAction) newTestConfigMap(ctx context.Context, test *v1alpha1.Test) *v1.ConfigMap {
	controller := true
	blockOwnerDeletion := true

	sources := make(map[string]string)
	sources[test.Spec.Source.Name] = test.Spec.Source.Content

	if test.Spec.Settings.Name != "" {
		sources[test.Spec.Settings.Name] = test.Spec.Settings.Content
	}

	for _, testResource := range test.Spec.Resources {
		sources[testResource.Name] = testResource.Content
	}

	cm := v1.ConfigMap{
		TypeMeta: metav1.TypeMeta{
			Kind:       "ConfigMap",
			APIVersion: v1.SchemeGroupVersion.String(),
		},
		ObjectMeta: metav1.ObjectMeta{
			Namespace: test.Namespace,
			Name:      TestResourceNameFor(test),
			Labels: map[string]string{
				"app":                "yaks",
				v1alpha1.TestLabel:   test.Name,
				v1alpha1.TestIdLabel: test.Status.TestID,
			},
			OwnerReferences: []metav1.OwnerReference{
				{
					APIVersion:         test.APIVersion,
					Kind:               test.Kind,
					Name:               test.Name,
					UID:                test.UID,
					Controller:         &controller,
					BlockOwnerDeletion: &blockOwnerDeletion,
				},
			},
		},
		Data: sources,
	}
	return &cm
}

func (action *startAction) ensureServiceAccountRoles(ctx context.Context, namespace string) error {
	rb := rbacv1.RoleBinding{}
	rbKey := ctrl.ObjectKey{
		Name:      config.ViewerServiceAccount,
		Namespace: namespace,
	}

	err := action.client.Get(ctx, rbKey, &rb)
	if err != nil && k8serrors.IsNotFound(err) {
		// Create proper service account and roles
		err = install.ViewerServiceAccountRoles(ctx, action.client, namespace)
		if err != nil {
			return err
		}
	} else if err != nil {
		return err
	}

	operatorNamespace, err := envvar.GetOperatorNamespace()
	if err != nil {
		return err
	}

	labelSelector := metav1.ListOptions{
		LabelSelector: fmt.Sprintf("%s=%s", config.AppendToViewerLabel, "true"),
	}

	if err := action.applyOperatorRoles(ctx, namespace, operatorNamespace, labelSelector); err != nil {
		return err
	}

	instance, err := v1alpha1.GetInstance(ctx, action.client, operatorNamespace)
	if err != nil && !k8serrors.IsNotFound(err) {
		return err
	}

	if v1alpha1.IsGlobal(instance) {
		err := action.applyOperatorClusterRoles(ctx, namespace, labelSelector)
		if err != nil {
			return err
		}
	}

	return nil
}

func (action *startAction) applyOperatorRoles(ctx context.Context, namespace string, operatorNamespace string, labelSelector metav1.ListOptions) error {
	// Apply labeled YAKS operator roles to service account
	roleList, err := action.client.RbacV1().Roles(operatorNamespace).List(ctx, labelSelector)
	if err != nil {
		return err
	}

	for _, r := range roleList.Items {
		role := &rbacv1.Role{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: namespace,
				Name:      strings.ReplaceAll(r.Name, config.OperatorServiceAccount, config.ViewerServiceAccount),
				Labels: map[string]string{
					"app": "yaks",
				},
			},
			Rules: r.Rules,
		}

		_, err := action.client.RbacV1().Roles(namespace).Create(ctx, role, metav1.CreateOptions{})
		if err != nil && k8serrors.IsAlreadyExists(err) {
			continue
		} else if err != nil {
			return err
		}
	}

	// Apply labeled YAKS operator role bindings to service account
	rbList, err := action.client.RbacV1().RoleBindings(operatorNamespace).List(ctx, labelSelector)
	if err != nil {
		return err
	}

	for _, rb := range rbList.Items {
		roleBinding := &rbacv1.RoleBinding{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: namespace,
				Name:      strings.ReplaceAll(rb.Name, config.OperatorServiceAccount, config.ViewerServiceAccount),
				Labels: map[string]string{
					"app": "yaks",
				},
			},
			Subjects: []rbacv1.Subject{
				{
					Kind:      "ServiceAccount",
					Name:      config.ViewerServiceAccount,
					Namespace: namespace,
				},
			},
			RoleRef: rbacv1.RoleRef{
				APIGroup: rb.RoleRef.APIGroup,
				Kind:     "Role",
				Name:     strings.ReplaceAll(rb.RoleRef.Name, config.OperatorServiceAccount, config.ViewerServiceAccount),
			},
		}

		_, err := action.client.RbacV1().RoleBindings(namespace).Create(ctx, roleBinding, metav1.CreateOptions{})
		if err != nil && k8serrors.IsAlreadyExists(err) {
			continue
		} else if err != nil {
			return err
		}
	}

	return nil
}

func (action *startAction) applyOperatorClusterRoles(ctx context.Context, namespace string, labelSelector metav1.ListOptions) error {
	// Apply labeled cluster roles to service account
	crList, err := action.client.RbacV1().ClusterRoles().List(ctx, labelSelector)
	if err != nil {
		return err
	}

	for _, cr := range crList.Items {
		role := &rbacv1.Role{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: namespace,
				Name:      strings.ReplaceAll(cr.Name, config.OperatorServiceAccount, config.ViewerServiceAccount),
				Labels: map[string]string{
					"app": "yaks",
				},
			},
			Rules: cr.Rules,
		}

		_, err := action.client.RbacV1().Roles(namespace).Create(ctx, role, metav1.CreateOptions{})
		if err != nil && k8serrors.IsAlreadyExists(err) {
			continue
		} else if err != nil {
			return err
		}
	}

	// Apply labeled cluster role bindings to service account
	crbList, err := action.client.RbacV1().ClusterRoleBindings().List(ctx, labelSelector)
	if err != nil {
		return err
	}

	for _, crb := range crbList.Items {
		roleBinding := &rbacv1.RoleBinding{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: namespace,
				Name:      strings.ReplaceAll(crb.Name, config.OperatorServiceAccount, config.ViewerServiceAccount),
				Labels: map[string]string{
					"app": "yaks",
				},
			},
			Subjects: []rbacv1.Subject{
				{
					Kind:      "ServiceAccount",
					Name:      config.ViewerServiceAccount,
					Namespace: namespace,
				},
			},
			RoleRef: rbacv1.RoleRef{
				APIGroup: crb.RoleRef.APIGroup,
				Kind:     "Role",
				Name:     strings.ReplaceAll(crb.RoleRef.Name, config.OperatorServiceAccount, config.ViewerServiceAccount),
			},
		}

		_, err := action.client.RbacV1().RoleBindings(namespace).Create(ctx, roleBinding, metav1.CreateOptions{})
		if err != nil && k8serrors.IsAlreadyExists(err) {
			continue
		} else if err != nil {
			return err
		}
	}

	return nil
}

func (action *startAction) addSelenium(test *v1alpha1.Test, job *batchv1.Job) {
	if test.Spec.Selenium.Image != "" {
		shareProcessNamespace := true
		job.Spec.Template.Spec.ShareProcessNamespace = &shareProcessNamespace

		// set explicit non-root user for all containers - required for killing the supervisord process later
		uid := int64(1000)
		job.Spec.Template.Spec.SecurityContext = &v1.PodSecurityContext{
			RunAsUser: &uid,
		}

		job.Spec.Template.Spec.Containers = append(job.Spec.Template.Spec.Containers, v1.Container{
			Name:            "selenium",
			Image:           test.Spec.Selenium.Image,
			ImagePullPolicy: v1.PullIfNotPresent,
			VolumeMounts: []v1.VolumeMount{
				{
					Name:      "dshm",
					MountPath: "/dev/shm",
				},
			},
		})

		job.Spec.Template.Spec.Volumes = append(job.Spec.Template.Spec.Volumes, v1.Volume{
			Name: "dshm",
			VolumeSource: v1.VolumeSource{
				EmptyDir: &v1.EmptyDirVolumeSource{
					Medium:    v1.StorageMediumMemory,
					SizeLimit: resource.NewScaledQuantity(2, resource.Giga),
				},
			},
		})

		job.Spec.Template.Spec.Containers[0].SecurityContext = &v1.SecurityContext{
			Capabilities: &v1.Capabilities{
				Add: []v1.Capability{"SYS_PTRACE"},
			},
		}

		// Add selenium profile that will shutdown the selenium container when test is finished
		job.Spec.Template.Spec.Containers[0].Command = append(job.Spec.Template.Spec.Containers[0].Command, "-Pselenium")
	}
}

func (action *startAction) addKubeDock(test *v1alpha1.Test, job *batchv1.Job) {
	if test.Spec.KubeDock.Image != "" {
		shareProcessNamespace := true
		job.Spec.Template.Spec.ShareProcessNamespace = &shareProcessNamespace

		// set explicit non-root user for all containers - required for killing the supervisord process later
		uid := int64(1000)
		job.Spec.Template.Spec.SecurityContext = &v1.PodSecurityContext{
			RunAsUser: &uid,
		}

		job.Spec.Template.Spec.Containers = append(job.Spec.Template.Spec.Containers, v1.Container{
			Name:            "kubedock",
			Image:           test.Spec.KubeDock.Image,
			ImagePullPolicy: v1.PullIfNotPresent,
			Args:            []string{"server", "--reverse-proxy"},
		})

		job.Spec.Template.Spec.Containers[0].Env = append(job.Spec.Template.Spec.Containers[0].Env,
			v1.EnvVar{
				Name:  "DOCKER_HOST",
				Value: "tcp://localhost:2475",
			},
			v1.EnvVar{
				// needed to get it working with kubedock
				Name:  "TESTCONTAINERS_RYUK_DISABLED",
				Value: "true",
			},
			v1.EnvVar{
				// needed to get it working with kubedock
				Name:  "TESTCONTAINERS_CHECKS_DISABLE",
				Value: "true",
			})

		job.Spec.Template.Spec.Containers[0].SecurityContext = &v1.SecurityContext{
			Capabilities: &v1.Capabilities{
				Add: []v1.Capability{"SYS_PTRACE"},
			},
		}

		// Add kubedock profile that will shutdown the kubedock container when test is finished
		job.Spec.Template.Spec.Containers[0].Command = append(job.Spec.Template.Spec.Containers[0].Command, "-Pkubedock")
	}
}

func (action *startAction) bindSecrets(ctx context.Context, test *v1alpha1.Test, job *batchv1.Job) error {
	var options = metav1.ListOptions{
		LabelSelector: fmt.Sprintf("%s=%s", v1alpha1.TestLabel, test.Name),
	}
	if test.Spec.Secret != "" {
		options.LabelSelector = fmt.Sprintf("%s=%s",
			v1alpha1.TestConfigurationLabel, test.Spec.Secret)
	}
	secrets, err := action.client.CoreV1().Secrets(test.Namespace).List(ctx, options)
	if err != nil {
		return err
	}

	var found bool
	for _, item := range secrets.Items {
		if item.Labels != nil && item.Labels[v1alpha1.TestConfigurationLabel] != test.Spec.Secret {
			continue
		}

		job.Spec.Template.Spec.Volumes = append(job.Spec.Template.Spec.Volumes, v1.Volume{
			Name: "secrets",
			VolumeSource: v1.VolumeSource{
				Secret: &v1.SecretVolumeSource{
					SecretName: item.Name,
				},
			},
		})
		found = true
		break
	}

	if !found {
		job.Spec.Template.Spec.Volumes = append(job.Spec.Template.Spec.Volumes, v1.Volume{
			Name: "secrets",
			VolumeSource: v1.VolumeSource{
				EmptyDir: &v1.EmptyDirVolumeSource{},
			},
		})
	}

	return nil
}

func (action *startAction) injectSnap(ctx context.Context, job *batchv1.Job) error {
	bucket := "yaks"
	options := snap.SnapOptions{
		Bucket: bucket,
	}
	s3, err := snap.NewSnap(action.config, job.Namespace, true, options)
	if err != nil {
		return err
	}
	installed, err := s3.IsInstalled(ctx)
	if err != nil {
		return err
	}
	if installed {
		// Adding env var to enable the S3 service
		url, err := s3.GetEndpoint(ctx)
		if err != nil {
			return err
		}
		creds, err := s3.GetCredentials(ctx)
		if err != nil {
			return err
		}

		job.Spec.Template.Spec.Containers[0].Env = append(job.Spec.Template.Spec.Containers[0].Env,
			v1.EnvVar{
				Name:  "YAKS_S3_REPOSITORY_URL",
				Value: url,
			},
			v1.EnvVar{
				Name:  "YAKS_S3_REPOSITORY_BUCKET",
				Value: bucket,
			},
			v1.EnvVar{
				Name: "YAKS_S3_REPOSITORY_ACCESS_KEY",
				ValueFrom: &v1.EnvVarSource{
					SecretKeyRef: &v1.SecretKeySelector{
						LocalObjectReference: v1.LocalObjectReference{
							Name: creds.SecretName,
						},
						Key: creds.AccessKeyEntry,
					},
				},
			},
			v1.EnvVar{
				Name: "YAKS_S3_REPOSITORY_SECRET_KEY",
				ValueFrom: &v1.EnvVarSource{
					SecretKeyRef: &v1.SecretKeySelector{
						LocalObjectReference: v1.LocalObjectReference{
							Name: creds.SecretName,
						},
						Key: creds.SecretKeyEntry,
					},
				},
			})
	}
	return nil
}
