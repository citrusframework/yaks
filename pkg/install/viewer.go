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
	"github.com/citrusframework/yaks/pkg/util/camelk"
	"github.com/citrusframework/yaks/pkg/util/knative"
	"github.com/citrusframework/yaks/pkg/util/openshift"

	"github.com/citrusframework/yaks/pkg/client"
)

// ViewerServiceAccountRoles installs the viewer service account and related roles in the given namespace
func ViewerServiceAccountRoles(ctx context.Context, c client.Client, namespace string) error {
	isOpenShift, err := openshift.IsOpenShift(c)
	if err != nil {
		return err
	}
	if isOpenShift {
		if err := installViewerServiceAccountRolesOpenShift(ctx, c, namespace); err != nil {
			return err
		}
	} else {
		if err := installViewerServiceAccountRolesKubernetes(ctx, c, namespace); err != nil {
			return err
		}
	}

	// Additionally, install Knative resources (roles and bindings)
	isKnative, err := knative.IsInstalled(ctx, c)
	if err != nil {
		return err
	}
	if isKnative {
		if err := installViewerServiceAccountRolesKnative(ctx, c, namespace); err != nil {
			return err
		}
	}

	// Additionally, install Camel K resources (roles and bindings)
	isCamelK, err := camelk.IsInstalled(ctx, c)
	if err != nil {
		return err
	}
	if isCamelK {
		if err := installViewerServiceAccountRolesCamelK(ctx, c, namespace); err != nil {
			return err
		}
	}

	return nil
}

func installViewerServiceAccountRolesOpenShift(ctx context.Context, c client.Client, namespace string) error {
	return ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"viewer-service-account.yaml",
		"viewer-role-openshift.yaml",
		"viewer-role-binding.yaml",
	)
}

func installViewerServiceAccountRolesKubernetes(ctx context.Context, c client.Client, namespace string) error {
	return ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"viewer-service-account.yaml",
		"viewer-role-kubernetes.yaml",
		"viewer-role-binding.yaml",
	)
}

func installViewerServiceAccountRolesKnative(ctx context.Context, c client.Client, namespace string) error {
	return ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"viewer-role-knative.yaml",
		"viewer-role-binding-knative.yaml",
	)
}

func installViewerServiceAccountRolesCamelK(ctx context.Context, c client.Client, namespace string) error {
	return ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"viewer-role-camel-k.yaml",
		"viewer-role-binding-camel-k.yaml",
	)
}
