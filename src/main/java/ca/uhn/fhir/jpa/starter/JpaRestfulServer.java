package ca.uhn.fhir.jpa.starter;

import javax.servlet.ServletException;

public class JpaRestfulServer extends BaseJpaRestfulServer {

  private static final long serialVersionUID = 1L;

  public static JpaRestfulServer INSTANCE = null;
  public static final String API_KEY_HEADER_NAME = "x-api-key";
  
  @Override
  protected void initialize() throws ServletException {
    super.initialize();

    INSTANCE = this;


    ApiKeyValidatorInterceptor apiKeyValidatorInterceptor = new ApiKeyValidatorInterceptor(API_KEY_HEADER_NAME, "HAPI_API_KEY");
    registerInterceptor(apiKeyValidatorInterceptor);
    
  }

}
