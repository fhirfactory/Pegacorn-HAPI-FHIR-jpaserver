FROM fhirfactory/pegacorn-base-docker-wildfly:1.1.0

# Copy and run the cli scripts to modify the standalone.xml configuration.
COPY cli/ssl-configuration.cli $JBOSS_HOME/bin/ssl-configuration.cli

# After running each command the content of the "$JBOSS_HOME/standalone/configuration/standalone_xml_history/current" directory
# needs to be deleted as each steps expects it to be empty.  Maybe there is another way??
#Temporarily disable client certificate authentication with a SSL certificate
#RUN $JBOSS_HOME/bin/jboss-cli.sh --file=$JBOSS_HOME/bin/ssl-configuration.cli && \
#    rm -rf $JBOSS_HOME/standalone/configuration/standalone_xml_history/current/*

# deploy the application
COPY target/*.war $JBOSS_HOME/standalone/deployments/

COPY setup-env-then-start-wildfly-as-jboss.sh /
COPY start-wildfly.sh /

ARG IMAGE_BUILD_TIMESTAMP
ENV IMAGE_BUILD_TIMESTAMP=${IMAGE_BUILD_TIMESTAMP}
RUN echo IMAGE_BUILD_TIMESTAMP=${IMAGE_BUILD_TIMESTAMP}

USER root
# Install gosu based on
# 1. https://gist.github.com/rafaeltuelho/6b29827a9337f06160a9
# 2. https://github.com/tianon/gosu
# 3. https://github.com/tianon/gosu/releases/download/1.12/gosu-amd64
COPY gosu-amd64 /usr/local/bin/gosu
RUN chmod +x /usr/local/bin/gosu && \
	chmod +x /setup-env-then-start-wildfly-as-jboss.sh && \
   	chmod +x /start-wildfly.sh

CMD	["/setup-env-then-start-wildfly-as-jboss.sh"]
