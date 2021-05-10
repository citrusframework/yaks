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

package install

import (
	"context"
	"fmt"
	"strings"

	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/config"
	"github.com/citrusframework/yaks/pkg/util/camelk"
	"github.com/citrusframework/yaks/pkg/util/envvar"
	"github.com/citrusframework/yaks/pkg/util/knative"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"
	"github.com/citrusframework/yaks/pkg/util/openshift"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	v1 "k8s.io/api/rbac/v1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	ctrl "sigs.k8s.io/controller-runtime/pkg/client"
)

// OperatorConfiguration --
type OperatorConfiguration struct {
	CustomImage           string
	CustomImagePullPolicy string
	Namespace             string
	Global                bool
	ClusterType           string
}

// Operator installs the operator resources in the given namespace
func Operator(ctx context.Context, c client.Client, cfg OperatorConfiguration, force bool) error {
	return OperatorOrCollect(ctx, c, cfg, nil, force)
}

// OperatorOrCollect installs the operator resources or adds them to the collector if present
func OperatorOrCollect(ctx context.Context, c client.Client, cfg OperatorConfiguration, collection *kubernetes.Collection, force bool) error {
	customizer := customizer(cfg)

	isOpenShift, err := openshift.IsOpenShiftClusterType(c, cfg.ClusterType)
	if err != nil {
		return err
	}
	if isOpenShift {
		if err := installOpenShift(ctx, c, cfg.Namespace, customizer, collection, force); err != nil {
			return err
		}
	} else {
		if err := installKubernetes(ctx, c, cfg.Namespace, customizer, collection, force); err != nil {
			return err
		}
	}

	// Make sure that instance CR installed in operator namespace can be used by others
	if err := InstallInstanceViewerRole(ctx, c, cfg.Namespace, customizer, collection, force); err != nil {
		return err
	}

	// Additionally, install Knative resources (roles and bindings)
	isKnative, err := knative.IsInstalled(ctx, c)
	if err != nil {
		return err
	}
	if isKnative {
		if err := InstallKnative(ctx, c, cfg.Namespace, customizer, collection, force); err != nil {
			return err
		}
	}

	// Additionally, install Camel K resources (roles and bindings)
	isCamelK, err := camelk.IsInstalled(ctx, c)
	if err != nil {
		return err
	}
	if isCamelK {
		if err := InstallCamelK(ctx, c, cfg.Namespace, customizer, collection, force); err != nil {
			return err
		}
	}

	if errmtr := InstallServiceMonitors(ctx, c, cfg.Namespace, customizer, collection, force); errmtr != nil {
		if k8serrors.IsAlreadyExists(errmtr) {
			return errmtr
		}
		fmt.Println("Warning: the operator will not be able to create servicemonitors for metrics. Try installing as cluster-admin to allow the creation of servicemonitors.")
	}

	return nil
}

func customizer(cfg OperatorConfiguration) ResourceCustomizer {
	return func(o ctrl.Object) ctrl.Object {
		if cfg.CustomImage != "" {
			if d, ok := o.(*appsv1.Deployment); ok {
				if d.Labels["yaks.citrusframework.org/component"] == "operator" {
					d.Spec.Template.Spec.Containers[0].Image = cfg.CustomImage
				}
			}
		}

		if cfg.CustomImagePullPolicy != "" {
			if d, ok := o.(*appsv1.Deployment); ok {
				if d.Labels["yaks.citrusframework.org/component"] == "operator" {
					d.Spec.Template.Spec.Containers[0].ImagePullPolicy = corev1.PullPolicy(cfg.CustomImagePullPolicy)
				}
			}
		}

		if cfg.Global {
			if d, ok := o.(*appsv1.Deployment); ok {
				if d.Labels["yaks.citrusframework.org/component"] == "operator" {
					// Make the operator watch all namespaces
					envvar.SetVal(&d.Spec.Template.Spec.Containers[0].Env, "WATCH_NAMESPACE", "")
				}
			}

			// Turn Role & RoleBinding into their equivalent cluster types
			if r, ok := o.(*v1.Role); ok {
				if strings.HasPrefix(r.Name, config.OperatorServiceAccount) {
					o = &v1.ClusterRole{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: cfg.Namespace,
							Name:      r.Name,
							Labels:    r.Labels,
						},
						Rules: r.Rules,
					}
				}
			}

			if rb, ok := o.(*v1.RoleBinding); ok {
				if strings.HasPrefix(rb.Name, config.OperatorServiceAccount) {
					rb.Subjects[0].Namespace = cfg.Namespace

					o = &v1.ClusterRoleBinding{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: cfg.Namespace,
							Name:      rb.Name,
							Labels:    rb.Labels,
						},
						Subjects: rb.Subjects,
						RoleRef: v1.RoleRef{
							APIGroup: rb.RoleRef.APIGroup,
							Kind:     "ClusterRole",
							Name:     rb.RoleRef.Name,
						},
					}
				}
			}
		}
		return o
	}
}

func installOpenShift(ctx context.Context, c client.Client, namespace string, customizer ResourceCustomizer, collection *kubernetes.Collection, force bool) error {
	return ResourcesOrCollect(ctx, c, namespace, collection, force, customizer,
		"/manager/operator-service-account.yaml",
		"/rbac-openshift/operator-role-openshift.yaml",
		"/rbac-openshift/operator-role-binding-openshift.yaml",
		"/manager/operator-deployment.yaml",
	)
}

func installKubernetes(ctx context.Context, c client.Client, namespace string, customizer ResourceCustomizer, collection *kubernetes.Collection, force bool) error {
	return ResourcesOrCollect(ctx, c, namespace, collection, force, customizer,
		"/manager/operator-service-account.yaml",
		"/rbac-kubernetes/operator-role-kubernetes.yaml",
		"/rbac-kubernetes/operator-role-binding-kubernetes.yaml",
		"/manager/operator-deployment.yaml",
	)
}

func InstallKnative(ctx context.Context, c client.Client, namespace string, customizer ResourceCustomizer, collection *kubernetes.Collection, force bool) error {
	if err := ResourcesOrCollect(ctx, c, namespace, collection, force, customizer,
		"/rbac-kubernetes/operator-role-knative.yaml",
		"/rbac-kubernetes/operator-role-binding-knative.yaml",
	); err != nil {
		return err
	}

	fmt.Printf("Added Knative addon to YAKS operator in namespace '%s'\n", namespace)
	return nil
}

func InstallCamelK(ctx context.Context, c client.Client, namespace string, customizer ResourceCustomizer, collection *kubernetes.Collection, force bool) error {
	if err := ResourcesOrCollect(ctx, c, namespace, collection, force, customizer,
		"/rbac-kubernetes/operator-role-camel-k.yaml",
		"/rbac-kubernetes/operator-role-binding-camel-k.yaml",
	); err != nil {
		return err
	}

	fmt.Printf("Added CamelK addon to YAKS operator in namespace '%s'\n", namespace)
	return nil
}

func InstallServiceMonitors(ctx context.Context, c client.Client, namespace string, customizer ResourceCustomizer, collection *kubernetes.Collection, force bool) error {
	return ResourcesOrCollect(ctx, c, namespace, collection, force, customizer,
		"/rbac-kubernetes/operator-role-servicemonitors.yaml",
		"/rbac-kubernetes/operator-role-binding-servicemonitors.yaml",
	)
}

// installs the role that allows any user ro access to instances in all namespaces
func InstallInstanceViewerRole(ctx context.Context, c client.Client, namespace string, customizer ResourceCustomizer, collection *kubernetes.Collection, force bool) error {
	return ResourcesOrCollect(ctx, c, namespace, collection, force, customizer,
		"/rbac-kubernetes/user-global-instance-viewer-role.yaml",
		"/rbac-kubernetes/user-global-instance-viewer-role-binding.yaml",
	)
}
