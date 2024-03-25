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

package config

import (
	"os"

	"github.com/citrusframework/yaks/pkg/util/defaults"
)

const (
	OperatorServiceAccount = "yaks-operator"
	ViewerServiceAccount   = "yaks-viewer"
	AppendToViewerLabel    = "yaks.citrusframework.org/append-to-viewer"
	OperatorLabel          = "yaks.citrusframework.org/operator"
	ComponentLabel         = "yaks.citrusframework.org/component"
	RoleKnative            = "knative"
	RoleCamelK             = "camelk"
	RoleStrimzi            = "strimzi"
)

func GetTestBaseImage() string {
	customEnv := os.Getenv("YAKS_TEST_BASE_IMAGE")
	if customEnv != "" {
		return customEnv
	}
	return getDefaultTestBaseImage()
}

func getDefaultTestBaseImage() string {
	return defaults.ImageName + ":" + defaults.Version
}
