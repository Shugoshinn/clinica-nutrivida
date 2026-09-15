variable "azure_tenant_id" {
  description = "ID del Inquilino (Tenant ID) de Azure Entra ID"
  type        = string
}

variable "azure_client_id" {
  description = "ID de la Aplicación (Client ID / Audience) registrada en Azure"
  type        = string
}

variable "backend_url_v1" {
  description = "URL de tu backend Spring Boot (ej. http://IP:8080/api)"
  type        = string
}