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
	"fmt"

	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/install"
	"github.com/pkg/errors"
	"github.com/spf13/cobra"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
)

func newCmdInstall(rootCmdOptions *RootCmdOptions) *cobra.Command {
	impl := installCmdOptions{
		RootCmdOptions: rootCmdOptions,
	}
	cmd := cobra.Command{
		PersistentPreRunE: impl.preRun,
		Use:               "install",
		Short:             "Install Yaks on a Kubernetes cluster",
		Long:              `Installs Yaks on a Kubernetes or OpenShift cluster.`,
		RunE:              impl.install,
	}

	cmd.Flags().BoolVar(&impl.clusterSetupOnly, "cluster-setup", false, "Execute cluster-wide operations only (may require admin rights)")
	cmd.Flags().BoolVar(&impl.skipOperatorSetup, "skip-operator-setup", false, "Do not install the operator in the namespace (in case there's a global one)")
	cmd.Flags().BoolVar(&impl.skipClusterSetup, "skip-cluster-setup", false, "Skip the cluster-setup phase")

	return &cmd
}

type installCmdOptions struct {
	*RootCmdOptions
	clusterSetupOnly  bool
	skipOperatorSetup bool
	skipClusterSetup  bool
}

// nolint: gocyclo
func (o *installCmdOptions) install(_ *cobra.Command, _ []string) error {
	if !o.skipClusterSetup {
		// Let's use a client provider during cluster installation, to eliminate the problem of CRD object caching
		clientProvider := client.Provider{Get: o.NewCmdClient}

		err := install.SetupClusterwideResourcesOrCollect(o.Context, clientProvider, nil)
		if err != nil && k8serrors.IsForbidden(err) {
			fmt.Println("Current user is not authorized to create cluster-wide objects like custom resource definitions or cluster roles: ", err)

			meg := `please login as cluster-admin and execute "yaks install --cluster-setup" to install cluster-wide resources (one-time operation)`
			return errors.New(meg)
		} else if err != nil {
			return err
		}
	}

	if o.clusterSetupOnly {
		fmt.Println("Yaks cluster setup completed successfully")
	} else {
		c, err := o.GetCmdClient()
		if err != nil {
			return err
		}

		namespace := o.Namespace

		if !o.skipOperatorSetup {
			cfg := install.OperatorConfiguration{
				Namespace: namespace,
			}
			err = install.OperatorOrCollect(o.Context, c, cfg, nil)
			if err != nil {
				return err
			}
			fmt.Println("Yaks setup completed successfully")
		} else {
			fmt.Println("Yaks operator installation skipped")
		}
	}

	return nil
}
