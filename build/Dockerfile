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

ARG MAVEN_VERSION="3.6.3"
ARG SHA="c35a1803a6e70a126e80b2b3ae33eed961f83ed74d18fcd16909b2d44d7dada3203f1ffe726c17ef8dcca2dcaa9fca676987befeadc9b9f759967a8cb77181c0"
ARG BASE_URL="https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries"

ENV OPERATOR=/usr/local/bin/yaks \
    OPERATOR_ARGS=operator \
    USER_UID=1001 \
    USER_NAME=yaks \
    HOME=/root \
    APP_DIR=/deployments/data/yaks-runtime-maven \
    APP_SETTINGS=/deployments/artifacts \
    APP_LIBS=/deployments/artifacts/m2

RUN mkdir -p /usr/share/maven \
    && curl -Lso /tmp/maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    && echo "${SHA} /tmp/maven.tar.gz" | sha512sum -c - \
    && tar -xzC /usr/share/maven --strip-components=1 -f /tmp/maven.tar.gz \
    && rm -v /tmp/maven.tar.gz \
    && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

# install operator binary
COPY build/_output/bin/yaks ${OPERATOR}

COPY build/bin /usr/local/bin

# add dependencies
COPY build/_maven_repository ${APP_LIBS}

# add YAKS runtime
COPY build/_maven_project/yaks-runtime-maven ${APP_DIR}

COPY build/settings.xml ${APP_SETTINGS}/

USER 0
RUN  /usr/local/bin/user_setup

RUN chgrp -R 0 ${APP_LIBS} && \
    chmod -R g=u ${APP_LIBS} && \
    chgrp -R 0 ${APP_DIR} && \
    chmod -R g=u ${APP_DIR} && \
    chgrp -R 0 ${APP_SETTINGS} && \
    chmod -R g=u ${APP_SETTINGS}

# Let's not use ENTRYPOINT so we can override libs in the base image

USER ${USER_UID}
