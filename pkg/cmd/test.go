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
	"github.com/citrusframework/yaks/pkg/install"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"net/http"
	"os"
	"os/exec"
	"path"
	"regexp"
	r "runtime"
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
	NamespaceEnv       = "YAKS_NAMESPACE"
	RepositoriesEnv    = "YAKS_REPOSITORIES"
	DependenciesEnv    = "YAKS_DEPENDENCIES"
	LoggersEnv         = "YAKS_LOGGERS"

	CucumberOptions    = "CUCUMBER_OPTIONS"
	CucumberGlue       = "CUCUMBER_GLUE"
	CucumberFeatures   = "CUCUMBER_FEATURES"
	CucumberFilterTags = "CUCUMBER_FILTER_TAGS"

	DefaultStepTimeout = "30m"
)

func newCmdTest(rootCmdOptions *RootCmdOptions) (*cobra.Command, *testCmdOptions) {
	options := testCmdOptions{
		RootCmdOptions: rootCmdOptions,
	}

	cmd := cobra.Command{
		Use:             "test [options] [test file to execute]",
		Short:           "Execute a test on Kubernetes",
		Long:            `Deploys and executes a test on Kubernetes.`,
		Args:            options.validateArgs,
		PreRunE:         decode(&options),
		RunE:            options.run,
	}

	cmd.Flags().StringArray("maven-repository", nil, "Adds custom Maven repository URL that is added to the runtime.")
	cmd.Flags().StringArrayP("logger", "l", nil, "Adds logger configuration setting log levels.")
	cmd.Flags().StringArrayP("dependency", "d", nil, "Adds runtime dependencies that get automatically loaded before the test is executed.")
	cmd.Flags().StringArrayP("upload", "u", nil, "Upload a given library to the cluster to allow it to be used by tests.")
	cmd.Flags().StringP("settings", "s", "", "Path to runtime settings file. File content is added to the test runtime and can hold runtime dependency information for instance.")
	cmd.Flags().StringArrayP("env", "e", nil, "Set an environment variable in the integration container. E.g \"-e MY_VAR=my-value\"")
	cmd.Flags().StringArrayP("tag", "t", nil, "Specify a tag filter to only run tests that match given tag expression")
	cmd.Flags().StringArrayP("feature", "f", nil, "Feature file to include in the test run")
	cmd.Flags().StringArray("resource", nil, "Add a resource")
	cmd.Flags().StringArray("property-file", nil, "Bind a property file to the test. E.g. \"--property-file test.properties\"")
	cmd.Flags().StringArrayP("glue", "g", nil, "Additional glue path to be added in the Cucumber runtime options")
	cmd.Flags().StringP("options", "o", "", "Cucumber runtime options")
	cmd.Flags().String("dump", "", "Dump output format. One of: json|yaml. If set the test CR is created and printed to the CLI output instead of running the test.")
	cmd.Flags().StringP("report", "r", "junit", "Create test report in given output format")

	return &cmd, &options
}

type testCmdOptions struct {
	*RootCmdOptions
	Repositories  []string `mapstructure:"maven-repository"`
	Dependencies  []string `mapstructure:"dependency"`
	Logger        []string `mapstructure:"logger"`
	Uploads       []string `mapstructure:"upload"`
	Settings      string `mapstructure:"settings"`
	Env           []string `mapstructure:"env"`
	Tags          []string `mapstructure:"tag"`
	Features      []string `mapstructure:"feature"`
	Resources     []string `mapstructure:"resources"`
	PropertyFiles []string `mapstructure:"property-files"`
	Glue          []string `mapstructure:"glue"`
	Options       string `mapstructure:"options"`
	DumpFormat    string `mapstructure:"dump"`
	ReportFormat report.OutputFormat `mapstructure:"report"`
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
	if o.ReportFormat != report.DefaultOutput && o.ReportFormat != report.SummaryOutput {
		defer report.GenerateReport(&results, o.ReportFormat)
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

	if runConfig.Config.Namespace.Temporary {
		if namespace, err := o.createTempNamespace(runConfig, c); namespace != nil {
			if runConfig.Config.Namespace.AutoRemove {
				defer deleteTempNamespace(namespace, c, o.Context)
			}

			if err != nil {
				return err
			}
		} else if err != nil {
			return err
		}
	}

	if err = o.uploadArtifacts(runConfig); err != nil {
		return err
	}

	defer runSteps(runConfig.Post, runConfig.Config.Namespace.Name, runConfig.BaseDir)
	if err = runSteps(runConfig.Pre, runConfig.Config.Namespace.Name, runConfig.BaseDir); err != nil {
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

	if runConfig.Config.Namespace.Temporary {
		if namespace, err := o.createTempNamespace(runConfig, c); err != nil {
			return err
		} else if namespace != nil && runConfig.Config.Namespace.AutoRemove {
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

	defer runSteps(runConfig.Post, runConfig.Config.Namespace.Name, runConfig.BaseDir)
	if err = runSteps(runConfig.Pre, runConfig.Config.Namespace.Name, runConfig.BaseDir); err != nil {
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

	if runConfig.BaseDir == "" {
		runConfig.BaseDir = getBaseDir(source)
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

	// looking for operator in current namespace
	operator, err := o.findOperator(c, o.Namespace)
	if err != nil && k8serrors.IsNotFound(err) {
		// looking for operator in cluster-wide operator namespace
		if operatorNamespace, namespaceErr := getDefaultOperatorNamespace(c, runConfig); namespaceErr == nil {
			operator, err = o.findOperator(c, operatorNamespace)
			if err != nil {
				return namespace, err
			}
		} else {
			return namespace, namespaceErr
		}
	} else if err != nil {
		return namespace, err
	}

	if isOperatorGlobal(operator) {
		// Using global operator to manage temporary namespaces, no action required
		return namespace, nil
	}

	// no operator or non-global operator found, deploy into temp namespace
	// Let's use a client provider during cluster installation, to eliminate the problem of CRD object caching
	clientProvider := client.Provider{Get: o.NewCmdClient}

	if err := setupCluster(o.Context, clientProvider, nil); err != nil {
		return namespace, err
	}

	if err := o.setupOperator(c, namespaceName); err != nil {
		return namespace, err
	}

	return namespace, nil
}

func (o *testCmdOptions) setupOperator(c client.Client, namespace string) error {
	var cluster v1alpha1.ClusterType
	if isOpenshift, err := openshift.IsOpenShift(c); err != nil {
		return err;
	} else if isOpenshift {
		cluster = v1alpha1.ClusterTypeKubernetes
	} else {
		cluster = v1alpha1.ClusterTypeKubernetes
	}

	cfg := install.OperatorConfiguration{
		CustomImage:           "",
		CustomImagePullPolicy: "",
		Namespace:             namespace,
		Global:                false,
		ClusterType:           string(cluster),
	}
	err := install.OperatorOrCollect(o.Context, c, cfg, nil, true)

	return err
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

	for _, resource := range runConfig.Config.Runtime.Resources {
		data, err := o.loadData(path.Join(runConfig.BaseDir, resource))
		if err != nil {
			return nil, err
		}

		test.Spec.Resources = append(test.Spec.Resources, v1alpha1.ResourceSpec{
			Name:    path.Base(resource),
			Content: data,
		})
	}

	for _, resource := range o.Resources {
		data, err := o.loadData(path.Join(runConfig.BaseDir, resource))
		if err != nil {
			return nil, err
		}

		test.Spec.Resources = append(test.Spec.Resources, v1alpha1.ResourceSpec{
			Name:    path.Base(resource),
			Content: data,
		})
	}

	for _, propertyFile := range o.PropertyFiles {
		data, err := o.loadData(path.Join(runConfig.BaseDir, propertyFile))
		if err != nil {
			return nil, err
		}

		test.Spec.Resources = append(test.Spec.Resources, v1alpha1.ResourceSpec{
			Name:    path.Base(propertyFile),
			Content: data,
		})
	}

	if settings, err := o.newSettings(runConfig); err != nil {
		return nil, err
	} else if settings != nil {
		test.Spec.Settings = *settings
	}

	if err := o.setupEnvSettings(&test, runConfig); err != nil {
		return nil, err
	}

	if runConfig.Config.Runtime.Secret != "" {
		test.Spec.Secret = runConfig.Config.Runtime.Secret;
	}

	switch o.DumpFormat {
		case "":
			// continue..
		case "yaml":
			data, err := kubernetes.ToYAML(&test)
			if err != nil {
				return nil, err
			}
			fmt.Print(string(data))
			return nil, nil

		case "json":
			data, err := kubernetes.ToJSON(&test)
			if err != nil {
				return nil, err
			}
			fmt.Print(string(data))
			return nil, nil

		default:
			return nil, fmt.Errorf("invalid dump output format option '%s', should be one of: yaml|json", o.DumpFormat)
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
		// Hold the resource from the operator controller
		clone.Status.Phase = v1alpha1.TestPhaseUpdating
		err = c.Status().Update(o.Context, clone)
		if err != nil {
			return nil, err
		}
		// Update the spec
		test.ResourceVersion = clone.ResourceVersion
		err = c.Update(o.Context, &test)
		if err != nil {
			return nil, err
		}
		// Reset status
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
		}, 30*time.Minute)

		cancel()
	}()

	if err := o.printLogs(ctx, name, runConfig); err != nil {
		return nil, err
	}

	fmt.Printf("Test %s\n", string(status))
	return &test, status.AsError()
}

func (o *testCmdOptions) uploadArtifacts(runConfig *config.RunConfig) error {
	for _, lib := range o.Uploads {
		additionalDep, err := uploadLocalArtifact(o.RootCmdOptions, lib, runConfig.Config.Namespace.Name)
		if err != nil {
			return err
		}
		o.Dependencies = append(o.Dependencies, additionalDep)
	}
	return nil
}

func (o *testCmdOptions) setupEnvSettings(test *v1alpha1.Test, runConfig *config.RunConfig) error {
	env := make([]string, 0)

	env = append(env, NamespaceEnv+"="+runConfig.Config.Namespace.Name)

	if o.Tags != nil {
		env = append(env, CucumberFilterTags+"="+strings.Join(o.Tags, ","))
	} else if len(runConfig.Config.Runtime.Cucumber.Tags) > 0 {
		env = append(env, CucumberFilterTags+"="+strings.Join(runConfig.Config.Runtime.Cucumber.Tags, ","))
	}

	if o.Features != nil {
		env = append(env, CucumberFeatures+"="+strings.Join(o.Features, ","))
	}

	if o.Glue != nil {
		env = append(env, CucumberGlue+"="+strings.Join(o.Glue, ","))
	} else if len(runConfig.Config.Runtime.Cucumber.Glue) > 0 {
		env = append(env, CucumberGlue+"="+strings.Join(runConfig.Config.Runtime.Cucumber.Glue, ","))
	}

	if len(o.Options) > 0 {
		env = append(env, CucumberOptions+"="+o.Options)
	} else if len(runConfig.Config.Runtime.Cucumber.Options) > 0 {
		env = append(env, CucumberOptions+"="+runConfig.Config.Runtime.Cucumber.Options)
	}

	if len(o.Repositories) > 0 {
		env = append(env, RepositoriesEnv+"="+strings.Join(o.Repositories, ","))
	}

	if len(o.Dependencies) > 0 {
		env = append(env, DependenciesEnv+"="+strings.Join(o.Dependencies, ","))
	}

	if len(o.Logger) > 0 {
		env = append(env, LoggersEnv+"="+strings.Join(o.Logger, ","))
	}

	for _, envConfig := range runConfig.Config.Runtime.Env {
		env = append(env, envConfig.Name+"="+envConfig.Value)
	}

	if o.Env != nil {
		env = append(env, o.Env...)
	}

	if len(env) > 0 {
		test.Spec.Env = env
	}

	return nil
}

func (o *testCmdOptions) newSettings(runConfig *config.RunConfig) (*v1alpha1.SettingsSpec, error) {
	if o.Settings != "" {
		rawName := o.Settings
		configData, err := o.loadData(path.Join(runConfig.BaseDir, rawName))

		if err != nil {
			return nil, err
		}

		settings := v1alpha1.SettingsSpec {
			Name:    kubernetes.SanitizeFileName(rawName),
			Content: configData,
		}

		return &settings, nil
	}

	if len(runConfig.Config.Runtime.Settings.Dependencies) > 0 ||
		len(runConfig.Config.Runtime.Settings.Repositories) > 0 ||
		len(runConfig.Config.Runtime.Settings.Loggers) > 0 {
		configData, err := yaml.Marshal(runConfig.Config.Runtime.Settings)

		if err != nil {
			return nil, err
		}

		settings := v1alpha1.SettingsSpec {
			Name:    "yaks.settings.yaml",
			Content: string(configData),
		}

		return &settings, nil
	}

	return nil, nil
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
		LabelSelector:  labels.SelectorFromSet(labels.Set{"yaks.citrusframework.org/test": name}),
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
			desc := step.Name
			if desc == "" {
				desc = fmt.Sprintf("script %s", step.Script)
			}
			if err := runScript(step.Script, desc, namespace, baseDir, step.Timeout); err != nil {
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

			_, err = file.WriteString("#!/bin/bash\n\nset -e\n\n")
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

			desc := step.Name
			if desc == "" {
				desc = fmt.Sprintf("inline command %d", idx)
			}
			if err := runScript(file.Name(), desc, namespace, baseDir, step.Timeout); err != nil {
				return err
			}
		}
	}

	return nil
}

func runScript(scriptFile, desc, namespace, baseDir, timeout string) error {
	if timeout == "" {
		timeout = DefaultStepTimeout
	}
	actualTimeout, err := time.ParseDuration(timeout)
	if err != nil {
		return err
	}
	ctx, cancel := context.WithTimeout(context.Background(), actualTimeout)
	defer cancel()
	var executor string
	if r.GOOS == "windows" {
		executor = "powershell.exe"
	} else {
		executor = "/bin/bash"
	}

	command := exec.CommandContext(ctx, executor, scriptFile)

	command.Env = os.Environ()
	command.Env = append(command.Env, fmt.Sprintf("%s=%s", NamespaceEnv, namespace))

	command.Dir = baseDir

	command.Stderr = os.Stderr
	command.Stdout = os.Stdout

	fmt.Printf("Running %s: \n", desc)
	if err := command.Run(); err != nil {
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
