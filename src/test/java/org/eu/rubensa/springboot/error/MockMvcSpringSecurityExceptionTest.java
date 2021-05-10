package org.eu.rubensa.springboot.error;

import javax.servlet.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

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
        /**
         * When you add the Security starter without custom security configurations,
         * Spring Boot endpoints will be secured using HTTP basic authentication with a
         * default user and generated password. To override that, you can configure
         * credentials in application.properties as follows
         */
        "spring.security.user.name=username", "spring.security.user.password=password" })
/**
 * Default Security The security auto-configuration no longer exposes options
 * and uses Spring Security defaults as much as possible. One noticeable side
 * effect of that is the use of Spring Security’s content negotiation for
 * authorization (form login).
 * 
 * Spring Boot 2.0 doesn’t deviate too much from Spring Security’s defaults, as
 * a result of which some of the endpoints that bypassed Spring Security in
 * Spring Boot 1.5 are now secure by default. These include the error endpoint
 * and paths to static resources such as /css/&#42;&#42;, /js/&#42;&#42;,
 * /images/&#42;&#42;, /webjars/&#42;&#42;, /&#42;&#42;/favicon.ico. If you want
 * to open these up, you need to explicitly configure that.
 * 
 * see: https://github.com/spring-projects/spring-boot/issues/14474
 */
public class MockMvcSpringSecurityExceptionTest {
  private static final String USER_NAME = "username";
  private static final String USER_PASSWORD = "password";

  @Autowired
  private WebApplicationContext context;
  @Autowired
  private Filter springSecurityFilterChain;

  /**
   * MockMvc is not a real servlet environment, therefore it does not redirect
   * error responses to ErrorController, which produces validation response.
   * <p>
   * See: https://github.com/spring-projects/spring-boot/issues/5574
   */
  // @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
  }

  @Test
  public void testNoCredentials() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/exception").contentType(MediaType.APPLICATION_JSON))
        /**
         * The exception is among Spring Security exceptions so it is handled by the
         * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
         * {@link BasicAuthenticationEntryPoint}.
         */
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andExpect(MockMvcResultMatchers.status().reason("Unauthorized"));
  }

  @Test
  public void testWrongPassword() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/exception")
            .with(SecurityMockMvcRequestPostProcessors.httpBasic(USER_NAME, "wrongpassword"))
            .contentType(MediaType.APPLICATION_JSON))
        /**
         * The exception is among Spring Security exceptions so it is handled by the
         * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
         * {@link BasicAuthenticationEntryPoint}.
         */
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andExpect(MockMvcResultMatchers.status().reason("Unauthorized"));
  }

  @Test
  public void testDenyAll() throws Exception {
    String exceptionParam = "deny-all";

    mockMvc
        .perform(MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam)
            .with(SecurityMockMvcRequestPostProcessors.httpBasic(USER_NAME, USER_PASSWORD))
            .contentType(MediaType.APPLICATION_JSON))
        /**
         * The exception is among Spring Security exceptions so it is handled by the
         * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
         * {@link BasicAuthenticationEntryPoint}.
         */
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andExpect(MockMvcResultMatchers.status().reason("Unauthorized"));
  }

  @Test
  public void testBadCredentials() throws Exception {
    String exceptionParam = "bad-credentials";

    mockMvc
        .perform(MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam)
            .with(SecurityMockMvcRequestPostProcessors.httpBasic(USER_NAME, USER_PASSWORD))
            .contentType(MediaType.APPLICATION_JSON))
        /**
         * The exception is among Spring Security exceptions so it is handled by the
         * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
         * {@link AccessDeniedHandlerImpl}.
         */
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andExpect(MockMvcResultMatchers.status().reason("Unauthorized"));
  }

  @Test
  public void testAccessDenied() throws Exception {
    String exceptionParam = "access-denied";

    mockMvc
        .perform(MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam)
            .with(SecurityMockMvcRequestPostProcessors.httpBasic(USER_NAME, USER_PASSWORD))
            .contentType(MediaType.APPLICATION_JSON))
        /**
         * The exception is among Spring Security exceptions so it is handled by the
         * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
         * {@link AccessDeniedHandlerImpl}.
         */
        .andExpect(MockMvcResultMatchers.status().isForbidden())
        .andExpect(MockMvcResultMatchers.status().reason("Forbidden"));
  }

  @Test
  public void testAccountExpired() throws Exception {
    String exceptionParam = "account-expired";

    mockMvc
        .perform(MockMvcRequestBuilders.get("/exception/{exception_id}", exceptionParam)
            .with(SecurityMockMvcRequestPostProcessors.httpBasic(USER_NAME, USER_PASSWORD))
            .contentType(MediaType.APPLICATION_JSON))
        /**
         * The exception is among Spring Security exceptions so it is handled by the
         * {@link FilterChainProxy}' {@link ExceptionTranslationFilter}'
         * {@link BasicAuthenticationEntryPoint}.
         */
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andExpect(MockMvcResultMatchers.status().reason("Unauthorized"));
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
   * are PreAuthorize, PostAuthorize. It also has support for JSR-250.
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
