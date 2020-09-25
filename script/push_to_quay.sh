#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Error invoking push_to_quay.sh: version required"
    exit 1
fi
if [ -z "$QUAY_USERNAME" ]; then
	echo "Environment variable QUAY_USERNAME missing"
	exit 2
fi
if [ -z "$QUAY_PASSWORD" ]; then
	echo "Environment variable QUAY_PASSWORD missing"
	exit 3
fi

VERSION=$1

PACKAGE=yaks
ORGANIZATION=citrusframework

location=$(dirname $0)
rootdir=$(realpath ${location}/..)

export AUTH_TOKEN=$(curl -sH "Content-Type: application/json" -XPOST https://quay.io/cnr/api/v1/users/login -d '{"user": {"username": "'"${QUAY_USERNAME}"'", "password": "'"${QUAY_PASSWORD}"'"}}' | jq -r '.token')

operator-courier --verbose push $rootdir/deploy/olm-catalog/${PACKAGE}/ ${ORGANIZATION} ${PACKAGE} ${VERSION} "$AUTH_TOKEN"
