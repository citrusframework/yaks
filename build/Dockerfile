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

FROM adoptopenjdk/openjdk11:slim

ARG MAVEN_VERSION="3.8.6"
ARG MAVEN_HOME="/usr/share/maven"
ARG SHA="f790857f3b1f90ae8d16281f902c689e4f136ebe584aba45e4b1fa66c80cba826d3e0e52fdd04ed44b4c66f6d3fe3584a057c26dfcac544a60b301e6d0f91c26"
ARG BASE_URL="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries"

ENV USER_UID=1001 \
    HOME=/root \
    APP_DIR=/deployments/data/yaks-runtime-maven \
    APP_SETTINGS=/deployments/artifacts \
    APP_LIBS=/deployments/artifacts/m2

USER 0

RUN mkdir -p ${MAVEN_HOME} \
    && curl -Lso /tmp/maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    && echo "${SHA} /tmp/maven.tar.gz" | sha512sum -c - \
    && tar -xzC ${MAVEN_HOME} --strip-components=1 -f /tmp/maven.tar.gz \
    && rm -v /tmp/maven.tar.gz \
    && ln -s ${MAVEN_HOME}/bin/mvn /usr/bin/mvn

COPY build/bin /usr/local/bin

# add dependencies
COPY build/_maven_repository ${APP_LIBS}

# add YAKS runtime
COPY build/_maven_project/yaks-runtime-maven ${APP_DIR}

COPY build/settings.xml ${APP_SETTINGS}/

RUN  /usr/local/bin/user_setup

RUN chgrp -R 0 ${APP_LIBS} && \
    chmod -R g=u ${APP_LIBS} && \
    chgrp -R 0 ${APP_DIR} && \
    chmod -R g=u ${APP_DIR} && \
    chgrp -R 0 ${APP_SETTINGS} && \
    chmod -R g=u ${APP_SETTINGS}

USER ${USER_UID}

# install operator binary
COPY build/_output/bin/yaks /usr/local/bin/yaks
