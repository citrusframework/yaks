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
	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"text/tabwriter"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	k8sclient "sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/spf13/cobra"
)

type listCmdOptions struct {
	*RootCmdOptions
}

func newCmdList(rootCmdOptions *RootCmdOptions) (*cobra.Command, *listCmdOptions) {
	options := listCmdOptions{
		RootCmdOptions: rootCmdOptions,
	}
	cmd := cobra.Command{
		Use:     "list",
		Short:   "List tests",
		Long:    `List the results of all tests on given namespace.`,
		Aliases: []string{"ls"},
		PreRunE: decode(&options),
		RunE:    options.run,
	}

	return &cmd, &options
}

func (o *listCmdOptions) run(cmd *cobra.Command, args []string) error {
	c, err := o.GetCmdClient()
	if err != nil {
		return err
	}

	testList := v1alpha1.TestList{
		TypeMeta: metav1.TypeMeta{
			APIVersion: v1alpha1.SchemeGroupVersion.String(),
			Kind:       v1alpha1.TestKind,
		},
	}

	namespace := o.Namespace

	options := []k8sclient.ListOption{
		k8sclient.InNamespace(namespace),
	}

	err = c.List(o.Context, &testList, options...)
	if err != nil {
		return err
	}

	if len(testList.Items) == 0 {
		fmt.Printf("No tests found in %s namespace.\n", namespace)
		return nil
	}

	w := tabwriter.NewWriter(cmd.OutOrStdout(), 0, 8, 1, '\t', 0)
	fmt.Fprintln(w, "NAME\tPHASE\tTOTAL\tPASSED\tFAILED\tSKIPPED\tERRORS")
	for _, test := range testList.Items {
		fmt.Fprintf(w, "%s\t%s\t%d\t%d\t%d\t%d\t%d\n", test.Name, string(test.Status.Phase),
			test.Status.Results.Summary.Total,
			test.Status.Results.Summary.Passed,
			test.Status.Results.Summary.Failed,
			test.Status.Results.Summary.Skipped,
			test.Status.Results.Summary.Errors)
	}
	return w.Flush()
}
