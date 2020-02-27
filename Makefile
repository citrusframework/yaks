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

IMAGE_NAME := docker.io/yaks/yaks
VERSION := devel

GOLDFLAGS += -X main.GitCommit=$(GIT_COMMIT)
GOFLAGS = -ldflags "$(GOLDFLAGS)" -gcflags=-trimpath=$(GO_PATH) -asmflags=-trimpath=$(GO_PATH)

default: test

clean:
	./hack/clean.sh

generate:
	operator-sdk generate k8s

build: build-yaks

test: build
	go test ./...

build-yaks:
	go build $(GOFLAGS) -o yaks ./cmd/manager/*.go

build-resources:
	./hack/embed_resources.sh deploy

cross-compile:
	./hack/cross_compile.sh $(VERSION)

docker-image:
	mkdir -p build/_output/bin
	operator-sdk build $(IMAGE_NAME):$(VERSION)

images-no-test: package-artifacts-no-test docker-image

images: test package-artifacts docker-image

images-push:
	docker push $(IMAGE_NAME):$(VERSION)

release: clean images cross-compile images-push

package-artifacts-no-test:
	./hack/package_maven_artifacts.sh -DskipTests

package-artifacts:
	./hack/package_maven_artifacts.sh

.PHONY: clean build build-yaks cross-compile test docker-image images images-no-test images-push package-artifacts package-artifacts-no-test release
