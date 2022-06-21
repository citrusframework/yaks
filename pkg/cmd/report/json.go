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

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
)

const (
	JsonReportFile = "test-reports.json"
)

func createJsonReport(results *v1alpha1.TestResults, outputDir string) (string, error) {
	if bytes, err := json.MarshalIndent(results, "", "  "); err == nil {
		report := string(bytes)

		fileError := writeReport(report, JsonReportFile, outputDir)
		if fileError != nil {
			return "", fileError
		}

		return report, nil
	} else {
		return "", err
	}
}
