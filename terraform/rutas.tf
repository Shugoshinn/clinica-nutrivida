# Integración hacia tu Spring Boot (v1)
resource "aws_apigatewayv2_integration" "backend_v1" {
  api_id             = aws_apigatewayv2_api.clinica_api.id
  integration_type   = "HTTP_PROXY"
  integration_uri    = "${var.backend_url_v1}/datos" # Integración HTTP
  integration_method = "GET"
}

# Ruta protegida /v1/datos
resource "aws_apigatewayv2_route" "ruta_datos_v1" {
  api_id    = aws_apigatewayv2_api.clinica_api.id
  route_key = "GET /v1/datos"
  target    = "integrations/${aws_apigatewayv2_integration.backend_v1.id}"

  # Se asocia el autorizador de Azure a esta ruta
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt_azure.id
}

# Integración proxy hacia todas las rutas /api/* del BFF (reservas, fichas, nutricionistas)
resource "aws_apigatewayv2_integration" "backend_api" {
  api_id             = aws_apigatewayv2_api.clinica_api.id
  integration_type   = "HTTP_PROXY"
  integration_uri    = "${var.backend_base_url}/api/{proxy}"
  integration_method = "ANY"
}

# Ruta protegida por el autorizador JWT de Azure; el BFF vuelve a validar el token y aplica los roles
resource "aws_apigatewayv2_route" "api_proxy" {
  api_id    = aws_apigatewayv2_api.clinica_api.id
  route_key = "ANY /api/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.backend_api.id}"

  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt_azure.id
}
