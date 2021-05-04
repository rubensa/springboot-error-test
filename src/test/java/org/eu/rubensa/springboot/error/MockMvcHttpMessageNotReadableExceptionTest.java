package org.eu.rubensa.springboot.error;

import java.util.Map;

import org.assertj.core.api.Assertions;
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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    properties = { "server.error.include-message=always" })
public class MockMvcHttpMessageNotReadableExceptionTest {
  /**
   * MockMvc is not a real servlet environment, therefore it does not redirect
   * error responses to ErrorController, which produces validation response.
   * <p>
   * See: https://github.com/spring-projects/spring-boot/issues/5574
   */
  @Autowired
  private MockMvc mockMvc;

  @Test
  public void testHttpMessageNotReadableException() throws Exception {
    String content = "{invalidjsonhere}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/test/validation").contentType(MediaType.APPLICATION_JSON).content(content))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andExpect(result -> Assertions.assertThat(result.getResolvedException())
            .isInstanceOf(HttpMessageNotReadableException.class))
        .andExpect(result -> Assertions.assertThat(result.getResolvedException().getMessage())
            .contains("JSON parse error: Unexpected character ('i' (code 105))"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status", CoreMatchers.is(400)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.error", CoreMatchers.is("Bad Request")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message",
            CoreMatchers.containsString("JSON parse error: Unexpected character ('i' (code 105))")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("/test/validation")));
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
