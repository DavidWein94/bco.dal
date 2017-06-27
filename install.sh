#!/bin/bash
APP_NAME='bco-dal'
clear &&
echo "=== clean ${APP_NAME} ===" &&
mvn clean $@ &&
clear &&
echo "=== install and ${APP_NAME} ===" &&
mvn install \
        -DskipTests=true \
        -Dmaven.test.skip=true \
        -Dlicense.skipAddThirdParty=true \
        -Dlicense.skipUpdateProjectLicense=true \
        -Dlicense.skipDownloadLicenses \
        -Dlicense.skipCheckLicense=true \
        -Dmaven.license.skip=true $@ &&
clear &&
echo "=== ${APP_NAME} is successfully installed ==="
