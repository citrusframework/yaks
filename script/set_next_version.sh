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

# Exit if any error occurs
# Fail on a single failed command in a pipeline (if supported)
set -o pipefail

# Save global script args, use "help" as default
if [ -z "$1" ]; then
    ARGS=("help")
else
    ARGS=("$@")
fi

# Fail on error and undefined vars (please don't use global vars, but evaluation of functions for return values)
set -eu

location=$(dirname $0)
working_dir=$(realpath ${location}/../)

source "$location/util/common_funcs"
source "$location/util/version_funcs"
source "$location/util/olm_funcs"

snapshot_version=$(get_snapshot_version "$working_dir/java")
check_error $snapshot_version

next_version=$(get_next_snapshot_version "$working_dir/java")
check_error $next_version

set_next_version "$working_dir" "$next_version" "$snapshot_version"
update_olm "$working_dir" "$next_version" "$snapshot_version"