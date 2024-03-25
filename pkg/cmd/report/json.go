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
	"encoding/json"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/util"
)

const (
	JSONReportFile = "test-reports.json"
)

func createJSONReport(results *v1alpha1.TestResults) (string, error) {
	outputDir, err := util.CreateInWorkingDir(OutputDir)
	if err != nil {
		return "", err
	}

	bytes, err := json.MarshalIndent(results, "", "  ")
	if err != nil {
		return "", err
	}

	report := string(bytes)
	err = writeReport(report, JSONReportFile, outputDir)
	return report, err
}
