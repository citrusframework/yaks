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

    echo "Building virtual file system ..."
    eval go run ${working_dir}/cmd/util/vfs-gen/ config

    # Cross compile binaries
    local build_dir=${working_dir}/xtmp
    local build_flags=$(readopt --go-flags)

    cross_compile "$working_dir" yaks-${release_version}-linux-64bit "$build_dir" linux amd64 "$build_flags"
    cross_compile "$working_dir" yaks-${release_version}-mac-64bit "$build_dir" darwin amd64 "$build_flags"
    cross_compile "$working_dir" yaks-${release_version}-mac-arm64bit "$build_dir" darwin arm64 "$build_flags"
    cross_compile "$working_dir" yaks-${release_version}-windows-64bit "$build_dir" windows amd64 "$build_flags"

    # Update project metadata to new release version
    if [ $(hasflag --major-release) ] && [ ! $(hasflag --snapshot-release) ]; then
        update_project_metadata "$working_dir" "$release_version" "$next_version"
    else
        update_project_metadata "$working_dir" "$release_version" "$snapshot_version"
    fi

    if [ ! $(hasflag --local-release) ] && [ ! $(hasflag --snapshot-release) ]; then
        # Commit project metadata for new version
        git_commit "$working_dir" project.yml "Update project metadata for $release_version"

        # Commit docs overview.adoc
        git_commit "$working_dir" overview.adoc "Update docs overview for $release_version"
    fi

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

    # Build Docker images
    mkdir -p ${working_dir}/build/_output/bin
    docker_build "$working_dir" "$image" "$release_version" amd64 "$build_flags"
    docker_build "$working_dir" "$image" "$release_version" arm64 "$build_flags"
    docker tag ${image}-amd64:${release_version} ${image}:${release_version}

    if [ ! $(hasflag --snapshot-release) ] && [ ! $(hasflag --local-release) ]; then
        # Release staging repo
        # NOTE: not working recently (because of timeouts) - fallback to doing this manually
        # release_staging_repo "$working_dir" "$maven_opts"

        # Push Docker images (if configured)
        if [ ! $(hasflag --no-docker-push) ]; then
            echo "==== Pushing Docker images $image:$release_version"
            # Push docker image
            docker push ${image}-amd64:${release_version}
            docker push ${image}-arm64:${release_version}
            docker push ${image}:${release_version}
            docker manifest create ${image}:${release_version} --amend ${image}-amd64:${release_version} --amend ${image}-arm64:${release_version}
            docker manifest push --purge ${image}:${release_version}
        fi

        # Push everything (if configured)
        git_push "$working_dir" "$release_version"
    fi

    # Use next snapshot version for major release only
    if [ $(hasflag --major-release) ] && [ ! $(hasflag --snapshot-release) ]; then
        # Set next snapshot version in sources
        set_next_version "$working_dir" "$next_version" "$snapshot_version"

        # Update OLM sources with next version
        update_olm "$working_dir" "$next_version" "$snapshot_version"

        # Update to new snapshot version
        update_next_snapshot "$working_dir" "$next_version"
    fi

    if [ ! $(hasflag --local-release) ] && [ ! $(hasflag --no-git-push) ]; then
        # Update docs overview with new snapshot version
        update_docs_overview "$working_dir" "$release_version" "$snapshot_version"

        local remote=$(get_git_remote)

        # Push changes
        git push ${remote}
    fi

    echo "==== Finished release $release_version"
}

docker_build() {
    local working_dir="$1"
    local image="$2"
    local release_version="$3"
    local image_arch="$4"
    local build_flags="$5"

    echo "==== Building Docker image $image-$image_arch:$release_version"
    eval CGO_ENABLED=0 GOOS=linux GOARCH=${image_arch} go build "$build_flags" -o ${working_dir}/build/_output/bin/yaks-${image_arch} ${working_dir}/cmd/manager/*.go
    docker buildx build --platform=linux/${image_arch} --build-arg IMAGE_ARCH=${image_arch} --load -t ${image}-${image_arch}:${release_version} -f ${working_dir}/build/Dockerfile ${working_dir}
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

update_next_snapshot() {
    local working_dir="$1"
    local next_version="$2"

    if [ $(hasflag --no-git-push) ]; then
        echo "Skip git push"
        return
    fi

    cd $working_dir
    echo "==== Using next snapshot version $next_version"
    git add * || true
    git commit -a -m "Use next snapshot version $next_version"
}

update_project_metadata() {
    local working_dir="$1"
    local version="$2"
    local next_version="$3"

    local file="$working_dir/.github/project.yml"

    echo "Update project metadata to version: $version"

    sed -i "s/current-version: .*/current-version: $version/" $file
    sed -i "s/next-version: .*/next-version: $next_version/g" $file

    file="$working_dir/Makefile"

    sed -i "s/LAST_RELEASED_VERSION := .*/LAST_RELEASED_VERSION := $version/" $file

    file="$working_dir/java/tools/yaks-jbang/dist/YaksJBang.java"

    sed -i "s/yaks.jbang.version:.*}/yaks.jbang.version:$version}/" $file
}

update_docs_overview() {
    local working_dir="$1"
    local version="$2"
    local snapshot_version="$3"

    local file="$working_dir/java/docs/overview.adoc"

    # Insert snapshot docs in line 18
    local line=$(sed -n "/$version/s/$version/$snapshot_version/gp" $file)
    sed -i "18 i $line" $file
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
