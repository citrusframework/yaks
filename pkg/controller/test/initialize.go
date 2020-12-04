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
	"github.com/citrusframework/yaks/pkg/util/defaults"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/util/digest"
	"github.com/rs/xid"
)

// NewInitializeAction creates a new initialize action
func NewInitializeAction() Action {
	return &initializeAction{}
}

type initializeAction struct {
	baseAction
}

// Name returns a common name of the action
func (action *initializeAction) Name() string {
	return "initialize"
}

// CanHandle tells whether this action can handle the test
func (action *initializeAction) CanHandle(build *v1alpha1.Test) bool {
	return build.Status.Phase == v1alpha1.TestPhaseNone ||
		build.Status.Phase == v1alpha1.TestPhaseNew
}

// Handle handles the test
func (action *initializeAction) Handle(ctx context.Context, test *v1alpha1.Test) (*v1alpha1.Test, error) {
	testDigest, err := digest.ComputeForTest(test)
	if err != nil {
		return nil, err
	}

	test.Status.Phase = v1alpha1.TestPhasePending
	test.Status.TestID = xid.New().String()
	test.Status.Digest = testDigest
	test.Status.Version = defaults.Version
	return test, nil
}
