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

if [ "$#" -ne 0 ]; then
    echo "usage: $0"
    exit 1
fi

# disable gomodules when executing go clean:
#
#    https://github.com/golang/go/issues/31002
#
GO111MODULE=off go clean

# remove built binaries
rm -f license-check
rm -f yaks
rm -f yaks-*

# Dir clean
rm -rf dist
rm -rf bundle

#remove build outputs
rm -rf build/_maven_repository
rm -rf build/_maven_project
rm -rf build/_output
mkdir -p build/_maven_repository
mkdir -p build/_maven_project
mkdir -p build/_output

# Maven clean

cd $rootdir/java && ./mvnw \
    -q \
    clean
