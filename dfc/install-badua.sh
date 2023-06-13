#!/bin/sh

BADUA_GROUP_ID="br.usp.each.saeg"
BADUA_VERSION="0.8.1"
BADUA_AGENT_RT="./ba-dua-agent-rt-0.8.1-all.jar"
BADUA_CLI="./ba-dua-cli-0.8.1-all.jar"

mvn install:install-file \
        -DgroupId=${BADUA_GROUP_ID} \
        -DartifactId=ba-dua-agent-rt \
        -Dversion=${BADUA_VERSION} \
        -Dclassifier=all \
        -Dpackaging=jar \
        -Dfile=${BADUA_AGENT_RT}

mvn install:install-file \
        -DgroupId=${BADUA_GROUP_ID} \
        -DartifactId=ba-dua-cli \
        -Dversion=${BADUA_VERSION} \
        -Dpackaging=jar \
        -Dfile=${BADUA_CLI}
