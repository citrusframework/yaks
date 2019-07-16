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

	"github.com/jboss-fuse/yaks/pkg/apis/yaks/v1alpha1"
	v1 "k8s.io/api/core/v1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
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
	phase, err := action.getTestPodPhase(ctx, test)
	if err != nil && k8serrors.IsNotFound(err) {
		test.Status.Phase = v1alpha1.TestPhaseError
	} else if err != nil {
		return nil, err
	}

	if phase == v1.PodSucceeded {
		test.Status.Phase = v1alpha1.TestPhasePassed
	} else if phase == v1.PodFailed {
		test.Status.Phase = v1alpha1.TestPhaseFailed
	}

	return test, nil
}

func (action *evaluateAction) getTestPodPhase(ctx context.Context, test *v1alpha1.Test) (v1.PodPhase, error) {
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
		return "", err
	}
	return pod.Status.Phase, nil
}
