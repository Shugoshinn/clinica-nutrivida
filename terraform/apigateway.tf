resource "aws_apigatewayv2_api" "clinica_api" {
  name          = "Mi_Api_GateWay"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = ["http://localhost:4200"] # Cambiar en producción a tu dominio[cite: 7]
    allow_methods = ["GET", "POST", "PUT", "DELETE", "OPTIONS"] #[cite: 7]
    allow_headers = ["Content-Type", "Authorization", "X-Amz-Date", "X-Api-Key"] #[cite: 7]
  }
}

# Etapa de despliegue (Stage)
resource "aws_apigatewayv2_stage" "desarrollo" {
  api_id      = aws_apigatewayv2_api.clinica_api.id
  name        = "Desarrollo" #[cite: 8]
  auto_deploy = true
}