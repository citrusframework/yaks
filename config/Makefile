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

#
# Include the main common Makefile containing
# basic common recipes and vars
#
include common/Makefile

#
# Allows for resources to be loaded from outside the root location of
# the kustomize config file. Ensures that resource don't need to be
# copied around the file system.
#
# See https://kubectl.docs.kubernetes.io/faq/kustomize
#
KOPTIONS := --load-restrictor LoadRestrictionsNone

TMPL := tmpl
GEN := gen
YAML := yaml

PACKAGE := yaks-operator
BUNDLE_DIR := bundle
PKG := ../pkg
CRD := crd/bases
MANAGER := manager
MANIFESTS := manifests
RBAC_KUBE := rbac-kubernetes
RBAC_OS := rbac-openshift
SCRIPT := ./script

OP_PATCHES := operator/patches
IMAGE_PULL_POLICY_PATCH := image-pull-policy-patch
WATCH_NAMESPACE_PATCH := watch-namespace-patch
KUBE_ROLES := camel-k knative kubernetes servicemonitors binding-camel-k binding-knative binding-kubernetes binding-servicemonitors
OS_ROLES := openshift binding-openshift

# The namespace of the installation
NAMESPACE ?= yaks

# Image pull policy: [IfNotPresent|Always]
IMAGE_PULL_POLICY ?= IfNotPresent

# Global: [true|false]
# - On operator: set namespace to all using WATCH_NAMESPACE env var
GLOBAL ?= false

#
# Vars that can be overridden by external env vars
#
DEBUG ?= false

#
# Unless overridden, determine the platform of the cluster
# based on connection to it from the installed client
#
PLATFORM ?= $(shell $(SCRIPT)/platform_check.sh)

#
# Macro for editing kustomization to define
# the image reference
#
# Parameter: directory of the kustomization.yaml
#
define set-kustomize-image
	$(if $(filter $(DEFAULT_IMAGE):$(DEFAULT_TAG),$(IMAGE_NAME):$(TAG)),,\
		@cd $(1) && $(KUSTOMIZE) edit set image $(DEFAULT_IMAGE)=$(IMAGE_NAME):$(TAG))
endef

#
# Macro for editing kustomization to define
# the namespace
#
# Parameter: directory of the kustomization.yaml
#
define set-kustomize-namespace
	@cd $(1) && $(KUSTOMIZE) edit set namespace $(NAMESPACE)
endef

.PHONY: check-admin setup .operator-patches operator infrastructure example

#
# Checks if the cluster user has the necessary privileges to be a cluster-admin
# In this case if the user can list the CRDs then probably a cluster-admin
#
check-admin: kubectl
	@output=$$(kubectl get crd 2>&1) || (echo "**** Cannot continue as user is not a Cluster-Admin ****"; exit 1)

#
# Modify the rbac-kubernetes resource definition depending on GLOBAL
#
.setup-kubernetes:
# Resets back to default by removing any reference to rbac-openshift
	@cd setup && $(KUSTOMIZE) edit remove resource ../$(RBAC_OS)
ifeq ($(GLOBAL),true)
	@cd setup && \
		$(KUSTOMIZE) edit remove resource ../$(RBAC_KUBE) && \
		$(KUSTOMIZE) edit add resource ../$(RBAC_KUBE)-global
else
	@cd setup && \
		$(KUSTOMIZE) edit add resource ../$(RBAC_KUBE) && \
		$(KUSTOMIZE) edit remove resource ../$(RBAC_KUBE)-global
endif

#
# Modify the rbac-openshift resource definition depending on GLOBAL
#
.setup-openshift: .setup-kubernetes
ifeq ($(GLOBAL),true)
	@cd setup && \
		$(KUSTOMIZE) edit remove resource ../$(RBAC_OS) && \
		$(KUSTOMIZE) edit add resource ../$(RBAC_OS)-global
else
	@cd setup && \
		$(KUSTOMIZE) edit add resource ../$(RBAC_OS) && \
		$(KUSTOMIZE) edit remove resource ../$(RBAC_OS)-global
endif

#
# Setup the installation by installing crds, roles and granting
# privileges for the installing user.
#
# PARAMETERS:
#   NAMESPACE:         Sets the namespace for the operator resources
#   GLOBAL:            Converts all roles & bindings to cluster-level
#   PLATFORM:          Override the platform check to specify the type of cluster [kubernetes|openshift]
#   DEBUG:             Prints the resources rather than applies them [true|false]
#
setup: kustomize kubectl check-admin .setup-$(PLATFORM)
# Set the namespace in the setup kustomization yaml
	@$(call set-kustomize-namespace,$@)
#
# Build the resources
# Post-process RoleBindings to fix the namespace in the refs (not yet handled by kustomize)
# Either apply to the cluster or output to CLI
#
ifeq ($(DEBUG), false)
	@$(KUSTOMIZE) build $(KOPTIONS) $@ | \
		sed 's/placeholder/$(NAMESPACE)/' | \
		kubectl apply -f -
else
	@$(KUSTOMIZE) build $(KOPTIONS) $@ | \
		sed 's/placeholder/$(NAMESPACE)/'
endif

.operator-patches:
# Set the image reference of the kustomization
	@$(call set-kustomize-image,operator)
# Generate the ImagePullPolicy patch
	@sed 's/{IMAGE_PULL_POLICY}/$(IMAGE_PULL_POLICY)/' $(OP_PATCHES)/$(IMAGE_PULL_POLICY_PATCH).$(TMPL) > $(OP_PATCHES)/$(IMAGE_PULL_POLICY_PATCH).$(GEN).$(YAML)
# Generate the Watch Namespace patch depending on the value of the GLOBAL property
ifeq ($(GLOBAL), true)
	@sed 's/{WATCH_NAMESPACE}/\"\"/' $(OP_PATCHES)/$(WATCH_NAMESPACE_PATCH).$(TMPL) > $(OP_PATCHES)/$(WATCH_NAMESPACE_PATCH).$(GEN).$(YAML)
else
	@sed 's/{WATCH_NAMESPACE}/$(NAMESPACE)/' $(OP_PATCHES)/$(WATCH_NAMESPACE_PATCH).$(TMPL) > $(OP_PATCHES)/$(WATCH_NAMESPACE_PATCH).$(GEN).$(YAML)
endif

#
# Install the operator deployment and related resources
#
# PARAMETERS:
#   NAMESPACE:         Sets the namespace for the operator resources
#   IMAGE_NAME         Sets the image of the operator
#   TAG:               Sets the tag of the image of the operator
#   IMAGE_PULL_POLICY: Change the pull policy of the operator image [IfNotPresent|Always]
#   GLOBAL:            Changes the Watch Namespace property to watch all namespaces [true|false]
#   DEBUG:             Prints the resources rather than applies them [true|false]
#
operator: kustomize kubectl .operator-patches
# Set the namespace in the operator kustomization yaml
	@$(call set-kustomize-namespace,$@)
#
# Build the resources
# Either apply to the cluster or output to CLI
#
ifeq ($(DEBUG), false)
	@$(KUSTOMIZE) build $(KOPTIONS) $@ | kubectl apply -f -
else
	@$(KUSTOMIZE) build $(KOPTIONS) $@
endif

#
# Installs the operator deployment and in addition installs a default CRs
#
# PARAMETERS:
#   NAMESPACE:         Sets the namespace for the operator resources
#   IMAGE:             Sets the image of the operator
#   TAG:               Sets the tag of the image of the operator
#   IMAGE_PULL_POLICY: Change the pull policy of the operator image [IfNotPresent|Always]
#   GLOBAL:            Changes the Watch Namespace property to watch all namespaces [true|false]
#   DEBUG:             Prints the resources rather than applies them [true|false]
#
#
example: kustomize kubectl .operator-patches
# Set the namespace in the operator kustomization yaml
	@$(call set-kustomize-namespace,$@)
#
# Build the resources
# Either apply to the cluster or output to CLI
#
ifeq ($(DEBUG), false)
	@$(KUSTOMIZE) build $(KOPTIONS) $@ | kubectl apply -f -
else
	@$(KUSTOMIZE) build $(KOPTIONS) $@
endif

#
##################################
# B U N D L E  G E N E R A T I O N
##################################
#
.PHONY: bundle manifests generate

# Default bundle image tag
BUNDLE_IMG ?= $(IMAGE_NAME)-bundle:$(VERSION)

# Options for 'bundle-build'
ifneq ($(origin CHANNELS), undefined)
BUNDLE_CHANNELS := --channels=$(CHANNELS)
endif
ifneq ($(origin DEFAULT_CHANNEL), undefined)
BUNDLE_DEFAULT_CHANNEL := --default-channel=$(DEFAULT_CHANNEL)
endif
ifneq ($(origin PACKAGE), undefined)
BUNDLE_PACKAGE := --package=$(PACKAGE)
endif
BUNDLE_METADATA_OPTS ?= $(BUNDLE_CHANNELS) $(BUNDLE_DEFAULT_CHANNEL) $(BUNDLE_PACKAGE)

# Produce CRDs that work back to Kubernetes 1.11 (no version conversion)
CRD_OPTIONS ?= "crd:trivialVersions=true"

# Get the currently used golang install path (in GOPATH/bin, unless GOBIN is set)
ifeq (,$(shell go env GOBIN))
GOBIN=$(shell go env GOPATH)/bin
else
GOBIN=$(shell go env GOBIN)
endif

# Generate code
generate: generate-deepcopy generate-crds

TIMESTAMP=$(shell date -u '+%F %T %Z')

# Generate bundle manifests and metadata, then validate generated files.
bundle: generate-crds kustomize
ifeq ($(PLATFORM),openshift)
	@cd $(MANIFESTS) && $(KUSTOMIZE) edit add resource ../$(RBAC_OS)
else
	@cd $(MANIFESTS) && $(KUSTOMIZE) edit remove resource ../$(RBAC_OS)
endif
# (Re)-generate the manifests directory
	@operator-sdk generate kustomize manifests \
		--apis-dir $(PKG)/apis \
		--input-dir $(MANIFESTS) \
		--output-dir $(MANIFESTS)
# Sets the operator image to the preferred image:tag
	@$(call set-kustomize-image,$(MANIFESTS))
# Generates the bundle complete with manifests
	@$(KUSTOMIZE) build $(KOPTIONS) $(MANIFESTS) | \
		operator-sdk generate bundle \
			-q --overwrite --version $(VERSION) \
			--kustomize-dir $(MANIFESTS) $(BUNDLE_METADATA_OPTS)
# Updates the container-image attribute
	@sed -i 's~containerImage:.*~containerImage: $(IMAGE_NAME):$(TAG)~' $(BUNDLE_DIR)/$(MANIFESTS)/*.clusterserviceversion.yaml
# Sets the createdAt timestamp
	@sed -i 's/createdAt:.*/createdAt: "$(TIMESTAMP)"/' $(BUNDLE_DIR)/$(MANIFESTS)/*.clusterserviceversion.yaml
# Add licence headers to bundle manifest directory
	@$(SCRIPT)/add_license.sh $(BUNDLE_DIR)/$(MANIFESTS) $(SCRIPT)/headers/yaml.txt
# Moves the docker file into the bundle directory
	@mv bundle.Dockerfile $(BUNDLE_DIR)/Dockerfile && \
	sed -i 's/bundle\///g' $(BUNDLE_DIR)/Dockerfile
	@operator-sdk bundle validate $(BUNDLE_DIR)

# Build the bundle image.
.PHONY: bundle-build
bundle-build: bundle
	cd bundle && docker build -f Dockerfile -t $(BUNDLE_IMG) .

clean:
	find . -name "*.gen.*" -delete
	rm -rf bundle
