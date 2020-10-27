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
	"encoding/json"
	"fmt"
	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	v1 "k8s.io/api/core/v1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"path"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// NewEvaluateAction creates a new evaluate action
func NewEvaluateAction() Action {
	return &evaluateAction{}
}

type evaluateAction struct {
	baseAction
}

// Name returns a common name of the action
func (action *evaluateAction) Name() string {
	return "evaluate"
}

// CanHandle tells whether this action can handle the test
func (action *evaluateAction) CanHandle(build *v1alpha1.Test) bool {
	return build.Status.Phase == v1alpha1.TestPhaseRunning
}

// Handle handles the test
func (action *evaluateAction) Handle(ctx context.Context, test *v1alpha1.Test) (*v1alpha1.Test, error) {
	status, err := action.getTestPodStatus(ctx, test)
	if err != nil && k8serrors.IsNotFound(err) {
		test.Status.Phase = v1alpha1.TestPhaseError
	} else if err != nil {
		return nil, err
	}

	if status.Phase == v1.PodSucceeded {
		test.Status.Phase = v1alpha1.TestPhasePassed
		err = action.addTestResults(status, test)
	} else if status.Phase == v1.PodFailed {
		test.Status.Phase = v1alpha1.TestPhaseFailed
		err = action.addTestResults(status, test)
	}

	if err != nil {
		return nil, err
	}

	return test, nil
}

func (action *evaluateAction) addTestResults(status v1.PodStatus, test *v1alpha1.Test) error {
	var reportJson = []byte(status.ContainerStatuses[0].State.Terminated.Message)
	if err := json.Unmarshal(reportJson, &test.Status.Results); err != nil {
		return err
	}

	errors := make([]string, 0)
	for _, result := range test.Status.Results.Tests {
		if result.ErrorType != "" {
			_, className := path.Split(result.ClassName)
			errors = append(errors, fmt.Sprintf("'%s' (%s) failed with '%s'",
				result.Name, className, result.ErrorMessage))
		}
	}

	if len(errors) > 0 {
		bytes, err := json.Marshal(errors)
		if err != nil {
			return err
		}
		test.Status.Errors = string(bytes);
	}

	return nil
}

func (action *evaluateAction) getTestPodStatus(ctx context.Context, test *v1alpha1.Test) (v1.PodStatus, error) {
	pod := v1.Pod{
		TypeMeta: metav1.TypeMeta{
			Kind:       "Pod",
			APIVersion: v1.SchemeGroupVersion.String(),
		},
		ObjectMeta: metav1.ObjectMeta{
			Namespace: test.Namespace,
			Name:      TestPodNameFor(test),
		},
	}
	key := client.ObjectKey{
		Namespace: test.Namespace,
		Name:      TestPodNameFor(test),
	}
	if err := action.client.Get(ctx, key, &pod); err != nil {
		return v1.PodStatus{}, err
	}
	return pod.Status, nil
}
