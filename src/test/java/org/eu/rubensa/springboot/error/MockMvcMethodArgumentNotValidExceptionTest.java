package org.eu.rubensa.springboot.error;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Using this annotation will disable full auto-configuration and instead apply
 * only configuration relevant to MVC tests
 * (i.e. @Controller, @ControllerAdvice, @JsonComponent,
 * Converter/GenericConverter, Filter, WebMvcConfigurer and
 * HandlerMethodArgumentResolver beans but not @Component, @Service
 * or @Repository beans).
 * <p>
 * By default, tests annotated with @WebMvcTest will also auto-configure Spring
 * Security and MockMvc.
 * <p>
 * For more fine-grained control of MockMVC the @AutoConfigureMockMvc annotation
 * can be used.
 * <p>
 * By default MockMVC printOnlyOnFailure = true so information is printed only
 * if the test fails.
 */
@WebMvcTest(
    // From Spring 2.3.0 "server.error.include-message" and
    // "server.error.include-binding-errors" is set to "never"
    properties = { "server.error.include-message=always" },
    /**
     * Exclude a specific Auto-configuration class from tests' configuration
     */
    excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class MockMvcMethodArgumentNotValidExceptionTest {
  /**
   * MockMvc is not a real servlet environment, therefore it does not redirect
   * error responses to ErrorController, which produces validation response.
   * <p>
   * See: https://github.com/spring-projects/spring-boot/issues/5574
   */
  @Autowired
  private MockMvc mockMvc;

  @Test
  public void testMethodArgumentNotValidException() throws Exception {
    String content = "{\"value2\": \"\"}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/test/validation").contentType(MediaType.APPLICATION_JSON).content(content))
        /**
         * The exception is among standard Spring MVC exceptions so it is handled by
         * {@link DefaultHandlerExceptionResolver}.
         */
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andExpect(result -> Assertions.assertThat(result.getResolvedException())
            .isInstanceOf(MethodArgumentNotValidException.class))
        .andExpect(result -> Assertions.assertThat(result.getResolvedException().getMessage())
            .contains("Validation failed for argument [0]"));
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
      public void doPost(/**
                          * The @Valid on the method argument in a controller is handled by the
                          * ModelAttributeMethodProcessor internally and leads to a web specific binding
                          * exception, the MethodArgumentNotValidException.
                          * <p>
                          * The ModelAttributeMethodProcessor is called (indirectly) from the
                          * RequestMappingHandlerAdapter when preparing the method invocation. Instead
                          * of @Valid you could also use the @Validated annotation on the method
                          * argument.
                          */
      @Valid @RequestBody TestRequestBody requestBody) {
      }
    }

    public static class TestRequestBody {
      @NotNull
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
