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
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// EDIT THIS FILE!  THIS IS SCAFFOLDING FOR YOU TO OWN!
// NOTE: json tags are required.  Any new fields you add must have json tags for the fields to be serialized.

// InstanceSpec provides the state of a yaks instance
// +k8s:openapi-gen=true
type InstanceSpec struct {
	// INSERT ADDITIONAL SPEC FIELDS - desired state of cluster
	// Important: Run "operator-sdk generate k8s" to regenerate code after modifying this file
	// Add custom validation using kubebuilder tags: https://book-v1.book.kubebuilder.io/beyond_basics/generating_crd.html

	Operator  OperatorSpec   `json:"operator,omitempty"`
}

// OperatorSpec--
type OperatorSpec struct {
	Global    bool 	 `json:"global"`
	Pod       string `json:"pod,omitempty"`
	Namespace string `json:"namespace,omitempty"`
}

// InstanceStatus defines the observed state of a yaks instance
// +k8s:openapi-gen=true
type InstanceStatus struct {
	// INSERT ADDITIONAL STATUS FIELD - define observed state of cluster
	// Important: Run "operator-sdk generate k8s" to regenerate code after modifying this file
	// Add custom validation using kubebuilder tags: https://book-v1.book.kubebuilder.io/beyond_basics/generating_crd.html

	Version string `json:"version,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +k8s:openapi-gen=true
// +genclient
// +kubebuilder:resource:path=instances,scope=Namespaced,categories=yaks;testing
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Global",type=boolean,JSONPath=`.spec.operator.global`,description="True if YAKS instance is global"
// +kubebuilder:printcolumn:name="Pod",type=string,JSONPath=`.spec.operator.pod`,description="The YAKS operator pod name"
// +kubebuilder:printcolumn:name="Version",type=string,JSONPath=`.status.version`,description="The YAKS version"

// Instance is the Schema for the yaks instance
type Instance struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   InstanceSpec   `json:"spec,omitempty"`
	Status InstanceStatus `json:"status,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// InstanceList contains a list of yaks instances
type InstanceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items    []Instance `json:"items"`
}