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
	"fmt"
	"os"
	"strings"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
)

const (
	OperatorWatchNamespaceEnv = "WATCH_NAMESPACE"
	OperatorNamespaceEnv      = "OPERATOR_NAMESPACE"
)

// TestPodNameFor returns the name to use for the testing pod
func TestPodNameFor(test *v1alpha1.Test) string {
	return fmt.Sprintf("test-%s-%s", test.Name, test.Status.TestID)
}

// TestResourceNameFor returns the name to use for generic testing resources
func TestResourceNameFor(test *v1alpha1.Test) string {
	return fmt.Sprintf("test-%s", test.Name)
}

// IsCurrentOperatorGlobal returns true if the operator is configured to watch all namespaces
func IsCurrentOperatorGlobal() bool {
	if watchNamespace, envSet := os.LookupEnv(OperatorWatchNamespaceEnv); !envSet || strings.TrimSpace(watchNamespace) == "" {
		return true
	}
	return false
}

// GetOperatorNamespace returns the namespace where the current operator is located (if set)
func GetOperatorNamespace() string {
	if podNamespace, envSet := os.LookupEnv(OperatorNamespaceEnv); envSet {
		return podNamespace
	}
	return ""
}
