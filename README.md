# Building Manager Network

Spring Boot Project for Building Manager Network

## Quick Start with Docker

The easiest way to run the application is using Docker Compose, which will start all required services.

### Prerequisites

- Docker Desktop installed and running
- At least 4GB of available RAM

### Running the Application

1. **Clone the repository** (if not already done):
   ```bash
   git clone <repository-url>
   cd building-manager-network
   ```

2. **Start all services**:
   ```bash
   docker-compose up -d
   ```

3. **Check service status**:
   ```bash
   docker-compose ps
   ```

4. **View logs** (optional):
   ```bash
   # View all logs
   docker-compose logs -f
   
   # View specific service logs
   docker-compose logs -f app
   docker-compose logs -f postgres
   docker-compose logs -f mail-dev
   ```

### Accessing the Application

Once all services are running, you can access:

- **API Base URL**: http://localhost:8080/api/v1/
- **Swagger Documentation**: http://localhost:8080/api/v1/swagger-ui.html
- **MailDev Web UI**: http://localhost:1080 (for viewing emails)

### Stopping the Application

```bash
docker-compose down
```

To also remove volumes (database data):
```bash
docker-compose down -v
```

## Manual Setup (Alternative)

If you prefer to run the application manually:

### Prerequisites

- Java 21
- Maven 3.6+
- PostgreSQL 15+
- Docker (for MailDev)

### Steps

1. **Start MailDev** (for email testing):
   ```bash
   docker run -d -p 1080:1080 -p 1025:1025 maildev/maildev
   ```

2. **Start PostgreSQL** or use Docker:
   ```bash
   docker run -d -p 5432:5432 \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=password \
     -e POSTGRES_DB=postgres \
     postgres:15-alpine
   ```

3. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

## Configuration

The application uses the following default configuration:

- **Database**: PostgreSQL on localhost:5432
- **Email**: MailDev on localhost:1025 (SMTP) and localhost:1080 (Web UI)
- **API**: http://localhost:8080/api/v1/
- **JWT Expiration**: 30 minutes
- **Profile**: dev (active by default)

## API Testing

### 1. Register a new user:
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe", 
    "email": "john.doe@example.com",
    "password": "password123"
  }'
```

### 2. Login:
```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "password123"
  }'
```

### 3. Check application health:
```bash
curl http://localhost:8080/api/v1/actuator/health
```

## Troubleshooting

### Docker Issues

**If Docker Desktop isn't running:**
1. Start Docker Desktop
2. Wait for it to fully initialize
3. Try running `docker-compose up -d` again

**If ports are already in use:**
- Check if ports 5432, 8080, 1080, or 1025 are occupied
- Stop conflicting services or modify docker-compose.yml

**If the app container fails to start:**
```bash
# Check logs
docker-compose logs app

# Rebuild the image
docker-compose build --no-cache app
docker-compose up -d
```

### Database Issues

**If database connection fails:**
```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Application Issues

**If the application won't start:**
- Check Java version: `java -version` (should be 21)
- Verify Maven: `mvn -version`
- Check application logs for specific errors

## Development

### Rebuilding the Application

After making code changes:
```bash
docker-compose build app
docker-compose up -d app
```

### Hot Reload (Development)

For development with hot reload, you can run the application locally while using Docker for infrastructure:

1. Start only infrastructure services:
   ```bash
   docker-compose up -d postgres mail-dev
   ```

2. Run the application locally:
   ```bash
   ./mvnw spring-boot:run
   ```

## Architecture

The application consists of:

- **Spring Boot Application** (Java 21)
- **PostgreSQL Database** (v15)
- **MailDev** (Email testing service)

All services are containerized and orchestrated with Docker Compose for easy deployment and development.
