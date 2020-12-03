#!/usr/bin/env bash
# NOTE: this file should have Unix (LF) EOL conversion performed on it to avoid: "env: can't execute 'bash ': No such file or directory"

echo "Staring setup-env-then-start-wildfly-as-jboss.sh as user $(whoami) with params $@"

echo "DOCKER IMAGE_BUILD_TIMESTAMP=${IMAGE_BUILD_TIMESTAMP}"
echo "HELM_RELEASE_TIME=${HELM_RELEASE_TIME}"

# Copy the certificate files based on
# 1. https://stackoverflow.com/questions/55072221/deploying-postgresql-docker-with-ssl-certificate-and-key-with-volumes
# 2. https://itnext.io/postgresql-docker-image-with-ssl-certificate-signed-by-a-custom-certificate-authority-ca-3df41b5b53

echo "Copying certificates like /var/lib/pegacorn-ssl-certs/$HAPI_DATASOURCE_USERNAME.* to /etc/ssl/certs/"

cp /var/lib/pegacorn-ssl-certs/$HAPI_DATASOURCE_USERNAME.pk8 /etc/ssl/certs/
cp /var/lib/pegacorn-ssl-certs/$HAPI_DATASOURCE_USERNAME.cer /etc/ssl/certs/
cp /var/lib/pegacorn-ssl-certs/ca.cer /etc/ssl/certs/pegacorn-ca.cer

chmod 400 /etc/ssl/certs/$HAPI_DATASOURCE_USERNAME.pk8
chown jboss:jboss /etc/ssl/certs/$HAPI_DATASOURCE_USERNAME.pk8 
chmod 400 /etc/ssl/certs/$HAPI_DATASOURCE_USERNAME.cer
chown jboss:jboss /etc/ssl/certs/$HAPI_DATASOURCE_USERNAME.cer 
chmod 400 /etc/ssl/certs/pegacorn-ca.cer
chown jboss:jboss /etc/ssl/certs/pegacorn-ca.cer 

ls -la /etc/ssl/certs/

mkdir -p /var/lib/pegacorn-keystores
cp /var/lib/pegacorn-ssl-certs/$KUBERNETES_SERVICE_NAME.$MY_POD_NAMESPACE.jks /var/lib/pegacorn-keystores/keystore.jks
cp /var/lib/pegacorn-ssl-certs/$KUBERNETES_SERVICE_NAME.$MY_POD_NAMESPACE-truststore.jks /var/lib/pegacorn-keystores/truststore.jks

chmod 400 /var/lib/pegacorn-keystores/keystore.jks
chown jboss:jboss /var/lib/pegacorn-keystores/keystore.jks 
chmod 400 /var/lib/pegacorn-keystores/truststore.jks
chown jboss:jboss /var/lib/pegacorn-keystores/truststore.jks 

ls -la /var/lib/pegacorn-keystores/

# As we are connecting to postgres on the same host as hapi-fhir, add the host file entry mapping to the dynamically assigned
# IP address of the host, so verify-full ssl mode can be used (otherwise only verify-ca ssl mode could be used as the hostname
# in the JDBC connecting string would be the host IP which wouldn't match the subject common name of the server certificate 
# of the postgres instance
echo "Adding hosts entry to /etc/hosts $MY_HOST_IP $DATASOURCE_SERVICE_NAME.$MY_POD_NAMESPACE"

echo "$MY_HOST_IP $DATASOURCE_SERVICE_NAME.$MY_POD_NAMESPACE" >> /etc/hosts
cat /etc/hosts

export HAPI_DATASOURCE_URL="jdbc:postgresql://${DATASOURCE_SERVICE_NAME}.${MY_POD_NAMESPACE}:${DATASOURCE_PORT_AND_DBNAME}?ssl=true&sslmode=verify-full&sslcert=/etc/ssl/certs/${HAPI_DATASOURCE_USERNAME}.cer&sslkey=/etc/ssl/certs/${HAPI_DATASOURCE_USERNAME}.pk8&sslrootcert=/etc/ssl/certs/pegacorn-ca.cer&sslpassword=${DB_USER_KEY_PASSWORD}"

# then start /start-wildfly.sh script as jboss user
# NOTE: gosu is used instead of su-exec as the wildfly docker image is based on centos, whereas the postgres one is based on alpine,
# and the Alpine su-exec program is a substitute for gosu (see https://devops.stackexchange.com/a/5242 and
# https://github.com/docker-library/postgres/blob/33bccfcaddd0679f55ee1028c012d26cd196537d/12/docker-entrypoint.sh line 281 vs
# https://github.com/docker-library/postgres/blob/33bccfcaddd0679f55ee1028c012d26cd196537d/12/alpine/docker-entrypoint.sh line 281
exec gosu jboss "/start-wildfly.sh" "$@"
