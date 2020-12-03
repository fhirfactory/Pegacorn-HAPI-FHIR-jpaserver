package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.empi.rules.config.EmpiRuleValidator;
import ca.uhn.fhir.jpa.empi.config.EmpiConsumerConfig;
import ca.uhn.fhir.jpa.empi.config.EmpiSubmitterConfig;
import ca.uhn.fhir.jpa.empi.svc.EmpiSearchParamSvc;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class ApplicationContext extends AnnotationConfigWebApplicationContext {

  public ApplicationContext() {
    FhirVersionEnum fhirVersion = HapiProperties.getFhirVersion();
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

    if (HapiProperties.getSubscriptionWebsocketEnabled()) {
      register(WebsocketDispatcherConfig.class);
    }

    if (HapiProperties.getSubscriptionEmailEnabled()
      || HapiProperties.getSubscriptionRestHookEnabled()
      || HapiProperties.getSubscriptionWebsocketEnabled()) {
      register(SubscriptionSubmitterConfig.class);
      register(SubscriptionProcessorConfig.class);
      register(SubscriptionChannelConfig.class);
    }
    
    if (HapiProperties.getEmpiEnabled()) {
      register(EmpiSubmitterConfig.class);
      register(EmpiConsumerConfig.class);
      register(EmpiConfig.class);
    } else {
        // For some reason after pulling from hapi-fhir-jpaserver-starter v5.1.0, had to register these two beans to avoid
        // the application startup exception: 
        // Context initialization failed: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating 
        // bean with name 'empiSettings' defined in class path resource [ca/uhn/fhir/jpa/starter/EmpiConfig.class]: 
        // Unsatisfied dependency expressed through method 'empiSettings' parameter 0; nested exception is 
        // org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 
        // 'ca.uhn.fhir.empi.rules.config.EmpiRuleValidator' available: expected at least 1 bean which qualifies as autowire 
        // candidate. Dependency annotations: {}
        register(EmpiSearchParamSvc.class);
        register(EmpiRuleValidator.class);        
    }

  }

}
