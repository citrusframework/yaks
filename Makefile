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

VERSION := 0.12.0-SNAPSHOT
SNAPSHOT_VERSION := 0.12.0-SNAPSHOT
OPERATOR_VERSION := $(subst -SNAPSHOT,,$(VERSION))
LAST_RELEASED_IMAGE_NAME := yaks-operator
LAST_RELEASED_VERSION := 0.10.0

CONTROLLER_GEN_VERSION := v0.6.1
OPERATOR_SDK_VERSION := v1.14.0
KUSTOMIZE_VERSION := v4.1.2
DEFAULT_IMAGE := docker.io/citrusframework/yaks
IMAGE_NAME ?= $(DEFAULT_IMAGE)

RELEASE_GIT_REMOTE := upstream
GIT_COMMIT := $(shell if [ -d .git ]; then git rev-list -1 HEAD; else echo "$(CUSTOM_VERSION)"; fi)
LINT_GOGC := 20
LINT_DEADLINE := 10m

# olm bundle vars
OLM_CHANNELS := alpha
OLM_DEFAULT_CHANNEL := alpha
OLM_PACKAGE := yaks

CSV_VERSION := $(VERSION:-SNAPSHOT=)
CSV_NAME := $(OLM_PACKAGE).v$(CSV_VERSION)
# Final CSV name that replaces the name required by the operator-sdk
# Has to be replaced after the bundle has been generated
CSV_PRODUCTION_NAME := $(OLM_PACKAGE)-operator.v$(CSV_VERSION)
CSV_DISPLAY_NAME := YAKS Operator
CSV_SUPPORT := Citrus Framework
CSV_REPLACES := $(LAST_RELEASED_IMAGE_NAME).v$(LAST_RELEASED_VERSION)
CSV_FILENAME := $(OLM_PACKAGE).clusterserviceversion.yaml
CSV_PATH := config/manifests/bases/$(CSV_FILENAME)
CSV_GENERATED_PATH := bundle/manifests/$(CSV_FILENAME)

BUNDLE_IMAGE_NAME := $(IMAGE_NAME)-bundle

# Build
ifdef GIT_COMMIT
GOLDFLAGS += -X github.com/citrusframework/yaks/pkg/util/defaults.GitCommit=$(GIT_COMMIT)
else
$(warning Could not retrieve a valid Git Commit)
endif

GOFLAGS = -ldflags "$(GOLDFLAGS)" -trimpath

.DEFAULT_GOAL := default

default: test

clean:
	./script/clean.sh

check-licenses:
	./script/check_licenses.sh

check-repo:
	./script/check_repo.sh

generate: generate-deepcopy generate-crd generate-client

generate-client:
	./script/gen_client.sh

generate-crd: controller-gen
	CONTROLLER_GEN=$(CONTROLLER_GEN) ./script/gen_crd.sh

generate-deepcopy: controller-gen
	cd pkg/apis/yaks && $(CONTROLLER_GEN) paths="./..." object

lint:
	GOGC=$(LINT_GOGC) golangci-lint run --config .golangci.yml --out-format tab --deadline $(LINT_DEADLINE) --verbose

lint-fix:
	GOGC=$(LINT_GOGC) golangci-lint run --config .golangci.yml --out-format tab --deadline $(LINT_DEADLINE) --fix

build: build-resources build-yaks

go-imports:
	goimports -w ./pkg

test: build
	go test ./...

build-yaks:
	go build $(GOFLAGS) -o yaks ./cmd/manager/*.go

build-resources:
	go generate ./pkg/...

set-version-file:
	./script/set_version_file.sh $(VERSION) $(SNAPSHOT_VERSION) $(IMAGE_NAME)

set-version:
	./script/set_version.sh $(VERSION) $(SNAPSHOT_VERSION) $(IMAGE_NAME)

set-next-version:
	./script/set_next_version.sh --snapshot-version $(SNAPSHOT_VERSION)

cross-compile:
	./script/cross_compile.sh $(VERSION) '$(GOFLAGS)'

docker-build:
	./script/docker-build.sh $(IMAGE_NAME):$(VERSION) '$(GOFLAGS)'

images-no-test: build package-artifacts-no-test docker-build

images: test package-artifacts docker-build

images-push:
	docker push $(IMAGE_NAME):$(VERSION)

prepare-release: check-repo clean check-licenses

release-dry-run: prepare-release
	./script/release.sh --release-version $(VERSION) --snapshot-version $(SNAPSHOT_VERSION) --image $(IMAGE_NAME) --go-flags '$(GOFLAGS)' --git-remote $(RELEASE_GIT_REMOTE) --skip-tests --dry-run --no-git-push --keep-staging-repo

release: prepare-release
	./script/release.sh --release-version $(VERSION) --snapshot-version $(SNAPSHOT_VERSION) --image $(IMAGE_NAME) --go-flags '$(GOFLAGS)' --git-remote $(RELEASE_GIT_REMOTE) --skip-tests

release-local: prepare-release
	./script/release.sh --release-version $(VERSION) --snapshot-version $(SNAPSHOT_VERSION) --image $(IMAGE_NAME) --go-flags '$(GOFLAGS)' --git-remote $(RELEASE_GIT_REMOTE) --local-release --no-git-push --no-docker-push

release-major: prepare-release
	./script/release.sh --release-version $(VERSION) --snapshot-version $(SNAPSHOT_VERSION) --image $(IMAGE_NAME) --go-flags '$(GOFLAGS)' --git-remote $(RELEASE_GIT_REMOTE) --skip-tests --major-release

release-snapshot: prepare-release
	./script/release.sh --snapshot-release --snapshot-version $(SNAPSHOT_VERSION) --image $(IMAGE_NAME) --go-flags '$(GOFLAGS)' --git-remote $(RELEASE_GIT_REMOTE) --no-git-push

release-nightly: prepare-release
	./script/release.sh --snapshot-release --release-version $(VERSION) --snapshot-version $(SNAPSHOT_VERSION) --image $(IMAGE_NAME) --go-flags '$(GOFLAGS)' --git-remote $(RELEASE_GIT_REMOTE) --no-git-push

generate-docs:
	./script/docs.sh docs --release-version $(VERSION) --local --html-only

release-docs:
	./script/docs.sh docs --release-version $(VERSION)

release-docs-major:
	./script/docs.sh docs --release-version $(VERSION) --major-release

release-docs-dry-run:
	./script/docs.sh docs --release-version $(VERSION) --dry-run

package-artifacts-no-test:
	./script/package_maven_artifacts.sh --release-version $(VERSION) --local-release --skip-tests

package-artifacts:
	./script/package_maven_artifacts.sh --release-version $(VERSION) --local-release

snapshot-version:
	@echo $(SNAPSHOT_VERSION)

version:
	@echo $(VERSION)

.PHONY: clean build build-yaks build-resources generate-docs release-docs release-docs-dry-run release-docs-major update-olm cross-compile test docker-build images images-no-test images-push package-artifacts package-artifacts-no-test release release-snapshot set-version-file set-version set-next-version check-repo check-licenses snapshot-version version

# find or download controller-gen if necessary
controller-gen:
ifeq (, $(shell command -v controller-gen 2> /dev/null))
	@{ \
	set -e ;\
	CONTROLLER_GEN_TMP_DIR=$$(mktemp -d) ;\
	cd $$CONTROLLER_GEN_TMP_DIR ;\
	go mod init tmp ;\
	go get sigs.k8s.io/controller-tools/cmd/controller-gen@$(CONTROLLER_GEN_VERSION) ;\
	rm -rf $$CONTROLLER_GEN_TMP_DIR ;\
	}
CONTROLLER_GEN=$(GOBIN)/controller-gen
else
CONTROLLER_GEN=$(shell command -v controller-gen 2> /dev/null)
endif

kustomize:
ifeq (, $(shell command -v kustomize 2> /dev/null))
	@{ \
	set -e ;\
	KUSTOMIZE_GEN_TMP_DIR=$$(mktemp -d) ;\
	cd $$KUSTOMIZE_GEN_TMP_DIR ;\
	go mod init tmp ;\
	go get sigs.k8s.io/kustomize/kustomize/v4@$(KUSTOMIZE_VERSION) ;\
	rm -rf $$KUSTOMIZE_GEN_TMP_DIR ;\
	}
KUSTOMIZE=$(GOBIN)/kustomize
else
KUSTOMIZE=$(shell command -v kustomize 2> /dev/null)
endif

operator-sdk:
ifeq (, $(shell command -v operator-sdk 2> /dev/null))
	@{ \
	set -e ;\
	if [ "$(shell uname -s 2>/dev/null || echo Unknown)" == "Darwin" ] ; then \
		curl \
			-L https://github.com/operator-framework/operator-sdk/releases/download/$(OPERATOR_SDK_VERSION)/operator-sdk_darwin_amd64 \
			-o operator-sdk ; \
	else \
		curl \
			-L https://github.com/operator-framework/operator-sdk/releases/download/$(OPERATOR_SDK_VERSION)/operator-sdk_linux_amd64 \
			-o operator-sdk ; \
	fi ;\
	chmod +x operator-sdk ;\
	mv operator-sdk $(GOBIN)/ ;\
	}
OPERATOR_SDK=$(GOBIN)/operator-sdk
else
	@{ \
	echo -n "operator-sdk already installed: "; \
  operator-sdk version | sed -n 's/.*"v\([^"]*\)".*/\1/p'; \
	echo " If this is less than $(OPERATOR_SDK_VERSION) then please consider moving it aside and allowing the approved version to be downloaded."; \
	}
OPERATOR_SDK=$(shell command -v operator-sdk 2> /dev/null)
endif

.PHONY: generate-crd v1alpha1 pre-bundle bundle bundle-build

v1alpha1: operator-sdk
	@# operator-sdk generate ... cannot execute across separate modules so need to temporarily move api
	$(OPERATOR_SDK) generate kustomize manifests --apis-dir pkg/apis/yaks/v1alpha1 -q
	@# Adds the licence header to the csv file.
	./script/add_license.sh config/manifests/bases ./script/headers/yaml.txt
	./script/add_createdAt.sh config/manifests/bases

#
# Tailor the manifest according to default values for this project
# Note. to successfully make the bundle the name must match that specified in the PROJECT file
#
pre-bundle:
# bundle name must match that which appears in PROJECT file
	@sed -i 's/projectName: .*/projectName: $(OLM_PACKAGE)/' PROJECT
	@sed -i 's~^    containerImage: .*~    containerImage: $(IMAGE_NAME):$(VERSION)~' $(CSV_PATH)
	@sed -i 's/^    support: .*/    support: $(CSV_SUPPORT)/' $(CSV_PATH)
	@sed -i 's/^  name: .*.\(v.*\)/  name: $(CSV_NAME)/' $(CSV_PATH)
	@sed -i 's/^  displayName: .*/  displayName: $(CSV_DISPLAY_NAME)/' $(CSV_PATH)
	@sed -i 's/^  version: .*/  version: $(CSV_VERSION)/' $(CSV_PATH)
	@sed -i 's/^  replaces: .*/  replaces: $(CSV_REPLACES)/' $(CSV_PATH)

bundle: generate-crd v1alpha1 kustomize pre-bundle
	@# Sets the operator image to the preferred image:tag
	@cd config/manifests && $(KUSTOMIZE) edit set image $(IMAGE_NAME)=$(IMAGE_NAME):$(VERSION)
	@# Build kustomize manifests
	@$(KUSTOMIZE) build config/manifests | \
		$(OPERATOR_SDK) generate bundle \
			-q --overwrite --version $(OPERATOR_VERSION) \
			--kustomize-dir config/manifests \
			--channels=$(OLM_CHANNELS) --default-channel=$(OLM_DEFAULT_CHANNEL) --package=$(OLM_PACKAGE)
	@# Move the dockerfile into the bundle directory
	@# Rename the CSV name to conform with the existing released operator versions
	@# This cannot happen in pre-bundle as the operator-sdk generation expects a CSV name the same as PACKAGE
	@mv bundle.Dockerfile bundle/Dockerfile && sed -i 's/bundle\///g' bundle/Dockerfile
	@sed -i "s/^  name: $(CSV_NAME)/  name: $(CSV_PRODUCTION_NAME)/" $(CSV_GENERATED_PATH)
	@# Adds the licence headers to the csv file
	./script/add_license.sh bundle/manifests ./script/headers/yaml.txt
	$(OPERATOR_SDK) bundle validate ./bundle

	cp bundle/manifests/yaks.citrusframework.org_* deploy/olm-catalog/yaks/$(SNAPSHOT_VERSION)/
	cp $(CSV_GENERATED_PATH) deploy/olm-catalog/yaks/$(SNAPSHOT_VERSION)/$(OLM_PACKAGE).v$(subst -SNAPSHOT,-snapshot,$(SNAPSHOT_VERSION)).clusterserviceversion.yaml

# Build the bundle image.
bundle-build: bundle
	cd bundle && docker build -f Dockerfile -t $(BUNDLE_IMAGE_NAME) .
