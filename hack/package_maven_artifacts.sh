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

./mvnw clean install $@

# install YAKS Maven extension to runtime project in image
echo Install YAKS Maven extension

mkdir -p $PWD/../build/_maven_project/yaks-runtime-maven/.mvn
cp tools/maven/yaks-maven-extension/target/classes/extensions.xml $PWD/../build/_maven_project/yaks-runtime-maven/.mvn/

# copy all dependencies to image m2 repository
echo Copy project dependencies ...

./mvnw \
    --quiet \
    -f runtime/yaks-runtime-maven/pom.xml \
    -DskipTests \
    -DoutputDirectory=$PWD/../build/_maven_repository \
    dependency:copy-dependencies

# install YAKS artifacts to image m2 repository
echo Install YAKS artifacts ...

./mvnw \
    -DskipTests \
    -Dmaven.repo.local=$PWD/../build/_maven_repository \
    jar:jar \
    install:install

# preload Maven plugins so they are part of the image
echo Load project plugins ...

./mvnw \
    --quiet \
    -f runtime/yaks-runtime-maven/pom.xml \
    -Dmaven.repo.local=$PWD/../build/_maven_repository \
    test