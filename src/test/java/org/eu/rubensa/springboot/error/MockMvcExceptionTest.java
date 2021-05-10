package org.eu.rubensa.springboot.error;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.BadArgumentsException;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.BasicException;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.ChainedException;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.InternalException;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
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
public class MockMvcExceptionTest {
  /**
   * MockMvc is not a real servlet environment, therefore it does not redirect
   * error responses to ErrorController, which produces error response.
   * <p>
   * See: https://github.com/spring-projects/spring-boot/issues/5574
   */
  @Autowired
  private MockMvc mockMvc;

  @Test
  public void givenBasic_whenGetSpecificException_thenExceptionThrown() throws Exception {
    String exceptionParam = "basic";

    Assertions
        .assertThatThrownBy(() -> mockMvc.perform(MockMvcRequestBuilders
            .get("/exception/{exception_id}", exceptionParam).contentType(MediaType.APPLICATION_JSON)))
        /**
         * The exception is not handled by {@link MockMvc} so it is thrown encapsulated
         * inside a NestedServletException.
         */
        .hasCauseInstanceOf(BasicException.class).hasMessageContaining("basic exception");
  }

  @Test
  public void givenChained_whenGetSpecificException_thenExceptionThrown() throws Exception {
    String exceptionParam = "chained";

    Assertions
        .assertThatThrownBy(() -> mockMvc.perform(MockMvcRequestBuilders
            .get("/exception/{exception_id}", exceptionParam).contentType(MediaType.APPLICATION_JSON)))
        /**
         * The exception is not handled by {@link MockMvc} so it is thrown encapsulated
         * inside a NestedServletException.
         */
        .hasCauseInstanceOf(ChainedException.class).hasMessageContaining("chained exception")
        .hasRootCauseInstanceOf(IOException.class).getRootCause().hasMessageContaining("child IOException message");
  }

  @Test
  public void givenNotFound_whenGetSpecificException_thenNotFoundCode() throws Exception {
    String exceptionParam = "not_found";

    mockMvc
        .perform(MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam)
            .contentType(MediaType.APPLICATION_JSON))
        /**
         * The Exception is annotated with {@link ResponseStatus} so it is handled by
         * {@link ResponseStatusExceptionResolver}.
         */
        .andExpect(MockMvcResultMatchers.status().isNotFound())
        .andExpect(result -> Assertions.assertThat(result.getResolvedException())
            .isInstanceOf(ResourceNotFoundException.class))
        .andExpect(result -> Assertions.assertThat(result.getResolvedException().getMessage())
            .isEqualTo("resource not found"));
  }

  @Test
  public void givenBadArguments_whenGetSpecificException_thenBadRequest() throws Exception {
    String exceptionParam = "bad_arguments";

    mockMvc
        .perform(MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam)
            .contentType(MediaType.APPLICATION_JSON))
        /**
         * The Exception is annotated with {@link ResponseStatus} so it is handled by
         * {@link ResponseStatusExceptionResolver}.
         */
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andExpect(
            result -> Assertions.assertThat(result.getResolvedException()).isInstanceOf(BadArgumentsException.class))
        .andExpect(
            result -> Assertions.assertThat(result.getResolvedException().getMessage()).isEqualTo("bad arguments"));
  }

  @Test
  public void givenOther_whenGetSpecificException_thenInternalServerError() throws Exception {
    String exceptionParam = "dummy";

    mockMvc
        .perform(MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam)
            .contentType(MediaType.APPLICATION_JSON))
        /**
         * The Exception is annotated with {@link ResponseStatus} so it is handled by
         * {@link ResponseStatusExceptionResolver}.
         */
        .andExpect(MockMvcResultMatchers.status().isInternalServerError())
        .andExpect(result -> Assertions.assertThat(result.getResolvedException()).isInstanceOf(InternalException.class))
        .andExpect(
            result -> Assertions.assertThat(result.getResolvedException().getMessage()).isEqualTo("internal error"));
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
        if ("basic".equals(pException)) {
          throw new BasicException("basic exception");
        } else if ("chained".equals(pException)) {
          throw new ChainedException("chained exception", new IOException("child IOException message"));
        } else if ("not_found".equals(pException)) {
          throw new ResourceNotFoundException("resource not found");
        } else if ("bad_arguments".equals(pException)) {
          throw new BadArgumentsException("bad arguments");
        } else {
          throw new InternalException("internal error");
        }
      }
    }

    public class BasicException extends RuntimeException {
      public BasicException(String message) {
        super(message);
      }
    }

    public class ChainedException extends RuntimeException {
      public ChainedException(String message, Throwable cause) {
        super(message, cause);
      }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public class ResourceNotFoundException extends RuntimeException {
      public ResourceNotFoundException(String message) {
        super(message);
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

  }
}
