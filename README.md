# Pegacorn-HAPI-FHIR-jpaserver

This project is a fork of https://github.com/hapifhir/hapi-fhir-jpaserver-starter with modifications to allow it to run on [wildfly](https://wildfly.org/) and to use [pegacorn-postgresql](https://github.com/fhirfactory/pegacorn-postgresql) as the backing database. Pegacorn uses this project:
1. As a persistance service for [FHIR](https://www.hl7.org/fhir/) resources. See Design/DESIGN-Highly-available-Postgres-in-a-site-using-petasos.txt for more detail. 
2. To expose a REST API to interact with the FHIR resources

The following certificates need to be provided to the docker image via a host volume (defined in the hostPathCerts helm chart variable), so all possible nodes that can host this docker container must have these available:
1. $HAPI_DATASOURCE_USER.pk8 - the private key of the user to connect to the postgres database with.  As detailed on https://jdbc.postgresql.org/documentation/head/connect.html this must be in PKCS-8 DER format.
2. $HAPI_DATASOURCE_USER.cer - client certificate of the user to connect to the postgres database with
3. ca.cer - the Certificate Authority certificate chain to trust
The certificates can be created by following [How to create certificates](https://github.com/fhirfactory/pegacorn-postgresql#how-to-create-certificates).  If you are using Docker for Windows, the instructions at the following link can be used to [Copy the host-files to the DockerDesktop VM](https://github.com/fhirfactory/pegacorn-postgresql#how-to-create-certificates).

# To create the docker image
```
cd <To the directory where you have git cloned this project>
# NOTE: junit tests are skipped as instructed in https://github.com/hapifhir/hapi-fhir-jpaserver-starter#postgresql-configuration
mvn clean install -DskipTests
docker build --rm -t pegacorn/pegacorn-hapi-fhir-jpaserver:4.1.0-custom-snapshot --file Dockerfile.wildfly .
```

# To run on an existing Kubernetes cluster with helm
NOTE: these instructions assume that [pegacorn-postgresql](https://github.com/fhirfactory/pegacorn-postgresql) has already been deployed to the Kubernetes cluster.
```
# If you are running a Highly Available Cluster of Kubernetes nodes, a Kubernetes secret to connect to your docker registry will need to be created
  kubectl create secret docker-registry acr-secret-site-a --namespace site-a --docker-server=https://<your docker registry>.azurecr.io --docker-username=<service-principal-username> --docker-password="<service-principal-password>"
  # and then on each of the helm commands the following values must be added to the comma delimited list of values:
  # ,acrSecretName=acr-secret-site-a,dockerRepo=<your docker registry>.azurecr.io/,imagePullPolicy=Always
# If you want to be able to log into the Wildfly Administrator Console, the following values
  # ,wildflyAdminUser=admin,wildflyAdminPwd=Admin123
  
cd <To the directory where you have git cloned this project>
helm upgrade pegacorn-fhirplace-gateway-site-a --install --namespace site-a --set serviceName="pegacorn-fhirplace-gateway",nodeAffinityLabel=fhirplace,basePort=30002,imageTag=4.1.0-custom-snapshot,dataSourceServiceName=pegacorn-fhirplace,dataSourcePortAndDBName="30000/hapi",hostPathCerts=/kube-vols/certificates,dbUser=pegacorn,jvmMaxHeapSizeMB=768,wildflyLogLevel=INFO helm

helm upgrade pegacorn-hestia-gateway-site-a --install --namespace site-a --set serviceName="pegacorn-hestia-gateway",nodeAffinityLabel=hestia,basePort=30012,imageTag=4.1.0-custom-snapshot,dataSourceServiceName=pegacorn-hestia,dataSourcePortAndDBName="30010/hapi",hostPathCerts=/data/certificates,dbUser=pegacorn,jvmMaxHeapSizeMB=768,wildflyLogLevel=INFO helm
```

# To test everything is working
View http://localhost:30002/hapi-fhir-jpaserver/resource?serverId=home&pretty=false&resource=Patient
Click on the CRUD Operations button and Create with JSON:
```
{
  "resourceType": "Patient",
  "active": true,
  "name": [
    {
      "use": "official",
      "family": "Chalmers",
      "given": [
        "Peter",
        "James"
      ]
    },
    {
      "use": "usual",
      "given": [
        "Jim"
      ]
    },
    {
      "use": "maiden",
      "family": "Windsor",
      "given": [
        "Peter",
        "James"
      ],
      "period": {
        "end": "2002"
      }
    }
  ]
}
```
View http://localhost:30002/hapi-fhir-jpaserver/search?serverId=home&pretty=true&resource=Patient&param.0.qualifier=&param.0.0=&param.0.name=_language&param.0.type=string&sort_by=&sort_direction=&resource-search-limit= and confirm the patient has been created.

If the values for the Wildfly Administrator Console have been specified in the helm command the, the Wildfly Administrator Console will be accessible at: http://localhost:30003/console/index.html