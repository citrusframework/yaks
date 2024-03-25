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

package install

import (
	"context"
	"fmt"

	"github.com/citrusframework/yaks/pkg/util/strimzi"

	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/util/camelk"
	"github.com/citrusframework/yaks/pkg/util/knative"
	"github.com/citrusframework/yaks/pkg/util/openshift"
)

// ViewerServiceAccountRoles installs the viewer service account and related roles in the given namespace.
func ViewerServiceAccountRoles(ctx context.Context, c client.Client, namespace string) error {
	isOpenShift, err := openshift.IsOpenShift(c)
	if err != nil {
		return err
	}
	if isOpenShift {
		if err := ViewerServiceAccountRolesOpenShift(ctx, c, namespace); err != nil {
			return err
		}
	} else {
		if err := ViewerServiceAccountRolesKubernetes(ctx, c, namespace); err != nil {
			return err
		}
	}

	// Additionally, install Knative resources (roles and bindings)
	if isKnative, err := knative.IsInstalled(ctx, c); err != nil {
		return err
	} else if isKnative {
		if err := ViewerServiceAccountRolesKnative(ctx, c, namespace); err != nil {
			return err
		}
	}

	// Additionally, install Camel K resources (roles and bindings)
	if isCamelK, err := camelk.IsInstalled(ctx, c); err != nil {
		return err
	} else if isCamelK {
		if err := ViewerServiceAccountRolesCamelK(ctx, c, namespace); err != nil {
			return err
		}
	}

	// Additionally, install Strimzi resources (roles and bindings)
	if isStrimzi, err := strimzi.IsInstalled(ctx, c); err != nil {
		return err
	} else if isStrimzi {
		if err := ViewerServiceAccountRolesStrimzi(ctx, c, namespace); err != nil {
			return err
		}
	}

	return nil
}

func ViewerServiceAccountRolesOpenShift(ctx context.Context, c client.Client, namespace string) error {
	return ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"/infrastructure/deploy/viewer-service-account.yaml",
		"/infrastructure/rbac/openshift/viewer-role-openshift.yaml",
		"/infrastructure/rbac/openshift/viewer-role-binding-openshift.yaml",
	)
}

func ViewerServiceAccountRolesKubernetes(ctx context.Context, c client.Client, namespace string) error {
	return ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"/infrastructure/deploy/viewer-service-account.yaml",
		"/infrastructure/rbac/viewer-role-kubernetes.yaml",
		"/infrastructure/rbac/viewer-role-binding-kubernetes.yaml",
	)
}

func ViewerServiceAccountRolesKnative(ctx context.Context, c client.Client, namespace string) error {
	if err := ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"/infrastructure/rbac/viewer-role-knative.yaml",
		"/infrastructure/rbac/viewer-role-binding-knative.yaml",
	); err != nil {
		return err
	}

	fmt.Printf("Added Knative addon to YAKS viewer service account in namespace '%s'\n", namespace)

	return nil
}

func ViewerServiceAccountRolesCamelK(ctx context.Context, c client.Client, namespace string) error {
	if err := ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"/infrastructure/rbac/viewer-role-camel-k.yaml",
		"/infrastructure/rbac/viewer-role-binding-camel-k.yaml",
	); err != nil {
		return err
	}

	fmt.Printf("Added CamelK addon to YAKS viewer service account in namespace '%s'\n", namespace)

	return nil
}

func ViewerServiceAccountRolesStrimzi(ctx context.Context, c client.Client, namespace string) error {
	if err := ResourcesOrCollect(ctx, c, namespace, nil, true, IdentityResourceCustomizer,
		"/infrastructure/rbac/viewer-role-strimzi.yaml",
		"/infrastructure/rbac/viewer-role-binding-strimzi.yaml",
	); err != nil {
		return err
	}

	fmt.Printf("Added Strimzi addon to YAKS viewer service account in namespace '%s'\n", namespace)

	return nil
}
