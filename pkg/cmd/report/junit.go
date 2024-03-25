/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package report

import (
	"encoding/xml"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/util"
)

const (
	JUnitReportFile          = "junit-reports.xml"
	XMLProcessingInstruction = `<?xml version="1.0" encoding="UTF-8"?>`
)

type JUnitReport struct {
	Suite []TestSuite `xml:"testsuite"`
}

type TestSuite struct {
	Name     string     `xml:"name,attr"`
	Errors   int        `xml:"errors,attr"`
	Failures int        `xml:"failures,attr"`
	Skipped  int        `xml:"skipped,attr"`
	Tests    int        `xml:"tests,attr"`
	Time     float32    `xml:"time,attr"`
	TestCase []TestCase `xml:"testcase"`
}

type TestCase struct {
	Name      string  `xml:"name,attr"`
	ClassName string  `xml:"classname,attr"`
	Time      float32 `xml:"time,attr"`
	SystemOut string  `xml:"system-out,omitempty"`
	Failure   *Failure
	Error     *Error
}

type Failure struct {
	XMLName    xml.Name `xml:"failure,omitempty"`
	Message    string   `xml:"message,attr,omitempty"`
	Type       string   `xml:"type,attr,omitempty"`
	Stacktrace string   `xml:",chardata"`
}

type Error struct {
	XMLName    xml.Name `xml:"error,omitempty"`
	Message    string   `xml:"message,attr,omitempty"`
	Type       string   `xml:"type,attr,omitempty"`
	Stacktrace string   `xml:",chardata"`
}

func createJUnitReport(results *v1alpha1.TestResults) (string, error) {
	outputDir, err := util.CreateInWorkingDir(OutputDir)
	if err != nil {
		return "", err
	}

	var report = JUnitReport{
		Suite: []TestSuite{},
	}

	for _, testSuite := range results.Suites {
		var suite = TestSuite{
			Name:     testSuite.Name,
			Failures: testSuite.Summary.Failed,
			Skipped:  testSuite.Summary.Skipped,
			Tests:    testSuite.Summary.Total,
			Errors:   testSuite.Summary.Errors,
		}

		for _, test := range testSuite.Tests {
			testCase := TestCase{
				Name:      test.Name,
				ClassName: test.ClassName,
			}

			if len(test.ErrorMessage) > 0 {
				testCase.Failure = &Failure{
					Message:    test.ErrorMessage,
					Type:       test.ErrorType,
					Stacktrace: "",
				}
			}

			suite.TestCase = append(suite.TestCase, testCase)
		}

		report.Suite = append(report.Suite, suite)
	}

	// need to workaround marshalling in order to overwrite local element name of root element
	tmp := struct {
		JUnitReport
		XMLName struct{} `xml:"testsuites"`
	}{JUnitReport: report}

	bytes, err := xml.MarshalIndent(tmp, "", "  ")
	if err != nil {
		return "", err
	}

	xmlReport := XMLProcessingInstruction + string(bytes)

	err = writeReport(xmlReport, JUnitReportFile, outputDir)
	return xmlReport, err
}
