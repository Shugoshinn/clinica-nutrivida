package com.nutrivida.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrivida.backend.security.AzureJwtRolesConverter;
import com.nutrivida.backend.security.RestSecurityErrorHandler;
import com.nutrivida.backend.security.SecurityProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

import static com.nutrivida.backend.security.Roles.ADMINISTRADOR;
import static com.nutrivida.backend.security.Roles.NUTRICIONISTA;
import static com.nutrivida.backend.security.Roles.PACIENTE;
import static com.nutrivida.backend.security.Roles.SECRETARIA;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter,
                                                   RestSecurityErrorHandler errorHandler) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Preflight de CORS sin token
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Autorización por roles (claim "roles" del JWT)
                .requestMatchers(HttpMethod.POST, "/api/nutricionistas/**").hasRole(ADMINISTRADOR)
                .requestMatchers(HttpMethod.DELETE, "/api/nutricionistas/**").hasRole(ADMINISTRADOR)
                .requestMatchers(HttpMethod.GET, "/api/nutricionistas/**").hasAnyRole(SECRETARIA, ADMINISTRADOR)
                .requestMatchers("/api/fichas/**").hasAnyRole(NUTRICIONISTA, ADMINISTRADOR)
                .requestMatchers(HttpMethod.POST, "/api/reservas").hasAnyRole(PACIENTE, SECRETARIA, ADMINISTRADOR)
                .requestMatchers("/api/reservas/**").hasAnyRole(SECRETARIA, ADMINISTRADOR)

                // Sin accesos anónimos
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(errorHandler)
                .accessDeniedHandler(errorHandler))
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(errorHandler)
                .accessDeniedHandler(errorHandler)
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    /**
     * Decodificador JWT que verifica firma (JWKS del tenant), vigencia (exp/nbf), issuer y audience.
     */
    @Bean
    public JwtDecoder jwtDecoder(OAuth2ResourceServerProperties resourceServerProperties,
                                 SecurityProperties securityProperties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(resourceServerProperties.getJwt().getJwkSetUri())
                .build();

        decoder.setJwtValidator(tokenValidator(securityProperties));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> tokenValidator(SecurityProperties securityProperties) {
        List<String> issuers = securityProperties.issuers();
        List<String> audiences = securityProperties.audiences();

        OAuth2TokenValidator<Jwt> timestamp = new JwtTimestampValidator(Duration.ofSeconds(60));
        OAuth2TokenValidator<Jwt> issuer = new JwtClaimValidator<String>("iss", issuers::contains);
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null
                && jwt.getAudience().stream().anyMatch(audiences::contains)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                        "El token no está dirigido a esta API (audience inválido)", null));

        return new DelegatingOAuth2TokenValidator<>(timestamp, issuer, audience);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(SecurityProperties securityProperties) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new AzureJwtRolesConverter(securityProperties));
        return converter;
    }

    @Bean
    public RestSecurityErrorHandler restSecurityErrorHandler(ObjectMapper objectMapper) {
        return new RestSecurityErrorHandler(objectMapper);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
