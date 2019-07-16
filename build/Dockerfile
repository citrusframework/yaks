#FROM registry.access.redhat.com/ubi7/ubi-minimal:latest
FROM fabric8/s2i-java:3.0-java8

ENV OPERATOR=/usr/local/bin/yaks \
    USER_UID=1001 \
    USER_NAME=yaks \
    HOME=/root

ADD build/_maven_output /tmp/artifacts/m2

USER 0
RUN chgrp -R 0 /tmp/artifacts/m2 \
 && chmod -R g=u /tmp/artifacts/m2

# install operator binary
COPY build/_output/bin/yaks ${OPERATOR}

COPY build/bin /usr/local/bin
RUN  /usr/local/bin/user_setup

ENTRYPOINT ["/usr/local/bin/entrypoint"]

USER ${USER_UID}
