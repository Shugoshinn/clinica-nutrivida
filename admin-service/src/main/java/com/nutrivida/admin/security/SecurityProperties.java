package com.nutrivida.admin.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Configuración del Tenant de Azure Entra ID y de la autorización por roles (application.yml -> app.security).
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String tenantId,
        String clientId,
        List<String> issuers,
        List<String> audiences,
        String defaultRole,
        Map<String, String> roleMappings) {

    public SecurityProperties {
        issuers = issuers == null ? List.of() : List.copyOf(issuers);
        audiences = audiences == null ? List.of() : List.copyOf(audiences);
        roleMappings = roleMappings == null ? Map.of() : Map.copyOf(roleMappings);
    }
}
