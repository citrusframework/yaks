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
	"io"
	"k8s.io/apimachinery/pkg/api/meta"
	"k8s.io/client-go/kubernetes"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/cmd/config"
	"github.com/citrusframework/yaks/pkg/util/olm"
	"github.com/pkg/errors"
	"github.com/spf13/viper"

	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/util/kubernetes/customclient"
	"github.com/spf13/cobra"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func newCmdUninstall(rootCmdOptions *RootCmdOptions) (*cobra.Command, *uninstallCmdOptions) {
	options := uninstallCmdOptions{
		RootCmdOptions: rootCmdOptions,
	}

	cmd := cobra.Command{
		Use:     "uninstall",
		Short:   "Uninstall YAKS from a Kubernetes cluster",
		Long:    `Uninstalls YAKS from a Kubernetes or OpenShift cluster.`,
		PreRunE: options.decode,
		RunE:    options.uninstall,
	}

	cmd.Flags().Bool("skip-operator", false, "Do not uninstall the YAKS Operator in the current namespace")
	cmd.Flags().Bool("skip-crd", true, "Do not uninstall the YAKS Custom Resource Definitions (CRD)")
	cmd.Flags().Bool("skip-tests", false, "Do not uninstall the YAKS tests in the current namespace")
	cmd.Flags().Bool("skip-role-bindings", false, "Do not uninstall the YAKS Role Bindings in the current namespace")
	cmd.Flags().Bool("skip-roles", false, "Do not uninstall the YAKS Roles in the current namespace")
	cmd.Flags().Bool("skip-cluster-role-bindings", true, "Do not uninstall the YAKS Cluster Role Bindings")
	cmd.Flags().Bool("skip-cluster-roles", true, "Do not uninstall the YAKS Cluster Roles")
	cmd.Flags().Bool("skip-service-accounts", false, "Do not uninstall the YAKS Service Accounts in the current namespace")
	cmd.Flags().Bool("skip-config-maps", false, "Do not uninstall the YAKS Config Maps in the current namespace")

	cmd.Flags().Bool("global", false, "Indicates that a global installation is going to be uninstalled (affects OLM)")
	cmd.Flags().Bool("olm", true, "Try to uninstall via OLM (Operator Lifecycle Manager) if available")
	cmd.Flags().String("olm-operator-name", olm.DefaultOperatorName, "Name of the YAKS operator in the OLM source or marketplace")
	cmd.Flags().String("olm-package", olm.DefaultPackage, "Name of the YAKS package in the OLM source or marketplace")
	cmd.Flags().String("olm-global-namespace", olm.DefaultGlobalNamespace, "A namespace containing an OperatorGroup that defines "+
		"global scope for the operator (used in combination with the --global flag)")
	cmd.Flags().Bool("all", false, "Do uninstall all YAKS resources in all namespaces")

	return &cmd, &options
}

type uninstallCmdOptions struct {
	*RootCmdOptions
	SkipOperator            bool `mapstructure:"skip-operator"`
	SkipCrd                 bool `mapstructure:"skip-crd"`
	SkipTests               bool `mapstructure:"skip-tests"`
	SkipRoleBindings        bool `mapstructure:"skip-role-bindings"`
	SkipRoles               bool `mapstructure:"skip-roles"`
	SkipClusterRoleBindings bool `mapstructure:"skip-cluster-role-bindings"`
	SkipClusterRoles        bool `mapstructure:"skip-cluster-roles"`
	SkipServiceAccounts     bool `mapstructure:"skip-service-accounts"`
	SkipConfigMaps          bool `mapstructure:"skip-config-maps"`
	Global                  bool `mapstructure:"global"`
	OlmEnabled              bool `mapstructure:"olm"`
	UninstallAll            bool `mapstructure:"all"`

	OlmOptions olm.Options
}

var defaultListOptions = metav1.ListOptions{
	LabelSelector: config.DefaultAppLabel,
}

// nolint: gocyclo
func (o *uninstallCmdOptions) uninstall(cmd *cobra.Command, _ []string) error {
	c, err := o.GetCmdClient()
	if err != nil {
		return err
	}

	uninstallViaOLM := false
	if o.OlmEnabled {
		var err error
		if uninstallViaOLM, err = olm.IsAPIAvailable(o.Context, c, o.Namespace); err != nil {
			return errors.Wrap(err, "error while checking OLM availability. Run with '--olm=false' to skip this check")
		}

		if uninstallViaOLM {
			fmt.Println(cmd.OutOrStdout(), "OLM is available in the cluster")
			if err = olm.Uninstall(o.Context, c, o.Namespace, o.Global, o.OlmOptions); err != nil {
				return err
			}
			where := fmt.Sprintf("from namespace %s", o.Namespace)
			if o.Global {
				where = "globally"
			}
			fmt.Fprintf(cmd.OutOrStdout(), "YAKS OLM service removed %s\n", where)
		}
	}

	var namespaces []string
	if o.UninstallAll {
		instances := v1alpha1.InstanceList{}
		if err = c.List(o.Context, &instances); err != nil {
			if meta.IsNoMatchError(err) {
				fmt.Fprintf(cmd.OutOrStdout(), "Unable to locate operator instances in other namespaces - "+
					"continue to uninstall from namespace '%s'\n", o.Namespace)
			} else {
				return err
			}
		}

		for _, instance := range instances.Items {
			if instance.Spec.Operator.Namespace != o.Namespace {
				namespaces = append(namespaces, instance.Spec.Operator.Namespace)
			}
		}
	}

	namespaces = append(namespaces, o.Namespace)

	for _, namespace := range namespaces {
		if err = o.uninstallNamespaceResources(o.Context, c, namespace); err != nil {
			return err
		}

		if !uninstallViaOLM {
			if !o.SkipOperator {
				if err = o.uninstallOperator(o.Context, c, cmd.OutOrStdout(), namespace); err != nil {
					return err
				}
			}

			if err = o.uninstallNamespaceRoles(o.Context, c, namespace); err != nil {
				return err
			}
		}
	}

	if err = o.uninstallClusterWideResources(o.Context, c); err != nil {
		return err
	}

	return nil
}

func (o *uninstallCmdOptions) uninstallOperator(ctx context.Context, c client.Client, writer io.Writer, namespace string) error {
	api := c.AppsV1()

	deployments, err := api.Deployments(namespace).List(ctx, defaultListOptions)
	if err != nil {
		return err
	}

	for _, deployment := range deployments.Items {
		err := api.Deployments(namespace).Delete(ctx, deployment.Name, metav1.DeleteOptions{})
		if err != nil {
			return err
		}

		if o.Verbose {
			fmt.Println("Deployment " + deployment.Name + " deleted")
		}
	}

	instance := v1alpha1.Instance{
		TypeMeta: metav1.TypeMeta{
			Kind:       v1alpha1.InstanceKind,
			APIVersion: v1alpha1.SchemeGroupVersion.String(),
		},
		ObjectMeta: metav1.ObjectMeta{
			Namespace: namespace,
			Name:      "yaks",
		},
	}

	if err := c.Delete(o.Context, &instance); err != nil && !k8serrors.IsNotFound(err) && !meta.IsNoMatchError(err) {
		return err
	}

	fmt.Fprintf(writer, "YAKS Operator removed from namespace %s\n", namespace)

	return nil
}

func (o *uninstallCmdOptions) uninstallClusterWideResources(ctx context.Context, c client.Client) error {
	if !o.SkipCrd || o.UninstallAll {
		if err := o.uninstallCrd(ctx, c); err != nil {
			if k8serrors.IsForbidden(err) {
				return createActionNotAuthorizedError()
			}
			return err
		}
		fmt.Printf("YAKS Custom Resource Definitions removed from cluster\n")
	}

	if !o.SkipClusterRoleBindings || o.UninstallAll {
		if err := o.uninstallClusterRoleBindings(ctx, c); err != nil {
			if k8serrors.IsForbidden(err) {
				return createActionNotAuthorizedError()
			}
			return err
		}
		fmt.Printf("YAKS Cluster Role Bindings removed from cluster\n")
	}

	if !o.SkipClusterRoles || o.UninstallAll {
		if err := o.uninstallClusterRoles(ctx, c); err != nil {
			if k8serrors.IsForbidden(err) {
				return createActionNotAuthorizedError()
			}
			return err
		}
		fmt.Printf("YAKS Cluster Roles removed from cluster\n")
	}

	return nil
}

func (o *uninstallCmdOptions) uninstallNamespaceRoles(ctx context.Context, c client.Client, namespace string) error {
	if !o.SkipRoleBindings {
		if err := o.uninstallRoleBindings(ctx, c, namespace); err != nil {
			return err
		}
		fmt.Printf("YAKS Role Bindings removed from namespace %s\n", namespace)
	}

	if !o.SkipRoles {
		if err := o.uninstallRoles(ctx, c, namespace); err != nil {
			return err
		}
		fmt.Printf("YAKS Roles removed from namespace %s\n", namespace)
	}

	if !o.SkipServiceAccounts {
		if err := o.uninstallServiceAccounts(ctx, c, namespace); err != nil {
			return err
		}
		fmt.Printf("YAKS Service Accounts removed from namespace %s\n", namespace)
	}

	return nil
}

func (o *uninstallCmdOptions) uninstallNamespaceResources(ctx context.Context, c client.Client, namespace string) error {
	if !o.SkipTests {
		if err := o.uninstallTests(ctx, c, namespace); err != nil {
			return err
		}
		fmt.Printf("YAKS Tests removed from namespace %s\n", namespace)
	}

	if !o.SkipConfigMaps {
		if err := o.uninstallConfigMaps(ctx, c, namespace); err != nil {
			return err
		}
		fmt.Printf("YAKS Config Maps removed from namespace %s\n", namespace)
	}

	return nil
}

func (o *uninstallCmdOptions) uninstallCrd(ctx context.Context, c kubernetes.Interface) error {
	restClient, err := customclient.GetClientFor(c, "apiextensions.k8s.io", "v1")
	if err != nil {
		return err
	}

	result := restClient.
		Delete().
		Param("labelSelector", config.DefaultAppLabel).
		Resource("customresourcedefinitions").
		Do(ctx)

	if result.Error() != nil {
		return result.Error()
	}

	return nil
}

func (o *uninstallCmdOptions) uninstallRoles(ctx context.Context, c client.Client, namespace string) error {
	api := c.RbacV1()

	roles, err := api.Roles(namespace).List(ctx, defaultListOptions)
	if err != nil {
		return err
	}

	for _, role := range roles.Items {
		err := api.Roles(namespace).Delete(ctx, role.Name, metav1.DeleteOptions{})
		if err != nil {
			return err
		}

		if o.Verbose {
			fmt.Println("Role " + role.Name + " deleted")
		}
	}

	return nil
}

func (o *uninstallCmdOptions) uninstallRoleBindings(ctx context.Context, c client.Client, namespace string) error {
	api := c.RbacV1()

	roleBindings, err := api.RoleBindings(namespace).List(ctx, defaultListOptions)
	if err != nil {
		return err
	}

	for _, roleBinding := range roleBindings.Items {
		err := api.RoleBindings(namespace).Delete(ctx, roleBinding.Name, metav1.DeleteOptions{})
		if err != nil {
			return err
		}

		if o.Verbose {
			fmt.Println("RoleBinding " + roleBinding.Name + " deleted")
		}
	}

	return nil
}

func (o *uninstallCmdOptions) uninstallClusterRoles(ctx context.Context, c client.Client) error {
	api := c.RbacV1()

	clusterRoles, err := api.ClusterRoles().List(ctx, defaultListOptions)
	if err != nil {
		return err
	}

	for _, clusterRole := range clusterRoles.Items {
		err := api.ClusterRoles().Delete(ctx, clusterRole.Name, metav1.DeleteOptions{})
		if err != nil {
			return err
		}

		if o.Verbose {
			fmt.Println("ClusterRole " + clusterRole.Name + " deleted")
		}
	}

	return nil
}

func (o *uninstallCmdOptions) uninstallClusterRoleBindings(ctx context.Context, c client.Client) error {
	api := c.RbacV1()

	clusterRoleBindings, err := api.ClusterRoleBindings().List(ctx, defaultListOptions)
	if err != nil {
		return err
	}

	for _, clusterRoleBinding := range clusterRoleBindings.Items {
		err := api.ClusterRoleBindings().Delete(ctx, clusterRoleBinding.Name, metav1.DeleteOptions{})
		if err != nil {
			return err
		}

		if o.Verbose {
			fmt.Println("ClusterRoleBinding " + clusterRoleBinding.Name + " deleted")
		}
	}

	return nil
}

func (o *uninstallCmdOptions) uninstallServiceAccounts(ctx context.Context, c client.Client, namespace string) error {
	api := c.CoreV1()

	serviceAccountList, err := api.ServiceAccounts(namespace).List(ctx, defaultListOptions)
	if err != nil {
		return err
	}

	for _, serviceAccount := range serviceAccountList.Items {
		err := api.ServiceAccounts(namespace).Delete(ctx, serviceAccount.Name, metav1.DeleteOptions{})
		if err != nil {
			return err
		}

		if o.Verbose {
			fmt.Println("ServiceAccount " + serviceAccount.Name + " deleted")
		}
	}

	return nil
}

func (o *uninstallCmdOptions) uninstallTests(ctx context.Context, c client.Client, namespace string) error {
	return deleteAllTests(ctx, c, namespace, o.Verbose)
}

func (o *uninstallCmdOptions) uninstallConfigMaps(ctx context.Context, c client.Client, namespace string) error {
	api := c.CoreV1()

	configMapsList, err := api.ConfigMaps(namespace).List(ctx, defaultListOptions)
	if err != nil {
		return err
	}

	for _, configMap := range configMapsList.Items {
		err := api.ConfigMaps(namespace).Delete(ctx, configMap.Name, metav1.DeleteOptions{})
		if err != nil {
			return err
		}

		if o.Verbose {
			fmt.Println("ConfigMap " + configMap.Name + " deleted")
		}
	}

	return nil
}

func createActionNotAuthorizedError() error {
	fmt.Println("Current user is not authorized to remove cluster-wide objects like custom resource definitions or cluster roles")
	msg := `login as cluster-admin and execute "yaks uninstall" or use flags "--skip-crd --skip-cluster-roles --skip-cluster-role-bindings"`
	return errors.New(msg)
}

func (o *uninstallCmdOptions) decode(cmd *cobra.Command, _ []string) error {
	path := pathToRoot(cmd)
	if err := decodeKey(o, path); err != nil {
		return err
	}

	o.OlmOptions.OperatorName = viper.GetString(path + ".olm-operator-name")
	o.OlmOptions.Package = viper.GetString(path + ".olm-package")
	o.OlmOptions.GlobalNamespace = viper.GetString(path + ".olm-global-namespace")

	return nil
}
