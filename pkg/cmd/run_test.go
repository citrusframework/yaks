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
package cmd

import (
	"fmt"
	"os"
	r "runtime"
	"testing"
	"time"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/cmd/config"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var timeout, _ = time.ParseDuration("10s")

func TestStepOsCheck(t *testing.T) {
	steps := []config.StepConfig{
		{
			Name: "os-check-ok",
			Run:  "echo Should run",
			If:   fmt.Sprintf("os=%s", r.GOOS),
		},
		{
			Name: "os-check-fail",
			Run:  "echo Should not run &fail",
			If:   "os=foo",
		},
	}

	var scriptError error
	saveErr := func(err error) {
		scriptError = err
	}
	runSteps(steps, "default", "", &v1alpha1.TestResults{}, timeout, saveErr)

	require.NoError(t, scriptError)
}

func TestStepEnvCheck(t *testing.T) {
	err := os.Setenv("foo", "bar")
	require.NoError(t, err)

	steps := []config.StepConfig{
		{
			Name: "env-check-ok",
			Run:  "echo Should run",
			If:   "env:foo",
		},
		{
			Name: "env-value-check-ok",
			Run:  "echo Should run",
			If:   "env:foo=bar",
		},
		{
			Name: "env-check-fail",
			Run:  "echo Should not run &fail",
			If:   "env:wrong",
		},
		{
			Name: "env-value-check-fail",
			Run:  "echo Should not run &fail",
			If:   "env:foo=wrong",
		},
	}

	var scriptError error
	saveErr := func(err error) {
		scriptError = err
	}
	runSteps(steps, "default", "", &v1alpha1.TestResults{}, timeout, saveErr)

	require.NoError(t, scriptError)
}

func TestStepCheckCombinations(t *testing.T) {
	err := os.Setenv("foo", "bar")
	require.NoError(t, err)

	steps := []config.StepConfig{
		{
			Name: "env-check-ok",
			Run:  "echo Should run",
			If:   fmt.Sprintf("env:foo && os=%s", r.GOOS),
		},
		{
			Name: "env-value-check-ok",
			Run:  "echo Should run",
			If:   fmt.Sprintf("env:foo=bar && os=%s", r.GOOS),
		},
		{
			Name: "env-check-fail",
			Run:  "echo Should not run &fail",
			If:   "env:foo && os=foo",
		},
		{
			Name: "env-value-check-fail",
			Run:  "echo Should not run &fail",
			If:   fmt.Sprintf("os=%s && env:foo=wrong", r.GOOS),
		},
	}

	var scriptError error
	saveErr := func(err error) {
		scriptError = err
	}
	runSteps(steps, "default", "", &v1alpha1.TestResults{}, timeout, saveErr)

	require.NoError(t, scriptError)
}

func TestResolveScriptFileName(t *testing.T) {
	assert.Equal(t, "pre.sh", resolve("pre.sh"))
	assert.Equal(t, fmt.Sprintf("pre-%s.sh", r.GOOS), resolve("pre-{{os.type}}.sh"))
	assert.Equal(t, fmt.Sprintf("pre-%s-%s.sh", r.GOOS, r.GOARCH), resolve("pre-{{os.type}}-{{os.arch}}.sh"))
}

func TestStepOnFailure(t *testing.T) {
	steps := []config.StepConfig{
		{
			Name: "failure-check-ok",
			Run:  "echo Should run",
			If:   fmt.Sprintf("os=%s", r.GOOS),
		},
		{
			Name: "failure-check-fail",
			Run:  "echo Should not run &fail",
			If:   "failure()",
		},
	}

	var scriptError error
	saveErr := func(err error) {
		scriptError = err
	}
	runSteps(steps, "default", "", &v1alpha1.TestResults{}, timeout, saveErr)

	require.NoError(t, scriptError)
}
