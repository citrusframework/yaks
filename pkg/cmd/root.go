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
	"os"

	"github.com/citrusframework/yaks/pkg/client"
	"github.com/spf13/cobra"
)

const yaksCommandLongDescription = `Yaks is Yet Another Kamel Subproject.
`

// RootCmdOptions --
type RootCmdOptions struct {
	Context    context.Context
	_client    client.Client
	KubeConfig string
	Namespace  string
}

// NewYaksCommand --
func NewYaksCommand(ctx context.Context) (*cobra.Command, error) {
	options := RootCmdOptions{
		Context: ctx,
	}
	var cmd = cobra.Command{
		Use:   "yaks",
		Short: "Yaks is a awesome client tool for running tests natively on Kubernetes",
		Long:  yaksCommandLongDescription,
	}

	cmd.PersistentFlags().StringVar(&options.KubeConfig, "config", os.Getenv("KUBECONFIG"), "Path to the config file to use for CLI requests")
	cmd.PersistentFlags().StringVarP(&options.Namespace, "namespace", "n", "", "Namespace to use for all operations")

	cmd.AddCommand(newCmdTest(&options))
	cmd.AddCommand(newCmdInstall(&options))
	cmd.AddCommand(newCmdOperator(&options))
	cmd.AddCommand(newCmdUpload(&options))
	cmd.AddCommand(newCmdVersion(&options))

	return &cmd, nil
}
