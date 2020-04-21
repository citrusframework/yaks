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

package cmd

import (
	"context"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"os/exec"
	"path"
	"regexp"
	"strings"
	"text/template"
	"time"

	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/cmd/config"
	"github.com/citrusframework/yaks/pkg/cmd/report"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"
	"github.com/citrusframework/yaks/pkg/util/openshift"
	"github.com/fatih/color"
	"github.com/google/uuid"
	projectv1 "github.com/openshift/api/project/v1"
	"github.com/pkg/errors"
	"github.com/spf13/cobra"
	"github.com/wercker/stern/stern"
	corev1 "k8s.io/api/core/v1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	k8sclient "sigs.k8s.io/controller-runtime/pkg/client"
)

const (
	FileSuffix = ".feature"
	ConfigFile = "yaks-config.yaml"
)

const (
	CucumberOptions    = "CUCUMBER_OPTIONS"
	CucumberGlue       = "CUCUMBER_GLUE"
	CucumberFeatures   = "CUCUMBER_FEATURES"
	CucumberFilterTags = "CUCUMBER_FILTER_TAGS"
)

func newCmdTest(rootCmdOptions *RootCmdOptions) *cobra.Command {
	options := testCmdOptions{
		RootCmdOptions: rootCmdOptions,
	}

	cmd := cobra.Command{
		PersistentPreRunE: options.preRun,
		Use:               "test [options] [test file to execute]",
		Short:             "Execute a test on Kubernetes",
		Long:              `Deploys and execute a pod on Kubernetes for running tests.`,
		PreRunE:           options.validateArgs,
		RunE:              options.run,
		SilenceUsage:      true,
	}

	cmd.Flags().StringArrayVarP(&options.dependencies, "dependency", "d", nil, "Adds runtime dependencies that get automatically loaded before the test is executed.")
	cmd.Flags().StringArrayVarP(&options.uploads, "upload", "u", nil, "Upload a given library to the cluster to allow it to be used by tests.")
	cmd.Flags().StringVarP(&options.settings, "settings", "s", "", "Path to runtime settings file. File content is added to the test runtime and can hold runtime dependency information for instance.")
	cmd.Flags().StringArrayVarP(&options.env, "env", "e", nil, "Set an environment variable in the integration container. E.g \"-e MY_VAR=my-value\"")
	cmd.Flags().StringArrayVarP(&options.tags, "tag", "t", nil, "Specify a tag filter to only run tests that match given tag expression")
	cmd.Flags().StringArrayVarP(&options.features, "feature", "f", nil, "Feature file to include in the test run")
	cmd.Flags().StringArrayVarP(&options.glue, "glue", "g", nil, "Additional glue path to be added in the Cucumber runtime options")
	cmd.Flags().StringVarP(&options.options, "options", "o", "", "Cucumber runtime options")
	cmd.Flags().VarP(&options.report, "report", "r", "Create test report in given output format")

	return &cmd
}

type testCmdOptions struct {
	*RootCmdOptions
	dependencies []string
	uploads      []string
	settings     string
	env          []string
	tags         []string
	features     []string
	glue		 []string
	options		 string
	report		 report.OutputFormat
}

func (o *testCmdOptions) validateArgs(_ *cobra.Command, args []string) error {
	if len(args) != 1 {
		return errors.New(fmt.Sprintf("accepts exactly 1 test name to execute, received %d", len(args)))
	}

	return nil
}

func (o *testCmdOptions) run(_ *cobra.Command, args []string) error {
	var err error
	source := args[0]

	results := v1alpha1.TestResults{}
	defer report.PrintSummaryReport(&results)
	if o.report != report.DefaultOutput && o.report != report.SummaryOutput {
		defer report.GenerateReport(&results, o.report)
	}

	if isDir(source) {
		err = o.runTestGroup(source, &results)
		if err == nil && len(results.Errors) > 0 {
			err = errors.New("There are test failures!")
		}
	} else {
		err = o.runTest(source, &results)
	}

	return err
}

func (o *testCmdOptions) runTest(source string, results *v1alpha1.TestResults) error {
	c, err := o.GetCmdClient()
	if err != nil {
		return err
	}

	var runConfig *config.RunConfig
	if runConfig, err = o.getRunConfig(source); err != nil {
		return err
	}

	testNamespace := runConfig.Config.Namespace.Name
	if runConfig.Config.Namespace.Temporary {
		if namespace, err := o.createTempNamespace(runConfig, c); err != nil {
			return err
		} else if namespace != nil && runConfig.Config.Namespace.AutoRemove {
			testNamespace = namespace.GetName()
			defer deleteTempNamespace(namespace, c, o.Context)
		}
	}

	if err = o.uploadArtifacts(runConfig); err != nil {
		return err
	}

	baseDir := getBaseDir(source)
	defer runSteps(runConfig.Post, testNamespace, baseDir)
	if err = runSteps(runConfig.Pre, testNamespace, baseDir); err != nil {
		return err
	}

	var test *v1alpha1.Test
	test, err = o.createAndRunTest(c, source, runConfig)
	if test != nil {
		report.AppendTestResults(results, test.Status.Results)

		if saveErr := report.SaveTestResults(test); saveErr != nil {
			fmt.Printf("Failed to save test results: %s", saveErr.Error())
		}
	}
	return err
}

func (o *testCmdOptions) runTestGroup(source string, results *v1alpha1.TestResults) error {
	c, err := o.GetCmdClient()
	if err != nil {
		return err
	}

	var runConfig *config.RunConfig
	if runConfig, err = o.getRunConfig(source); err != nil {
		return err
	}

	var testNamespace = runConfig.Config.Namespace.Name
	if runConfig.Config.Namespace.Temporary {
		if namespace, err := o.createTempNamespace(runConfig, c); err != nil {
			return err
		} else if namespace != nil && runConfig.Config.Namespace.AutoRemove {
			testNamespace = namespace.GetName()
			defer deleteTempNamespace(namespace, c, o.Context)
		}
	}

	if err = o.uploadArtifacts(runConfig); err != nil {
		return err
	}

	var files []os.FileInfo
	if files, err = ioutil.ReadDir(source); err != nil {
		return err
	}

	baseDir := getBaseDir(source)
	defer runSteps(runConfig.Post, testNamespace, baseDir)
	if err = runSteps(runConfig.Pre, testNamespace, baseDir); err != nil {
		return err
	}

	suiteErrors := make([]string, 0)
	for _, f := range files {
		name := path.Join(source, f.Name())
		if f.IsDir() && runConfig.Config.Recursive {
			groupError := o.runTestGroup(name, results)
			if groupError != nil {
				suiteErrors = append(suiteErrors, groupError.Error())
			}
		} else if strings.HasSuffix(f.Name(), FileSuffix) {
			var test *v1alpha1.Test
			var testError error
			test, testError = o.createAndRunTest(c, name, runConfig)
			if test != nil {
				report.AppendTestResults(results, test.Status.Results)

				if saveErr := report.SaveTestResults(test); saveErr != nil {
					fmt.Printf("Failed to save test results: %s", saveErr.Error())
				}
			}

			if testError != nil {
				suiteErrors = append(suiteErrors, testError.Error())
			}
		}
	}

	if len(suiteErrors) > 0 {
        results.Errors = append(results.Errors, suiteErrors...)
	}

	return nil
}

func getBaseDir(source string) string {
	if isRemoteFile(source) {
		return ""
	}

	if isDir(source) {
		return source
	} else {
		dir, _ := path.Split(source)
		return dir
	}
}

func (o *testCmdOptions) getRunConfig(source string) (*config.RunConfig, error) {
	var configFile string
	var runConfig *config.RunConfig

	if isRemoteFile(source) {
		return config.NewWithDefaults(), nil
	}

	if isDir(source) {
		// search for config file in given directory
		configFile = path.Join(source, ConfigFile)
	} else {
		// search for config file in same directory as given file
		dir, _ := path.Split(source)
		configFile = path.Join(dir, ConfigFile)
	}

	runConfig, err := config.LoadConfig(configFile)
	if err != nil {
		return nil, err
	}

	if runConfig.Config.Namespace.Name == "" && !runConfig.Config.Namespace.Temporary {
		runConfig.Config.Namespace.Name = o.Namespace
	}

	return runConfig, nil
}

func (o *testCmdOptions) createTempNamespace(runConfig *config.RunConfig, c client.Client) (metav1.Object, error) {
	namespaceName := "yaks-" + uuid.New().String()
	namespace, err := initializeTempNamespace(namespaceName, c, o.Context)
	if err != nil {
		return nil, err
	}
	runConfig.Config.Namespace.Name = namespaceName

	cmdOptionsCopy := o.RootCmdOptions
	cmdOptionsCopy.Namespace = namespaceName
	if err := newCmdInstall(cmdOptionsCopy).Execute(); err != nil {
		return namespace, err
	}

	return namespace, nil
}

func (o *testCmdOptions) createAndRunTest(c client.Client, rawName string, runConfig *config.RunConfig) (*v1alpha1.Test, error) {
	namespace := runConfig.Config.Namespace.Name
	fileName := kubernetes.SanitizeFileName(rawName)
	name := kubernetes.SanitizeName(rawName)

	if name == "" {
		return nil, errors.New("unable to determine test name")
	}

	data, err := o.loadData(rawName)
	if err != nil {
		return nil, err
	}

	test := v1alpha1.Test{
		TypeMeta: metav1.TypeMeta{
			Kind:       v1alpha1.TestKind,
			APIVersion: v1alpha1.SchemeGroupVersion.String(),
		},
		ObjectMeta: metav1.ObjectMeta{
			Namespace: namespace,
			Name:      name,
		},
		Spec: v1alpha1.TestSpec{
			Source: v1alpha1.SourceSpec{
				Name:     fileName,
				Content:  data,
				Language: v1alpha1.LanguageGherkin,
			},
		},
	}

	settings, err := o.newSettings()

	if err != nil {
		return nil, err
	} else if settings != nil {
		test.Spec.Settings = *settings
	} else if len(o.dependencies) > 0 {
		test.Spec.Settings = v1alpha1.SettingsSpec{
			Name:    "",
			Content: strings.Join(o.dependencies, ","),
		}
	}

	if err := o.setupEnvSettings(&test, runConfig); err != nil {
		return nil, err
	}

	existed := false
	err = c.Create(o.Context, &test)
	if err != nil && k8serrors.IsAlreadyExists(err) {
		existed = true
		clone := test.DeepCopy()
		var key k8sclient.ObjectKey
		key, err = k8sclient.ObjectKeyFromObject(clone)
		if err != nil {
			return nil, err
		}
		err = c.Get(o.Context, key, clone)
		if err != nil {
			return nil, err
		}
		test.ResourceVersion = clone.ResourceVersion
		err = c.Update(o.Context, &test)
		if err != nil {
			return nil, err
		}
		// Reset status as well
		test.Status = v1alpha1.TestStatus{}
		err = c.Status().Update(o.Context, &test)
	}

	if err != nil {
		return nil, err
	}

	if !existed {
		fmt.Printf("test \"%s\" created\n", name)
	} else {
		fmt.Printf("test \"%s\" updated\n", name)
	}

	ctx, cancel := context.WithCancel(o.Context)
	var status v1alpha1.TestPhase = "Unknown"
	go func() {
		err = kubernetes.WaitCondition(o.Context, c, &test, func(obj interface{}) (bool, error) {
			if val, ok := obj.(*v1alpha1.Test); ok {
				if val.Status.Phase == v1alpha1.TestPhaseDeleting ||
					val.Status.Phase == v1alpha1.TestPhaseError ||
					val.Status.Phase == v1alpha1.TestPhasePassed ||
					val.Status.Phase == v1alpha1.TestPhaseFailed {
					status = val.Status.Phase
					return true, nil
				}
			}
			return false, nil
		}, 10*time.Minute)

		cancel()
	}()

	if err := o.printLogs(ctx, name, runConfig); err != nil {
		return nil, err
	}

	fmt.Printf("Test %s\n", string(status))
	return &test, status.AsError()
}

func (o *testCmdOptions) uploadArtifacts(runConfig *config.RunConfig) error {
	for _, lib := range o.uploads {
		additionalDep, err := uploadLocalArtifact(o.RootCmdOptions, lib, runConfig.Config.Namespace.Name)
		if err != nil {
			return err
		}
		o.dependencies = append(o.dependencies, additionalDep)
	}
	return nil
}

func (o *testCmdOptions) setupEnvSettings(test *v1alpha1.Test, runConfig *config.RunConfig) error {
	env := make([]string, 0)

	if o.tags != nil {
		env = append(env, CucumberFilterTags+"="+strings.Join(o.tags, ","))
	} else if len(runConfig.Config.Runtime.Cucumber.Tags) > 0 {
		env = append(env, CucumberFilterTags+"="+strings.Join(runConfig.Config.Runtime.Cucumber.Tags, ","))
	}

	if o.features != nil {
		env = append(env, CucumberFeatures+"="+strings.Join(o.features, ","))
	}

	if o.glue != nil {
		env = append(env, CucumberGlue+"="+strings.Join(o.glue, ","))
	} else if len(runConfig.Config.Runtime.Cucumber.Glue) > 0 {
		env = append(env, CucumberGlue+"="+strings.Join(runConfig.Config.Runtime.Cucumber.Glue, ","))
	}

	if len(o.options) > 0 {
		env = append(env, CucumberOptions+"="+o.options)
	} else if len(runConfig.Config.Runtime.Cucumber.Options) > 0 {
		env = append(env, CucumberOptions+"="+runConfig.Config.Runtime.Cucumber.Options)
	}

	if o.env != nil {
		copy(env, o.env)
	}

	if len(env) > 0 {
		test.Spec.Env = env
	}

	return nil
}

func (o *testCmdOptions) newSettings() (*v1alpha1.SettingsSpec, error) {
	runtimeDependencies := o.dependencies

	if len(runtimeDependencies) > 0 {
		settings := v1alpha1.SettingsSpec{
			Content: strings.Join(runtimeDependencies, ","),
		}
		return &settings, nil
	}

	if o.settings == "" {
		return nil, nil
	}

	rawName := o.settings
	settingsFileName := kubernetes.SanitizeFileName(rawName)
	configData, err := o.loadData(rawName)

	if err != nil {
		return nil, err
	}

	settings := v1alpha1.SettingsSpec{
		Name:    settingsFileName,
		Content: configData,
	}

	return &settings, nil
}

func (o *testCmdOptions) printLogs(ctx context.Context, name string, runConfig *config.RunConfig) error {
	t := "{{color .PodColor .PodName}} {{color .ContainerColor .ContainerName}} {{.Message}}"
	funs := map[string]interface{}{
		"color": func(color color.Color, text string) string {
			return color.SprintFunc()(text)
		},
	}
	templ, err := template.New("log").Funcs(funs).Parse(t)
	if err != nil {
		return err
	}

	//tail := int64(100)
	conf := stern.Config{
		Namespace:  runConfig.Config.Namespace.Name,
		PodQuery:   regexp.MustCompile(".*"),
		KubeConfig: client.GetValidKubeConfig(o.KubeConfig),
		//TailLines: &tail,
		ContainerQuery: regexp.MustCompile(".*"),
		LabelSelector:  labels.SelectorFromSet(labels.Set{"org.citrusframework.yaks/test": name}),
		//LabelSelector: labels.SelectorFromSet(labels.Set{"name": "yaks"}),
		ContainerState: stern.ContainerState(stern.RUNNING),
		Since:          172800000000000,
		Template:       templ,
	}
	if err := stern.Run(ctx, &conf); err != nil {
		return err
	}
	return nil
}

func isRemoteFile(fileName string) bool {
	return strings.HasPrefix(fileName, "http://") || strings.HasPrefix(fileName, "https://")
}

func isDir(fileName string) bool {
	if isRemoteFile(fileName) {
		return false
	}

	if info, err := os.Stat(fileName); err == nil {
		return info.IsDir()
	}

	return false
}

func runSteps(steps []config.StepConfig, namespace, baseDir string) error {
	for idx, step := range steps {
		if len(step.Script) > 0 {
			var scriptFile string

			if len(baseDir) > 0 && !path.IsAbs(step.Script) {
				scriptFile = path.Join(baseDir, step.Script)
			} else {
				scriptFile = step.Script
			}

			if err := runScript(scriptFile, fmt.Sprintf("script %s", scriptFile), namespace, baseDir); err != nil {
				return err
			}
		}

		if len(step.Run) > 0 {
			// Let's save it to a bash script to allow for multiline scripts
			file, err := ioutil.TempFile("", "yaks-script-*.sh")
			if err != nil {
				return err
			}
			defer os.Remove(file.Name())

			_, err = file.WriteString("#!/bin/bash\n\n")
			if err != nil {
				return err
			}

			_, err = file.WriteString(step.Run)
			if err != nil {
				return err
			}

			if err = file.Close(); err != nil {
				return err
			}

			// Make it executable
			if err = os.Chmod(file.Name(), 0777); err != nil {
				return err
			}

			if err := runScript(file.Name(), fmt.Sprintf("inline command %d", idx), namespace, baseDir); err != nil {
				return err
			}
		}
	}

	return nil
}

func runScript(scriptFile, desc, namespace, baseDir string) error {
	command := exec.Command(scriptFile)

	command.Env = os.Environ()
	command.Env = append(command.Env, fmt.Sprintf("YAKS_NAMESPACE=%s", namespace))

	command.Dir = baseDir

	if out, err := command.Output(); err == nil {
		fmt.Printf("Running %s: \n%s\n", desc, out)
	} else {
		fmt.Printf("Failed to run %s: \n%v\n", desc, err)
		return err
	}
	return nil
}

func initializeTempNamespace(name string, c client.Client, context context.Context) (metav1.Object, error) {
	var obj runtime.Object

	if oc, err := openshift.IsOpenShift(c); err != nil {
		panic(err)
	} else if oc {
		scheme := c.GetScheme()
		projectv1.AddToScheme(scheme)

		obj = &projectv1.ProjectRequest{
			TypeMeta: metav1.TypeMeta{
				APIVersion: projectv1.GroupVersion.String(),
				Kind:       "ProjectRequest",
			},
			ObjectMeta: metav1.ObjectMeta{
				Name: name,
			},
		}
	} else {
		obj = &corev1.Namespace{
			TypeMeta: metav1.TypeMeta{
				APIVersion: "v1",
				Kind:       "Namespace",
			},
			ObjectMeta: metav1.ObjectMeta{
				Name: name,
			},
		}
	}
	fmt.Printf("Creating new test namespace %s\n", name)
	err := c.Create(context, obj)
	return obj.(metav1.Object), err
}

func deleteTempNamespace(ns metav1.Object, c client.Client, context context.Context) {
	if oc, err := openshift.IsOpenShift(c); err != nil {
		panic(err)
	} else if oc {
		prj := &projectv1.Project{
			TypeMeta: metav1.TypeMeta{
				APIVersion: projectv1.GroupVersion.String(),
				Kind:       "Project",
			},
			ObjectMeta: metav1.ObjectMeta{
				Name: ns.GetName(),
			},
		}
		if err = c.Delete(context, prj); err != nil {
			fmt.Fprintf(os.Stderr, "WARN: Failed to AutoRemove namespace %s\n", ns.GetName())
		}
	} else {
		if err = c.Delete(context, ns.(runtime.Object)); err != nil {
			fmt.Fprintf(os.Stderr, "WARN: Failed to AutoRemove namespace %s\n", ns.GetName())
		}
	}
	fmt.Printf("AutoRemove namespace %s\n", ns.GetName())
}

func (*testCmdOptions) loadData(fileName string) (string, error) {
	var content []byte
	var err error

	if isRemoteFile(fileName) {
		/* #nosec */
		resp, err := http.Get(fileName)
		if err != nil {
			return "", err
		}
		defer resp.Body.Close()

		content, err = ioutil.ReadAll(resp.Body)
		if err != nil {
			return "", err
		}
	} else {
		content, err = ioutil.ReadFile(fileName)
		if err != nil {
			return "", err
		}
	}

	return string(content), nil
}
