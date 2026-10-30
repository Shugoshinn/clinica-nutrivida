output "api_gateway_url" {
  description = "URL base del API Gateway para configurar en el frontend React (VITE_API_URL)"
  value       = aws_apigatewayv2_stage.desarrollo.invoke_url
}