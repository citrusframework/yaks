#!/bin/bash

#
# Copyright the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

location=$(dirname $0)
apidir=$location/../pkg/apis/yaks

echo "Generating CRDs..."

cd "$apidir"
$(go env GOPATH)/bin/controller-gen paths=./... output:crd:artifacts:config=../../../config/crd/bases output:crd:dir=../../../config/crd/bases crd:crdVersions=v1

# cleanup working directory in $apidir
rm -rf ./config

# to root
cd ../../../

version=$(make -s version | tr '[:upper:]' '[:lower:]')
echo "Version for OLM: $version"

deploy_crd_file() {
  source=$1

  # Make a copy to serve as the base for post-processing
  cp "$source" "${source}.orig"

  # Post-process source
  cat ./script/headers/yaml.txt > "$source"
  echo "" >> "$source"
  cat "${source}.orig" | sed -n '/^---/,/^status/p;/^status/q' \
    | sed '1d;$d' \
    | sed 's/^metadata:/metadata:\n  labels:\n    app: yaks/' >> "$source"

  for dest in "${@:2}"; do
    cp "$source" "$dest"
  done

  # Remove the copy as no longer required
  rm -f "${source}.orig"
}

deploy_crd() {
  name=$1
  plural=$2

  deploy_crd_file ./config/crd/bases/yaks.citrusframework.org_"$plural".yaml \
    ./helm/yaks/crds/crd-"$name".yaml \
    ./deploy/olm-catalog/yaks/"$version"/yaks.citrusframework.org_"$plural".yaml
}

deploy_crd instance instances
deploy_crd test tests
