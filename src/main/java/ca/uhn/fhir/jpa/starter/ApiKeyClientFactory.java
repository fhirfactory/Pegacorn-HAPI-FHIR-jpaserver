package ca.uhn.fhir.jpa.starter;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.util.ITestingUiClientFactory;

public class ApiKeyClientFactory implements ITestingUiClientFactory {

    private String apiKeyHeaderName; 
    
    public ApiKeyClientFactory(String apiKeyHeaderName) {
        if (StringUtils.isBlank(apiKeyHeaderName)) {
            throw new IllegalArgumentException("The apiKeyHeaderName parameter must be specified");
        }

        this.apiKeyHeaderName = apiKeyHeaderName;
    }    
    
    @Override
    public IGenericClient newClient(FhirContext theFhirContext, HttpServletRequest theRequest, String theServerBaseUrl) {
        // Create a client
        IGenericClient client = theFhirContext.newRestfulGenericClient(theServerBaseUrl);

        String apiKey = theRequest.getHeader(apiKeyHeaderName);
        if (StringUtils.isBlank(apiKey)) {
            apiKey = ApiKeyValidatorInterceptor.getCookieValue(apiKeyHeaderName, theRequest);
        }
        if (StringUtils.isBlank(apiKey)) {
            apiKey = ApiKeyValidatorInterceptor.getAuthHeaderValue(theRequest);
        }

        if (StringUtils.isNotBlank(apiKey)) {
            client.registerInterceptor(new ApiKeyClientInterceptor(apiKeyHeaderName, apiKey));
        }

        return client;
    }
}
