package com.nutrivida.backend.security;

/**
 * Roles de la aplicación (App Roles de Azure Entra ID -> claim "roles").
 */
public final class Roles {

    public static final String PACIENTE = "Paciente";
    public static final String NUTRICIONISTA = "Nutricionista";
    public static final String SECRETARIA = "Secretaria";
    public static final String ADMINISTRADOR = "Administrador";

    private Roles() {
    }
}
