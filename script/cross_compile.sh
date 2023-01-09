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

location=$(dirname $0)
working_dir=$(realpath ${location}/../)
build_dir=$(realpath ${working_dir}/xtmp)

if [ "$#" -ne 2 ]; then
    echo "usage: $0 version build_flags"
    exit 1
fi

version="$1"
build_flags="$2"

source "$location/util/go_funcs"

cd $working_dir

cross_compile "$working_dir" yaks-${version}-linux-64bit "$build_dir" linux amd64 "$build_flags"
cross_compile "$working_dir" yaks-${version}-mac-64bit "$build_dir" darwin amd64 "$build_flags"
cross_compile "$working_dir" yaks-${version}-mac-arm64bit "$build_dir" darwin arm64 "$build_flags"
cross_compile "$working_dir" yaks-${version}-windows-64bit "$build_dir" windows amd64 "$build_flags"
