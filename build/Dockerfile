FROM fabric8/s2i-java:3.0-java8

ENV OPERATOR=/usr/local/bin/yaks \
    OPERATOR_ARGS=operator \
    USER_UID=1001 \
    USER_NAME=yaks \
    HOME=/root \
    APP_DIR=/deployments/data/yaks-runtime-maven \
    APP_SETTINGS=/deployments/artifacts \
    APP_LIBS=/deployments/artifacts/m2

# install operator binary
COPY build/_output/bin/yaks ${OPERATOR}

COPY build/bin /usr/local/bin

# TODO create a more efficient way to manage dependencies than to hardcode them
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
