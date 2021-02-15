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
	"os"
	"path"
)

func getInWorkingDir(dir string) (string, error) {
	workingDir, err := os.Getwd()
	if err != nil {
		return "", err
	}

	outputDir := path.Join(workingDir, dir)
	_, err = os.Stat(outputDir)

	return outputDir, err
}

func createInWorkingDir(dir string) (string, error) {
	workingDir, err := os.Getwd()
	if err != nil {
		return "", err
	}

	newDir := path.Join(workingDir, dir)
	err = createIfNotExists(newDir)
	return newDir, err
}

func removeFromWorkingDir(dir string) error {
	workingDir, err := os.Getwd()
	if err != nil {
		return err
	}

	toDelete := path.Join(workingDir, dir)
	if _, err := os.Stat(dir); err == nil {
		err = os.RemoveAll(toDelete)
		if err != nil {
			return err
		}
	} else if !os.IsNotExist(err) {
		return err
	}

	return nil
}

func createIfNotExists(dir string) error {
	if _, err := os.Stat(dir); err != nil && os.IsNotExist(err) {
		if err := os.Mkdir(dir, 0755); err != nil {
			return err
		}
	}

	return nil
}

func writeReport(report string, fileName string, outputDir string) error {
	if err := createIfNotExists(outputDir); err != nil {
		return err
	}

	reportFile, err := os.Create(path.Join(outputDir, fileName))
	if err != nil {
		return err
	}

	if _, err := reportFile.Write([]byte(report)); err != nil {
		return err
	}

	return nil
}
