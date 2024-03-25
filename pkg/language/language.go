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

package language

import (
	"fmt"
	"strings"
)

type Language interface {
	GetName() string
	SupportsFile(fileName string) bool
}

type Gherkin struct {
}
type Groovy struct {
}
type XML struct {
}
type YAML struct {
}

func (language *Gherkin) GetName() string {
	return "feature"
}

func (language *Gherkin) SupportsFile(fileName string) bool {
	return strings.HasSuffix(fileName, fmt.Sprintf(".%s", language.GetName()))
}

func (language *Groovy) GetName() string {
	return "groovy"
}

func (language *Groovy) SupportsFile(fileName string) bool {
	return strings.HasSuffix(fileName, fmt.Sprintf("it.%s", language.GetName())) ||
		strings.HasSuffix(fileName, fmt.Sprintf("test.%s", language.GetName()))
}

func (language *XML) GetName() string {
	return "xml"
}

func (language *XML) SupportsFile(fileName string) bool {
	return strings.HasSuffix(fileName, fmt.Sprintf("it.%s", language.GetName())) ||
		strings.HasSuffix(fileName, fmt.Sprintf("test.%s", language.GetName()))
}

func (language *YAML) GetName() string {
	return "yaml"
}

func (language *YAML) SupportsFile(fileName string) bool {
	return strings.HasSuffix(fileName, fmt.Sprintf("it.%s", language.GetName())) ||
		strings.HasSuffix(fileName, fmt.Sprintf("test.%s", language.GetName()))
}

// KnownLanguages is the list of all supported test languages.
var KnownLanguages = []Language{&Gherkin{}, &Groovy{}, &XML{}, &YAML{}}
