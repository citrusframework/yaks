# ---------------------------------------------------------------------------
# Copyright the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ---------------------------------------------------------------------------

VERSION := 0.20.0-SNAPSHOT
SNAPSHOT_VERSION := 0.20.0-SNAPSHOT
OPERATOR_VERSION := $(subst -SNAPSHOT,,$(VERSION))
LAST_RELEASED_IMAGE_NAME := yaks-operator
LAST_RELEASED_VERSION := 0.20.0

CONTROLLER_GEN_VERSION := v0.15.0
CODEGEN_VERSION := v0.30.2
OPERATOR_SDK_VERSION := v1.28.0
KUSTOMIZE_VERSION := v4.5.4
DEFAULT_IMAGE := docker.io/citrusframework/yaks
IMAGE_NAME ?= $(DEFAULT_IMAGE)
# Check for arm64, aarch64, fallback to amd64
OS_ARCH = $(if $(filter arm64 aarch64,$(shell uname -m)),arm64,amd64)
IMAGE_ARCH ?= $(OS_ARCH)

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

YAKS_JBANG := yaks@citrusframework/yaks

# Build
ifdef GIT_COMMIT
GOLDFLAGS += -X github.com/citrusframework/yaks/pkg/util/defaults.GitCommit=$(GIT_COMMIT)
else
$(warning Could not retrieve a valid Git Commit)
endif

GOLDFLAGS += -X github.com/citrusframework/yaks/pkg/cmd/jbang.YaksApp=$(YAKS_JBANG)
GOLDFLAGS += -X github.com/citrusframework/yaks/pkg/cmd/jbang.YaksVersion=$(VERSION)
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

generate-client: codegen-tools-install
	./script/gen_client.sh

generate-crd: codegen-tools-install
	./script/gen_crd.sh

generate-deepcopy: codegen-tools-install
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
	@echo "####### Building yaks CLI for linux/$(IMAGE_ARCH) architecture..."
	CGO_ENABLED=0 GOOS=linux GOARCH=$(IMAGE_ARCH) go build $(GOFLAGS) -o build/_output/bin/yaks-$(IMAGE_ARCH) ./cmd/manager/*.go
	# Symbolic link to a local CLI
	ln -sf build/_output/bin/yaks-$(IMAGE_ARCH) ./yaks

build-yaks-platform:
	# Perform only when running on OS other than linux
ifneq ($(shell uname -s 2>/dev/null || echo Unknown),Linux)
	@echo "####### Building platform specific yaks CLI for $(OS_ARCH) architecture..."
	CGO_ENABLED=0 GOARCH=$(OS_ARCH) go build $(GOFLAGS) -o build/_output/bin/yaks-$(OS_ARCH) ./cmd/manager/*.go
	# Symbolic link to a local CLI
	ln -sf build/_output/bin/yaks-$(OS_ARCH) ./yaks
endif

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

image-build:
	./script/docker-build.sh $(IMAGE_NAME) $(IMAGE_ARCH) $(VERSION) '$(GOFLAGS)'

images-no-test: build package-artifacts-no-test image-build build-yaks-platform

images: test package-artifacts image-build build-yaks-platform

# Make sure the current docker builder must supports the wanted platform list, which may not be the case for the default builder
#
# docker buildx inspect
# ...
# Platforms: linux/amd64*, linux/arm64*
#
# docker buildx create --name mybuilder --platform linux/amd64,linux/arm64
# docker buildx use mybuilder
images-all:
	make IMAGE_ARCH=arm64 images-no-test
	make IMAGE_ARCH=amd64 images-no-test

images-push:
	docker push $(IMAGE_NAME):$(VERSION)-amd64
	docker push $(IMAGE_NAME):$(VERSION)
	@if docker inspect $(IMAGE_NAME):$(VERSION)-arm64 &> /dev/null; then \
		echo "Image $(IMAGE_NAME):$(VERSION)-arm64 exists, building the multiarch manifest"; \
		docker push $(IMAGE_NAME):$(VERSION)-arm64; \
		docker manifest create $(IMAGE_NAME):$(VERSION) --amend $(IMAGE_NAME):$(VERSION)-amd64 --amend $(IMAGE_NAME):$(VERSION)-arm64; \
		docker manifest push --purge $(IMAGE_NAME):$(VERSION); \
	fi

prepare-release: check-repo clean check-licenses

release-dry-run: prepare-release
	./script/release.sh --release-version $(VERSION) --snapshot-version $(SNAPSHOT_VERSION) --image $(IMAGE_NAME) --go-flags '$(GOFLAGS)' --git-remote $(RELEASE_GIT_REMOTE) --skip-tests --dry-run --no-git-push --keep-staging-repo

release: prepare-release
	./script/release.sh --release-version $(VERSION) --snapshot-version $(SNAPSHOT_VERSION) --image $(IMAGE_NAME) --go-flags '$(GOFLAGS)' --git-remote $(RELEASE_GIT_REMOTE) --skip-tests

release-local: prepare-release
	./script/release.sh --release-version $(VERSION) --snapshot-version $(SNAPSHOT_VERSION) --image $(IMAGE_NAME) --go-flags '$(GOFLAGS)' --git-remote $(RELEASE_GIT_REMOTE) --local-release --no-git-push --no-docker-push

release-major: prepare-release
	./script/release.sh --release-version $(VERSION) --snapshot-version $(SNAPSHOT_VERSION) --next-version $(NEXT_VERSION) --image $(IMAGE_NAME) --go-flags '$(GOFLAGS)' --git-remote $(RELEASE_GIT_REMOTE) --skip-tests --major-release

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

.PHONY: clean build build-yaks build-yaks-platform build-resources generate-docs release-docs release-docs-dry-run release-docs-major update-olm cross-compile test image-build images images-no-test images-all images-push package-artifacts package-artifacts-no-test release release-snapshot set-version-file set-version set-next-version check-repo check-licenses snapshot-version version

codegen-tools-install: controller-gen
	@# We must force the installation to make sure we are using the correct version
	@# Note: as there is no --version in the tools, we cannot rely on cached local versions
	@echo "Installing k8s.io/code-generator tools with version $(CODEGEN_VERSION)"
	go install k8s.io/code-generator/cmd/client-gen@$(CODEGEN_VERSION)
	go install k8s.io/code-generator/cmd/lister-gen@$(CODEGEN_VERSION)
	go install k8s.io/code-generator/cmd/informer-gen@$(CODEGEN_VERSION)

# find or download controller-gen if necessary
controller-gen:
ifeq (, $(shell command -v controller-gen 2> /dev/null))
	go install sigs.k8s.io/controller-tools/cmd/controller-gen@$(CONTROLLER_GEN_VERSION)
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

detect-os:
ifeq '$(findstring ;,$(PATH))' ';'
OS := Windows
OS_LOWER := windows
else
OS := $(shell echo $$(uname 2>/dev/null) || echo Unknown)
OS := $(patsubst CYGWIN%,Cygwin,$(OS))
OS := $(patsubst MSYS%,MSYS,$(OS))
OS := $(patsubst MINGW%,MSYS,$(OS))
OS_LOWER := $(shell echo $(OS) | tr '[:upper:]' '[:lower:]')
endif
ifeq ($(shell uname -m), arm64)
OS_ARCH=arm64
else
OS_ARCH=amd64
endif

operator-sdk: detect-os
	@echo "####### Installing operator-sdk version $(OPERATOR_SDK_VERSION)..."
	set -e ;\
	curl \
		-s -L https://github.com/operator-framework/operator-sdk/releases/download/$(OPERATOR_SDK_VERSION)/operator-sdk_$(OS_LOWER)_amd64 \
		-o operator-sdk ; \
	chmod +x operator-sdk ;\
	mkdir -p $(GOBIN) ;\
	mv operator-sdk $(GOBIN)/ ;
OPERATOR_SDK=$(GOBIN)/operator-sdk

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
