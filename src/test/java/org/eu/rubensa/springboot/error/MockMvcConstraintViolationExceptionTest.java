package org.eu.rubensa.springboot.error;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Objects;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintViolationException;
import javax.validation.Payload;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
    properties = { "server.error.include-message=always" },
    /**
     * Exclude a specific Auto-configuration class from tests' configuration
     */
    excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class MockMvcConstraintViolationExceptionTest {
  /**
   * MockMvc is not a real servlet environment, therefore it does not redirect
   * error responses to ErrorController, which produces validation response.
   * <p>
   * See: https://github.com/spring-projects/spring-boot/issues/5574
   */
  @Autowired
  private MockMvc mockMvc;

  @Test
  public void testConstraintViolationException() throws Exception {
    String content = "{\"value2\": \"\"}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/test/validation").contentType(MediaType.APPLICATION_JSON).content(content))
        .andExpect(MockMvcResultMatchers.status().isInternalServerError())
        .andExpect(result -> Assertions.assertThat(result.getResolvedException())
            .isInstanceOf(ConstraintViolationException.class))
        .andExpect(result -> Assertions.assertThat(result.getResolvedException().getMessage())
            .contains("doSomething.requestBody.value2: size must be between 1 and 255"))
        .andExpect(result -> Assertions.assertThat(result.getResolvedException().getMessage())
            .contains("doSomething.requestBody.value: must not be null"))
        .andExpect(result -> Assertions.assertThat(result.getResolvedException().getMessage())
            .contains("doSomething.requestBody: Values not equal"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status", CoreMatchers.is(500)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.error", CoreMatchers.is("Internal Server Error")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message",
            CoreMatchers.containsString("doSomething.requestBody.value2: size must be between 1 and 255")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message",
            CoreMatchers.containsString("doSomething.requestBody.value: must not be null")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message",
            CoreMatchers.containsString("doSomething.requestBody: Values not equal")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("/test/validation")));
  }

  @Test
  void testConstraintViolationExceptionForParameter() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/test/validation/parameter-validation")
            .contentType(MediaType.APPLICATION_JSON).param("page", "-1"))
        .andExpect(MockMvcResultMatchers.status().isInternalServerError())
        .andExpect(result -> Assertions.assertThat(result.getResolvedException())
            .isInstanceOf(ConstraintViolationException.class))
        .andExpect(result -> Assertions.assertThat(result.getResolvedException().getMessage())
            .isEqualTo("someGetMethod.page: must be greater than or equal to 0"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status", CoreMatchers.is(500)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.error", CoreMatchers.is("Internal Server Error")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.message",
            CoreMatchers.is("someGetMethod.page: must be greater than or equal to 0")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("/test/validation/parameter-validation")));
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
    /**
     * The @Validated on the class is handled by the MethodValidationInterceptor
     * which is a generic purpose validation mechanism for classes.
     * <p>
     * Due to this it throws a ConstraintViolationException.
     */
    @Validated
    public static class TestParameterValidationController {
      @GetMapping("/test/validation/parameter-validation")
      public void someGetMethod(@RequestParam("page") @Min(value = 0) int page) {
      }
    }

    @Service
    /**
     * The @Validated on the class is handled by the MethodValidationInterceptor
     * which is a generic purpose validation mechanism for classes.
     * <p>
     * Due to this it throws a ConstraintViolationException.
     */
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
