package com.nutrivida.admin.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Traduce los claims del JWT de Azure a authorities de Spring Security:
 * - claim "roles" (App Roles)      -> ROLE_<rol>
 * - claim "scp" (scopes delegados) -> SCOPE_<scope>
 * Si el token no trae roles (cuentas personales de Microsoft), se usa el correo del token
 * contra app.security.role-mappings y, en último caso, el rol por defecto.
 */
public class AzureJwtRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final List<String> EMAIL_CLAIMS = List.of("preferred_username", "email", "upn", "unique_name");

    private final SecurityProperties properties;

    public AzureJwtRolesConverter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }

        if (authorities.isEmpty()) {
            String mappedRole = EMAIL_CLAIMS.stream()
                    .map(jwt::getClaimAsString)
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> properties.roleMappings().get(value.toLowerCase(Locale.ROOT)))
                    .filter(role -> role != null)
                    .findFirst()
                    .orElse(properties.defaultRole());
            authorities.add(new SimpleGrantedAuthority("ROLE_" + mappedRole));
        }

        String scopes = jwt.getClaimAsString("scp");
        if (scopes != null) {
            for (String scope : scopes.split(" ")) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
            }
        }
        return authorities;
    }
}
