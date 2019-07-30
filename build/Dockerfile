#FROM registry.access.redhat.com/ubi7/ubi-minimal:latest
FROM fabric8/s2i-java:3.0-java8

ENV OPERATOR=/usr/local/bin/yaks \
    OPERATOR_ARGS=operator \
    USER_UID=1001 \
    USER_NAME=yaks \
    HOME=/root

# install operator binary
COPY build/_output/bin/yaks ${OPERATOR}

COPY build/bin /usr/local/bin

USER 0
RUN  /usr/local/bin/user_setup

# TODO create a more efficient way to manage dependencies than to hardcode them
ADD build/_maven_dependencies /deployments/dependencies

ENTRYPOINT ["/usr/local/bin/entrypoint"]

USER ${USER_UID}
