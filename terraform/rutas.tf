# Integración hacia tu Spring Boot (v1)
resource "aws_apigatewayv2_integration" "backend_v1" {
  api_id             = aws_apigatewayv2_api.clinica_api.id
  integration_type   = "HTTP_PROXY"
  integration_uri    = "${var.backend_url_v1}/datos" # Integración HTTP[cite: 8]
  integration_method = "GET" #[cite: 8]
}

# Ruta protegida /v1/datos
resource "aws_apigatewayv2_route" "ruta_datos_v1" {
  api_id    = aws_apigatewayv2_api.clinica_api.id
  route_key = "GET /v1/datos" #[cite: 2]
  target    = "integrations/${aws_apigatewayv2_integration.backend_v1.id}"

  # Se asocia el autorizador de Azure a esta ruta
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt_azure.id
}