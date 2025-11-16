output "vpc_id" {
  value = aws_vpc.this.id
}

output "private_subnet_ids" {
  value = local.private_subnets
}

output "cloud_map_namespace" {
  value = aws_service_discovery_private_dns_namespace.this.name
}

output "service_discovery_name" {
  description = "Internal DNS name to reach the service from within the VPC"
  value       = "${aws_service_discovery_service.this.name}.${aws_service_discovery_private_dns_namespace.this.name}"
}

output "rds_endpoint" {
  value = aws_db_instance.this.address
}

output "redis_primary_endpoint" {
  value = aws_elasticache_replication_group.this.primary_endpoint_address
}

output "ecr_repository_url" {
  value = aws_ecr_repository.this.repository_url
}
