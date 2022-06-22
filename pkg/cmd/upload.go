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
	"os"

	"github.com/citrusframework/yaks/pkg/client"
	snap "github.com/container-tools/snap/pkg/api"
	"github.com/spf13/cobra"
)

func newCmdUpload(rootCmdOptions *RootCmdOptions) (*cobra.Command, *uploadCmdOptions) {
	options := uploadCmdOptions{
		RootCmdOptions: rootCmdOptions,
	}

	cmd := cobra.Command{
		Use:          "upload artifact",
		Short:        "Upload a local test artifact to the cluster",
		Long:         `Upload a local test artifact to the cluster so that it can be used when running a test.`,
		Args:         options.validateArgs,
		PreRunE:      decode(&options),
		RunE:         options.run,
		SilenceUsage: true,
	}

	return &cmd, &options
}

type uploadCmdOptions struct {
	*RootCmdOptions
}

func (o *uploadCmdOptions) validateArgs(_ *cobra.Command, args []string) error {
	if len(args) != 1 {
		return fmt.Errorf("accepts exactly 1 local artifact to upload, received %d", len(args))
	}

	if _, err := os.Stat(args[0]); err != nil {
		return err
	}

	return nil
}

func (o *uploadCmdOptions) run(cmd *cobra.Command, args []string) error {
	artifact, err := uploadLocalArtifact(o.RootCmdOptions, args[0], o.Namespace)
	if err != nil {
		return err
	}
	_, err = fmt.Fprintf(cmd.OutOrStdout(), "Uploaded artifact %s\n", artifact)
	if err != nil {
		return err
	}

	return nil
}

func uploadLocalArtifact(opts *RootCmdOptions, path string, namespace string) (string, error) {
	config, err := client.GetOutOfClusterConfig(opts.KubeConfig)
	if err != nil {
		return "", err
	}

	bucket := "yaks"
	options := snap.SnapOptions{
		Bucket: bucket,
	}
	s3, err := snap.NewSnap(config, namespace, false, options)
	if err != nil {
		return "", err
	}
	return s3.Deploy(opts.Context, path)
}
