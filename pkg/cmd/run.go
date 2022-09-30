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
	"io"
	"io/ioutil"
	"os"
	"os/exec"
	"path"
	r "runtime"
	"strings"
	"time"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/cmd/config"
	"github.com/citrusframework/yaks/pkg/cmd/report"
	"github.com/citrusframework/yaks/pkg/install"
	"github.com/citrusframework/yaks/pkg/language"
	"github.com/citrusframework/yaks/pkg/util"
	"github.com/citrusframework/yaks/pkg/util/envvar"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"
	k8slog "github.com/citrusframework/yaks/pkg/util/kubernetes/log"
	"github.com/citrusframework/yaks/pkg/util/openshift"

	"github.com/google/uuid"
	projectv1 "github.com/openshift/api/project/v1"
	"github.com/pkg/errors"
	"github.com/spf13/cobra"
	"gopkg.in/yaml.v2"
	corev1 "k8s.io/api/core/v1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	ctrl "sigs.k8s.io/controller-runtime/pkg/client"
)

const (
	ConfigFile    = "yaks-config.yaml"
	SettingsFile  = "yaks.settings.yaml"
	KubeDockImage = "joyrex2001/kubedock:0.9.1"
	SeleniumImage = "selenium/standalone-chrome:106.0"
)

const (
	CucumberOptions    = "CUCUMBER_OPTIONS"
	CucumberGlue       = "CUCUMBER_GLUE"
	CucumberFeatures   = "CUCUMBER_FEATURES"
	CucumberFilterTags = "CUCUMBER_FILTER_TAGS"
)

func newCmdRun(rootCmdOptions *RootCmdOptions) (*cobra.Command, *runCmdOptions) {
	options := runCmdOptions{
		RootCmdOptions: rootCmdOptions,
	}

	cmd := cobra.Command{
		Use:     "run [options] [test file to execute]",
		Short:   "Run tests",
		Long:    `Deploys and executes a test on given namespace.`,
		Args:    options.validateArgs,
		Aliases: []string{"test"},
		PreRunE: decode(&options),
		RunE:    options.run,
	}

	cmd.Flags().StringArray("maven-repository", nil, "Adds custom Maven repository URL that is added to the runtime.")
	cmd.Flags().StringArrayP("logger", "l", nil, "Adds logger configuration by setting log levels. E.g \"-l org.example=INFO\"")
	cmd.Flags().StringArrayP("dependency", "d", nil, "Adds runtime dependencies that get automatically loaded before the test is executed.")
	cmd.Flags().StringArrayP("upload", "u", nil, "Upload a given library to the cluster to allow it to be used by tests.")
	cmd.Flags().StringP("settings", "s", "", "Path to runtime settings file. File content is added to the test runtime and can hold runtime dependency information for instance.")
	cmd.Flags().StringArrayP("env", "e", nil, "Set an environment variable in the integration container. E.g \"-e MY_VAR=my-value\"")
	cmd.Flags().StringArray("resource", nil, "Add a resource")
	cmd.Flags().StringArray("property-file", nil, "Bind a property file to the test. E.g. \"--property-file test.properties\"")
	cmd.Flags().String("timeout", "", "Time to wait for individual test to complete")

	cmd.Flags().Bool("dump", false, "Dump all test resources in namespace after running the test.")
	cmd.Flags().String("print", "", "Print output format. One of: json|yaml. If set the test CR is created and printed to the CLI output instead of running the test.")
	cmd.Flags().StringP("report", "r", "junit", "Create test report in given output format")
	cmd.Flags().BoolP("wait", "w", true, "Wait for the test to be complete")
	cmd.Flags().Bool("logs", true, "Print test logs")

	// Cucumber specific options
	cmd.Flags().StringArrayP("feature", "f", nil, "Feature file to include in the test run")
	cmd.Flags().StringArrayP("tag", "t", nil, "Specify a tag filter to only run tests that match given tag expression")
	cmd.Flags().StringArrayP("glue", "g", nil, "Additional glue path to be added in the Cucumber runtime options")
	cmd.Flags().StringP("options", "o", "", "Cucumber runtime options")

	// completion support
	configureKnownCompletions(&cmd)

	return &cmd, &options
}

type runCmdOptions struct {
	*RootCmdOptions
	Repositories  []string            `mapstructure:"maven-repository"`
	Dependencies  []string            `mapstructure:"dependency"`
	Logger        []string            `mapstructure:"logger"`
	Uploads       []string            `mapstructure:"upload"`
	Settings      string              `mapstructure:"settings"`
	Env           []string            `mapstructure:"env"`
	Tags          []string            `mapstructure:"tag"`
	Features      []string            `mapstructure:"feature"`
	Resources     []string            `mapstructure:"resources"`
	PropertyFiles []string            `mapstructure:"property-files"`
	Glue          []string            `mapstructure:"glue"`
	Options       string              `mapstructure:"options"`
	Dump          bool                `mapstructure:"dump"`
	PrintFormat   string              `mapstructure:"print"`
	ReportFormat  report.OutputFormat `mapstructure:"report"`
	Timeout       string              `mapstructure:"timeout"`
	Wait          bool                `mapstructure:"wait"`
	Logs          bool                `mapstructure:"logs"`
}

func (o *runCmdOptions) validateArgs(_ *cobra.Command, args []string) error {
	if len(args) != 1 {
		return fmt.Errorf("accepts exactly 1 test name to execute, received %d", len(args))
	}

	return nil
}

func (o *runCmdOptions) run(cmd *cobra.Command, args []string) error {

	results := v1alpha1.TestResults{}
	if o.Wait {
		defer report.PrintSummaryReport(&results)
		defer report.GenerateReport(nil, cmd.OutOrStderr(), &results, o.ReportFormat)
	}

	if source := args[0]; isDir(source) {
		o.runTestGroup(cmd, source, &results)
	} else {
		o.runTest(cmd, source, &results)
	}

	if hasErrors(&results) {
		return errors.New("test suite failed")
	}

	return nil
}

func (o *runCmdOptions) runTest(cmd *cobra.Command, source string, results *v1alpha1.TestResults) {
	c, err := o.GetCmdClient()
	if err != nil {
		handleTestError("", source, results, err)
		return
	}

	var runConfig *config.RunConfig
	if runConfig, err = o.getRunConfig(source); err != nil {
		handleTestError("", source, results, err)
		return
	}

	if runConfig.Config.Namespace.Temporary {
		if namespace, err := o.createTempNamespace(runConfig, cmd, c); namespace != nil {
			if runConfig.Config.Namespace.AutoRemove && o.Wait {
				defer deleteTempNamespace(o.Context, c, namespace)
			}

			if err != nil {
				handleTestError(runConfig.Config.Namespace.Name, source, results, err)
				return
			}
		} else if err != nil {
			handleTestError(runConfig.Config.Namespace.Name, source, results, err)
			return
		}
	}

	if err = o.uploadArtifacts(runConfig); err != nil {
		handleTestError(runConfig.Config.Namespace.Name, source, results, err)
		return
	}

	handleError := func(err error) {
		handleTestError(runConfig.Config.Namespace.Name, source, results, err)
	}
	defer runSteps(runConfig.Post, runConfig.Config.Namespace.Name, runConfig.BaseDir, results, handleError)
	if !runSteps(runConfig.Pre, runConfig.Config.Namespace.Name, runConfig.BaseDir, results, handleError) {
		return
	}

	suite := v1alpha1.TestSuite{}
	var test *v1alpha1.Test
	test, err = o.createAndRunTest(o.Context, c, cmd, source, runConfig)
	if test != nil {
		handleTestResult(test, &suite)
		results.Suites = append(results.Suites, suite)

		if err != nil {
			suite.Errors = append(suite.Errors, err.Error())
		}
	} else if err != nil {
		handleTestError(runConfig.Config.Namespace.Name, source, results, err)
	}
}

func (o *runCmdOptions) runTestGroup(cmd *cobra.Command, source string, results *v1alpha1.TestResults) {
	c, err := o.GetCmdClient()
	if err != nil {
		handleTestError("", source, results, err)
		return
	}

	var runConfig *config.RunConfig
	if runConfig, err = o.getRunConfig(source); err != nil {
		handleTestError("", source, results, err)
		return
	}

	if runConfig.Config.Namespace.Temporary {
		if namespace, err := o.createTempNamespace(runConfig, cmd, c); err != nil {
			handleTestError(runConfig.Config.Namespace.Name, source, results, err)
			return
		} else if namespace != nil && runConfig.Config.Namespace.AutoRemove && o.Wait {
			defer deleteTempNamespace(o.Context, c, namespace)
		}
	}

	if err = o.uploadArtifacts(runConfig); err != nil {
		handleTestError(runConfig.Config.Namespace.Name, source, results, err)
		return
	}

	var files []os.FileInfo
	if files, err = ioutil.ReadDir(source); err != nil {
		handleTestError(runConfig.Config.Namespace.Name, source, results, err)
		return
	}

	handleError := func(err error) {
		handleTestError(runConfig.Config.Namespace.Name, source, results, err)
	}
	defer runSteps(runConfig.Post, runConfig.Config.Namespace.Name, runConfig.BaseDir, results, handleError)
	if !runSteps(runConfig.Pre, runConfig.Config.Namespace.Name, runConfig.BaseDir, results, handleError) {
		return
	}

	for _, f := range files {
		name := path.Join(source, f.Name())
		if f.IsDir() && runConfig.Config.Recursive {
			o.runTestGroup(cmd, name, results)
		} else if isKnownLanguage(f.Name()) {
			suite := v1alpha1.TestSuite{}
			var test *v1alpha1.Test
			test, err = o.createAndRunTest(o.Context, c, cmd, name, runConfig)
			if test != nil {
				handleTestResult(test, &suite)
				results.Suites = append(results.Suites, suite)

				if err != nil {
					suite.Errors = append(suite.Errors, err.Error())
				}
			} else if err != nil {
				handleTestError(runConfig.Config.Namespace.Name, name, results, err)
			}
		}
	}
}

func isKnownLanguage(fileName string) bool {
	for _, lang := range language.KnownLanguages {
		if lang.SupportsFile(fileName) {
			return true
		}
	}

	return false
}

func handleTestError(namespace string, source string, results *v1alpha1.TestResults, err error) {
	suite := v1alpha1.TestSuite{
		Errors: []string{
			fmt.Sprintf("%s - %s", k8serrors.ReasonForError(err), err.Error()),
		},
	}

	handleTestResult(report.GetErrorResult(namespace, source, err), &suite)
	results.Suites = append(results.Suites, suite)
}

func handleTestResult(test *v1alpha1.Test, suite *v1alpha1.TestSuite) {
	report.AppendTestResults(suite, test.Status.Results)

	if saveErr := report.SaveTestResults(test); saveErr != nil {
		fmt.Printf("Failed to save test results: %s\n", saveErr.Error())
	}
}

func (o *runCmdOptions) getRunConfig(source string) (*config.RunConfig, error) {
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

	if o.Dump {
		runConfig.Config.Dump.Enabled = true
	}

	return runConfig, nil
}

func (o *runCmdOptions) createTempNamespace(runConfig *config.RunConfig, cmd *cobra.Command, c client.Client) (metav1.Object, error) {
	namespaceName := "yaks-" + uuid.New().String()
	namespace, err := initializeTempNamespace(o.Context, c, namespaceName)
	if err != nil {
		return nil, err
	}
	runConfig.Config.Namespace.Name = namespaceName

	// looking for existing operator instance in current namespace
	instance, err := v1alpha1.GetOrFindInstance(o.Context, c, o.Namespace)
	if err != nil && k8serrors.IsNotFound(err) {
		// looking for global operator instance
		instance, err = v1alpha1.FindGlobalInstance(o.Context, c)

		if err != nil && k8serrors.IsForbidden(err) {
			// not allowed to list all instances on the clusterr
			return namespace, nil
		} else if err != nil {
			return namespace, err
		}
	}

	if instance != nil && v1alpha1.IsGlobal(instance) {
		// Using global operator to manage temporary namespaces, no action required
		return namespace, nil
	}

	fmt.Println("Adding new operator instance to temporary namespace by default")

	// no operator or non-global operator found, deploy into temp namespace
	// Let's use a client provider during cluster installation, to eliminate the problem of CRD object caching
	clientProvider := client.Provider{Get: o.NewCmdClient}

	if err := setupCluster(o.Context, clientProvider, nil); err != nil {
		return namespace, err
	}

	if err := o.setupOperator(runConfig, cmd, c); err != nil {
		return namespace, err
	}

	return namespace, nil
}

func (o *runCmdOptions) setupOperator(runConfig *config.RunConfig, cmd *cobra.Command, c client.Client) error {
	namespace := runConfig.Config.Namespace.Name
	var cluster v1alpha1.ClusterType
	isOpenshift, err := openshift.IsOpenShift(c)
	if err != nil {
		return err
	}

	if isOpenshift {
		cluster = v1alpha1.ClusterTypeOpenShift
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
	err = install.OperatorOrCollect(o.Context, cmd, c, cfg, nil, true)

	for _, role := range runConfig.Config.Operator.Roles {
		err = applyOperatorRole(o.Context, c, resolvePath(runConfig, role), namespace, install.IdentityResourceCustomizer)
		if err != nil {
			return err
		}
	}

	return err
}

func (o *runCmdOptions) createAndRunTest(ctx context.Context, c client.Client, cmd *cobra.Command, rawName string, runConfig *config.RunConfig) (*v1alpha1.Test, error) {
	namespace := runConfig.Config.Namespace.Name
	fileName := kubernetes.SanitizeFileName(rawName)
	name := kubernetes.SanitizeName(rawName)

	if name == "" {
		return nil, errors.New("unable to determine test name")
	}

	data, err := loadData(ctx, rawName)
	if err != nil {
		return nil, err
	}

	test, err := o.configureTest(ctx, namespace, fileName, name, getLanguage(rawName), data, runConfig)
	if err != nil {
		return nil, err
	}

	switch o.PrintFormat {
	case "":
		// continue ...
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
		return nil, fmt.Errorf("invalid dump output format option '%s', should be one of: yaml|json", o.PrintFormat)
	}

	existed := false
	err = c.Create(ctx, &test)
	if err != nil && k8serrors.IsAlreadyExists(err) {
		existed = true
		clone := test.DeepCopy()
		key := ctrl.ObjectKeyFromObject(clone)
		err = c.Get(ctx, key, clone)
		if err != nil {
			return nil, err
		}
		// Hold the resource from the operator controller
		clone.Status.Phase = v1alpha1.TestPhaseUpdating
		err = c.Status().Update(ctx, clone)
		if err != nil {
			return nil, err
		}
		// Update the spec
		test.ResourceVersion = clone.ResourceVersion
		err = c.Update(ctx, &test)
		if err != nil {
			return nil, err
		}
		// Reset status
		test.Status = v1alpha1.TestStatus{}
		err = c.Status().Update(ctx, &test)
	}

	if err != nil {
		return nil, err
	}

	if !existed {
		fmt.Printf("Test '%s' created\n", name)
	} else {
		fmt.Printf("Test '%s' updated\n", name)
	}

	status := v1alpha1.TestPhaseNew
	if o.Wait {
		ctxWithCancel, cancel := context.WithCancel(ctx)

		go func() {
			timeout := config.DefaultTimeout

			if runConfig.Config.Timeout != "" {
				timeout = runConfig.Config.Timeout
			}

			if o.Timeout != "" {
				timeout = o.Timeout
			}

			waitTimeout, parseErr := time.ParseDuration(timeout)
			if parseErr != nil {
				fmt.Printf("Failed to parse test timeout setting - %s\n", parseErr.Error())
				waitTimeout, _ = time.ParseDuration(config.DefaultTimeout)
			}

			err = kubernetes.WaitCondition(ctxWithCancel, c, &test, func(obj interface{}) (bool, error) {
				if val, ok := obj.(*v1alpha1.Test); ok {
					if val.Status.Phase != v1alpha1.TestPhaseNone {
						status = val.Status.Phase
					}

					if val.Status.Phase == v1alpha1.TestPhaseDeleting ||
						val.Status.Phase == v1alpha1.TestPhaseError ||
						val.Status.Phase == v1alpha1.TestPhasePassed ||
						val.Status.Phase == v1alpha1.TestPhaseFailed {
						return true, nil
					}
				}
				return false, nil
			}, waitTimeout)

			cancel()
		}()

		if o.Logs {
			if err := k8slog.Print(ctxWithCancel, c, namespace, name, cmd.OutOrStdout()); err != nil {
				return nil, err
			}
		}

		// Let's add a Wait point, otherwise the script terminates
		<-ctxWithCancel.Done()

		fmt.Printf("Test '%s' finished with status: %s\n", name, string(status))
	} else {
		fmt.Printf("Test '%s' started\n", name)
	}

	if runConfig.Config.Dump.Enabled {
		if runConfig.Config.Dump.FailedOnly &&
			test.Status.Phase != v1alpha1.TestPhaseFailed && test.Status.Phase != v1alpha1.TestPhaseError &&
			len(test.Status.Errors) == 0 && !hasSuiteErrors(&test.Status.Results) {
			fmt.Println("Skip dump for successful test")
		} else {
			var fileName string
			if runConfig.Config.Dump.File != "" {
				fileName = runConfig.Config.Dump.File
			} else {
				fileName = fmt.Sprintf("%s-dump.log", test.Name)
			}

			fmt.Printf("Dump test '%s' to file '%s'\n", name, path.Join(runConfig.Config.Dump.Directory, fileName))

			var flags int
			if runConfig.Config.Dump.Append {
				flags = os.O_RDWR | os.O_CREATE | os.O_APPEND
			} else {
				flags = os.O_RDWR | os.O_CREATE
			}

			if outputDir, err := util.CreateInWorkingDir(runConfig.Config.Dump.Directory); err != nil {
				fmt.Println(err)
			} else if err = util.WithFile(path.Join(outputDir, fileName), flags, 0o644, func(out io.Writer) error {
				return dumpTest(ctx, c, test.Name, namespace, out, runConfig.Config.Dump.Lines, runConfig.Config.Dump.Includes)
			}); err != nil {
				fmt.Println(err)
			}
		}
	}

	return &test, status.AsError(name)
}

func (o *runCmdOptions) configureTest(ctx context.Context, namespace string, fileName string, name string, lang language.Language, data string, runConfig *config.RunConfig) (v1alpha1.Test, error) {
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
			Runtime: v1alpha1.RuntimeSpec{
				Logger:  o.Logger,
				Verbose: o.Verbose,
			},
			Source: v1alpha1.SourceSpec{
				Name:     fileName,
				Content:  data,
				Language: lang.GetName(),
			},
		},
	}

	for _, resource := range runConfig.Config.Runtime.Resources {
		data, err := loadData(ctx, resolvePath(runConfig, resource))
		if err != nil {
			return test, err
		}

		test.Spec.Resources = append(test.Spec.Resources, v1alpha1.ResourceSpec{
			Name:    path.Base(resource),
			Content: data,
		})
	}

	for _, resource := range o.Resources {
		data, err := loadData(ctx, resolvePath(runConfig, resource))
		if err != nil {
			return test, err
		}

		test.Spec.Resources = append(test.Spec.Resources, v1alpha1.ResourceSpec{
			Name:    path.Base(resource),
			Content: data,
		})
	}

	for _, propertyFile := range o.PropertyFiles {
		data, err := loadData(ctx, resolvePath(runConfig, propertyFile))
		if err != nil {
			return test, err
		}

		test.Spec.Resources = append(test.Spec.Resources, v1alpha1.ResourceSpec{
			Name:    path.Base(propertyFile),
			Content: data,
		})
	}

	if settings, err := o.newSettings(ctx, runConfig); err != nil {
		return test, err
	} else if settings != nil {
		test.Spec.Settings = *settings
	}

	o.setupEnvSettings(&test, runConfig)

	if runConfig.Config.Runtime.Secret != "" {
		test.Spec.Secret = runConfig.Config.Runtime.Secret
	}

	if runConfig.Config.Runtime.Selenium.Enabled {
		image := SeleniumImage

		if runConfig.Config.Runtime.Selenium.Image != "" {
			image = runConfig.Config.Runtime.Selenium.Image
		}

		test.Spec.Selenium = v1alpha1.SeleniumSpec{
			Image:     image,
			RunAsUser: runConfig.Config.Runtime.Selenium.RunAsUser,
		}
	}

	if runConfig.Config.Runtime.TestContainers.Enabled {
		image := KubeDockImage

		if runConfig.Config.Runtime.TestContainers.Image != "" {
			image = runConfig.Config.Runtime.TestContainers.Image
		}

		test.Spec.KubeDock = v1alpha1.KubeDockSpec{
			Image:     image,
			RunAsUser: runConfig.Config.Runtime.TestContainers.RunAsUser,
		}
	}

	return test, nil
}

func (o *runCmdOptions) uploadArtifacts(runConfig *config.RunConfig) error {
	for _, lib := range o.Uploads {
		additionalDep, err := uploadLocalArtifact(o.RootCmdOptions, resolvePath(runConfig, lib), runConfig.Config.Namespace.Name)
		if err != nil {
			return err
		}
		o.Dependencies = append(o.Dependencies, additionalDep)
	}
	return nil
}

func (o *runCmdOptions) setupEnvSettings(test *v1alpha1.Test, runConfig *config.RunConfig) {
	env := make([]string, 0)

	env = append(env, envvar.NamespaceEnv+"="+runConfig.Config.Namespace.Name)

	if len(o.Tags) > 0 {
		env = append(env, CucumberFilterTags+"="+strings.Join(o.Tags, ","))
	} else if len(runConfig.Config.Runtime.Cucumber.Tags) > 0 {
		env = append(env, CucumberFilterTags+"="+strings.Join(runConfig.Config.Runtime.Cucumber.Tags, ","))
	}

	if len(o.Features) > 0 {
		env = append(env, CucumberFeatures+"="+strings.Join(o.Features, ","))
	}

	if len(o.Glue) > 0 {
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
		env = append(env, envvar.RepositoriesEnv+"="+strings.Join(o.Repositories, ","))
	}

	if len(o.Dependencies) > 0 {
		env = append(env, envvar.DependenciesEnv+"="+strings.Join(o.Dependencies, ","))
	}

	for _, envConfig := range runConfig.Config.Runtime.Env {
		env = append(env, envConfig.Name+"="+envConfig.Value)
	}

	if len(o.Env) > 0 {
		env = append(env, o.Env...)
	}

	if len(env) > 0 {
		test.Spec.Env = env
	}
}

func (o *runCmdOptions) newSettings(ctx context.Context, runConfig *config.RunConfig) (*v1alpha1.SettingsSpec, error) {
	if o.Settings != "" {
		rawName := o.Settings
		configData, err := loadData(ctx, resolvePath(runConfig, rawName))

		if err != nil {
			return nil, err
		}

		settings := v1alpha1.SettingsSpec{
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

		settings := v1alpha1.SettingsSpec{
			Name:    SettingsFile,
			Content: string(configData),
		}

		return &settings, nil
	}

	return nil, nil
}

func runSteps(steps []config.StepConfig, namespace, baseDir string, results *v1alpha1.TestResults, handleError func(err error)) bool {
	for idx, step := range steps {
		if len(step.Name) == 0 {
			step.Name = fmt.Sprintf("step-%d", idx)
		}

		if skipStep(step, results) {
			fmt.Printf("Skip %s\n", step.Name)
			continue
		}

		if len(step.Script) > 0 {
			desc := step.Name
			if desc == "" {
				desc = fmt.Sprintf("script %s", step.Script)
			}
			if err := runScript(step.Script, desc, namespace, baseDir, hasErrors(results), step.Timeout); err != nil {
				handleError(fmt.Errorf(fmt.Sprintf("Failed to run %s: %v", desc, err)))
				return false
			}
		}

		if len(step.Run) > 0 {
			// Let's save it to a bash script to allow for multiline scripts
			file, err := ioutil.TempFile("", "yaks-script-*.sh")
			if err != nil {
				handleError(err)
				return false
			}
			defer os.Remove(file.Name())

			_, err = file.WriteString("#!/bin/bash\n\nset -e\n\n")
			if err != nil {
				handleError(err)
				return false
			}

			_, err = file.WriteString(step.Run)
			if err != nil {
				handleError(err)
				return false
			}

			if err = file.Close(); err != nil {
				handleError(err)
				return false
			}

			// Make it executable
			if err = os.Chmod(file.Name(), 0777); err != nil {
				handleError(err)
				return false
			}

			desc := step.Name
			if desc == "" {
				desc = fmt.Sprintf("inline command %d", idx)
			}
			if err := runScript(file.Name(), desc, namespace, baseDir, hasErrors(results), step.Timeout); err != nil {
				handleError(fmt.Errorf(fmt.Sprintf("Failed to run %s: %v", desc, err)))
				return false
			}
		}
	}

	return true
}

func skipStep(step config.StepConfig, results *v1alpha1.TestResults) bool {
	if step.If == "" {
		return false
	}

	conditions := strings.Split(step.If, " && ")

	skipStep := false
	for _, condition := range conditions {
		var keyValue []string
		if strings.Contains(condition, "=") {
			keyValue = strings.Split(condition, "=")
		} else {
			keyValue = []string{condition}
		}

		if strings.HasPrefix((keyValue)[0], "env:") {
			if value, ok := os.LookupEnv(strings.TrimPrefix((keyValue)[0], "env:")); ok {
				// support env name check when no expected value is given
				if len(keyValue) == 1 {
					// env name is available and value is ignored
					continue
				}
				skipStep = (keyValue)[1] != value
			} else {
				skipStep = true
			}
		}

		switch (keyValue)[0] {
		case "os":
			skipStep = (keyValue)[1] != r.GOOS
		case "failure()":
			skipStep = !hasErrors(results)
		}

		if skipStep {
			return true
		}
	}

	return false
}

func runScript(scriptFile string, desc string, namespace string, baseDir string, failed bool, timeout string) error {
	if timeout == "" {
		timeout = config.DefaultTimeout
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

	command := exec.CommandContext(ctx, executor, resolve(scriptFile)) //#nosec G204

	command.Env = os.Environ()
	command.Env = append(command.Env, fmt.Sprintf("%s=%s", envvar.NamespaceEnv, namespace))

	if failed {
		command.Env = append(command.Env, fmt.Sprintf("%s=%s", envvar.TestStatusEnv, "FAILED"))
	} else {
		command.Env = append(command.Env, fmt.Sprintf("%s=%s", envvar.TestStatusEnv, "SUCCESS"))
	}

	command.Dir = baseDir

	command.Stderr = os.Stderr
	command.Stdout = os.Stdout

	fmt.Printf("Running %s:\n", desc)
	if err := command.Run(); err != nil {
		fmt.Printf("Failed to run %s: \n%v\n", desc, err)
		return err
	}
	return nil
}

func resolve(fileName string) string {
	resolved := strings.ReplaceAll(fileName, "{{os.type}}", r.GOOS)
	resolved = strings.ReplaceAll(resolved, "{{os.arch}}", r.GOARCH)
	return resolved
}

func initializeTempNamespace(context context.Context, c client.Client, name string) (metav1.Object, error) {
	var obj ctrl.Object

	oc, err := openshift.IsOpenShift(c)
	if err != nil {
		panic(err)
	}

	if oc {
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
	if err := c.Create(context, obj); err != nil {
		return nil, err
	}

	if metaObj, ok := obj.(metav1.Object); ok {
		return metaObj, nil
	}

	return nil, fmt.Errorf("type assertion failed %v", obj)
}

func deleteTempNamespace(context context.Context, c client.Client, ns metav1.Object) {
	oc, err := openshift.IsOpenShift(c)
	if err != nil {
		panic(err)
	}

	if oc {
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
		if nsObj, ok := ns.(ctrl.Object); ok {
			if err = c.Delete(context, nsObj); err != nil {
				fmt.Fprintf(os.Stderr, "WARN: Failed to AutoRemove namespace %s\n", ns.GetName())
			}
		}
	}
	fmt.Printf("AutoRemove namespace %s\n", ns.GetName())
}
