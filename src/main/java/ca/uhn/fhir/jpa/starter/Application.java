package ca.uhn.fhir.jpa.starter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;

public class Application extends AnnotationConfigWebApplicationContext {

@Autowired
AppProperties appProperties;

  public Application() {
    FhirVersionEnum fhirVersion = appProperties.getFhir_version();
    if (fhirVersion == FhirVersionEnum.DSTU2) {
      register(FhirServerConfigDstu2.class, FhirServerConfigCommon.class);
    } else if (fhirVersion == FhirVersionEnum.DSTU3) {
      register(FhirServerConfigDstu3.class, FhirServerConfigCommon.class);
    } else if (fhirVersion == FhirVersionEnum.R4) {
      register(FhirServerConfigR4.class, FhirServerConfigCommon.class);
    } else if (fhirVersion == FhirVersionEnum.R5) {
      register(FhirServerConfigR5.class, FhirServerConfigCommon.class);
    } else {
      throw new IllegalStateException();
    }
    
    AppProperties.Subscription nested = new AppProperties.Subscription();
    if (nested.getWebsocket_enabled()) {
        register(WebsocketDispatcherConfig.class);
      }
    
    if (nested.getResthook_enabled()
    	|| nested.getWebsocket_enabled()) {
    	register(SubscriptionSubmitterConfig.class);
    	register(SubscriptionProcessorConfig.class);
    	register(SubscriptionChannelConfig.class);
    	    }

  }

}
