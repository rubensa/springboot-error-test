package org.eu.rubensa.springboot.error;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    properties = { "server.error.include-message=always",
        // (never/alway/on-trace-param) by default is never
        "server.error.include-stacktrace=on-trace-param" })
public class TestRestTemplateExceptionTest {
  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  public void givenNotFound_whenGetSpecificException_thenNotFoundCode() throws Exception {
    String exceptionParam = "not_found";

    final ResponseEntity<JsonNode> response = restTemplate.exchange("/exception/{exception_id}", HttpMethod.GET, null,
        JsonNode.class, exceptionParam);
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(404);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Not Found");
    Assertions.assertThat(jsonResponse.findValue("message").asText()).isEqualTo("resource not found");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/exception/not_found");
  }

  @Test
  public void givenBadArguments_whenGetSpecificException_thenBadRequest() throws Exception {
    String exceptionParam = "bad_arguments";

    final ResponseEntity<JsonNode> response = restTemplate.exchange("/exception/{exception_id}", HttpMethod.GET, null,
        JsonNode.class, exceptionParam);
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(400);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Bad Request");
    Assertions.assertThat(jsonResponse.findValue("message").asText()).isEqualTo("bad arguments");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/exception/bad_arguments");
  }

  @Test
  public void givenOther_whenGetSpecificException_thenInternalServerError() throws Exception {
    String exceptionParam = "dummy";

    final ResponseEntity<JsonNode> response = restTemplate.exchange("/exception/{exception_id}", HttpMethod.GET, null,
        JsonNode.class, exceptionParam);
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(500);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Internal Server Error");
    Assertions.assertThat(jsonResponse.findValue("message").asText()).isEqualTo("internal error");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/exception/dummy");
  }

  @Test
  public void givenNoStatus_whenGetSpecificException_thenInternalServerError() throws Exception {
    String exceptionParam = "no_status";

    final ResponseEntity<JsonNode> response = restTemplate.exchange("/exception/{exception_id}", HttpMethod.GET, null,
        JsonNode.class, exceptionParam);
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(500);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Internal Server Error");
    Assertions.assertThat(jsonResponse.findValue("message").asText()).isEqualTo("no status");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/exception/no_status");
  }

  @Test
  public void givenChained_whenGetSpecificException_thenInternalServerError() throws Exception {
    String exceptionParam = "chained";

    final ResponseEntity<JsonNode> response = restTemplate.exchange("/exception/{exception_id}?trace=true",
        HttpMethod.GET, null, JsonNode.class, exceptionParam);
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(500);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Internal Server Error");
    Assertions.assertThat(jsonResponse.findValue("message").asText()).isEqualTo("chained exception");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/exception/chained");
    Assertions.assertThat(jsonResponse.findValue("trace").asText()).contains("child IOException message");
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
    public class TestController {
      @GetMapping("/exception/{exception_id}")
      public void getSpecificException(@PathVariable("exception_id") String pException) {
        if ("not_found".equals(pException)) {
          throw new ResourceNotFoundException("resource not found");
        } else if ("bad_arguments".equals(pException)) {
          throw new BadArgumentsException("bad arguments");
        } else if ("no_status".equals(pException)) {
          throw new NoStatusException("no status");
        } else if ("chained".equals(pException)) {
          throw new ChainedException("chained exception", new IOException("child IOException message"));
        } else {
          throw new InternalException("internal error");
        }
      }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public class BadArgumentsException extends RuntimeException {
      public BadArgumentsException(String message) {
        super(message);
      }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public class InternalException extends RuntimeException {
      public InternalException(String message) {
        super(message);
      }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public class ResourceNotFoundException extends RuntimeException {
      public ResourceNotFoundException(String message) {
        super(message);
      }
    }

    public class NoStatusException extends RuntimeException {
      public NoStatusException(String message) {
        super(message);
      }
    }

    public class ChainedException extends RuntimeException {
      public ChainedException(String message, Throwable cause) {
        super(message, cause);
      }
    }
  }
}
