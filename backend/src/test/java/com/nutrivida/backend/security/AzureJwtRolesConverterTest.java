package com.nutrivida.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AzureJwtRolesConverterTest {

    private final AzureJwtRolesConverter converter = new AzureJwtRolesConverter(new SecurityProperties(
            "tenant", "client", List.of(), List.of(), "Paciente",
            Map.of("secretaria@nutrivida.cl", "Secretaria")));

    @Test
    void usaElClaimRoles() {
        assertThat(authorities(Map.of("roles", List.of("Administrador"), "scp", "access_as_user")))
                .containsExactly("ROLE_Administrador", "SCOPE_access_as_user");
    }

    @Test
    void usaElCorreoCuandoNoHayRoles() {
        assertThat(authorities(Map.of("preferred_username", "Secretaria@NutriVida.cl")))
                .containsExactly("ROLE_Secretaria");
    }

    @Test
    void asignaRolPorDefecto() {
        assertThat(authorities(Map.of("email", "paciente@gmail.com"))).containsExactly("ROLE_Paciente");
    }

    private List<String> authorities(Map<String, Object> claims) {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "RS256"), claims);
        return converter.convert(jwt).stream().map(GrantedAuthority::getAuthority).toList();
    }
}
