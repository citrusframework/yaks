#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [ "$#" -ne 2 ]; then
    echo "usage: $0 image:version flags"
    exit 1
fi

location=$(dirname $0)
image="$1"
build_flags="$2"

cd $location/..

export GOOS=linux

mkdir -p build/_output/bin
eval go build "$build_flags" -o build/_output/bin/yaks ./cmd/manager/*.go
docker build -t $image -f build/Dockerfile .