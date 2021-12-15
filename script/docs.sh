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
rootdir=$(realpath ${location}/../)

# Perform the docs release
generate_docs() {
    # Main application directory
    local working_dir="$1"

    # Validate release versions. Release versions have the format "1.3.4"
    local release_version=$(get_release_version "$working_dir/java")
    check_error $release_version

    local snapshot_version=$(get_snapshot_version "$working_dir/java")
    check_error $snapshot_version

    local next_version=$(get_next_snapshot_version "$working_dir/java")
    check_error $next_version

    if [ ! $(hasflag --local) ]; then
        if [ ! $(hasflag --dry-run -n) ]; then
            # Verify that there are no modified file in git repo
            check_git_clean "$working_dir"
        fi

        set_docs_version "$working_dir" "$release_version" "$snapshot_version"
    fi

    # Calculate common maven options
    local maven_opts="$(docs_maven_opts)"

    echo "==== Generate docs $release_version ($working_dir)"

    cd "$working_dir/java"

    if [ $(hasflag --local) ]; then
        echo "==== Generate and release to local docs"
        ./mvnw ${maven_opts} package -Dproject.docs.version=${release_version}

        open_url "file:///$working_dir/java/target/generated-docs/index.html"
    elif [ $(hasflag --dry-run -n) ]; then
        echo "==== Generate and release dry-run"
        ./mvnw ${maven_opts} package -Dproject.docs.version=${release_version}
    else
        echo "==== Generate and release to GitHub pages"
        ./mvnw ${maven_opts} verify -Dproject.docs.version=${release_version}
    fi

    if [ ! $(hasflag --local) ]; then
        if [ $(hasflag --major-release) ]; then
            update_docs_version "$working_dir" "$release_version" "$next_version"
        else
            update_docs_version "$working_dir" "$release_version" "$snapshot_version"
        fi

        if [[ "$release_version" != "$snapshot_version" ]]; then
            # Commit overview.adoc for new version
            git_commit "$working_dir" overview.adoc "Add docs version for $release_version"
        fi
    fi

    echo "==== Finished docs release $release_version"
}

docs_maven_opts() {
    local profiles="docs-html"
    local maven_opts="--batch-mode -V -e -N"

    if [ $(hasflag --quiet -q) ]; then
        maven_opts="$maven_opts -q"
    fi

    if [ ! $(hasflag --html-only) ]; then
        profiles="$profiles,docs-pdf"
    fi

    if [ ! $(hasflag --local) ]; then
        profiles="$profiles,release-docs"
    fi

    maven_opts="$maven_opts -P$profiles"

    echo $maven_opts
}

update_docs_version() {
    local working_dir="$1"
    local version="$2"
    local snapshot_version="$3"
    local file="$working_dir/java/docs/overview.adoc"

    if [[ "$version" == "$snapshot_version" ]]; then
        return
    fi

    echo "Updating to version: $version"

    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        sed -i "18p" $file
        sed -i "18s/$version/$snapshot_version/g" $file
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # Mac OSX
        sed -i '' "18p" $file
        sed -i '' "18s/$version/$snapshot_version/g" $file
    fi
}

set_docs_version() {
    local working_dir="$1"
    local version="$2"
    local snapshot_version="$3"
    local file="$working_dir/java/docs/overview.adoc"

    if [[ "$version" == "$snapshot_version" ]]; then
        echo "Using version: $version"
        return
    fi

    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        sed -i "s/$snapshot_version/$version/g" $file
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # Mac OSX
        sed -i '' "s/$snapshot_version/$version/g" $file
    fi
}

source "$location/util/common_funcs"
source "$location/util/git_funcs"
source "$location/util/version_funcs"

ERROR_FILE="$(mktemp /tmp/yaks-output.XXXXXX)"
trap "print_error $ERROR_FILE" EXIT

if [ $(hasflag --verbose) ]; then
    export PS4='+($(basename ${BASH_SOURCE[0]}):${LINENO}): ${FUNCNAME[0]:+${FUNCNAME[0]}(): }'
    set -x
fi

cd $rootdir

generate_docs "$rootdir"