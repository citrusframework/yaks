#!/bin/sh

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

location=$(dirname $0)

if [ "$#" -ne 0 ]; then
    echo "usage: $0"
    exit 1
fi

# remove built binaries
rm -f yaks
rm -f yaks-*

cd ${location}/../java

# Maven clean

./mvnw \
    clean

cd ../build

#remove build outputs
rm -rf _maven_repository
mkdir -p _maven_repository
rm -rf _maven_project
mkdir -p _maven_project
rm -rf _output
mkdir -p _output