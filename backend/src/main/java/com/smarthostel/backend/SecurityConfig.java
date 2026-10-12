package com.smarthostel.backend;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

  @Bean UserDetailsService userDetailsService(UserAccountRepository accounts) {
    return username -> accounts.findByUsernameIgnoreCase(username)
        .map(account -> org.springframework.security.core.userdetails.User.withUsername(account.getUsername())
            .password(account.getPasswordHash()).roles(account.getRole()).build())
        .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Account not found"));
  }

  @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean SecurityContextRepository securityContextRepository() {
    var repository = new HttpSessionSecurityContextRepository();
    repository.setDisableUrlRewriting(true);
    return repository;
  }

  @Bean SessionFreshnessFilter sessionFreshnessFilter(UserAccountRepository accounts){return new SessionFreshnessFilter(accounts);}
  @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<SessionFreshnessFilter> disableContainerRegistration(SessionFreshnessFilter filter){
    var registration=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;
  }

  @Bean SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      SessionFreshnessFilter sessionFreshnessFilter,
      SecurityContextRepository securityContextRepository,
      @org.springframework.beans.factory.annotation.Value("${SESSION_COOKIE_SECURE:false}") boolean secureCookies) throws Exception {
    var csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrfRepository.setCookiePath("/");
    csrfRepository.setCookieCustomizer(cookie -> cookie.secure(secureCookies).sameSite("Lax"));
    http
        .addFilterAfter(sessionFreshnessFilter,org.springframework.security.web.context.SecurityContextHolderFilter.class)
        .securityContext(context -> context.securityContextRepository(securityContextRepository))
        .cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/auth/csrf", "/api/auth/config", "/api/auth/login", "/api/auth/register", "/hello", "/error", "/healthz").permitAll()
            .anyRequest().authenticated())
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .logout(logout -> logout.logoutUrl("/api/auth/logout").logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
        .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, exception) -> response.sendError(401)));
    return http.build();
  }

  @Bean org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource(
      @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String origins) {
    var config = new org.springframework.web.cors.CorsConfiguration();
    config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
    config.setAllowCredentials(true);
    var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
