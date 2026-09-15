resource "aws_apigatewayv2_authorizer" "jwt_azure" {
  api_id           = aws_apigatewayv2_api.clinica_api.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "AzureEntraID-Authorizer"

  jwt_configuration {
    audience = [var.azure_client_id] # El Application ID del backend[cite: 1]
    
    # El valor del Issuer tiene la forma general de Microsoft Entra:
    issuer   = "https://login.microsoftonline.com/${var.azure_tenant_id}/v2.0" 
  }
}