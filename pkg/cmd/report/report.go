package report

import (
	"encoding/json"
	"errors"
	"fmt"
	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"
	"io/ioutil"
	"os"
	"path"
)

type OutputFormat string

// Set implements pflag/flag.Value
func (d *OutputFormat) Set(s string) error {
	*d = OutputFormat(s)
	return nil
}

// Type implements pflag.Value
func (d *OutputFormat) Type() string {
	return "string"
}

// Type implements pflag/flag.Value
func (d *OutputFormat) String() string {
	return string(*d)
}

const (
	OutputDir = "_output"

	DefaultOutput OutputFormat = ""
	JsonOutput    OutputFormat = "json"
	JUnitOutput   OutputFormat = "junit"
	SummaryOutput OutputFormat = "summary"
)

func GenerateReport(results *v1alpha1.TestResults, output OutputFormat) (string, error) {
	switch output {
		case JUnitOutput:
			outputDir, err := createInWorkingDir(OutputDir)
			if err != nil {
				return "", err
			}

			if junitReport, err := createJUnitReport(results, outputDir); err != nil {
				return "", err
			} else {
				return junitReport, nil
			}
		case SummaryOutput, DefaultOutput:
			summaryReport := GetSummaryReport(results)
			return summaryReport, nil
		case JsonOutput:
			if bytes, err := json.MarshalIndent(results, "", "  "); err != nil {
				return "", err
			} else {
				return string(bytes), nil
			}
		default:
			return "", errors.New(fmt.Sprintf("Unsupported report output format '%s'. Please use one of 'summary', 'json', 'junit'", output))
	}
}

func AppendTestResults(results *v1alpha1.TestResults, result v1alpha1.TestResults) {
	results.Summary.Passed += result.Summary.Passed
	results.Summary.Failed += result.Summary.Failed
	results.Summary.Skipped += result.Summary.Skipped
	results.Summary.Undefined += result.Summary.Undefined
	results.Summary.Pending += result.Summary.Pending
	results.Summary.Total += result.Summary.Total

	for _, result := range result.Tests {
		results.Tests = append(results.Tests, result)
	}
}

func SaveTestResults(test *v1alpha1.Test) error {
	outputDir, err := createInWorkingDir(OutputDir)

	reportFile, err := os.Create(path.Join(outputDir, kubernetes.SanitizeName(test.Name)) + ".json")
	if err != nil {
		return err
	}

	bytes, _ := json.Marshal(test.Status.Results)
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
	if err != nil {
		if os.IsNotExist(err) {
			return &results, nil
		} else {
			return &results, err
		}
	}

	var files []os.FileInfo
	files, err = ioutil.ReadDir(outputDir)
	if err != nil {
		return &results, err
	}

	for _, file := range files {
		if path.Ext(file.Name()) != ".json" {
			continue
		}

		content, err := ioutil.ReadFile(path.Join(outputDir, file.Name()))
		if err != nil {
			return &results, err
		}

		var result v1alpha1.TestResults
		err = json.Unmarshal(content, &result)
		if err != nil {
			return &results, err
		}

		AppendTestResults(&results, result)
	}

	return &results, nil
}

func PrintSummaryReport(results *v1alpha1.TestResults) {
	fmt.Printf("%s\n", GetSummaryReport(results))
}

func GetSummaryReport(results *v1alpha1.TestResults) string {
	summary := fmt.Sprintf("Test results: Total: %d, Passed: %d, Failed: %d, Skipped: %d\n",
		results.Summary.Total, results.Summary.Passed, results.Summary.Failed, results.Summary.Skipped)

	for _, test := range results.Tests {
		result := "Passed"
		if len(test.ErrorMessage) > 0 {
			result = fmt.Sprintf("Failure caused by %s - %s", test.ErrorType, test.ErrorMessage)
		}
		_, className := path.Split(test.ClassName)
		summary += fmt.Sprintf("\t%s (%s): %s\n", test.Name, className, result)
	}

	if len(results.Errors) > 0 {
		if prettyPrint, err := json.MarshalIndent(results.Errors, "", "  "); err == nil {
			summary += fmt.Sprintf("\nErrors: \n%s", string(prettyPrint))
		} else {
			fmt.Printf("Failed to read error details from test results: %s", err.Error())
		}
	}
	return summary
}
