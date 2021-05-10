package org.eu.rubensa.springboot.error;

import com.fasterxml.jackson.databind.JsonNode;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The @SpringBootTest annotation will load the fully ApplicationContext. This
 * will not use slicing and scan for all the stereotype annotations
 * (@Component, @Service, @Respository and @Controller / @RestController) and
 * loads the full application context.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
    // From Spring 2.3.0 "server.error.include-message" and
    // "server.error.include-binding-errors" is set to "never"
    properties = { "server.error.include-message=always" })
/**
 * Exclude a specific Auto-configuration class from tests' configuration
 */
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
public class TestRestTemplateHttpMessageNotReadableExceptionTest {
  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  public void testHttpMessageNotReadableException() throws Exception {
    String content = "{invalidjsonhere}";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> request = new HttpEntity<>(content, headers);

    final ResponseEntity<JsonNode> response = restTemplate.exchange("/test/validation", HttpMethod.POST, request,
        JsonNode.class);
    /**
     * The exception is among standard Spring MVC exceptions so it is handled by
     * {@link DefaultHandlerExceptionResolver}.
     * <p>
     * The servlet container (Tomcat) checks that the response has an error so it
     * redirects to the error page. In this case, the Spring Boot
     * {@link BasicErrorController}.
     */
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(400);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Bad Request");
    Assertions.assertThat(jsonResponse.findValue("message").asText())
        .contains("JSON parse error: Unexpected character ('i' (code 105))");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/test/validation");
  }

  /**
   * A nested @Configuration class wild be used instead of the application’s
   * primary configuration.
   * <p>
   * Unlike a nested @Configuration class, which would be used instead of your
   * application’s primary configuration, a nested @TestConfiguration class is
   * used in addition to your application’s primary configuration.
   */
  @Configuration
  /**
   * Tells Spring Boot to start adding beans based on classpath settings, other
   * beans, and various property settings.
   */

  @EnableAutoConfiguration
  /**
   * The @ComponentScan tells Spring to look for other components, configurations,
   * and services in the the TestWebConfig package, letting it find the
   * TestController class.
   * <p>
   * We only want to test the classes defined inside this test configuration
   */
  static class TestConfig {
    @RestController
    public static class TestController {
      @PostMapping("/test/validation")
      public void doPostWithoutValidation(@RequestBody TestRequestBody requestBody) {
      }
    }

    public static class TestRequestBody {
      private String value;

      public String getValue() {
        return value;
      }

      public void setValue(String value) {
        this.value = value;
      }
    }
  }
}
