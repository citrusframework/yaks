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

package cmd

import (
	"context"
	"fmt"
	"io/ioutil"
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
					var files []os.FileInfo
					if files, err = ioutil.ReadDir(role); err != nil {
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
					var files []os.FileInfo
					if files, err = ioutil.ReadDir(role); err != nil {
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
	if role == config.RoleKnative {
		if err := install.InstallKnative(ctx, c, namespace, customizer, nil, true); err != nil {
			return err
		}
	} else if role == config.RoleCamelK {
		if err := install.InstallCamelK(ctx, c, namespace, customizer, nil, true); err != nil {
			return err
		}
	} else if strings.HasSuffix(role, ".yaml") {
		data, err := loadData(role)
		if err != nil {
			return err
		}

		obj, err := kubernetes.LoadResourceFromYaml(c.GetScheme(), data)
		if err != nil {
			return err
		}

		if r, ok := obj.(*v1.Role); ok {
			if !strings.HasPrefix(r.Name, config.OperatorServiceAccount) {
				return errors.New(fmt.Sprintf("invalid Role - please use '%s' as name prefix", config.OperatorServiceAccount))
			}
		} else if rb, ok := obj.(*v1.RoleBinding); ok {
			if !strings.HasPrefix(rb.Name, config.OperatorServiceAccount) {
				return errors.New(fmt.Sprintf("invalid RoleBinding - please use '%s' as name prefix", config.OperatorServiceAccount))
			}
		} else {
			return errors.New("unsupported resource type - expected Role or RoleBinding")
		}

		if err := install.RuntimeObjectOrCollect(ctx, c, namespace, nil, true, customizer(obj)); err != nil {
			return err
		}
		fmt.Printf("Added role permission '%s' from file %s to YAKS operator in namespace '%s'\n", obj.GetName(), path.Base(role), namespace)
	} else {
		return errors.New(fmt.Sprintf("unsupported role definition - please use one of '%s', '%s' or 'role.yaml'", config.RoleCamelK, config.RoleKnative))
	}

	return nil
}

func applyViewerRole(ctx context.Context, c client.Client, role string, namespace string) error {
	if role == config.RoleKnative {
		if err := install.InstallViewerServiceAccountRolesKnative(ctx, c, namespace); err != nil {
			return err
		}
	} else if role == config.RoleCamelK {
		if err := install.InstallViewerServiceAccountRolesCamelK(ctx, c, namespace); err != nil {
			return err
		}
	} else if strings.HasSuffix(role, ".yaml") {
		data, err := loadData(role)
		if err != nil {
			return err
		}

		obj, err := kubernetes.LoadResourceFromYaml(c.GetScheme(), data)
		if err != nil {
			return err
		}

		if r, ok := obj.(*v1.Role); ok {
			if !strings.HasPrefix(r.Name, config.ViewerServiceAccount) {
				return errors.New(fmt.Sprintf("invalid Role - please use '%s' as name prefix", config.ViewerServiceAccount))
			}
		} else if rb, ok := obj.(*v1.RoleBinding); ok {
			if !strings.HasPrefix(rb.Name, config.ViewerServiceAccount) {
				return errors.New(fmt.Sprintf("invalid RoleBinding - please use '%s' as name prefix", config.ViewerServiceAccount))
			}
		} else {
			return errors.New("unsupported resource type - expected Role or RoleBinding")
		}

		if err := install.RuntimeObjectOrCollect(ctx, c, namespace, nil, true, obj); err != nil {
			return err
		}
		fmt.Printf("Added role permission '%s' from file %s to YAKS viewer service account in namespace '%s'\n", obj.GetName(), path.Base(role), namespace)
	} else {
		return errors.New(fmt.Sprintf("unsupported role definition - please use one of '%s', '%s' or 'role.yaml'", config.RoleCamelK, config.RoleKnative))
	}

	return nil
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
