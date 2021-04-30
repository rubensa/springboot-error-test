package org.eu.rubensa.springboot.error;

import javax.servlet.RequestDispatcher;

import org.assertj.core.api.Assertions;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.BadArgumentsException;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.InternalException;
import org.eu.rubensa.springboot.error.MockMvcExceptionTest.TestConfig.ResourceNotFoundException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
    properties = { "server.error.include-message=always" })
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

    mockMvc.perform(
        MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam).contentType(MediaType.APPLICATION_JSON))
        .andDo(result -> {
          if (result.getResolvedException() != null) {//@formatter:off
            byte[] response = mockMvc.perform(MockMvcRequestBuilders.get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, result.getResponse().getStatus())
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, result.getRequest().getRequestURI())
                .requestAttr(RequestDispatcher.ERROR_EXCEPTION, result.getResolvedException())
                .requestAttr(RequestDispatcher.ERROR_MESSAGE, String.valueOf(result.getResolvedException().getMessage())))
                .andReturn().getResponse().getContentAsByteArray();//@formatter:on
            result.getResponse().getOutputStream().write(response);
          }
        }).andExpect(MockMvcResultMatchers.status().isNotFound())
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

    mockMvc.perform(
        MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam).contentType(MediaType.APPLICATION_JSON))
        .andDo(result -> {
          if (result.getResolvedException() != null) {//@formatter:off
            byte[] response = mockMvc.perform(MockMvcRequestBuilders.get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, result.getResponse().getStatus())
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, result.getRequest().getRequestURI())
                .requestAttr(RequestDispatcher.ERROR_EXCEPTION, result.getResolvedException())
                .requestAttr(RequestDispatcher.ERROR_MESSAGE, String.valueOf(result.getResolvedException().getMessage())))
                .andReturn().getResponse().getContentAsByteArray();//@formatter:on
            result.getResponse().getOutputStream().write(response);
          }
        }).andExpect(MockMvcResultMatchers.status().isBadRequest())
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

    mockMvc.perform(
        MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam).contentType(MediaType.APPLICATION_JSON))
        .andDo(result -> {
          if (result.getResolvedException() != null) {//@formatter:off
            byte[] response = mockMvc.perform(MockMvcRequestBuilders.get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, result.getResponse().getStatus())
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, result.getRequest().getRequestURI())
                .requestAttr(RequestDispatcher.ERROR_EXCEPTION, result.getResolvedException())
                .requestAttr(RequestDispatcher.ERROR_MESSAGE, String.valueOf(result.getResolvedException().getMessage())))
                .andReturn().getResponse().getContentAsByteArray();//@formatter:on
            result.getResponse().getOutputStream().write(response);
          }
        }).andExpect(MockMvcResultMatchers.status().isInternalServerError())
        .andExpect(result -> Assertions.assertThat(result.getResolvedException()).isInstanceOf(InternalException.class))
        .andExpect(
            result -> Assertions.assertThat(result.getResolvedException().getMessage()).isEqualTo("internal error"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status", CoreMatchers.is(500)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.error", CoreMatchers.is("Internal Server Error")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message", CoreMatchers.is("internal error")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("/exception/dummy")));
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
  }
}
