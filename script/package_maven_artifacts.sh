#!/bin/sh

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

set -e

location=$(dirname $0)

cd ${location}/../java

mkdir -p $PWD/../build/_maven_repository
mkdir -p $PWD/../build/_maven_project

# copy YAKS Maven project to image, this will be the runtime for the tests
echo Copy YAKS runtime ...

./mvnw \
    --quiet \
    -f runtime/yaks-runtime-maven/pom.xml \
    clean

cp -r runtime/yaks-runtime-maven $PWD/../build/_maven_project

# fresh build YAKS java modules
echo Build YAKS modules ...

./mvnw \
    clean \
    install $@

# install YAKS Maven extension to runtime project in image
echo Install YAKS Maven extension

mkdir -p $PWD/../build/_maven_project/yaks-runtime-maven/.mvn
mv $PWD/../build/_maven_project/yaks-runtime-maven/extensions.xml $PWD/../build/_maven_project/yaks-runtime-maven/.mvn/

# copy all dependencies to image
echo Copy project dependencies ...

./mvnw \
    --quiet \
    -f runtime/yaks-runtime-maven/pom.xml \
    -DskipTests \
    -Plocal-settings \
    resources:copy-resources

./mvnw \
    --quiet \
    -f runtime/yaks-runtime-maven/pom.xml \
    -s runtime/yaks-runtime-maven/target/settings_local.xml \
    -DskipTests \
    -Dmaven.repo.local=$PWD/../build/_maven_repository \
    de.qaware.maven:go-offline-maven-plugin:1.2.7:resolve-dependencies

# remove some of the tracking files Maven puts in the repository we created above
echo Clean tracking files ...

./mvnw \
    --quiet \
    -Dimage.repository.directory=$PWD/../build/_maven_repository \
    -Plocal-settings \
    clean:clean

# install YAKS Maven extension to image
echo Install YAKS Maven extension ...

./mvnw \
    --quiet \
    -f tools/pom.xml \
    -DskipTests \
    -Dmaven.repo.local=$PWD/../build/_maven_repository \
    install

# install YAKS runtime to image
echo Install YAKS runtime ...

./mvnw \
    --quiet \
    -f runtime/pom.xml \
    -Dmaven.repo.local=$PWD/../build/_maven_repository \
    install
