resource "aws_ecr_repository" "this" {
  name                 = var.project_name
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration { scan_on_push = true }

  tags = {
    Name        = "${local.name_prefix}-ecr"
    Environment = var.environment
  }
}

output "ecr_repository_url" {
  value = aws_ecr_repository.this.repository_url
}
