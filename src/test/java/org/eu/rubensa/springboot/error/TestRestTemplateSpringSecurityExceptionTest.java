package org.eu.rubensa.springboot.error;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        /**
         * When you add the Security starter without custom security configurations,
         * Spring Boot endpoints will be secured using HTTP basic authentication with a
         * default user and generated password. To override that, you can configure
         * credentials in application.properties as follows
         */
        "spring.security.user.name=username", "spring.security.user.password=password" })
public class TestRestTemplateSpringSecurityExceptionTest {
  private static final String USER_NAME = "username";
  private static final String USER_PASSWORD = "password";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  public void testNoCredentials() throws Exception {
    final ResponseEntity<JsonNode> response = testRestTemplate.exchange("/exception", HttpMethod.GET, null,
        JsonNode.class);
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    JsonNode jsonResponse = response.getBody();
    /**
     * The exception is among Spring Security exceptions so it is handled by the
     * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
     * {@link BasicAuthenticationEntryPoint}.
     * <p>
     * The servlet container (Tomcat) checks that the response has an error so it
     * redirects to the error page. In this case, the Spring Boot
     * {@link BasicErrorController} which exposes the error information via
     * {@Link DefaultErrorAttributes}.
     * <p>
     * Since Spring Boot 2.0 the /error end-point is protected so the exception
     * should not be "exposed" as JSON but curiously if no credentials provided the
     * error page is reached to "expose" the exception as JSON.
     * <p>
     * see: https://github.com/spring-projects/spring-security/issues/4467
     */
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(401);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Unauthorized");
    // This should be the exception message but is the same as error as the Tomcat
    // {@link StandardHostValve} stores the error message in the
    // javax.servlet.error.message request attribute during the /error forward.
    Assertions.assertThat(jsonResponse.findValue("message").asText()).contains("Unauthorized");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/exception");
  }

  @Test
  public void testWrongPassword() throws Exception {
    final ResponseEntity<JsonNode> response = testRestTemplate.withBasicAuth(USER_NAME, "wrongpassword")
        .exchange("/exception", HttpMethod.GET, null, JsonNode.class);
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    JsonNode jsonResponse = response.getBody();
    /**
     * The exception is among Spring Security exceptions so it is handled by the
     * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
     * {@link BasicAuthenticationEntryPoint}.
     * <p>
     * The servlet container (Tomcat) checks that the response has an error so it
     * redirects to the error page. In this case, the Spring Boot
     * {@link BasicErrorController} which exposes the error information via
     * {@Link DefaultErrorAttributes}.
     * <p>
     * Since Spring Boot 2.0 the /error end-point is protected so the exception is
     * not "exposed" as JSON, case the ExceptionTranslationFilter throws an
     * {@link AccessDeniedException} when processing the container forward to the
     * /error page.
     * <p>
     * see: https://github.com/spring-projects/spring-security/issues/4467
     */
    Assertions.assertThat(jsonResponse).isNull();
  }

  @Test
  public void testDenyAll() throws Exception {
    String exceptionParam = "deny-all";

    final ResponseEntity<JsonNode> response = testRestTemplate.withBasicAuth(USER_NAME, USER_PASSWORD)
        .exchange("/exception/{exception_id}", HttpMethod.GET, null, JsonNode.class, exceptionParam);
    /**
     * The exception is among Spring Security exceptions so it is handled by the
     * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
     * {@link BasicAuthenticationEntryPoint}.
     * <p>
     * The servlet container (Tomcat) checks that the response has an error so it
     * redirects to the error page. In this case, the Spring Boot
     * {@link BasicErrorController} which exposes the error information via
     * {@Link DefaultErrorAttributes}.
     * <p>
     * Since Spring Boot 2.0 the /error end-point is protected so the exception
     * should not be "exposed" as JSON but, in this case, the user is authenticated
     * so the error page is reached to "expose" the exception as JSON.
     * <p>
     * see: https://github.com/spring-projects/spring-security/issues/4467
     */
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(401);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Unauthorized");
    // This should be the exception message but is the same as error as the Tomcat
    // {@link StandardHostValve} stores the error message in the
    // javax.servlet.error.message request attribute during the /error forward.
    Assertions.assertThat(jsonResponse.findValue("message").asText()).contains("Unauthorized");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/exception/deny-all");
  }

  @Test
  public void testBadCredentials() throws Exception {
    String exceptionParam = "bad-credentials";

    final ResponseEntity<JsonNode> response = testRestTemplate.withBasicAuth(USER_NAME, USER_PASSWORD)
        .exchange("/exception/{exception_id}", HttpMethod.GET, null, JsonNode.class, exceptionParam);
    /**
     * The exception is among Spring Security exceptions so it is handled by the
     * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
     * {@link BasicAuthenticationEntryPoint}.
     * <p>
     * The servlet container (Tomcat) checks that the response has an error so it
     * redirects to the error page. In this case, the Spring Boot
     * {@link BasicErrorController} which exposes the error information via
     * {@Link DefaultErrorAttributes}.
     * <p>
     * Since Spring Boot 2.0 the /error end-point is protected so the exception
     * should not be "exposed" as JSON but, in this case, the user is authenticated
     * so the error page is reached to "expose" the exception as JSON.
     * <p>
     * see: https://github.com/spring-projects/spring-security/issues/4467
     */
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(401);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Unauthorized");
    // This should be the exception message but is the same as error as the Tomcat
    // {@link StandardHostValve} stores the error message in the
    // javax.servlet.error.message request attribute during the /error forward.
    Assertions.assertThat(jsonResponse.findValue("message").asText()).contains("Unauthorized");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/exception/bad-credentials");
  }

  @Test
  public void testAccessDenied() throws Exception {
    String exceptionParam = "access-denied";

    final ResponseEntity<JsonNode> response = testRestTemplate.withBasicAuth(USER_NAME, USER_PASSWORD)
        .exchange("/exception/{exception_id}", HttpMethod.GET, null, JsonNode.class, exceptionParam);
    /**
     * The exception is among Spring Security exceptions so it is handled by the
     * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
     * {@link BasicAuthenticationEntryPoint}.
     * <p>
     * The servlet container (Tomcat) checks that the response has an error so it
     * redirects to the error page. In this case, the Spring Boot
     * {@link BasicErrorController} which exposes the error information via
     * {@Link DefaultErrorAttributes}.
     * <p>
     * Since Spring Boot 2.0 the /error end-point is protected so the exception
     * should not be "exposed" as JSON but, in this case, the user is authenticated
     * so the error page is reached to "expose" the exception as JSON.
     * <p>
     * see: https://github.com/spring-projects/spring-security/issues/4467
     */
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(403);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Forbidden");
    // This should be the exception message but is the same as error as the Tomcat
    // {@link StandardHostValve} stores the error message in the
    // javax.servlet.error.message request attribute during the /error forward.
    Assertions.assertThat(jsonResponse.findValue("message").asText()).contains("Forbidden");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/exception/access-denied");
  }

  @Test
  public void testAccountExpired() throws Exception {
    String exceptionParam = "account-expired";

    final ResponseEntity<JsonNode> response = testRestTemplate.withBasicAuth(USER_NAME, USER_PASSWORD)
        .exchange("/exception/{exception_id}", HttpMethod.GET, null, JsonNode.class, exceptionParam);
    /**
     * The exception is among Spring Security exceptions so it is handled by the
     * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
     * {@link BasicAuthenticationEntryPoint}.
     * <p>
     * The servlet container (Tomcat) checks that the response has an error so it
     * redirects to the error page. In this case, the Spring Boot
     * {@link BasicErrorController} which exposes the error information via
     * {@Link DefaultErrorAttributes}.
     * <p>
     * Since Spring Boot 2.0 the /error end-point is protected so the exception
     * should not be "exposed" as JSON but, in this case, the user is authenticated
     * so the error page is reached to "expose" the exception as JSON.
     * <p>
     * see: https://github.com/spring-projects/spring-security/issues/4467
     */
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    JsonNode jsonResponse = response.getBody();
    Assertions.assertThat(jsonResponse.findValue("status").asInt()).isEqualTo(401);
    Assertions.assertThat(jsonResponse.findValue("error").asText()).isEqualTo("Unauthorized");
    // This should be the exception message but is the same as error as the Tomcat
    // {@link StandardHostValve} stores the error message in the
    // javax.servlet.error.message request attribute during the /error forward.
    Assertions.assertThat(jsonResponse.findValue("message").asText()).contains("Unauthorized");
    Assertions.assertThat(jsonResponse.findValue("path").asText()).isEqualTo("/exception/account-expired");
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
   * Provides AOP security on methods. Some of the annotations that it provides
   * are PreAuthorize, PostAuthoriz
   */
  @EnableGlobalMethodSecurity(prePostEnabled = true)
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
      @GetMapping("/deny-all")
      @PreAuthorize("denyAll()")
      public void getAccessDenied() {
      }

      @GetMapping("/exception/{exception_id}")
      public void getSpecificException(@PathVariable("exception_id") String pException) {
        if ("bad-credentials".equals(pException)) {
          throw new BadCredentialsException("Fake bad credentials");
        } else if ("access-denied".equals(pException)) {
          throw new AccessDeniedException("Fake access denied");
        } else if ("account-expired".equals(pException)) {
          throw new AccountExpiredException("Fake account expired");
        } else {
          throw new AuthenticationException(pException) {
          };
        }
      }
    }
  }
}
