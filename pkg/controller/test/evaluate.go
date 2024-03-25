/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test

import (
	"context"
	"encoding/json"
	"fmt"
	"path"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	batchv1 "k8s.io/api/batch/v1"
	v1 "k8s.io/api/core/v1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// NewEvaluateAction creates a new evaluate action.
func NewEvaluateAction() Action {
	return &evaluateAction{}
}

type evaluateAction struct {
	baseAction
}

// Name returns a common name of the action.
func (action *evaluateAction) Name() string {
	return "evaluate"
}

// CanHandle tells whether this action can handle the test.
func (action *evaluateAction) CanHandle(build *v1alpha1.Test) bool {
	return build.Status.Phase == v1alpha1.TestPhaseRunning
}

// Handle handles the test.
func (action *evaluateAction) Handle(ctx context.Context, test *v1alpha1.Test) (*v1alpha1.Test, error) {
	jobStatus, err := action.getTestJobStatus(ctx, test)
	if err != nil && k8serrors.IsNotFound(err) {
		test.Status.Phase = v1alpha1.TestPhaseError
		test.Status.Errors = "Missing job status for test " + test.Name
		return test, nil
	} else if err != nil {
		return nil, err
	}

	if jobStatus.Active > 0 || (jobStatus.Failed == 0 && jobStatus.Succeeded == 0) {
		return test, nil
	}

	if jobStatus.Succeeded > 0 {
		test.Status.Phase = v1alpha1.TestPhasePassed
	}

	if jobStatus.Failed > 0 {
		test.Status.Phase = v1alpha1.TestPhaseFailed
	}

	status, err := action.getTestPodStatus(ctx, test)
	if err != nil && k8serrors.IsNotFound(err) {
		test.Status.Phase = v1alpha1.TestPhaseError
		test.Status.Errors = err.Error()
		return test, nil
	} else if err != nil {
		return nil, err
	}
	err = action.addTestResults(status, test)

	if err != nil {
		return nil, err
	}

	return test, nil
}

func (action *evaluateAction) addTestResults(status v1.PodStatus, test *v1alpha1.Test) error {
	containerStatus := getTestContainerStatus(status.ContainerStatuses)

	if containerStatus.State.Terminated != nil && len(containerStatus.State.Terminated.Message) > 0 {
		reportJSON := []byte(containerStatus.State.Terminated.Message)

		if err := json.Unmarshal(reportJSON, &test.Status.Results); err != nil {
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
			test.Status.Errors = string(bytes)
		}
	}

	return nil
}

func getTestContainerStatus(statusList []v1.ContainerStatus) v1.ContainerStatus {
	for _, status := range statusList {
		if status.Name == "test" {
			return status
		}
	}

	return v1.ContainerStatus{}
}

func (action *evaluateAction) getTestPodStatus(ctx context.Context, test *v1alpha1.Test) (v1.PodStatus, error) {
	pods, err := action.client.CoreV1().Pods(test.Namespace).List(ctx, metav1.ListOptions{
		LabelSelector: v1alpha1.TestIDLabel + "=" + test.Status.TestID,
	})
	if err != nil {
		return v1.PodStatus{}, err
	}

	if len(pods.Items) == 0 {
		return v1.PodStatus{}, fmt.Errorf("missing test pod for test %s", test.Name)
	}

	return pods.Items[0].Status, nil
}

func (action *evaluateAction) getTestJobStatus(ctx context.Context, test *v1alpha1.Test) (batchv1.JobStatus, error) {
	job := batchv1.Job{
		TypeMeta: metav1.TypeMeta{
			Kind:       "Job",
			APIVersion: v1.SchemeGroupVersion.String(),
		},
		ObjectMeta: metav1.ObjectMeta{
			Namespace: test.Namespace,
			Name:      JobNameFor(test),
		},
	}
	key := client.ObjectKey{
		Namespace: test.Namespace,
		Name:      JobNameFor(test),
	}
	if err := action.client.Get(ctx, key, &job); err != nil {
		return batchv1.JobStatus{}, err
	}
	return job.Status, nil
}
