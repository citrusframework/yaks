package cmd

import (
	"encoding/csv"
	"fmt"
	"github.com/citrusframework/yaks/pkg/client"
	"github.com/mitchellh/mapstructure"
	"github.com/spf13/viper"
	"reflect"
	"strings"

	"github.com/pkg/errors"
	"github.com/spf13/cobra"
)

func (command *RootCmdOptions) preRun(cmd *cobra.Command, _ []string) error {
	if command.Namespace == "" {
		current, err := client.GetCurrentNamespace(command.KubeConfig)
		if err != nil {
			return errors.Wrap(err, "cannot get current namespace")
		}
		err = cmd.Flag("namespace").Value.Set(current)
		if err != nil {
			return err
		}
	}
	return nil
}

// GetCmdClient returns the client that can be used from command line tools
func (command *RootCmdOptions) GetCmdClient() (client.Client, error) {
	// Get the pre-computed client
	if command._client != nil {
		return command._client, nil
	}
	var err error
	command._client, err = command.NewCmdClient()
	return command._client, err
}

// NewCmdClient returns a new client that can be used from command line tools
func (command *RootCmdOptions) NewCmdClient() (client.Client, error) {
	return client.NewOutOfClusterClient(command.KubeConfig)
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

func stringToSliceHookFunc(comma rune) mapstructure.DecodeHookFunc {
	return func(
		f reflect.Kind,
		t reflect.Kind,
		data interface{}) (interface{}, error) {
		if f != reflect.String || t != reflect.Slice {
			return data, nil
		}

		s := data.(string)
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