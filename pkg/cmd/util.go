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
	"encoding/csv"
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"path"
	"path/filepath"
	"reflect"
	"strings"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/cmd/config"
	p "github.com/gertd/go-pluralize"
	"github.com/mitchellh/mapstructure"
	"github.com/spf13/cobra"
	"github.com/spf13/pflag"
	"github.com/spf13/viper"

	corev1 "k8s.io/api/core/v1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	ctrl "sigs.k8s.io/controller-runtime/pkg/client"
)

const (
	offlineCommandLabel = "yaks.citrusframework.org/cmd.offline"
)

func bindPFlagsHierarchy(cmd *cobra.Command) error {
	for _, c := range cmd.Commands() {
		if err := bindPFlags(c); err != nil {
			return err
		}

		if err := bindPFlagsHierarchy(c); err != nil {
			return err
		}
	}

	return nil
}

func bindPFlags(cmd *cobra.Command) error {
	prefix := pathToRoot(cmd)
	pl := p.NewClient()

	cmd.Flags().VisitAll(func(flag *pflag.Flag) {
		name := flag.Name
		name = strings.ReplaceAll(name, "_", "-")
		name = strings.ReplaceAll(name, ".", "-")

		if err := viper.BindPFlag(prefix+"."+name, flag); err != nil {
			log.Printf("error binding flag %s with prefix %s to viper: %v", flag.Name, prefix, err)
		}

		// this is a little bit of an hack to register plural version of properties
		// based on the naming conventions used by the flag type because it is not
		// possible to know what is the type of a flag
		flagType := strings.ToUpper(flag.Value.Type())
		if strings.Contains(flagType, "SLICE") || strings.Contains(flagType, "ARRAY") {
			if err := viper.BindPFlag(prefix+"."+pl.Plural(name), flag); err != nil {
				log.Printf("error binding plural flag %s with prefix %s to viper: %v", flag.Name, prefix, err)
			}
		}
	})

	return nil
}

func pathToRoot(cmd *cobra.Command) string {
	path := cmd.Name()

	for current := cmd.Parent(); current != nil; current = current.Parent() {
		name := current.Name()
		name = strings.ReplaceAll(name, "_", "-")
		name = strings.ReplaceAll(name, ".", "-")
		path = name + "." + path
	}

	return path
}

func decodeKey(target interface{}, key string) error {
	nodes := strings.Split(key, ".")
	settings := viper.AllSettings()

	for _, node := range nodes {
		v := settings[node]

		if v == nil {
			return nil
		}

		if m, ok := v.(map[string]interface{}); ok {
			settings = m
		} else {
			return fmt.Errorf("unable to find node %s", node)
		}
	}

	c := mapstructure.DecoderConfig{
		Result:           target,
		WeaklyTypedInput: true,
		DecodeHook: mapstructure.ComposeDecodeHookFunc(
			mapstructure.StringToIPNetHookFunc(),
			mapstructure.StringToTimeDurationHookFunc(),
			stringToSliceHookFunc(','),
		),
	}

	decoder, err := mapstructure.NewDecoder(&c)
	if err != nil {
		return err
	}

	err = decoder.Decode(settings)
	if err != nil {
		return err
	}

	return nil
}

func decode(target interface{}) func(*cobra.Command, []string) error {
	return func(cmd *cobra.Command, args []string) error {
		root := pathToRoot(cmd)
		if err := decodeKey(target, root); err != nil {
			return err
		}

		return nil
	}
}

func stringToSliceHookFunc(comma rune) mapstructure.DecodeHookFunc {
	return func(
		f reflect.Kind,
		t reflect.Kind,
		data interface{}) (interface{}, error) {
		if f != reflect.String || t != reflect.Slice {
			return data, nil
		}

		s, ok := data.(string)
		if !ok {
			return nil, fmt.Errorf("type assertion failed %v", data)
		}

		s = strings.TrimPrefix(s, "[")
		s = strings.TrimSuffix(s, "]")

		if s == "" {
			return []string{}, nil
		}

		stringReader := strings.NewReader(s)
		csvReader := csv.NewReader(stringReader)
		csvReader.Comma = comma
		csvReader.LazyQuotes = true

		return csvReader.Read()
	}
}

func cmdOnly(cmd *cobra.Command, options interface{}) *cobra.Command {
	return cmd
}

func isOfflineCommand(cmd *cobra.Command) bool {
	return cmd.Annotations[offlineCommandLabel] == "true"
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

func getBaseDir(source string) string {
	if isRemoteFile(source) {
		return ""
	}

	if isDir(source) {
		return source
	}

	dir, _ := path.Split(source)
	return dir
}

func resolvePath(runConfig *config.RunConfig, resource string) string {
	if filepath.IsAbs(resource) || isRemoteFile(resource) {
		return resource
	}

	return path.Join(runConfig.BaseDir, resource)
}

func hasErrors(results *v1alpha1.TestResults) bool {
	for i := range results.Suites {
		if hasSuiteErrors(&results.Suites[i]) {
			return true
		}
	}

	return false
}

func hasSuiteErrors(suite *v1alpha1.TestSuite) bool {
	if len(suite.Errors) > 0 || suite.Summary.Errors > 0 || suite.Summary.Failed > 0 {
		return true
	}

	return false
}

func loadData(ctx context.Context, fileName string) (string, error) {
	var content []byte
	var err error

	if isRemoteFile(fileName) {
		/* #nosec */
		var body io.Reader
		req, err := http.NewRequestWithContext(ctx, http.MethodGet, fileName, body)
		if err != nil {
			return "", err
		}

		response, err := http.DefaultClient.Do(req)
		if err != nil {
			return "", err
		}
		defer response.Body.Close()

		content, err = ioutil.ReadAll(body)
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

func hasServiceAccount(ctx context.Context, c ctrl.Client, namespace string, serviceAccount string) (bool, error) {
	sa := corev1.ServiceAccount{}
	saKey := ctrl.ObjectKey{
		Name:      serviceAccount,
		Namespace: namespace,
	}

	err := c.Get(ctx, saKey, &sa)

	if err != nil && k8serrors.IsNotFound(err) {
		return false, nil
	}

	return !k8serrors.IsNotFound(err), err
}
