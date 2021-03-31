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
	"github.com/citrusframework/yaks/pkg/cmd/report"
	"github.com/spf13/cobra"
	ctrl "sigs.k8s.io/controller-runtime/pkg/client"
)

func newCmdReport(rootCmdOptions *RootCmdOptions) (*cobra.Command, *reportCmdOptions) {
	options := reportCmdOptions{
		RootCmdOptions: rootCmdOptions,
	}

	cmd := cobra.Command{
		Use:     "report [options]",
		Short:   "Generate test report from last test run",
		Long:    `Generate test report holding results from last test run. Fetch results from cluster and/or collect from local test output.`,
		PreRunE: decode(&options),
		RunE:    options.run,
	}

	cmd.Flags().Bool("fetch", false, "Fetch latest test results from cluster.")
	cmd.Flags().StringP("output", "o", "summary", "The report output format, one of 'summary', 'json', 'junit'")
	cmd.Flags().BoolP("clean", "c", false, "Clean the report output folder before fetching results")

	return &cmd, &options
}

type reportCmdOptions struct {
	*RootCmdOptions
	Clean        bool                `mapstructure:"clean"`
	Fetch        bool                `mapstructure:"fetch"`
	OutputFormat report.OutputFormat `mapstructure:"output"`
}

func (o *reportCmdOptions) run(cmd *cobra.Command, _ []string) error {
	var results v1alpha1.TestResults
	if o.Fetch {
		if fetched, err := o.FetchResults(); err == nil {
			results = *fetched
		} else {
			return err
		}
	} else if loaded, err := report.LoadTestResults(); err == nil {
		results = *loaded
	} else {
		return err
	}

	content, err := report.GenerateReport(&results, o.OutputFormat)
	if err != nil {
		return err
	}
	_, err = fmt.Fprintf(cmd.OutOrStdout(), "%s\n", content)
	if err != nil {
		return err
	}

	return nil
}

func (o *reportCmdOptions) FetchResults() (*v1alpha1.TestResults, error) {
	c, err := o.GetCmdClient()
	if err != nil {
		return nil, err
	}

	if o.Clean {
		err = report.CleanReports()
		if err != nil {
			return nil, err
		}
	}

	results := v1alpha1.TestResults{}
	testList := v1alpha1.TestList{}
	if err := c.List(o.Context, &testList, ctrl.InNamespace(o.Namespace)); err != nil {
		return nil, err
	}

	for _, test := range testList.Items {
		suite := v1alpha1.TestSuite{}

		isNew := true
		for _, existing := range results.Suites {
			if existing.Name == test.Status.Results.Name {
				suite = existing
				isNew = false
				break
			}
		}

		report.AppendTestResults(&suite, test.Status.Results)

		if isNew {
			results.Suites = append(results.Suites, suite)
		}
		if err := report.SaveTestResults(&test); err != nil {
			fmt.Printf("Failed to save test results: %s", err.Error())
		}
	}

	for _, suite := range results.Suites {
		report.AppendSummary(&results.Summary, &suite.Summary)
	}

	return &results, nil
}
