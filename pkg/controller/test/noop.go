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
	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
)

// NewNoopAction is used to put an test resource out of the operator lifecycle.
//
// The resource must be updated by an external entity (e.g. the yaks CLI) to a new state when ready
// to start a new workflow.
func NewNoopAction() Action {
	return &noopAction{}
}

type noopAction struct {
	baseAction
}

func (action *noopAction) Name() string {
	return "noop"
}

func (action *noopAction) CanHandle(test *v1alpha1.Test) bool {
	return test.Status.Phase == v1alpha1.TestPhaseUpdating
}

func (action *noopAction) Handle(ctx context.Context, test *v1alpha1.Test) (*v1alpha1.Test, error) {
	// Do nothing and return no object to update
	return nil, nil
}
