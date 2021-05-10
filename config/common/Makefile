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

VERSION := 0.5.0
SNAPSHOT_VERSION := $(VERSION)-SNAPSHOT
LAST_RELEASED_VERSION := 0.2.0
DEFAULT_IMAGE := docker.io/citrusframework/yaks
DEFAULT_TAG := $(VERSION:.0=)
IMAGE_NAME ?= $(DEFAULT_IMAGE)
TAG ?= $(DEFAULT_TAG)
CHANNELS ?= alpha
DEFAULT_CHANNEL ?= alpha

PKG := ../../pkg
CRD := ../crd/bases
SCRIPT := ../script

.PHONY: kubectl kustomize controller-gen generate generate-deepcopy generate-crds

kubectl:
ifeq (, $(shell which kubectl))
$(error "No kubectl found in PATH. Please install and re-run")
endif

kustomize:
ifeq (, $(shell which kustomize))
	@{ \
	set -e ;\
	KUSTOMIZE_GEN_TMP_DIR=$$(mktemp -d) ;\
	cd $$KUSTOMIZE_GEN_TMP_DIR ;\
	go mod init tmp ;\
	go get sigs.k8s.io/kustomize/kustomize/v4@v4.0.5 ;\
	rm -rf $$KUSTOMIZE_GEN_TMP_DIR ;\
	}
KUSTOMIZE=$(GOBIN)/kustomize
else
KUSTOMIZE=$(shell which kustomize)
endif

# find or download controller-gen
# download controller-gen if necessary
controller-gen:
ifeq (, $(shell which controller-gen))
	@{ \
	set -e ;\
	CONTROLLER_GEN_TMP_DIR=$$(mktemp -d) ;\
	cd $$CONTROLLER_GEN_TMP_DIR ;\
	go mod init tmp ;\
	go get sigs.k8s.io/controller-tools/cmd/controller-gen@v0.4.1 ;\
	rm -rf $$CONTROLLER_GEN_TMP_DIR ;\
	}
CONTROLLER_GEN=$(GOBIN)/controller-gen
else
CONTROLLER_GEN=$(shell which controller-gen)
endif

generate-deepcopy: controller-gen
	@$(CONTROLLER_GEN) paths="$(PKG)/..." object

generate-crds: controller-gen
	@$(CONTROLLER_GEN) crd crd:crdVersions=v1 \
		paths=$(PKG)/apis/... \
		output:artifacts:config=$(CRD) \
		output:crd:dir=$(CRD)
	@sed -i '/creationTimestamp:/a\  labels:\n    app: yaks' \
		$(CRD)/yaks*.yaml
	@sed -i '/creationTimestamp: null/d' $(CRD)/yaks*.yaml
	@$(SCRIPT)/add_license.sh $(CRD) $(SCRIPT)/headers/yaml.txt kustomization.yaml
