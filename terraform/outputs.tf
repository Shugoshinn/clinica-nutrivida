output "api_gateway_url" {
  description = "URL base del API Gateway para configurar en Angular"
  value       = aws_apigatewayv2_stage.desarrollo.invoke_url
}