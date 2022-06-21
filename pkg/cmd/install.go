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
	"os"
	"strings"
	"time"

	"github.com/citrusframework/yaks/pkg/util/kubernetes"
	"github.com/citrusframework/yaks/pkg/util/olm"

	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/install"
	"github.com/pkg/errors"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
)

func newCmdInstall(rootCmdOptions *RootCmdOptions) (*cobra.Command, *installCmdOptions) {
	options := installCmdOptions{
		RootCmdOptions: rootCmdOptions,
	}
	cmd := cobra.Command{
		Use:     "install",
		Short:   "Installs YAKS on a Kubernetes cluster",
		Long:    `Installs YAKS on a Kubernetes or OpenShift cluster.`,
		PreRunE: options.decode,
		RunE: func(cmd *cobra.Command, args []string) error {
			if err := options.install(cmd, args); err != nil {
				if k8serrors.IsAlreadyExists(err) {
					return errors.Wrap(err, "YAKS seems already installed (use the --force option to overwrite existing resources)")
				}
				return err
			}
			return nil
		},
	}

	cmd.Flags().Bool("cluster-setup", false, "Execute cluster-wide operations only (may require admin rights)")
	cmd.Flags().String("cluster-type", "", "Set explicitly the cluster type to Kubernetes or OpenShift")
	cmd.Flags().Bool("no-operator-setup", false, "Do not install the operator in the namespace (in case there's a global one)")
	cmd.Flags().Bool("no-cluster-setup", false, "Skip the cluster-setup phase")
	cmd.Flags().Bool("global", true, "Configure the operator to watch all namespaces")
	cmd.Flags().Bool("force", false, "Force replacement of configuration resources when already present.")
	cmd.Flags().String("operator-id", "yaks", "Set the operator id")
	cmd.Flags().String("operator-image", "", "Set the operator Image used for the operator deployment")
	cmd.Flags().String("operator-image-pull-policy", "", "Set the operator ImagePullPolicy used for the operator deployment")
	cmd.Flags().StringP("output", "o", "", "Output format. One of: json|yaml")
	cmd.Flags().StringArray("env-vars", nil, "Add an environment variable to set in the operator Pod(s), as <name=value>")

	// olm
	cmd.Flags().Bool("olm", true, "Try to install everything via OLM (Operator Lifecycle Manager) if available")
	cmd.Flags().String("olm-operator-name", olm.DefaultOperatorName, "Name of the YAKS operator in the OLM source or marketplace")
	cmd.Flags().String("olm-package", olm.DefaultPackage, "Name of the YAKS package in the OLM source or marketplace")
	cmd.Flags().String("olm-channel", olm.DefaultChannel, "Name of the YAKS channel in the OLM source or marketplace")
	cmd.Flags().String("olm-source", olm.DefaultSource, "Name of the OLM source providing the YAKS package (defaults to the standard Operator Hub source)")
	cmd.Flags().String("olm-source-namespace", olm.DefaultSourceNamespace, "Namespace where the OLM source is available")
	cmd.Flags().String("olm-starting-csv", olm.DefaultStartingCSV, "Allow to install a specific version from the operator source instead of latest available "+
		"from the channel")
	cmd.Flags().String("olm-global-namespace", olm.DefaultGlobalNamespace, "A namespace containing an OperatorGroup that defines global scope for the "+
		"operator (used in combination with the --global flag)")

	return &cmd, &options
}

type installCmdOptions struct {
	*RootCmdOptions
	ClusterSetupOnly        bool     `mapstructure:"cluster-setup"`
	SkipOperatorSetup       bool     `mapstructure:"no-operator-setup"`
	SkipClusterSetup        bool     `mapstructure:"no-cluster-setup"`
	OperatorImage           string   `mapstructure:"operator-image"`
	OperatorImagePullPolicy string   `mapstructure:"operator-image-pull-policy"`
	Global                  bool     `mapstructure:"global"`
	Force                   bool     `mapstructure:"force"`
	Olm                     bool     `mapstructure:"olm"`
	ClusterType             string   `mapstructure:"cluster-type"`
	OutputFormat            string   `mapstructure:"output"`
	OperatorID              string   `mapstructure:"operator-id"`
	EnvVars                 []string `mapstructure:"env-vars"`
	olmOptions              olm.Options
}

// nolint: gocyclo
func (o *installCmdOptions) install(cobraCmd *cobra.Command, _ []string) error {
	var collection *kubernetes.Collection
	if o.OutputFormat != "" {
		collection = kubernetes.NewCollection()
	}

	// Let's use a client provider during cluster installation, to eliminate the problem of CRD object caching
	clientProvider := client.Provider{Get: o.NewCmdClient}

	// --operator-id={id} is a syntax sugar for '--env-vars YAKS_OPERATOR_ID={id}'
	o.EnvVars = append(o.EnvVars, fmt.Sprintf("YAKS_OPERATOR_ID=%s", strings.TrimSpace(o.OperatorID)))

	installViaOLM := false
	if o.Olm {
		var err error
		var olmClient client.Client
		if olmClient, err = clientProvider.Get(); err != nil {
			return err
		}
		var olmAvailable bool
		if olmAvailable, err = olm.IsAPIAvailable(o.Context, olmClient, o.Namespace); err != nil {
			return errors.Wrap(err, "error while checking OLM availability. Run with '--olm=false' to skip this check")
		}

		if olmAvailable {
			if installViaOLM, err = olm.HasPermissionToInstall(o.Context, olmClient, o.Namespace, o.Global, o.olmOptions); err != nil {
				return errors.Wrap(err, "error while checking permissions to install operator via OLM. Run with '--olm=false' to skip this check")
			}
			if !installViaOLM {
				fmt.Fprintln(cobraCmd.OutOrStdout(), "OLM is available but current user has not enough permissions to create the operator. "+
					"You can either ask your administrator to provide permissions (preferred) or run the install command with the `--olm=false` flag.")
				os.Exit(1)
			}
		}

		if installViaOLM && !o.ClusterSetupOnly {
			fmt.Fprintln(cobraCmd.OutOrStdout(), "OLM is available in the cluster")
			var installed bool
			if installed, err = olm.Install(o.Context, olmClient, o.Namespace, o.Global, o.olmOptions, o.EnvVars, collection); err != nil {
				return err
			}
			if !installed {
				fmt.Fprintln(cobraCmd.OutOrStdout(), "OLM resources are already available: skipping installation")
			}

			if err = install.WaitForAllCRDInstallation(o.Context, clientProvider, 90*time.Second); err != nil {
				return err
			}
		}
	}

	if o.ClusterSetupOnly || (!o.SkipClusterSetup && !installViaOLM) {
		err := setupCluster(o.Context, clientProvider, collection)
		if err != nil {
			return err
		}
	}

	if o.ClusterSetupOnly {
		if collection == nil {
			fmt.Fprintln(cobraCmd.OutOrStdout(), "YAKS cluster setup completed successfully")
		}
	} else {
		c, err := o.GetCmdClient()
		if err != nil {
			return err
		}

		if !o.SkipOperatorSetup && !installViaOLM {
			err = o.setupOperator(c, cobraCmd, collection)
			if err != nil {
				return err
			}
			fmt.Fprintln(cobraCmd.OutOrStdout(), "YAKS operator installation completed")
		} else if o.SkipOperatorSetup {
			fmt.Fprintln(cobraCmd.OutOrStdout(), "YAKS operator installation skipped")
		}

		if collection == nil {
			strategy := ""
			if installViaOLM {
				strategy = "via OLM subscription"
			}
			if o.Global {
				fmt.Println("YAKS installed in namespace", o.Namespace, strategy, "(global mode)")
			} else {
				fmt.Println("YAKS installed in namespace", o.Namespace, strategy)
			}
		}
	}

	if collection != nil {
		return o.printOutput(collection)
	}

	return nil
}

func setupCluster(ctx context.Context, clientProvider client.Provider, collection *kubernetes.Collection) error {
	err := install.SetupClusterWideResourcesOrCollect(ctx, clientProvider, collection)
	if err != nil && k8serrors.IsForbidden(err) {
		fmt.Println("Current user is not authorized to create cluster-wide objects like custom resource definitions or cluster roles: ", err)

		meg := `please login as cluster-admin and execute "yaks install --cluster-setup" to install cluster-wide resources (one-time operation)`
		return errors.New(meg)
	}

	return err
}

func (o *installCmdOptions) setupOperator(c client.Client, cmd *cobra.Command, collection *kubernetes.Collection) error {
	cfg := install.OperatorConfiguration{
		CustomImage:           o.OperatorImage,
		CustomImagePullPolicy: o.OperatorImagePullPolicy,
		Namespace:             o.Namespace,
		Global:                o.Global,
		ClusterType:           o.ClusterType,
		EnvVars:               o.EnvVars,
	}
	err := install.OperatorOrCollect(o.Context, cmd, c, cfg, collection, o.Force)

	return err
}

func (o *installCmdOptions) printOutput(collection *kubernetes.Collection) error {
	lst := collection.AsKubernetesList()
	switch o.OutputFormat {
	case "yaml":
		data, err := kubernetes.ToYAML(lst)
		if err != nil {
			return err
		}
		fmt.Print(string(data))
	case "json":
		data, err := kubernetes.ToJSON(lst)
		if err != nil {
			return err
		}
		fmt.Print(string(data))
	default:
		return errors.New("unknown output format: " + o.OutputFormat)
	}
	return nil
}

func (o *installCmdOptions) decode(cmd *cobra.Command, _ []string) error {
	path := pathToRoot(cmd)
	if err := decodeKey(o, path); err != nil {
		return err
	}

	o.olmOptions.OperatorName = viper.GetString(path + ".olm-operator-name")
	o.olmOptions.Package = viper.GetString(path + ".olm-package")
	o.olmOptions.Channel = viper.GetString(path + ".olm-channel")
	o.olmOptions.Source = viper.GetString(path + ".olm-source")
	o.olmOptions.SourceNamespace = viper.GetString(path + ".olm-source-namespace")
	o.olmOptions.StartingCSV = viper.GetString(path + ".olm-starting-csv")
	o.olmOptions.GlobalNamespace = viper.GetString(path + ".olm-global-namespace")

	return nil
}
