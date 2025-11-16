resource "aws_elasticache_subnet_group" "this" {
  name       = "${local.name_prefix}-redis-subnets"
  subnet_ids = local.private_subnets
}

# Cost-effective single-node Redis (no auth token). For prod, enable auth_token and TLS.
resource "aws_elasticache_replication_group" "this" {
  replication_group_id          = "${var.project_name}-${var.environment}-redis"
  replication_group_description = "Redis for ${local.name_prefix}"
  engine                        = "redis"
  engine_version                = "7.1"
  node_type                     = var.redis_node_type
  number_cache_clusters         = 1
  subnet_group_name             = aws_elasticache_subnet_group.this.name
  security_group_ids            = [aws_security_group.redis.id]
  at_rest_encryption_enabled    = true
  transit_encryption_enabled    = false
  auto_minor_version_upgrade    = true
  maintenance_window            = "sun:02:00-sun:03:00"
  port                          = 6379
  apply_immediately             = true

  lifecycle {
    ignore_changes = [engine_version]
  }
}

output "redis_primary_endpoint" {
  value = aws_elasticache_replication_group.this.primary_endpoint_address
}
