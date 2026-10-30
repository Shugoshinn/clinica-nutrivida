resource "aws_apigatewayv2_api" "clinica_api" {
  name          = "Mi_Api_GateWay"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins     = var.cors_allowed_origins
    allow_methods     = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
    allow_headers     = ["Content-Type", "Authorization", "X-Amz-Date", "X-Api-Key"]
    allow_credentials = true
    max_age           = 3600
  }
}

# Etapa de despliegue (Stage)
resource "aws_apigatewayv2_stage" "desarrollo" {
  api_id      = aws_apigatewayv2_api.clinica_api.id
  name        = "Desarrollo"
  auto_deploy = true
}