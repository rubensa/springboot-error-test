package org.eu.rubensa.springboot.error;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.databind.JsonNode;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

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
public class TestRestTemplateConstraintViolationExceptionTest {
  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  public void testConstraintViolationException() throws Exception {
    String content = "{\"value2\": \"\"}";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> request = new HttpEntity<>(content, headers);

    final ResponseEntity<JsonNode> response = restTemplate.exchange("/test/validation", HttpMethod.POST, request,
        JsonNode.class);
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(500);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Internal Server Error");
    Assertions.assertThat(jsonResponse.findValue("message").asText())
        .contains("doSomething.requestBody.value2: size must be between 1 and 255");
    Assertions.assertThat(jsonResponse.findValue("message").asText())
        .contains("doSomething.requestBody.value: must not be null");
    Assertions.assertThat(jsonResponse.findValue("message").asText())
        .contains("doSomething.requestBody: Values not equal");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/test/validation");
  }

  @Test
  void testConstraintViolationExceptionForParameter() throws Exception {
    UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/test/validation/parameter-validation")
        .queryParam("page", -1);
    final ResponseEntity<JsonNode> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, null,
        JsonNode.class);
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(500);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Internal Server Error");
    Assertions.assertThat(jsonResponse.findValue("message").asText())
        .contains("someGetMethod.page: must be greater than or equal to 0");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/test/validation/parameter-validation");
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
      private final TestService service;

      public TestController(TestService service) {
        this.service = service;
      }

      @PostMapping("/test/validation")
      public void doPostWithoutValidation(@RequestBody TestRequestBody requestBody) {
        service.doSomething(requestBody);
      }
    }

    @RestController
    @Validated
    public static class TestParameterValidationController {
      @GetMapping("/test/validation/parameter-validation")
      public void someGetMethod(@RequestParam("page") @Min(value = 0) int page) {
      }
    }

    @Service
    @Validated
    public static class TestService {
      void doSomething(@Valid TestRequestBody requestBody) {
      }
    }

    @ValuesEqual
    public static class TestRequestBody {
      @NotNull
      private String value;
      @NotNull
      @Size(min = 1, max = 255)
      private String value2;

      public String getValue() {
        return value;
      }

      public void setValue(String value) {
        this.value = value;
      }

      public String getValue2() {
        return value2;
      }

      public void setValue2(String value2) {
        this.value2 = value2;
      }
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ValuesEqualValidator.class)
    public @interface ValuesEqual {
      String message() default "Values not equal";

      Class<?>[] groups() default {};

      Class<? extends Payload>[] payload() default {};
    }

    public static class ValuesEqualValidator implements ConstraintValidator<ValuesEqual, TestRequestBody> {
      @Override
      public boolean isValid(TestRequestBody requestBody, ConstraintValidatorContext context) {
        return Objects.equals(requestBody.getValue(), requestBody.getValue2());
      }
    }
  }
}
