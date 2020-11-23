package ca.uhn.fhir.jpa.starter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;

/**
 * Validate that the correct ApiKey has been provided in HTTP request as either
 * 1. a HTTP Header value
 * 2. a Cookie value (to allow for ease of use with a browser e.g. in the browser's java console run:
 *    document.cookie="x-api-key={API_KEY}; path=/" )
 * 
 * @author Jasen Schremmer
 */
public class ApiKeyValidatorInterceptor extends InterceptorAdapter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiKeyValidatorInterceptor.class);
    
    private static final String ALLOW_INSECURE_REQUESTS = "ALLOW_INSECURE_REQUESTS";
    
    private String apiKeyHeaderName; 
    private String apiKeyValue;
    private boolean allowInsecureRequests;
    
    public ApiKeyValidatorInterceptor(String apiKeyHeaderName, String apiKeyEnvironmentVariableName) {
        if (StringUtils.isBlank(apiKeyHeaderName)) {
            throw new IllegalArgumentException("The apiKeyHeaderName parameter must be specified");
        }
        if (StringUtils.isBlank(apiKeyEnvironmentVariableName)) {
            throw new IllegalArgumentException("The apiKeyEnvironmentVariableName parameter must be specified");
        }

        this.apiKeyHeaderName = apiKeyHeaderName;
        apiKeyValue = System.getenv(apiKeyEnvironmentVariableName);
        if (StringUtils.isBlank(apiKeyValue)) {
            throw new IllegalArgumentException("The System Environment Variable " + apiKeyEnvironmentVariableName + " must be specified");
        }

        String allowInsecureRequestsStr = System.getenv(ALLOW_INSECURE_REQUESTS);
        if (StringUtils.isBlank(allowInsecureRequestsStr)) {
            allowInsecureRequests = false;
        } else {
            allowInsecureRequests = Boolean.parseBoolean(allowInsecureRequestsStr);
        }
        log.warn("allowInsecureRequests={}", allowInsecureRequests);
    }
    
    @Override
    public boolean incomingRequestPreProcessed(HttpServletRequest theRequest, HttpServletResponse theResponse) {
        String requestURL = theRequest.getRequestURL().toString().trim();
        log.debug("In ApiKeyValidatorInterceptor http request={}", requestURL);
        
        if (! (allowInsecureRequests || theRequest.isSecure())) {
            log.warn("Rejecting request " + requestURL + " as it was not secure");            
            throw new ForbiddenOperationException("Insecure requests are forbidden");            
        }
        
        String apiKeyHeaderValue = theRequest.getHeader(apiKeyHeaderName);
        if (apiKeyValue.equals(apiKeyHeaderValue)) {
            return true;
        }

        String apiKeyCookieValue = getCookieValue(apiKeyHeaderName, theRequest);
        if (apiKeyValue.equals(apiKeyCookieValue)) {
            return true;
        }

        String apiKeyAuthValue = getAuthHeaderValue(theRequest);
        if (apiKeyValue.equals(apiKeyAuthValue)) {
            return true;
        }
        
        log.warn("Rejecting request " + requestURL + " that did not have a valid apiKeyValue.  Header Value='" + 
                apiKeyHeaderValue + "', Cookie Value='" + apiKeyCookieValue + "', Auth Value='" + apiKeyAuthValue + "'");
        
        throw new AuthenticationException();
    }
    
    public static String getCookieValue(String cookieName, HttpServletRequest theRequest) {
        Cookie[] cookies = theRequest.getCookies();
        if (log.isDebugEnabled()) {            
            log.debug("Cookies are: " + getCookieArrayToString(cookies));            
        }
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            } 
        }
        return null;
    }

    public static String getCookieArrayToString(Cookie[] cookies) {
        if (cookies == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder(128);        
        for (Cookie cookie : cookies) {
            if (sb.length() > 0) {
                sb.append(",");        
            }
            sb.append(" [").append(cookie.getName()).append("=").append(cookie.getValue()).append("] ");        
        }
        return "{" + sb.toString() + "}";
    }

    private static final int AUTH_HEADER_PREFIX_LENGTH = Constants.HEADER_AUTHORIZATION_VALPREFIX_BEARER.length(); 
    public static String getAuthHeaderValue(HttpServletRequest theRequest) {
        // Reverse logic of ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor
        String apiKeyAuth = theRequest.getHeader(Constants.HEADER_AUTHORIZATION);

        if (StringUtils.isNotBlank(apiKeyAuth) && (apiKeyAuth.length() > AUTH_HEADER_PREFIX_LENGTH)) {
            return apiKeyAuth.substring(AUTH_HEADER_PREFIX_LENGTH);
        }
        return null;
    }
}