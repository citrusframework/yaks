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

package openshift

import (
	"strings"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/client-go/kubernetes"
)

// IsOpenShift returns true if we are connected to a OpenShift cluster or given cluster type marks cluster as OpenShift.
func IsOpenShiftClusterType(c kubernetes.Interface, clusterType string) (bool, error) {
	var res bool
	var err error
	if clusterType != "" {
		res = strings.EqualFold(clusterType, string(v1alpha1.ClusterTypeOpenShift))
	} else {
		res, err = IsOpenShift(c)
		if err != nil {
			return false, err
		}
	}
	return res, nil
}

// IsOpenShift returns true if we are connected to a OpenShift cluster.
func IsOpenShift(client kubernetes.Interface) (bool, error) {
	_, err := client.Discovery().ServerResourcesForGroupVersion("image.openshift.io/v1")
	if err != nil && errors.IsNotFound(err) {
		return false, nil
	} else if err != nil {
		return false, err
	}
	return true, nil
}
