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
	"strings"

	"github.com/pkg/errors"
	"github.com/spf13/viper"

	"github.com/citrusframework/yaks/pkg/client"
	"github.com/spf13/cobra"
)

const (
	ConfigNameEnv = "YAKS_CONFIG_NAME"
	ConfigPathEnv = "YAKS_CONFIG_PATH"

	commandShortDescription = `YAKS is a client tool for running tests natively on Kubernetes`
	commandLongDescription  = `YAKS is a platform to enable Cloud Native BDD testing on Kubernetes.`
)

// RootCmdOptions --.
type RootCmdOptions struct {
	RootContext   context.Context    `mapstructure:"-"`
	Context       context.Context    `mapstructure:"-"`
	ContextCancel context.CancelFunc `mapstructure:"-"`
	_client       client.Client      `mapstructure:"-"`
	KubeConfig    string             `mapstructure:"kube-config"`
	Namespace     string             `mapstructure:"namespace"`
	Verbose       bool               `mapstructure:"verbose"`
	Local         bool               `mapstructure:"local"`
}

// NewYaksCommand --.
func NewYaksCommand(ctx context.Context) (*cobra.Command, error) {
	childCtx, childCancel := context.WithCancel(ctx)
	options := RootCmdOptions{
		RootContext:   ctx,
		Context:       childCtx,
		ContextCancel: childCancel,
	}

	var err error
	var cmd = cobra.Command{
		BashCompletionFunction: bashCompletionFunction,
		PersistentPreRunE:      options.preRun,
		Use:                    "yaks",
		Short:                  commandShortDescription,
		Long:                   commandLongDescription,
		SilenceUsage:           true,
	}

	cmd.PersistentFlags().StringVar(&options.KubeConfig, "config", os.Getenv("KUBECONFIG"), "Path to the config file to use for CLI requests")
	cmd.PersistentFlags().StringVarP(&options.Namespace, "namespace", "n", "", "Namespace to use for all operations")
	cmd.PersistentFlags().BoolVarP(&options.Verbose, "verbose", "v", false, "Print details while performing an operation")
	cmd.PersistentFlags().BoolVar(&options.Local, "local", false, "Run command in local mode")

	cmd.AddCommand(newCmdCompletion(&cmd))
	cmd.AddCommand(newCmdVersion())
	cmd.AddCommand(cmdOnly(newCmdDump(&options)))
	cmd.AddCommand(cmdOnly(newCmdRun(&options)))
	cmd.AddCommand(cmdOnly(newCmdDelete(&options)))
	cmd.AddCommand(cmdOnly(newCmdList(&options)))
	cmd.AddCommand(cmdOnly(newCmdLog(&options)))
	cmd.AddCommand(cmdOnly(newCmdInstall(&options)))
	cmd.AddCommand(cmdOnly(newCmdRole(&options)))
	cmd.AddCommand(cmdOnly(newCmdUninstall(&options)))
	cmd.AddCommand(cmdOnly(newCmdOperator()))
	cmd.AddCommand(cmdOnly(newCmdUpload(&options)))
	cmd.AddCommand(cmdOnly(newCmdReport(&options)))

	if err := addHelpSubCommands(&cmd); err != nil {
		return &cmd, err
	}

	err = postAddCommandInit(&cmd)

	return &cmd, err
}

func postAddCommandInit(cmd *cobra.Command) error {
	if err := bindPFlagsHierarchy(cmd); err != nil {
		return err
	}

	configName := os.Getenv(ConfigNameEnv)
	if configName == "" {
		configName = DefaultConfigName
	}

	viper.SetConfigName(configName)

	configPath := os.Getenv(ConfigPathEnv)
	if configPath != "" {
		// if a specific config path is set, don't add
		// default locations
		viper.AddConfigPath(configPath)
	} else {
		viper.AddConfigPath(".")
		viper.AddConfigPath(".yaks")
		viper.AddConfigPath("$HOME/.yaks")
	}

	viper.AutomaticEnv()
	viper.SetEnvKeyReplacer(strings.NewReplacer(
		".", "_",
		"-", "_",
	))

	if err := viper.ReadInConfig(); err != nil && !errors.As(err, &viper.ConfigFileNotFoundError{}) {
		return err
	}

	return nil
}

func addHelpSubCommands(cmd *cobra.Command) error {
	cmd.InitDefaultHelpCmd()

	var helpCmd *cobra.Command
	for _, c := range cmd.Commands() {
		if c.Name() == "help" {
			helpCmd = c
			break
		}
	}

	if helpCmd == nil {
		return errors.New("could not find any configured help command")
	}

	return nil
}

func (command *RootCmdOptions) preRun(cmd *cobra.Command, _ []string) error {
	if !command.Local && !isOfflineCommand(cmd) {
		c, err := command.GetCmdClient()
		if err != nil {
			return errors.Wrap(err, "cannot get command client")
		}

		if command.Namespace == "" {
			var current string
			current, err = c.GetCurrentNamespace(command.KubeConfig)
			if err != nil {
				return errors.Wrap(err, "cannot get current namespace")
			}
			err = cmd.Flag("namespace").Value.Set(current)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

// GetCmdClient returns the client that can be used from command line tools.
func (command *RootCmdOptions) GetCmdClient() (client.Client, error) {
	// Get the pre-computed client
	if command._client != nil {
		return command._client, nil
	}
	var err error
	command._client, err = command.NewCmdClient()
	return command._client, err
}

// NewCmdClient returns a new client that can be used from command line tools.
func (command *RootCmdOptions) NewCmdClient() (client.Client, error) {
	return client.NewOutOfClusterClient(command.KubeConfig)
}
