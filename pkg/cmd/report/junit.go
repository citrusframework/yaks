package report

import (
	"encoding/xml"
	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"os"
	"path"
)

const (
	XmlProcessingInstruction = `<?xml version="1.0" encoding="UTF-8"?>`
)

type JUnitReport struct {
	Suite TestSuite `xml:"testsuite"`
}

type TestSuite struct {
	Name string `xml:"name,attr"`
	Errors int `xml:"errors,attr"`
	Failures int `xml:"failures,attr"`
	Skipped int `xml:"skipped,attr"`
	Tests int `xml:"tests,attr"`
	Time float32 `xml:"time,attr"`
	TestCase []TestCase `xml:"testcase"`
}

type TestCase struct {
	Name string `xml:"name,attr"`
	ClassName string `xml:"classname,attr"`
	Time float32 `xml:"time,attr"`
	SystemOut string `xml:"system-out,omitempty"`
	Failure *Failure
}

type Failure struct {
	XMLName xml.Name `xml:"failure,omitempty"`
	Message string `xml:"message,attr,omitempty"`
	Type string `xml:"type,attr,omitempty"`
	Stacktrace string `xml:",chardata"`
}

func createJUnitReport(results *v1alpha1.TestResults, outputDir string) (string, error) {
	var report = JUnitReport {
		Suite: TestSuite {
			Name: "org.citrusframework.yaks.JUnitReport",
			Failures: results.Summary.Failed,
			Skipped: results.Summary.Skipped,
			Tests: results.Summary.Total,
		},
	}

	for _, result := range results.Tests {
		_, testName := path.Split(result.Name)
		testCase := TestCase{
			Name: testName,
			ClassName: result.Name,
		}

		if len(result.ErrorMessage) > 0 {
			testCase.Failure = &Failure{
				Message:    result.ErrorMessage,
				Type:       result.ErrorType,
				Stacktrace: "",
			}
		}

		report.Suite.TestCase = append(report.Suite.TestCase, testCase)
	}

	// need to workaround marshalling in order to overwrite local element name of root element
	tmp := struct {
		TestSuite
		XMLName struct{} `xml:"testsuite"`
	}{TestSuite: report.Suite}
	if bytes, err := xml.MarshalIndent(tmp,"", "  "); err == nil {
		junitReport := XmlProcessingInstruction + string(bytes)

		if err := createIfNotExists(outputDir); err != nil {
			return "", nil
		}

		reportFile, err := os.Create(path.Join(outputDir, "junit-reports.xml"))
		if err != nil {
			return "", err
		}

		if _, err := reportFile.Write([]byte(junitReport)); err != nil {
			return "", err
		}
		return junitReport, nil
	} else {
		return "", err
	}
}
