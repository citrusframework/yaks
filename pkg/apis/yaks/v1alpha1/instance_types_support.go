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

package v1alpha1

import (
	"context"

	"github.com/citrusframework/yaks/pkg/util/defaults"

	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	ctrl "sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/citrusframework/yaks/pkg/util/envvar"
)

// NewInstanceList --
func NewInstanceList() InstanceList {
	return InstanceList{
		TypeMeta: metav1.TypeMeta{
			Kind:       InstanceKind,
			APIVersion: SchemeGroupVersion.String(),
		},
	}
}

// NewInstance --
func NewInstance(namespace string, name string) Instance {
	return Instance{
		TypeMeta: metav1.TypeMeta{
			Kind:       InstanceKind,
			APIVersion: SchemeGroupVersion.String(),
		},
		ObjectMeta: metav1.ObjectMeta{
			Namespace: namespace,
			Name:      name,
		},
	}
}

// GetOrFindInstance return the YAKS operator instance in given namespace or the operator namespace.
func GetOrFindInstance(ctx context.Context, client ctrl.Client, namespace string) (*Instance, error) {
	instance, err := GetInstance(ctx, client, namespace)
	if err != nil && k8serrors.IsNotFound(err) {
		if operatorNamespace, envErr := envvar.GetOperatorNamespace(); envErr == nil {
			if operatorNamespace != "" && operatorNamespace != namespace {
				instance, err = GetInstance(ctx, client, operatorNamespace)
				if err != nil {
					return nil, err
				}
			} else {
				return nil, err
			}
		} else {
			return nil, err
		}
	}

	return instance, err
}

// GetInstance return the YAKS operator instance in given namespace.
func GetInstance(ctx context.Context, client ctrl.Client, namespace string) (*Instance, error) {
	instance := NewInstance(namespace, defaults.InstanceName)
	if err := client.Get(ctx, ctrl.ObjectKeyFromObject(&instance), &instance); err != nil {
		return nil, err
	}

	return &instance, nil
}

// FindGlobalInstance list instances on the cluster and get the first global match.
func FindGlobalInstance(ctx context.Context, c ctrl.Client) (*Instance, error) {
	// looking for operator instances in other namespaces
	instanceList, err := ListInstances(ctx, c)
	if err != nil {
		return nil, err
	}

	for _, instance := range instanceList.Items {
		if instance.Spec.Operator.Global {
			return &instance, nil
		}
	}

	return nil, nil
}

// ListInstances lists all instances on the cluster.
func ListInstances(ctx context.Context, c ctrl.Client) (InstanceList, error) {
	instanceList := NewInstanceList()
	err := c.List(ctx, &instanceList)
	return instanceList, err
}

// IsGlobal returns true if the given instance is configured to watch all namespaces
func IsGlobal(instance *Instance) bool {
	if instance == nil {
		return false
	}

	return instance.Spec.Operator.Global
}
