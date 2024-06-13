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

location=$(dirname $0)
rootdir=$(realpath ${location}/../)

GO111MODULE=on

# Entering the client module
cd $rootdir/pkg/client/yaks

rm -rf ./clientset
rm -rf ./informers
rm -rf ./listers

echo "Generating Go client code..."

$(go env GOPATH)/bin/client-gen \
	--input=yaks/v1alpha1 \
	--go-header-file=$rootdir/script/headers/default.txt \
	--clientset-name "versioned"  \
	--input-base=github.com/citrusframework/yaks/pkg/apis \
	--output-dir=clientset \
	--output-pkg=github.com/citrusframework/yaks/pkg/client/yaks/clientset

$(go env GOPATH)/bin/lister-gen \
  $rootdir/pkg/apis/yaks/v1alpha1 \
	--go-header-file=$rootdir/script/headers/default.txt \
	--output-dir=listers \
	--output-pkg=github.com/citrusframework/yaks/pkg/client/yaks/listers

$(go env GOPATH)/bin/informer-gen \
  $rootdir/pkg/apis/yaks/v1alpha1 \
  --versioned-clientset-package=github.com/citrusframework/yaks/pkg/client/yaks/clientset/versioned \
	--listers-package=github.com/citrusframework/yaks/pkg/client/yaks/listers \
	--go-header-file=$rootdir/script/headers/default.txt \
	--output-dir=informers \
	--output-pkg=github.com/citrusframework/yaks/pkg/client/yaks/informers

# hack to fix test custom resource generated fake. otherwise generated fake file is handled as test scoped file
mv $rootdir/pkg/client/yaks/clientset/versioned/typed/yaks/v1alpha1/fake/fake_test.go \
   $rootdir/pkg/client/yaks/clientset/versioned/typed/yaks/v1alpha1/fake/fake_tests.go
