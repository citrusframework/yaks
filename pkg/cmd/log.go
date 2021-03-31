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
	"bufio"
	"context"
	"errors"
	"fmt"
	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/pkg/client"
	"github.com/citrusframework/yaks/pkg/cmd/config"
	"github.com/citrusframework/yaks/pkg/util/kubernetes"
	"io"
	corev1 "k8s.io/api/core/v1"
	"time"

	k8slog "github.com/citrusframework/yaks/pkg/util/kubernetes/log"
	"github.com/spf13/cobra"
	k8errors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/wait"
	k8sclient "sigs.k8s.io/controller-runtime/pkg/client"
)

func newCmdLog(rootCmdOptions *RootCmdOptions) (*cobra.Command, *logCmdOptions) {
	options := logCmdOptions{
		RootCmdOptions: rootCmdOptions,
	}

	cmd := cobra.Command{
		Use:     "log [test]",
		Short:   "Print the logs of given test",
		Long:    `Print the logs of given test.`,
		Aliases: []string{"logs"},
		Args:    options.validateArgs,
		PreRunE: decode(&options),
		RunE:    options.run,
	}

	cmd.Flags().String("timeout", "", "Time to wait for individual logs")

	return &cmd, &options
}

type logCmdOptions struct {
	*RootCmdOptions
	Timeout string `mapstructure:"timeout"`
}

func (o *logCmdOptions) validateArgs(_ *cobra.Command, args []string) error {
	if len(args) != 1 {
		return errors.New("log expects a test name as argument")
	}

	return nil
}

func (o *logCmdOptions) run(cmd *cobra.Command, args []string) error {
	c, err := o.GetCmdClient()
	if err != nil {
		return err
	}

	name := args[0]

	test := v1alpha1.Test{
		TypeMeta: metav1.TypeMeta{
			Kind:       v1alpha1.TestKind,
			APIVersion: v1alpha1.SchemeGroupVersion.String(),
		},
		ObjectMeta: metav1.ObjectMeta{
			Namespace: o.Namespace,
			Name:      name,
		},
	}
	key := k8sclient.ObjectKey{
		Namespace: o.Namespace,
		Name:      name,
	}

	var timeout string
	if o.Timeout != "" {
		timeout = o.Timeout
	} else {
		timeout = config.DefaultTimeout
	}

	waitTimeout, parseErr := time.ParseDuration(timeout)
	if parseErr != nil {
		fmt.Println(fmt.Sprintf("failed to parse test timeout setting - %s", parseErr.Error()))
		waitTimeout, _ = time.ParseDuration(config.DefaultTimeout)
	}

	var pollInterval = 2 * time.Second
	var currLogMsg = ""
	var newLogMsg = ""

	ctx, cancel := context.WithCancel(o.Context)
	err = wait.PollImmediate(pollInterval, waitTimeout, func() (done bool, err error) {

		//
		// Reduce repetition of messages by tracking the last message
		// and checking if its different from the new message
		//
		if newLogMsg != currLogMsg {
			fmt.Println(newLogMsg)
			currLogMsg = newLogMsg
		}

		//
		// Try and find the test
		//
		err = c.Get(ctx, key, &test)
		if err != nil && !k8errors.IsNotFound(err) {
			// different error so return
			return false, err
		}

		if k8errors.IsNotFound(err) {
			//
			// Don't have an integration yet so log and wait
			//
			newLogMsg = fmt.Sprintf("Test '%s' not yet available. Waiting ...", name)
			return false, nil
		}

		//
		// Found the integration so check its status using its phase
		//
		phase := test.Status.Phase
		switch phase {
		case v1alpha1.TestPhaseRunning:
			go func() {
				err = kubernetes.WaitCondition(o.Context, c, &test, func(obj interface{}) (bool, error) {
					if val, ok := obj.(*v1alpha1.Test); ok {
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

			//
			// Found the running test so step over to scraping its pod log
			//
			fmt.Printf("Test '%s' is now running. Showing log ...\n", name)
			if err := k8slog.Print(ctx, c, o.Namespace, name, cmd.OutOrStdout()); err != nil {
				return false, err
			} else {
				return true, nil
			}
		case v1alpha1.TestPhasePassed, v1alpha1.TestPhaseFailed, v1alpha1.TestPhaseError:
			//
			// Test is finished or even in error
			//
			fmt.Printf("Test '%s' is finished. Showing logs ...\n", name)
			if err := printLogs(ctx, c, o.Namespace, name, test.Status.TestID, cmd.OutOrStdout()); err != nil {
				return false, err
			} else {
				cancel()
				return true, nil
			}
		default:
			//
			// Test is new or still pending
			//
			newLogMsg = fmt.Sprintf("Test '%s' is in phase: %s ...", name, phase)
		}

		return false, nil
	})

	if err != nil {
		return err
	}

	// Let's add a wait point, otherwise the script terminates
	<-ctx.Done()

	return nil
}

func printLogs(ctx context.Context, c client.Client, namespace string, name string, testId string, out io.Writer) error {
	pods, err := c.CoreV1().Pods(namespace).List(ctx, metav1.ListOptions{
		LabelSelector: v1alpha1.TestIdLabel + "=" + testId,
	})

	if pods == nil || len(pods.Items) == 0 {
		return errors.New(fmt.Sprintf("unable to locate test pod for name %s in namespace %s", name, namespace))
	}

	logOptions := corev1.PodLogOptions{
		Follow:    false,
		Container: "test",
	}
	byteReader, err := c.CoreV1().Pods(namespace).GetLogs(pods.Items[0].Name, &logOptions).Stream(ctx)
	if err != nil {
		return err
	}

	reader := bufio.NewReader(byteReader)
	for {
		data, err := reader.ReadBytes('\n')
		if err == io.EOF {
			return nil
		}
		if err != nil {
			break
		}
		_, err = out.Write(data)
		if err != nil {
			break
		}
	}

	return nil
}
