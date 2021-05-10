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
	"fmt"

	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/util/camelk"
	"github.com/citrusframework/yaks/pkg/util/knative"
	"github.com/citrusframework/yaks/pkg/util/openshift"
)

// ViewerServiceAccountRoles installs the viewer service account and related roles in the given namespace
func ViewerServiceAccountRoles(ctx context.Context, c client.Client, namespace string) error {
	isOpenShift, err := openshift.IsOpenShift(c)
	if err != nil {
		return err
	}
	if isOpenShift {
		if err := InstallViewerServiceAccountRolesOpenShift(ctx, c, namespace); err != nil {
			return err
		}
	} else {
		if err := InstallViewerServiceAccountRolesKubernetes(ctx, c, namespace); err != nil {
			return err
		}
	}

	// Additionally, install Knative resources (roles and bindings)
	isKnative, err := knative.IsInstalled(ctx, c)
	if err != nil {
		return err
	}
	if isKnative {
		if err := InstallViewerServiceAccountRolesKnative(ctx, c, namespace); err != nil {
			return err
		}
	}

	// Additionally, install Camel K resources (roles and bindings)
	isCamelK, err := camelk.IsInstalled(ctx, c)
	if err != nil {
		return err
	}
	if isCamelK {
		if err := InstallViewerServiceAccountRolesCamelK(ctx, c, namespace); err != nil {
			return err
		}
	}

	return nil
}

func InstallViewerServiceAccountRolesOpenShift(ctx context.Context, c client.Client, namespace string) error {
	return ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"/infrastructure/deploy/viewer-service-account.yaml",
		"/infrastructure/rbac/viewer-role-openshift.yaml",
		"/infrastructure/rbac/viewer-role-binding-openshift.yaml",
	)
}

func InstallViewerServiceAccountRolesKubernetes(ctx context.Context, c client.Client, namespace string) error {
	return ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"/infrastructure/deploy/viewer-service-account.yaml",
		"/infrastructure/rbac/viewer-role-kubernetes.yaml",
		"/infrastructure/rbac/viewer-role-binding-kubernetes.yaml",
	)
}

func InstallViewerServiceAccountRolesKnative(ctx context.Context, c client.Client, namespace string) error {
	if err := ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"/infrastructure/rbac/viewer-role-knative.yaml",
		"/infrastructure/rbac/viewer-role-binding-knative.yaml",
	); err != nil {
		return err
	}

	fmt.Printf("Added Knative addon to YAKS viewer service account in namespace '%s'\n", namespace)

	return nil
}

func InstallViewerServiceAccountRolesCamelK(ctx context.Context, c client.Client, namespace string) error {
	if err := ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"/infrastructure/rbac/viewer-role-camel-k.yaml",
		"/infrastructure/rbac/viewer-role-binding-camel-k.yaml",
	); err != nil {
		return err
	}

	fmt.Printf("Added CamelK addon to YAKS viewer service account in namespace '%s'\n", namespace)

	return nil
}
