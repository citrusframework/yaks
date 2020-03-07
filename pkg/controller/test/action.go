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
	"github.com/jboss-fuse/yaks/pkg/client"
	"github.com/jboss-fuse/yaks/pkg/util/log"
	"k8s.io/client-go/rest"
)

// Action --
type Action interface {
	client.Injectable
	log.Injectable

	// a user friendly name for the action
	Name() string

	// returns true if the action can handle the test
	CanHandle(build *v1alpha1.Test) bool

	// executes the handling function
	Handle(ctx context.Context, build *v1alpha1.Test) (*v1alpha1.Test, error)
}

type baseAction struct {
	client client.Client
	config *rest.Config
	L      log.Logger
}

func (action *baseAction) InjectClient(client client.Client) {
	action.client = client
}

func (action *baseAction) InjectConfig(config *rest.Config) {
	action.config = config
}

func (action *baseAction) InjectLogger(log log.Logger) {
	action.L = log
}
