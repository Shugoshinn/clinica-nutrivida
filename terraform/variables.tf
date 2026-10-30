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

variable "cors_allowed_origins" {
  description = "Orígenes del frontend autorizados por CORS en el API Gateway"
  type        = list(string)
  default     = ["http://localhost", "http://localhost:4200", "http://localhost:5173"]
}

variable "backend_base_url" {
  description = "URL base del BFF Spring Boot sin /api (ej. http://api-nutrivida-v2.us-east-1.elasticbeanstalk.com)"
  type        = string
}
