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
	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/util/envvar"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"
	"github.com/citrusframework/yaks/pkg/util/openshift"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/api/rbac/v1beta1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"strings"
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
	customizer := func(o runtime.Object) runtime.Object {
		if cfg.CustomImage != "" {
			if d, ok := o.(*appsv1.Deployment); ok {
				if d.Labels["org.citrusframework.yaks/component"] == "operator" {
					d.Spec.Template.Spec.Containers[0].Image = cfg.CustomImage
				}
			}
		}

		if cfg.CustomImagePullPolicy != "" {
			if d, ok := o.(*appsv1.Deployment); ok {
				if d.Labels["org.citrusframework.yaks/component"] == "operator" {
					d.Spec.Template.Spec.Containers[0].ImagePullPolicy = corev1.PullPolicy(cfg.CustomImagePullPolicy)
				}
			}
		}

		if cfg.Global {
			if d, ok := o.(*appsv1.Deployment); ok {
				if d.Labels["org.citrusframework.yaks/component"] == "operator" {
					// Make the operator watch all namespaces
					envvar.SetVal(&d.Spec.Template.Spec.Containers[0].Env, "WATCH_NAMESPACE", "")
				}
			}

			// Turn Role & RoleBinding into their equivalent cluster types
			if r, ok := o.(*v1beta1.Role); ok {
				if strings.HasPrefix(r.Name, "yaks-operator") {
					o = &v1beta1.ClusterRole{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: cfg.Namespace,
							Name:      r.Name,
						},
						Rules: r.Rules,
					}
				}
			}

			if rb, ok := o.(*v1beta1.RoleBinding); ok {
				if strings.HasPrefix(rb.Name, "yaks-operator") {
					rb.Subjects[0].Namespace = cfg.Namespace

					o = &v1beta1.ClusterRoleBinding{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: cfg.Namespace,
							Name:      rb.Name,
						},
						Subjects: rb.Subjects,
						RoleRef: v1beta1.RoleRef{
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

	if errmtr := installServiceMonitors(ctx, c, cfg.Namespace, customizer, collection, force); errmtr != nil {
		if k8serrors.IsAlreadyExists(errmtr) {
			return errmtr
		}
		fmt.Println("Warning: the operator will not be able to create servicemonitors for metrics. Try installing as cluster-admin to allow the creation of servicemonitors.")
	}

	return nil
}

func installOpenShift(ctx context.Context, c client.Client, namespace string, customizer ResourceCustomizer, collection *kubernetes.Collection, force bool) error {
	return ResourcesOrCollect(ctx, c, namespace, collection, force, customizer,
		"operator-service-account.yaml",
		"operator-role-openshift.yaml",
		"operator-role-binding.yaml",
		"operator-deployment.yaml",
	)
}

func installKubernetes(ctx context.Context, c client.Client, namespace string, customizer ResourceCustomizer, collection *kubernetes.Collection, force bool) error {
	return ResourcesOrCollect(ctx, c, namespace, collection, force, customizer,
		"operator-service-account.yaml",
		"operator-role-kubernetes.yaml",
		"operator-role-binding.yaml",
		"operator-deployment.yaml",
	)
}

func installServiceMonitors(ctx context.Context, c client.Client, namespace string, customizer ResourceCustomizer, collection *kubernetes.Collection, force bool) error {
	return ResourcesOrCollect(ctx, c, namespace, collection, force, customizer,
		"operator-role-servicemonitors.yaml",
		"operator-role-binding-servicemonitors.yaml",
	)
}
