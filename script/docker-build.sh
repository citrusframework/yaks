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

if [ "$#" -ne 4 ]; then
    echo "usage: $0 image_name image_arch version build_flags"
    exit 1
fi

location=$(dirname $0)
image_name="$1"
image_arch="$2"
release_version="$3"
build_flags="$4"

cd $location/..

export GOOS=linux
export GOARCH=${image_arch}
export CGO_ENABLED=0

docker buildx build --platform=linux/${image_arch} --build-arg IMAGE_ARCH=${image_arch} --load -t ${image_name}-${image_arch}:${release_version} -f build/Dockerfile .
docker tag ${image_name}-${image_arch}:${release_version} ${image_name}:${release_version}