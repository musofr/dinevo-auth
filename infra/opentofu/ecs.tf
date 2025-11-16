locals {
  app_name         = var.project_name
  app_log_group    = "/ecs/${var.project_name}-${var.environment}"
  family_name      = "${var.project_name}-${var.environment}"
  image_uri        = "${aws_ecr_repository.this.repository_url}:${var.image_tag}"
}

resource "aws_ecs_cluster" "this" {
  name = "${local.family_name}-cluster"
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
  tags = { Name = "${local.family_name}-cluster" }
}

resource "aws_cloudwatch_log_group" "this" {
  name              = local.app_log_group
  retention_in_days = 14
}

# IAM: Task execution role (pull from ECR, push logs, read secrets)
data "aws_iam_policy_document" "task_exec_assume" {
  statement {
    effect = "Allow"
    principals { type = "Service" identifiers = ["ecs-tasks.amazonaws.com"] }
    actions = ["sts:AssumeRole"]
  }
}

resource "aws_iam_role" "task_execution" {
  name               = "${local.family_name}-exec-role"
  assume_role_policy = data.aws_iam_policy_document.task_exec_assume.json
}

resource "aws_iam_role_policy_attachment" "task_exec_ecr" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "secrets_access" {
  statement {
    effect = "Allow"
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret"
    ]
    resources = [
      aws_secretsmanager_secret.db_password.arn,
      aws_secretsmanager_secret.jwt_private.arn,
      aws_secretsmanager_secret.jwt_public.arn
    ]
  }
}

resource "aws_iam_policy" "secrets_access" {
  name   = "${local.family_name}-secrets-access"
  policy = data.aws_iam_policy_document.secrets_access.json
}

resource "aws_iam_role_policy_attachment" "task_exec_secrets" {
  role       = aws_iam_role.task_execution.name
  policy_arn = aws_iam_policy.secrets_access.arn
}

# IAM: Task role (application permissions, restricted; can be extended later)
resource "aws_iam_role" "task" {
  name               = "${local.family_name}-task-role"
  assume_role_policy = data.aws_iam_policy_document.task_exec_assume.json
}

# Service discovery service for the app
resource "aws_service_discovery_service" "this" {
  name = var.project_name
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.this.id
    dns_records {
      type = "A"
      ttl  = 10
    }
    routing_policy = "MULTIVALUE"
  }
  health_check_custom_config { failure_threshold = 1 }
}

resource "aws_ecs_task_definition" "this" {
  family                   = local.family_name
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.container_cpu
  memory                   = var.container_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = var.project_name,
      image     = local.image_uri,
      essential = true,
      portMappings = [
        {
          containerPort = var.container_port,
          protocol      = "tcp"
        }
      ],
      logConfiguration = {
        logDriver = "awslogs",
        options = {
          awslogs-group         = aws_cloudwatch_log_group.this.name,
          awslogs-region        = var.aws_region,
          awslogs-stream-prefix = var.project_name
        }
      },
      environment = [
        { name = "QUARKUS_HTTP_HOST", value = "0.0.0.0" },
        { name = "QUARKUS_DATASOURCE_DB_KIND", value = "postgresql" },
        { name = "MP_JWT_VERIFY_ISSUER", value = "https://auth.dinevo.it" },
        # Redis for Quarkus: hosts URI format
        { name = "QUARKUS_REDIS_HOSTS", value = "redis://${aws_elasticache_replication_group.this.primary_endpoint_address}:6379" },
        # JDBC URL will be formed from RDS endpoint
        { name = "QUARKUS_DATASOURCE_JDBC_URL", value = "jdbc:postgresql://${aws_db_instance.this.address}:5432/${var.db_name}" },
        { name = "QUARKUS_DATASOURCE_USERNAME", value = var.db_username },
        { name = "QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION", value = "update" }
      ],
      secrets = [
        { name = "QUARKUS_DATASOURCE_PASSWORD", valueFrom = aws_secretsmanager_secret.db_password.arn },
        { name = "SMALLRYE_JWT_SIGN_KEY", valueFrom = aws_secretsmanager_secret.jwt_private.arn },
        { name = "MP_JWT_VERIFY_PUBLICKEY", valueFrom = aws_secretsmanager_secret.jwt_public.arn }
      ]
    }
  ])
}

resource "aws_ecs_service" "this" {
  name            = "${local.family_name}-svc"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.this.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = local.private_subnets
    security_groups = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.this.arn
  }

  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200

  propagate_tags = "SERVICE"
}

output "service_discovery_dns" {
  value = "${aws_service_discovery_service.this.name}.${aws_service_discovery_private_dns_namespace.this.name}"
}
