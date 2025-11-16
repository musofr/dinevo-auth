resource "aws_db_subnet_group" "this" {
  name       = "${local.name_prefix}-db-subnets"
  subnet_ids = local.private_subnets
  tags       = { Name = "${local.name_prefix}-db-subnets" }
}

resource "aws_db_parameter_group" "this" {
  name        = "${local.name_prefix}-pg"
  family      = "postgres15"
  description = "Parameter group for ${local.name_prefix}"
}

resource "aws_db_instance" "this" {
  identifier              = "${var.project_name}-${var.environment}"
  engine                  = "postgres"
  engine_version          = "15.6"
  instance_class          = var.rds_instance_class
  db_name                 = var.db_name
  username                = var.db_username
  password                = random_password.db.result
  allocated_storage       = 20
  max_allocated_storage   = 100
  storage_type            = "gp3"
  multi_az                = false
  publicly_accessible     = false
  storage_encrypted       = true
  backup_retention_period = 0
  delete_automated_backups = true
  deletion_protection     = false
  skip_final_snapshot     = true
  apply_immediately       = true
  db_subnet_group_name    = aws_db_subnet_group.this.name
  parameter_group_name    = aws_db_parameter_group.this.name
  vpc_security_group_ids  = [aws_security_group.rds.id]
  port                    = 5432

  tags = { Name = "${local.name_prefix}-rds" }
}

output "rds_endpoint" {
  value = aws_db_instance.this.address
}
