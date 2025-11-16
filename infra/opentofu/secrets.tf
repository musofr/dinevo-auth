locals {
  secrets_prefix = "${var.project_name}/${var.environment}"
}

# Random password for DB
resource "random_password" "db" {
  length           = 20
  special          = true
  override_characters = "!@#%^*-_+"
}

# Secrets Manager secret for DB password
resource "aws_secretsmanager_secret" "db_password" {
  name        = "${local.secrets_prefix}/db-password"
  description = "RDS ${var.project_name} password"
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = random_password.db.result
}

# Secrets for JWT keys (user will set values later)
resource "aws_secretsmanager_secret" "jwt_private" {
  name        = "${local.secrets_prefix}/jwt-private"
  description = "JWT private key (PEM) for ${var.project_name}"
}

resource "aws_secretsmanager_secret" "jwt_public" {
  name        = "${local.secrets_prefix}/jwt-public"
  description = "JWT public key (PEM) for ${var.project_name}"
}

output "secrets_arns" {
  value = {
    db_password = aws_secretsmanager_secret.db_password.arn
    jwt_private = aws_secretsmanager_secret.jwt_private.arn
    jwt_public  = aws_secretsmanager_secret.jwt_public.arn
  }
}
