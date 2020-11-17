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

# Perform the release
release() {
    # Main application directory
    local working_dir="$1"

    # Validate release versions. Release versions have the format "1.3.4"
    local release_version=$(get_release_version "$working_dir/java")
    check_error $release_version

    local snapshot_version=$(get_snapshot_version "$working_dir/java")
    check_error $snapshot_version

    local next_version=$(get_next_snapshot_version "$working_dir/java")
    check_error $next_version

    local image=$(readopt --image)

    echo "==== Starting release $release_version based on $snapshot_version ($working_dir)"

    # Calculate common maven options
    local maven_opts="$(extract_maven_opts)"

    # Set release version in sources
    set_version "$working_dir" "$release_version" "$snapshot_version" "$image"
    update_olm "$working_dir" "$release_version" "$snapshot_version"

    # Cross compile binaries
    local build_dir=${working_dir}/xtmp
    local build_flags=$(readopt --go-flags)

    cross_compile "$working_dir" yaks-${release_version}-linux-64bit "$build_dir" linux amd64 "$build_flags"
    cross_compile "$working_dir" yaks-${release_version}-mac-64bit "$build_dir" darwin amd64 "$build_flags"
    cross_compile "$working_dir" yaks-${release_version}-windows-64bit "$build_dir" windows amd64 "$build_flags"

    # Build and stage artifacts
    build_artifacts "$working_dir" "$release_version" "$maven_opts"

    # For a test run, we are done
    if [ $(hasflag --dry-run -n) ]; then
        if [ ! $(hasflag --snapshot-release) ] && [ ! $(hasflag --local-release) ] && [ ! $(hasflag --keep-staging-repo) ]; then
            drop_staging_repo "$working_dir" "$maven_opts"
        fi

        echo "==== Dry run finished, nothing has been committed"
        echo "==== Use 'git reset --hard' to cleanup"
        exit 0
    fi

    # ========================================================================
    # Commit, tag, release, push
    # --------------------------

    if [ ! $(hasflag --snapshot-release) ] && [ ! $(hasflag --local-release) ]; then
        # Release staging repo
        release_staging_repo "$working_dir" "$maven_opts"
    fi

    # Build Docker image
    mkdir -p ${working_dir}/build/_output/bin
    export GOOS=linux
    eval go build "$build_flags" -o ${working_dir}/build/_output/bin/yaks ${working_dir}/cmd/manager/*.go
    docker build -t ${image}:${release_version} -f ${working_dir}/build/Dockerfile ${working_dir}

    # Push everything (if configured)
    git_push "$working_dir" "$release_version"

    # Push Docker image (if configured)
    docker_push "${working_dir}" "$image" "$release_version"

    # Use next snapshot version for major release only
    if [ $(hasflag --major-release) ] && [ ! $(hasflag --snapshot-release) ]; then
        # Set next snapshot version in sources
        set_next_version "$working_dir" "$next_version" "$snapshot_version"
        update_olm "$working_dir" "$next_version" "$snapshot_version"

        # Push new snapshot version (if configured)
        git_push_next_snapshot "$working_dir" "$next_version"
    fi

    echo "==== Finished release $release_version"
}

docker_push() {
    local working_dir="$1"
    local image="$2"
    local release_version="$3"

    echo "==== Pushing Docker image"
    if [ ! $(hasflag --no-docker-push) ]; then
        echo "Pushing image $image:$release_version"
        # Push docker image
        docker push ${image}:${release_version}
    fi
}

git_push() {
    local working_dir="$1"
    local release_version="$2"
    local tag=v$release_version
    local staging_branch="staging-v$release_version"
    local original_branch=$(git rev-parse --abbrev-ref HEAD)

    if [ $(hasflag --no-git-push) ]; then
        echo "Skip git push"
        return
    fi

    cd $working_dir
    echo "==== Pushing $release_version to GitHub"
    local remote=$(get_git_remote)

    echo "* Create staging branch $staging_branch"
    # Create staging branch
    git branch -D ${staging_branch} || true
    git checkout -b ${staging_branch}
    git add * || true
    git commit -a -m "Release ${release_version}"

    echo "* Using remote git repository '$remote'"

    # push tag to remote
    git tag --force ${tag} ${staging_branch}
    git push --force ${remote} ${tag}
    echo "* Tag ${tag} pushed to ${remote}"

    # Check out original branch
    git checkout ${original_branch}
}

git_push_next_snapshot() {
    local working_dir="$1"
    local next_version="$2"

    if [ $(hasflag --no-git-push) ]; then
        echo "Skip git push"
        return
    fi

    local remote=$(get_git_remote)

    cd $working_dir
    echo "==== Using next napshot version $next_version"
    git add * || true
    git commit -a -m "Use next snapshot version $next_version"

    # Push changes
    git push ${remote}
}

source "$location/util/common_funcs"
source "$location/util/build_funcs"
source "$location/util/git_funcs"
source "$location/util/version_funcs"
source "$location/util/maven_central_funcs"
source "$location/util/go_funcs"
source "$location/util/olm_funcs"

ERROR_FILE="$(mktemp /tmp/yaks-output.XXXXXX)"
trap "print_error $ERROR_FILE" EXIT

if [ $(hasflag --verbose) ]; then
    export PS4='+($(basename ${BASH_SOURCE[0]}):${LINENO}): ${FUNCNAME[0]:+${FUNCNAME[0]}(): }'
    set -x
fi

cd $rootdir

release "$rootdir"