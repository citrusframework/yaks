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

package digest

import (
	"crypto/sha256"
	"encoding/base64"

	"github.com/citrusframework/yaks/pkg/apis/yaks/v1alpha1"
	"github.com/citrusframework/yaks/version"
)

// ComputeForTest returns a digest of the fields that are relevant for detecting changes
func ComputeForTest(test *v1alpha1.Test) (string, error) {
	hash := sha256.New()
	// Operator version is relevant
	if _, err := hash.Write([]byte(version.Version)); err != nil {
		return "", err
	}
	// Source is relevant
	if _, err := hash.Write([]byte(test.Spec.Source.Language)); err != nil {
		return "", err
	}
	if _, err := hash.Write([]byte(test.Spec.Source.Content)); err != nil {
		return "", err
	}
	if _, err := hash.Write([]byte(test.Spec.Source.Name)); err != nil {
		return "", err
	}

	// Add a letter at the beginning and use URL safe encoding
	digest := "v" + base64.RawURLEncoding.EncodeToString(hash.Sum(nil))
	return digest, nil
}
