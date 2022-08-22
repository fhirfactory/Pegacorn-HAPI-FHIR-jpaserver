package ca.uhn.fhir.jpa.starter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import javax.servlet.ServletException;

@Import(AppProperties.class)
public class JpaRestfulServer extends BaseJpaRestfulServer {

  @Autowired
  AppProperties appProperties;

  private static final long serialVersionUID = 1L;
  public static JpaRestfulServer INSTANCE = null;
  public static final String API_KEY_HEADER_NAME = "x-api-key";

  public JpaRestfulServer() {
    super();
  }

  @Override
  protected void initialize() throws ServletException {
    super.initialize();
    
 // Add your own customization here
    
    INSTANCE = this;
    ApiKeyValidatorInterceptor apiKeyValidatorInterceptor = new ApiKeyValidatorInterceptor(API_KEY_HEADER_NAME, "HAPI_API_KEY");
    registerInterceptor(apiKeyValidatorInterceptor);

  }

}
