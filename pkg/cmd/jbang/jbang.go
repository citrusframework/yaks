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

package jbang

import (
	"fmt"
	"strings"

	"github.com/citrusframework/yaks/pkg/cmd/config"
	"github.com/citrusframework/yaks/pkg/util/defaults"
)

var YaksApp = "yaks@citrusframework/yaks"
var YaksVersion = defaults.Version

func AddDependencies(args []string, runConfig *config.RunConfig, dependencies ...string) []string {
	deps := make([]string, 0)
	for _, d := range runConfig.Config.Runtime.Settings.Dependencies {
		deps = append(deps, d.AsMavenGAV())
	}

	deps = append(deps, dependencies...)

	if len(deps) > 0 {
		resolved := make([]string, len(deps))
		for i, d := range deps {
			if strings.Contains(d, "@") {
				for key, version := range config.DefaultVersions {
					placeholder := fmt.Sprintf("@%s@", key)
					if strings.Contains(d, placeholder) {
						resolved[i] = strings.Replace(d, placeholder, version, 1)
					}
				}
			} else {
				resolved[i] = d
			}

		}
		return append(args, fmt.Sprintf("--deps=%s", strings.Join(resolved, ",")))
	}

	return args
}

func AddRepositories(args []string, runConfig *config.RunConfig, repositories ...string) []string {
	repos := make([]string, 0)
	for _, r := range runConfig.Config.Runtime.Settings.Repositories {
		repos = append(repos, r.URL)
	}

	repos = append(repos, repositories...)

	if len(repos) > 0 {
		return append(args, fmt.Sprintf("--repos=%s", strings.Join(repos, ",")))
	}

	return args
}

func AddOptions(args []string, runConfig *config.RunConfig) []string {
	for _, logger := range runConfig.Config.Runtime.Settings.Loggers {
		if logger.Name == "root" {
			args = append(args, fmt.Sprintf("--logging-level=%s", logger.Level))
		}
	}
	return args
}
