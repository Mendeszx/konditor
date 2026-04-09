package com.api.konditor.app.config.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuração central de segurança da aplicação.
 *
 * <p>Estratégia:
 *
 * <ul>
 *   <li>Stateless — sem sessão HTTP, apenas JWT
 *   <li>CSRF desabilitado — seguro para APIs REST sem estado
 *   <li>{@code /auth/google} e {@code /auth/refresh} são públicos
 *   <li>Todos os demais endpoints requerem JWT válido
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RestAuthenticationEntryPoint authenticationEntryPoint;

  @Value("${security.cors.allowed-origins:http://localhost:3000}")
  private List<String> allowedOrigins;

  /**
   * Define a cadeia de filtros de segurança.
   *
   * @param http builder do Spring Security
   * @return cadeia de filtros configurada
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/google")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/refresh")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Configura as origens permitidas para CORS. Em produção, defina {@code CORS_ALLOWED_ORIGINS} com
   * a URL exata do frontend.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  /**
   * Declara um {@link UserDetailsService} explícito para que o Spring Boot não auto-configure o
   * {@code inMemoryUserDetailsManager} nem gere uma senha aleatória no log.
   *
   * <p>Esta aplicação é 100% stateless (JWT) — nenhum endpoint usa autenticação por
   * username/password, portanto o bean nunca será invocado.
   */
  @Bean
  public UserDetailsService noOpUserDetailsService() {
    return username -> {
      throw new UsernameNotFoundException(
          "Esta aplicação usa autenticação JWT. UserDetailsService não é utilizado.");
    };
  }
}
