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

package install

import (
	"context"
	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"
)

// OperatorConfiguration --
type OperatorConfiguration struct {
	Namespace string
}

// Operator installs the operator resources in the given namespace
func Operator(ctx context.Context, c client.Client, cfg OperatorConfiguration) error {
	return OperatorOrCollect(ctx, c, cfg, nil)
}

// OperatorOrCollect installs the operator resources or adds them to the collector if present
func OperatorOrCollect(ctx context.Context, c client.Client, cfg OperatorConfiguration, collection *kubernetes.Collection) error {
	return ResourcesOrCollect(ctx, c, cfg.Namespace, collection, IdentityResourceCustomizer,
		"service_account.yaml",
		"role.yaml",
		"role_binding.yaml",
		"operator.yaml",
	)
}
