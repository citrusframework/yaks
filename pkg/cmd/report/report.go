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

package report

import (
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"path"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type OutputFormat string

// Set implements pflag/flag.Value.
func (d *OutputFormat) Set(s string) error {
	*d = OutputFormat(s)
	return nil
}

// Type implements pflag.Value.
func (d *OutputFormat) Type() string {
	return "string"
}

// Type implements pflag/flag.Value.
func (d *OutputFormat) String() string {
	return string(*d)
}

const (
	OutputDir = "_output"

	DefaultOutput OutputFormat = ""
	JSONOutput    OutputFormat = "json"
	JUnitOutput   OutputFormat = "junit"
	SummaryOutput OutputFormat = "summary"
)

func GenerateReport(out io.Writer, errorOut io.Writer, results *v1alpha1.TestResults, output OutputFormat) {
	outputDir, err := createInWorkingDir(OutputDir)
	if err != nil {
		printError(errorOut, err)
	}

	switch output {
	case SummaryOutput, DefaultOutput:
		printReport(out, GetSummaryReport(results))
	case JUnitOutput:
		if junitReport, err := createJUnitReport(results, outputDir); err != nil {
			printError(errorOut, err)
		} else {
			printReport(out, junitReport)
		}
	case JSONOutput:
		if jsonReport, err := createJSONReport(results, outputDir); err != nil {
			printError(errorOut, err)
		} else {
			printReport(out, jsonReport)
		}
	default:
		printError(errorOut, fmt.Errorf("unsupported report output format '%s'. Please use one of 'summary', 'json', 'junit'", output))
	}
}

func AppendTestResults(suites *v1alpha1.TestSuite, suite v1alpha1.TestSuite) {
	suites.Name = suite.Name

	AppendSummary(&suites.Summary, &suite.Summary)

	suites.Tests = append(suites.Tests, suite.Tests...)
}

func AppendSummary(overall *v1alpha1.TestSummary, summary *v1alpha1.TestSummary) {
	overall.Errors += summary.Errors
	overall.Passed += summary.Passed
	overall.Failed += summary.Failed
	overall.Skipped += summary.Skipped
	overall.Undefined += summary.Undefined
	overall.Pending += summary.Pending
	overall.Total += summary.Total
}

func SaveTestResults(test *v1alpha1.Test) error {
	outputDir, err := createInWorkingDir(OutputDir)
	if err != nil {
		return err
	}

	reportFile, err := os.Create(path.Join(outputDir, kubernetes.SanitizeName(test.Name)) + ".json")
	if err != nil {
		return err
	}

	bytes, err := json.Marshal(test.Status.Results)
	if err != nil {
		return err
	}

	if _, err := reportFile.Write(bytes); err != nil {
		return err
	}

	return nil
}

func CleanReports() error {
	err := removeFromWorkingDir(OutputDir)
	return err
}

func LoadTestResults() (*v1alpha1.TestResults, error) {
	results := v1alpha1.TestResults{}
	outputDir, err := getInWorkingDir(OutputDir)
	if err != nil && os.IsNotExist(err) {
		return &results, nil
	} else if err != nil {
		return &results, err
	}

	var files []os.FileInfo
	files, err = ioutil.ReadDir(outputDir)
	if err != nil {
		return &results, err
	}

	for _, file := range files {
		if path.Ext(file.Name()) != ".json" || file.Name() == JSONReportFile {
			continue
		}

		content, err := ioutil.ReadFile(path.Join(outputDir, file.Name()))
		if err != nil {
			return &results, err
		}

		var suite v1alpha1.TestSuite
		err = json.Unmarshal(content, &suite)
		if err != nil {
			return &results, err
		}

		isNew := true
		for i := range results.Suites {
			if results.Suites[i].Name == suite.Name {
				AppendTestResults(&results.Suites[i], suite)
				isNew = false
				break
			}
		}

		if isNew {
			results.Suites = append(results.Suites, suite)
		}
	}

	for _, suite := range results.Suites {
		AppendSummary(&results.Summary, &suite.Summary)
	}

	return &results, nil
}

func PrintSummaryReport(results *v1alpha1.TestResults) {
	fmt.Printf("%s\n", GetSummaryReport(results))
}

func GetSummaryReport(results *v1alpha1.TestResults) string {
	overall := v1alpha1.TestSuite{}

	for _, suite := range results.Suites {
		AppendTestResults(&overall, suite)
	}

	overall.Name = "All tests"

	summary := fmt.Sprintf("Test results: Total: %d, Passed: %d, Failed: %d, Errors: %d, Skipped: %d\n",
		overall.Summary.Total, overall.Summary.Passed, overall.Summary.Failed, overall.Summary.Errors, overall.Summary.Skipped)

	for _, test := range overall.Tests {
		result := "Passed"
		if len(test.ErrorMessage) > 0 {
			result = fmt.Sprintf("Failure caused by %s - %s", test.ErrorType, test.ErrorMessage)
		}
		_, className := path.Split(test.ClassName)
		summary += fmt.Sprintf("\t%s (%s): %s\n", test.Name, className, result)
	}

	if len(overall.Errors) > 0 {
		if prettyPrint, err := json.MarshalIndent(overall.Errors, "", "  "); err == nil {
			summary += fmt.Sprintf("\nErrors: %d\n%s", len(overall.Errors), string(prettyPrint))
		} else {
			fmt.Printf("Failed to read error details from test results: %s", err.Error())
		}
	}
	return summary
}

func GetErrorResult(namespace string, source string, err error) *v1alpha1.Test {
	return &v1alpha1.Test{
		TypeMeta: metav1.TypeMeta{
			Kind:       v1alpha1.TestKind,
			APIVersion: v1alpha1.SchemeGroupVersion.String(),
		},
		ObjectMeta: metav1.ObjectMeta{
			Namespace: namespace,
			Name:      source,
		},
		Status: v1alpha1.TestStatus{
			Errors: err.Error(),
			Results: v1alpha1.TestSuite{
				Name: source,
				Tests: []v1alpha1.TestResult{
					{
						Name:         kubernetes.SanitizeName(source),
						ClassName:    source,
						ErrorType:    GetErrorType(err),
						ErrorMessage: err.Error(),
					},
				},
				Summary: v1alpha1.TestSummary{
					Errors: 1,
				},
				Errors: []string{fmt.Sprintf("%s - %s", GetErrorType(err), err.Error())},
			},
		},
	}
}

func GetErrorType(err error) string {
	if errReason := k8serrors.ReasonForError(err); errReason != metav1.StatusReasonUnknown {
		return string(errReason)
	}

	return "InitializationError"
}

func printReport(out io.Writer, report string) {
	_, _ = fmt.Fprintf(out, "%s\n", report)
}

func printError(out io.Writer, err error) {
	_, _ = fmt.Fprintf(out, "%v\n", err)
}
