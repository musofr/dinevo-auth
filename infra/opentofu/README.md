Deploy dinevo-auth (private ECS Fargate) with OpenTofu in eu-south-1

Overview
- Private ECS Fargate service (no internet ingress)
- Internal service discovery via AWS Cloud Map (DNS: dinevo-auth.dinevo.local)
- RDS PostgreSQL (single-AZ, small instance)
- ElastiCache Redis (single node)
- Secrets Manager for JWT keys and DB password
- ECR repository for the container image

Prerequisites
- AWS account with permissions to manage VPC, ECS, IAM, RDS, ElastiCache, ECR, Secrets Manager
- OpenTofu >= 1.5 installed
- AWS CLI installed and configured (aws configure sso or access keys)
- Docker installed (to build and push the image)

Files
- providers.tf, variables.tf, network.tf, security.tf, ecr.tf, secrets.tf, rds.tf, redis.tf, ecs.tf, outputs.tf

Quick start
1) Initialize and plan
   - Set region and names using variables (defaults are eu-south-1 and test):
     ```bash
     cd infra/opentofu
     tofu init
     tofu plan -var aws_region=eu-south-1 -out plan.tfplan
     ```

2) Apply (provisions VPC, ECR, RDS, Redis, ECS, Secrets)
   ```bash
   tofu apply plan.tfplan
   ```
   Capture outputs, especially `ecr_repository_url`, `rds_endpoint`, `redis_primary_endpoint`, and `service_discovery_name`.

3) Set JWT secrets (required before running the task)
   Replace the placeholders with your PEM-formatted keys:
   ```bash
   # Private key (PEM) used to sign JWTs
   aws secretsmanager put-secret-value \
     --secret-id dinevo-auth/test/jwt-private \
     --secret-string "$(cat /path/to/privateKey.pem)" \
     --region eu-south-1

   # Public key (PEM) used to verify JWTs (if needed by this service)
   aws secretsmanager put-secret-value \
     --secret-id dinevo-auth/test/jwt-public \
     --secret-string "$(cat /path/to/publicKey.pem)" \
     --region eu-south-1
   ```

4) Build and push the container image to ECR
   ```bash
   # From project root
   ./mvnw -DskipTests package
   docker build -f src/main/docker/Dockerfile.jvm -t dinevo-auth:latest .

   # Login to ECR
   AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
   AWS_REGION=eu-south-1
   ECR_URL=$(tofu output -raw ecr_repository_url)
   aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin ${ECR_URL%/*}

   # Tag & push
   IMAGE_TAG=$(git rev-parse --short HEAD)
   docker tag dinevo-auth:latest $ECR_URL:$IMAGE_TAG
   docker push $ECR_URL:$IMAGE_TAG
   ```

5) Update the service to use the new image and scale up
   Option A: via OpenTofu variables:
   ```bash
   tofu apply -var image_tag=$(git rev-parse --short HEAD) -var desired_count=1
   ```
   Option B: update `image_tag` and `desired_count` in a tfvars file and apply.

How to reach the service internally
- Other microservices in the same VPC can call the service using Cloud Map DNS:
  - `http://dinevo-auth.dinevo.local:8080`
  Ensure their security groups allow egress to port 8080 and that this service’s SG (`ecs-tasks-sg`) allows ingress (it already allows VPC CIDR and same-SG access).

Quarkus runtime configuration (set by ECS task)
- Database:
  - `QUARKUS_DATASOURCE_DB_KIND=postgresql`
  - `QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://<rds-endpoint>:5432/<db>`
  - `QUARKUS_DATASOURCE_USERNAME=<db_username>`
  - `QUARKUS_DATASOURCE_PASSWORD` from Secrets Manager (`dinevo-auth/test/db-password`)
- JPA/Hibernate: `QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=update` (dev/test only)
- Redis: `QUARKUS_REDIS_HOSTS=redis://<redis-endpoint>:6379`
- JWT:
  - Signing: `SMALLRYE_JWT_SIGN_KEY` from Secrets Manager (`dinevo-auth/test/jwt-private`)
  - Verify: `MP_JWT_VERIFY_PUBLICKEY` from Secrets Manager (`dinevo-auth/test/jwt-public`)
  - Issuer: `MP_JWT_VERIFY_ISSUER=https://auth.dinevo.it`

Notes on cost and security (test env defaults)
- Single NAT Gateway (cost-effective but still ~30 USD/mo). For lower egress cost, consider VPC endpoints (ECR, CloudWatch, Secrets Manager) later.
- RDS: single-AZ, small instance, no backups (backup_retention=0), deletion protection disabled, skip final snapshot enabled.
- Redis: single node, no transit encryption, no AUTH token. For prod, enable both.
- ECS service is internal-only (no ALB). Discoverable via Cloud Map DNS.

Cleanup
```bash
tofu destroy
```

Troubleshooting
- Task fails immediately: check CloudWatch Logs (`/ecs/dinevo-auth-test`) and ensure JWT secrets have values.
- Can’t reach DB: verify SG rules (RDS SG allows from ECS tasks SG) and that the DB is in available state.
- DNS resolution: ensure callers are in the same VPC and use the `service_discovery_name` output.
