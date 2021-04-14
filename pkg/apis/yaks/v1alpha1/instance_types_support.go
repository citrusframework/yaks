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
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	ctrl "sigs.k8s.io/controller-runtime/pkg/client"
)

// GetInstance return the YAKS operator instance in given namespace
func GetInstance(ctx context.Context, client ctrl.Client, namespace string) (*Instance, error) {
	instance := Instance{
		TypeMeta: metav1.TypeMeta{
			Kind:       InstanceKind,
			APIVersion: SchemeGroupVersion.String(),
		},
	}

	key := ctrl.ObjectKey{
		Namespace: namespace,
		Name:      "yaks",
	}

	if err := client.Get(ctx, key, &instance); err != nil {
		return nil, err
	}

	return &instance, nil
}

// IsGlobal returns true if the given instance is configured to watch all namespaces
func IsGlobal(instance *Instance) bool {
	if instance == nil {
		return false
	}

	return instance.Spec.Operator.Global
}
