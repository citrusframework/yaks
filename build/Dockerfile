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

FROM eclipse-temurin:17

ARG MAVEN_VERSION="3.9.5"
ARG MAVEN_HOME="/usr/share/maven"
ARG SHA="4810523ba025104106567d8a15a8aa19db35068c8c8be19e30b219a1d7e83bcab96124bf86dc424b1cd3c5edba25d69ec0b31751c136f88975d15406cab3842b"
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
