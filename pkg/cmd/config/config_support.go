/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import (
	"os"
	"strings"

	"gopkg.in/yaml.v2"
)

func NewWithDefaults() *RunConfig {
	ns := NamespaceConfig{
		AutoRemove: true,
		Temporary:  false,
	}

	var config = Config{
		Recursive: true,
		Namespace: ns,
		Timeout:   DefaultTimeout,
		Dump: DumpConfig{
			Enabled:    false,
			Append:     false,
			FailedOnly: true,
			Directory:  "_output",
		},
	}
	return &RunConfig{Config: config, BaseDir: ""}
}

func LoadConfig(file string) (*RunConfig, error) {
	config := NewWithDefaults()
	data, err := os.ReadFile(file)
	if err != nil && os.IsNotExist(err) {
		return config, nil
	}
	if err = yaml.Unmarshal(data, config); err != nil {
		return nil, err
	}
	return config, nil
}

func (d DependencyConfig) AsMavenGAV() string {
	version := d.Version

	if strings.HasPrefix(version, "@") && strings.HasSuffix(version, "@") {
		if defaultVersion, ok := DefaultVersions[version[1:len(version)-1]]; ok {
			version = defaultVersion
		}
	}

	return strings.Join([]string{d.GroupID, d.ArtifactID, version}, ":")
}
