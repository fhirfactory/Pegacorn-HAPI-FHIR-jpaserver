package ca.uhn.fhir.jpa.starter;

import org.apache.commons.lang3.StringUtils;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

public class ApiKeyClientInterceptor implements IClientInterceptor {

    private String apiKeyHeaderName; 
    private String apiKey; 
    
    public ApiKeyClientInterceptor(String apiKeyHeaderName, String apiKey) {
        if (StringUtils.isBlank(apiKeyHeaderName)) {
            throw new IllegalArgumentException("The apiKeyHeaderName parameter must be specified");
        }
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalArgumentException("The apiKey parameter must be specified");
        }

        this.apiKeyHeaderName = apiKeyHeaderName;
        this.apiKey = apiKey;
    }    

    @Override
    public void interceptRequest(IHttpRequest theRequest) {
        theRequest.addHeader(apiKeyHeaderName, apiKey);
    }

    @Override
    public void interceptResponse(IHttpResponse theResponse) {
        // nothing
    }
}
