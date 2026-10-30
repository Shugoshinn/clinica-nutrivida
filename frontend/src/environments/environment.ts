// Configuración central del frontend. Los valores se pueden sobrescribir con variables VITE_* en build.
const tenantId = import.meta.env.VITE_AZURE_TENANT_ID || 'common';
const clientId = import.meta.env.VITE_AZURE_CLIENT_ID || 'db5f13af-8b1a-4d80-9944-17ea6ff024cf';

export const environment = {
  production: import.meta.env.PROD,
  // URL del API Gateway (AWS) que enruta hacia el BFF Spring Boot
  apiUrl: import.meta.env.VITE_API_URL || 'https://zphgtogiuc.execute-api.us-east-1.amazonaws.com/prod',
  msalConfig: {
    clientId,
    authority: `https://login.microsoftonline.com/${tenantId}`
  },
  // Scope expuesto por la API registrada en Azure Entra ID: el access token tendrá aud = api://<clientId>
  apiScopes: [`api://${clientId}/access_as_user`]
};
