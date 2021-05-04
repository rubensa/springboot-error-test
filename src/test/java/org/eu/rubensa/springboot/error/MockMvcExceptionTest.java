package org.eu.rubensa.springboot.error;

import java.io.IOException;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.BadArgumentsException;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.ChainedException;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.InternalException;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.NoStatusException;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.ResourceNotFoundException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

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
    properties = { "server.error.include-message=always",
        // (never/alway/on-trace-param) by default is never
        "server.error.include-stacktrace=on-trace-param" })
public class MockMvcExceptionTest {
  /**
   * MockMvc is not a real servlet environment, therefore it does not redirect
   * error responses to ErrorController, which produces validation response.
   * <p>
   * See: https://github.com/spring-projects/spring-boot/issues/5574
   */
  @Autowired
  private MockMvc mockMvc;

  @Test
  public void givenNotFound_whenGetSpecificException_thenNotFoundCode() throws Exception {
    String exceptionParam = "not_found";

    mockMvc
        .perform(MockMvcRequestBuilders
            .get("/exception/{exception_id}", exceptionParam).contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isNotFound())
        .andExpect(result -> Assertions.assertThat(result.getResolvedException())
            .isInstanceOf(ResourceNotFoundException.class))
        .andExpect(
            result -> Assertions.assertThat(result.getResolvedException().getMessage()).isEqualTo("resource not found"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status", CoreMatchers.is(404)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.error", CoreMatchers.is("Not Found")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message", CoreMatchers.is("resource not found")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("/exception/not_found")));
  }

  @Test
  public void givenBadArguments_whenGetSpecificException_thenBadRequest() throws Exception {
    String exceptionParam = "bad_arguments";

    mockMvc
        .perform(MockMvcRequestBuilders
            .get("/exception/{exception_id}", exceptionParam).contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andExpect(
            result -> Assertions.assertThat(result.getResolvedException()).isInstanceOf(BadArgumentsException.class))
        .andExpect(
            result -> Assertions.assertThat(result.getResolvedException().getMessage()).isEqualTo("bad arguments"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status", CoreMatchers.is(400)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.error", CoreMatchers.is("Bad Request")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message", CoreMatchers.is("bad arguments")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("/exception/bad_arguments")));
  }

  @Test
  public void givenOther_whenGetSpecificException_thenInternalServerError() throws Exception {
    String exceptionParam = "dummy";

    mockMvc
        .perform(MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isInternalServerError())
        .andExpect(result -> Assertions.assertThat(result.getResolvedException()).isInstanceOf(InternalException.class))
        .andExpect(
            result -> Assertions.assertThat(result.getResolvedException().getMessage()).isEqualTo("internal error"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status", CoreMatchers.is(500)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.error", CoreMatchers.is("Internal Server Error")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message", CoreMatchers.is("internal error")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("/exception/dummy")));
  }

  @Test
  public void givenNoStatus_whenGetSpecificException_thenInternalServerError() throws Exception {
    String exceptionParam = "no_status";

    mockMvc
        .perform(MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isInternalServerError())
        .andExpect(result -> Assertions.assertThat(result.getResolvedException()).isInstanceOf(NoStatusException.class))
        .andExpect(result -> Assertions.assertThat(result.getResolvedException().getMessage()).isEqualTo("no status"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status", CoreMatchers.is(500)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.error", CoreMatchers.is("Internal Server Error")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message", CoreMatchers.is("no status")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("/exception/no_status")));
  }

  @Test
  public void givenChained_whenGetSpecificException_thenInternalServerError() throws Exception {
    String exceptionParam = "chained";

    mockMvc
        .perform(MockMvcRequestBuilders.get("/exception/{exception_id}?trace=true", exceptionParam)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isInternalServerError())
        .andExpect(result -> Assertions.assertThat(result.getResolvedException()).isInstanceOf(ChainedException.class))
        .andExpect(
            result -> Assertions.assertThat(result.getResolvedException().getMessage()).isEqualTo("chained exception"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status", CoreMatchers.is(500)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.error", CoreMatchers.is("Internal Server Error")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message", CoreMatchers.is("chained exception")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("/exception/chained")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.trace", CoreMatchers.containsString("child IOException message")));
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
    /**
     * This advice is necessary because MockMvc is not a real servlet environment,
     * therefore it does not redirect error responses to {@link ErrorController},
     * which produces validation response. So we need to fake it in tests. It's not
     * ideal, but at least we can use classic MockMvc tests for testing error
     * response.
     */
    @ControllerAdvice
    public class MockMvcRestExceptionControllerAdvise extends ResponseEntityExceptionHandler {
      BasicErrorController errorController;

      public MockMvcRestExceptionControllerAdvise(BasicErrorController errorController) {
        this.errorController = errorController;
      }

      /**
       * Handle any generic {@link Exception} not handled by
       * {@link ResponseEntityExceptionHandler}
       */
      @ExceptionHandler({ Exception.class })
      public final ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
          status = responseStatus.value();
        }
        return handleExceptionInternal(ex, null, headers, status, request);
      }

      /**
       * Overrides
       * {@link ResponseEntityExceptionHandler#handleExceptionInternal(Exception,
       * Object, HttpHeaders, HttpStatus, WebRequest))} to expose error attributes to
       * {@link BasicErrorController} and uses it to handle the response.
       */
      @Override
      protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
          HttpStatus status, WebRequest request) {
        request.setAttribute(WebUtils.ERROR_STATUS_CODE_ATTRIBUTE, status.value(), WebRequest.SCOPE_REQUEST);
        request.setAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE,
            ((ServletWebRequest) request).getRequest().getRequestURI().toString(), WebRequest.SCOPE_REQUEST);
        request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        request.setAttribute(WebUtils.ERROR_MESSAGE_ATTRIBUTE, ex.getMessage(), WebRequest.SCOPE_REQUEST);

        ResponseEntity<Map<String, Object>> errorControllerResponeEntity = errorController
            .error(((ServletWebRequest) request).getRequest());
        return new ResponseEntity<>(errorControllerResponeEntity.getBody(), errorControllerResponeEntity.getHeaders(),
            errorControllerResponeEntity.getStatusCode());
      }
    }

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
