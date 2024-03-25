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

package cmd

import (
	"context"
	"fmt"
	"os"
	"path"
	"strings"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/config"
	"github.com/citrusframework/yaks/pkg/install"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"
	"github.com/pkg/errors"
	"github.com/spf13/cobra"

	v1 "k8s.io/api/rbac/v1"
	"k8s.io/apimachinery/pkg/api/meta"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	ctrl "sigs.k8s.io/controller-runtime/pkg/client"
)

func newCmdRole(rootCmdOptions *RootCmdOptions) (*cobra.Command, *roleCmdOptions) {
	options := roleCmdOptions{
		RootCmdOptions: rootCmdOptions,
	}
	cmd := cobra.Command{
		Use:     "role",
		Short:   "Manage YAKS operator roles and role bindings",
		Long:    `Add roles and role bindings to the YAKS operator in order to manage additional custom resources.`,
		PreRunE: decode(&options),
		RunE:    options.run,
	}

	cmd.Flags().StringArrayP("add", "a", nil, "Add given role permission")
	cmd.Flags().Bool("all", true, "Add roles to all YAKS operators in all namespaces")

	return &cmd, &options
}

type roleCmdOptions struct {
	*RootCmdOptions
	Add []string `mapstructure:"add"`
	All bool     `mapstructure:"all"`
}

func (o *roleCmdOptions) run(cmd *cobra.Command, args []string) error {
	c, err := o.GetCmdClient()
	if err != nil {
		return err
	}

	var namespaces = make(map[string]bool)
	namespaces[o.Namespace] = false
	if o.All {
		instances := v1alpha1.InstanceList{}
		if err = c.List(o.Context, &instances); err != nil {
			if meta.IsNoMatchError(err) {
				fmt.Println(cmd.OutOrStdout(), "Unable to locate operator instances in other namespaces")
			} else {
				return err
			}
		}

		for _, instance := range instances.Items {
			namespaces[instance.Spec.Operator.Namespace] = instance.Spec.Operator.Global
		}
	}

	for namespace, global := range namespaces {

		if operatorSA, err := hasServiceAccount(o.Context, c, namespace, config.OperatorServiceAccount); err != nil {
			return err
		} else if operatorSA {
			customizer := o.customizer(namespace, global)

			for _, role := range o.Add {
				if isDir(role) {
					var files []os.DirEntry
					if files, err = os.ReadDir(role); err != nil {
						return err
					}

					for _, f := range files {
						err = applyOperatorRole(o.Context, c, path.Join(role, f.Name()), namespace, customizer)
						if err != nil {
							return err
						}
					}
				} else if err := applyOperatorRole(o.Context, c, role, namespace, customizer); err != nil {
					return err
				}
			}
		}

		if viewerSA, err := hasServiceAccount(o.Context, c, namespace, config.ViewerServiceAccount); err != nil {
			return err
		} else if viewerSA {
			for _, role := range o.Add {
				if isDir(role) {
					var files []os.DirEntry
					if files, err = os.ReadDir(role); err != nil {
						return err
					}

					for _, f := range files {
						err = applyViewerRole(o.Context, c, path.Join(role, f.Name()), namespace)
						if err != nil {
							return err
						}
					}
				} else if err := applyViewerRole(o.Context, c, role, namespace); err != nil {
					return err
				}
			}
		}
	}

	return nil
}

func applyOperatorRole(ctx context.Context, c client.Client, role string, namespace string, customizer install.ResourceCustomizer) error {
	if strings.HasSuffix(role, ".yaml") {
		data, err := loadData(ctx, role)
		if err != nil {
			return err
		}

		obj, err := kubernetes.LoadResourceFromYaml(c.GetScheme(), data)
		if err != nil {
			return err
		}

		if r, ok := obj.(*v1.Role); ok {
			verifyRole(r, config.OperatorServiceAccount)
		} else if rb, ok := obj.(*v1.RoleBinding); ok {
			verifyRoleBinding(rb, config.OperatorServiceAccount)
		} else {
			return errors.New("unsupported resource type - expected Role or RoleBinding")
		}

		fmt.Printf("Adding role permission '%s' from file %s to YAKS operator in namespace '%s'\n", obj.GetName(), path.Base(role), namespace)
		return install.RuntimeObjectOrCollect(ctx, c, namespace, nil, true, customizer(obj))
	}

	switch role {
	case config.RoleKnative:
		return install.KnativeResources(ctx, c, namespace, customizer, nil, true)
	case config.RoleCamelK:
		return install.CamelKResources(ctx, c, namespace, customizer, nil, true)
	case config.RoleStrimzi:
		return install.StrimziResources(ctx, c, namespace, customizer, nil, true)
	default:
		return fmt.Errorf("unsupported role definition - please use one of '%s', '%s', '%s' or 'role.yaml'", config.RoleCamelK, config.RoleKnative, config.RoleStrimzi)
	}
}

func applyViewerRole(ctx context.Context, c client.Client, role string, namespace string) error {
	if strings.HasSuffix(role, ".yaml") {
		data, err := loadData(ctx, role)
		if err != nil {
			return err
		}

		obj, err := kubernetes.LoadResourceFromYaml(c.GetScheme(), data)
		if err != nil {
			return err
		}

		if r, ok := obj.(*v1.Role); ok {
			verifyRole(r, config.ViewerServiceAccount)
		} else if rb, ok := obj.(*v1.RoleBinding); ok {
			verifyRoleBinding(rb, config.ViewerServiceAccount)
		} else {
			return errors.New("unsupported resource type - expected Role or RoleBinding")
		}

		fmt.Printf("Adding role permission '%s' from file %s to YAKS viewer service account in namespace '%s'\n", obj.GetName(), path.Base(role), namespace)
		return install.RuntimeObjectOrCollect(ctx, c, namespace, nil, true, obj)
	}

	switch role {
	case config.RoleKnative:
		return install.ViewerServiceAccountRolesKnative(ctx, c, namespace)
	case config.RoleCamelK:
		return install.ViewerServiceAccountRolesCamelK(ctx, c, namespace)
	case config.RoleStrimzi:
		return install.ViewerServiceAccountRolesStrimzi(ctx, c, namespace)
	default:
		return fmt.Errorf("unsupported role definition - please use one of '%s', '%s', '%s' or 'role.yaml'", config.RoleCamelK, config.RoleKnative, config.RoleStrimzi)
	}
}

func verifyRole(r *v1.Role, serviceAccount string) {
	if !strings.HasPrefix(r.Name, serviceAccount) {
		r.Name = fmt.Sprintf("%s-%s", serviceAccount, r.Name)
	}
}

func verifyRoleBinding(rb *v1.RoleBinding, serviceAccount string) {
	if !strings.HasPrefix(rb.Name, serviceAccount) {
		rb.Name = fmt.Sprintf("%s-%s", serviceAccount, rb.Name)
	}

	if !strings.HasPrefix(rb.RoleRef.Name, serviceAccount) {
		rb.RoleRef.Name = fmt.Sprintf("%s-%s", serviceAccount, rb.RoleRef.Name)
	}

	found := false
	for _, subject := range rb.Subjects {
		if subject.Name == serviceAccount {
			found = true
			break
		}
	}
	if !found {
		rb.Subjects = append(rb.Subjects, v1.Subject{
			Kind: "ServiceAccount",
			Name: serviceAccount,
		})
	}
}

func (o *roleCmdOptions) customizer(namespace string, global bool) install.ResourceCustomizer {
	return func(o ctrl.Object) ctrl.Object {
		if global {
			// Turn Role & RoleBinding into their equivalent cluster types
			if r, ok := o.(*v1.Role); ok {
				if strings.HasPrefix(r.Name, config.OperatorServiceAccount) {
					o = &v1.ClusterRole{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: namespace,
							Name:      r.Name,
							Labels:    r.Labels,
						},
						Rules: r.Rules,
					}
				}
			}

			if rb, ok := o.(*v1.RoleBinding); ok {
				if strings.HasPrefix(rb.Name, config.OperatorServiceAccount) {
					rb.Subjects[0].Namespace = namespace

					o = &v1.ClusterRoleBinding{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: namespace,
							Name:      rb.Name,
							Labels:    rb.Labels,
						},
						Subjects: rb.Subjects,
						RoleRef: v1.RoleRef{
							APIGroup: rb.RoleRef.APIGroup,
							Kind:     "ClusterRole",
							Name:     rb.RoleRef.Name,
						},
					}
				}
			}
		}
		return o
	}
}
