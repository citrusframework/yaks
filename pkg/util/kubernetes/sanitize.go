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

package kubernetes

import (
	"path"
	"regexp"
	"strings"
	"unicode"

	scase "github.com/stoewer/go-strcase"
)

var disallowedChars = regexp.MustCompile(`[^a-z0-9-]`)
var disallowedCharsInFile = regexp.MustCompile(`[^A-Za-z0-9-_.]`)

// SanitizeName sanitizes the given name to be compatible with k8s.
func SanitizeName(name string) string {
	name = path.Base(name)

	parts := strings.Split(name, ".")
	if len(parts) > 2 {
		name = strings.Join(parts[:len(parts)-1], "-")
	} else {
		name = parts[0]
	}

	name = scase.KebabCase(name)
	name = strings.ToLower(name)
	name = disallowedChars.ReplaceAllString(name, "")
	name = strings.TrimFunc(name, isDisallowedStartEndChar)
	return name
}

func SanitizeFileName(name string) string {
	name = path.Base(name)
	name = disallowedCharsInFile.ReplaceAllString(name, "")
	return name
}

// SanitizeLabel sanitizes the given name to be compatible with k8s.
func SanitizeLabel(name string) string {
	name = strings.ToLower(name)
	name = disallowedChars.ReplaceAllString(name, "")
	name = strings.TrimFunc(name, isDisallowedStartEndChar)
	return name
}

func isDisallowedStartEndChar(r rune) bool {
	return !unicode.IsLetter(r) && !unicode.IsNumber(r)
}
