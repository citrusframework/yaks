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

package envvar

import (
	"os"

	corev1 "k8s.io/api/core/v1"
)

const (
	NamespaceEnvVar = "NAMESPACE"
)

// Get --.
func Get(vars []corev1.EnvVar, name string) *corev1.EnvVar {
	for i := 0; i < len(vars); i++ {
		if vars[i].Name == name {
			return &vars[i]
		}
	}

	return nil
}

// SetVal --.
func SetVal(vars *[]corev1.EnvVar, name string, value string) {
	if envVar := Get(*vars, name); envVar != nil {
		envVar.Value = value
		envVar.ValueFrom = nil
	} else {
		*vars = append(*vars, corev1.EnvVar{
			Name:  name,
			Value: value,
		})
	}
}

// SetValFrom --.
func SetValFrom(vars *[]corev1.EnvVar, name string, path string) {
	if envVar := Get(*vars, name); envVar != nil {
		envVar.Value = ""
		envVar.ValueFrom = &corev1.EnvVarSource{
			FieldRef: &corev1.ObjectFieldSelector{
				FieldPath: path,
			},
		}
	} else {
		*vars = append(*vars, corev1.EnvVar{
			Name: name,
			ValueFrom: &corev1.EnvVarSource{
				FieldRef: &corev1.ObjectFieldSelector{
					FieldPath: path,
				},
			},
		})
	}
}

// GetOperatorNamespace returns the Namespace the operator is installed.
func GetOperatorNamespace() string {
	if ns, found := os.LookupEnv(NamespaceEnvVar); found {
		return ns
	}

	return ""
}
