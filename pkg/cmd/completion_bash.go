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
	"fmt"

	"github.com/spf13/cobra"
)

// ******************************
//
//
//
// ******************************

const bashCompletionCmdLongDescription = `
To load completion run

. <(yaks completion bash)

To configure your bash shell to load completions for each session add to your bashrc

# ~/.bashrc or ~/.profile
. <(yaks completion bash)
`

var bashCompletionFunction = `
__yaks_deletion_policy() {
    local type_list="owner label"
    COMPREPLY=( $( compgen -W "${type_list}" -- "$cur") )
}

__yaks_kubectl_get_tests() {
    local template
    local kubectl_out

    template="{{ range .items  }}{{ .metadata.name }} {{ end }}"

    if kubectl_out=$(kubectl get -o template --template="${template}" tests 2>/dev/null); then
        COMPREPLY=( $( compgen -W "${kubectl_out}" -- "$cur" ) )
    fi
}

__custom_func() {
    case ${last_command} in
        yaks_delete)
            __yaks_kubectl_get_tests
            return
            ;;
        *)
            ;;
    esac
}
`

// ******************************
//
// COMMAND
//
// ******************************

func newCmdCompletionBash(root *cobra.Command) *cobra.Command {
	return &cobra.Command{
		Use:   "bash",
		Short: "Generates bash completion scripts",
		Long:  bashCompletionCmdLongDescription,
		Run: func(_ *cobra.Command, _ []string) {
			err := root.GenBashCompletion(root.OutOrStdout())
			if err != nil {
				fmt.Print(err.Error())
			}
		},
		Annotations: map[string]string{
			offlineCommandLabel: "true",
		},
	}
}

func configureKnownBashCompletions(command *cobra.Command) {
	configureBashAnnotationForFlag(
		command,
		"deletion-policy",
		map[string][]string{
			cobra.BashCompCustom: {"__yaks_deletion_policy"},
		},
	)
}

func configureBashAnnotationForFlag(command *cobra.Command, flagName string, annotations map[string][]string) {
	if flag := command.Flag(flagName); flag != nil {
		flag.Annotations = annotations
	}
}
