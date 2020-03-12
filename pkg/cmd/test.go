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

	return &cmd
}

type testCmdOptions struct {
	*RootCmdOptions
	dependencies []string
	uploads      []string
	settings     string
	env          []string
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
	c, err := o.GetCmdClient()

	if err != nil {
		return err
	}

	if isRemoteFile(source) || !isDir(source) {
		// do the regular deployment
		err = o.uploadArtifacts()
		_, err = o.createTest(c, source)
		return err
	}

	// execute directory deployment
	configFile := path.Join(source, ConfigFile)
	var conf *config.TestConfig

	results := make(map[string]error)
	defer printSummary(results)

	if conf, err = config.LoadConfig(configFile); err != nil {
		panic(err)
	}

	if conf.Config.Namespace.Temporary {
		namespaceName := "yaks-" + uuid.New().String()
		var namespace metav1.Object
		if namespace, err = initializeTempNamespace(namespaceName, c, o.Context); err != nil {
			panic(err)
		}
		if conf.Config.Namespace.AutoRemove {
			defer deleteTempNamespace(namespace, c, o.Context)
		}

		o.Namespace = namespaceName

		copy := o.RootCmdOptions
		if err = newCmdInstall(copy).Execute(); err != nil {
			panic(err)
		}

		err = o.uploadArtifacts()
	}

	if files, err := ioutil.ReadDir(source); err != nil {
		results[source] = err
	} else {
		for _, f := range files {
			if strings.HasSuffix(f.Name(), FileSuffix) {
				name := path.Join(source, f.Name())
				_, testError := o.createTest(c, name)
				if testError != nil {
					err = errors.New("There are test failures!")
				}
				results[name] = testError
			}
		}
	}

	return err
}

func printSummary(results map[string]error) {
	summary := "\n\nTest suite results:\n"
	for k, v := range results {
		result := "Passed"
		if v != nil {
			result = v.Error()
		}
		summary += fmt.Sprintf("\t%s: %s\n", k, result)
	}
	fmt.Print(summary)
}

func (o *testCmdOptions) createTest(c client.Client, rawName string) (*v1alpha1.Test, error) {
	namespace := o.Namespace
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

	settings, err := o.newTestSettings()

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

	if o.env != nil {
		test.Spec.Env = o.env
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

	if err := o.printLogs(ctx, name); err != nil {
		return nil, err
	}

	err = status.AsError()
	if err != nil {
		return nil, err
	}
	fmt.Printf("Test %s\n", string(status))
	return &test, nil
}

func (o *testCmdOptions) uploadArtifacts() error {
	for _, lib := range o.uploads {
		additionalDep, err := uploadLocalArtifact(o.RootCmdOptions, lib)
		if err != nil {
			return err
		}
		o.dependencies = append(o.dependencies, additionalDep)
	}
	return nil
}

func (o *testCmdOptions) newTestSettings() (*v1alpha1.SettingsSpec, error) {
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

func (o *testCmdOptions) printLogs(ctx context.Context, name string) error {
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
		Namespace:  o.Namespace,
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
	if info, err := os.Stat(fileName); err == nil {
		return info.IsDir()
	}
	return false
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

	if !isRemoteFile(fileName) {
		content, err = ioutil.ReadFile(fileName)
		if err != nil {
			return "", err
		}
	} else {
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
	}

	return string(content), nil
}
