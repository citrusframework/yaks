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
	"fmt"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// EDIT THIS FILE!  THIS IS SCAFFOLDING FOR YOU TO OWN!
// NOTE: json tags are required.  Any new fields you add must have json tags for the fields to be serialized.

// ClusterType is the kind of orchestration cluster the framework is installed into
type ClusterType string

const (
	// ClusterTypeOpenShift is used when targeting a OpenShift cluster
	ClusterTypeOpenShift ClusterType = "OpenShift"
	// ClusterTypeKubernetes is used when targeting a Kubernetes cluster
	ClusterTypeKubernetes ClusterType = "Kubernetes"
)

// TestSpec defines the desired state of Test
// +k8s:openapi-gen=true
type TestSpec struct {
	// INSERT ADDITIONAL SPEC FIELDS - desired state of cluster
	// Important: Run "operator-sdk generate k8s" to regenerate code after modifying this file
	// Add custom validation using kubebuilder tags: https://book-v1.book.kubebuilder.io/beyond_basics/generating_crd.html

	Source   SourceSpec   `json:"source,omitempty"`
	Settings SettingsSpec `json:"config,omitempty"`
	Env      []string     `json:"env,omitempty"`
	Secret   string       `json:"secret,omitempty"`
}

// SourceSpec--
type SourceSpec struct {
	Name     string   `json:"name,omitempty"`
	Content  string   `json:"content,omitempty"`
	Language Language `json:"language,omitempty"`
}

// SettingsSpec--
type SettingsSpec struct {
	Name    string `json:"name,omitempty"`
	Content string `json:"content,omitempty"`
}

// TestStatus defines the observed state of Test
// +k8s:openapi-gen=true
type TestStatus struct {
	// INSERT ADDITIONAL STATUS FIELD - define observed state of cluster
	// Important: Run "operator-sdk generate k8s" to regenerate code after modifying this file
	// Add custom validation using kubebuilder tags: https://book-v1.book.kubebuilder.io/beyond_basics/generating_crd.html

	Phase   TestPhase   `json:"phase,omitempty"`
	Results TestResults `json:"results,omitempty"`
	Errors	string      `json:"errors,omitempty"`
	TestID  string      `json:"testID,omitempty"`
	Digest  string      `json:"digest,omitempty"`
	Version string      `json:"version,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +k8s:openapi-gen=true
// +genclient
// +kubebuilder:resource:path=tests,scope=Namespaced,categories=yaks;testing
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type=string,JSONPath=`.status.phase`,description="The test phase"
// +kubebuilder:printcolumn:name="Total",type=string,JSONPath=`.status.results.summary.total`,description="The total amount of tests"
// +kubebuilder:printcolumn:name="Passed",type=string,JSONPath=`.status.results.summary.passed`,description="Passed tests"
// +kubebuilder:printcolumn:name="Failed",type=string,JSONPath=`.status.results.summary.failed`,description="Failed tests"
// +kubebuilder:printcolumn:name="Skipped",type=string,JSONPath=`.status.results.summary.skipped`,description="Skipped tests"
// +kubebuilder:printcolumn:name="Errors",type=string,JSONPath=`.status.errors`,description="Test error details"

// Test is the Schema for the tests API
type Test struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   TestSpec   `json:"spec,omitempty"`
	Status TestStatus `json:"status,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// TestList contains a list of Test
type TestList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items    []Test `json:"items"`
}

type TestResults struct {
	Summary TestSummary  `json:"summary,omitempty"`
	Tests	[]TestResult `json:"tests,omitempty"`
	Errors 	[]string 	 `json:"errors,omitempty"`
}

type TestSummary struct {
	Total  		int   	  `json:"total"`
	Passed 		int   	  `json:"passed"`
	Failed 		int   	  `json:"failed"`
	Skipped 	int   	  `json:"skipped"`
	Pending 	int   	  `json:"pending"`
	Undefined 	int   	  `json:"undefined"`
}

type TestResult struct {
	Name         string  `json:"name,omitempty"`
	ClassName    string  `json:"classname,omitempty"`
	ErrorType    string  `json:"errorType,omitempty"`
	ErrorMessage string  `json:"errorMessage,omitempty"`
}

// TestPhase
type TestPhase string

const (
	// TestKind
	TestKind string = "Test"

	// TestPhaseNone
	TestPhaseNone TestPhase = ""
	// TestPhasePending
	TestPhasePending TestPhase = "Pending"
	// TestPhaseRunning
	TestPhaseRunning TestPhase = "Running"
	// TestPhasePassed
	TestPhasePassed TestPhase = "Passed"
	// TestPhaseFailed
	TestPhaseFailed TestPhase = "Failed"
	// TestPhaseError
	TestPhaseError TestPhase = "Error"
	// TestPhaseDeleting
	TestPhaseDeleting TestPhase = "Deleting"
)

func (phase TestPhase) AsError() error {
	if phase != TestPhasePassed {
		return fmt.Errorf("Test %s", string(phase))
	}
	return nil
}

type Language string

const (
	// LanguageGherkin
	LanguageGherkin Language = "feature"
)

// TestLanguages is the list of all supported test languages
var TestLanguages = []Language{
	LanguageGherkin,
}
