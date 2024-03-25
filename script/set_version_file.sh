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

# Fail on error and undefined vars (please don't use global vars, but evaluation of functions for return values)
set -eu

if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then
    echo "usage: $0 version snapshot_version [image_name]"
    exit 1
fi

location=$(dirname $0)
working_dir=$(realpath ${location}/../)

version="$1"
snapshot_version="$2"
image_name=${3:-}

source "$location/util/common_funcs"
source "$location/util/version_funcs"

set_version_file "$working_dir" "$version" "$snapshot_version" "$image_name"
