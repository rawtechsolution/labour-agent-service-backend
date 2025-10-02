# Secure Authentication System for Startup

A comprehensive, scalable authentication system built with Spring Boot, designed for startups with role-based access control, secure token management, and mobile client support.

## Features

### 🔐 Authentication Methods
- **Email/Password Login** - Traditional authentication with BCrypt password hashing
- **OTP-based Login** - SMS-based authentication using Twilio
- **Role-based Authorization** - Support for ADMIN, CUSTOMER, AGENT, VENDOR roles

### 🛡️ Security Features
- **JWT Access & Refresh Tokens** - Stateless authentication with configurable expiration
- **Session Management** - Redis-based session storage with automatic cleanup
- **Mobile Security** - Android Keystore and iOS Keychain implementations
- **Password Encryption** - BCrypt with configurable strength
- **CORS Support** - Configurable cross-origin resource sharing

### 📱 Mobile Client Support
- **Android** - EncryptedSharedPreferences with Android Keystore
- **iOS** - Keychain Services with biometric authentication support
- **Secure Token Storage** - Hardware-backed security where available

### 🚀 Scalability
- **PostgreSQL** - Optimized with connection pooling and indexing
- **Redis Caching** - Fast session lookups and automatic expiration
- **Horizontal Scaling** - Stateless design for load balancing
- **Connection Pooling** - HikariCP for optimal database performance

## Technology Stack

- **Backend**: Spring Boot 3.2.0, Spring Security 6
- **Database**: PostgreSQL with JPA/Hibernate
- **Caching**: Redis for session management
- **Authentication**: JWT with refresh tokens
- **SMS Service**: Twilio for OTP delivery
- **Mobile**: Android Keystore, iOS Keychain

## Quick Start

### Prerequisites
- Java 17+
- PostgreSQL 12+
- Redis 6+
- Maven 3.6+

### 1. Database Setup
```bash
# Create PostgreSQL database
createdb startup_auth_db

# Run initialization script
psql -d startup_auth_db -f database/init.sql
```

### 2. Redis Setup
```bash
# Install Redis (Ubuntu/Debian)
sudo apt install redis-server

# Start Redis
sudo systemctl start redis-server
```

### 3. Environment Variables
Create a `.env` file or set environment variables:

```bash
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export JWT_SECRET=your-256-bit-secret-key-here-must-be-long-enough
export TWILIO_ACCOUNT_SID=your_twilio_sid
export TWILIO_AUTH_TOKEN=your_twilio_token
export TWILIO_PHONE_NUMBER=your_twilio_phone
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### 4. Build and Run
```bash
# Build the project
./mvnw clean package

# Run the application
java -jar target/secure-auth-system-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/v1/auth/register
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "securePassword123",
    "phone": "+1234567890"
}
```

#### Login with Email/Password
```http
POST /api/v1/auth/login
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "securePassword123"
}
```

#### Send OTP
```http
POST /api/v1/auth/send-otp
Content-Type: application/json

{
    "phoneNumber": "+1234567890"
}
```

#### Verify OTP
```http
POST /api/v1/auth/verify-otp
Content-Type: application/json

{
    "phoneNumber": "+1234567890",
    "otp": "123456"
}
```

#### Refresh Token
```http
POST /api/v1/auth/refresh-token
Content-Type: application/json

{
    "refreshToken": "your-refresh-token-here"
}
```

#### Logout
```http
POST /api/v1/auth/logout
Authorization: Bearer your-access-token
Content-Type: application/json

{
    "refreshToken": "your-refresh-token-here"
}
```

### Response Format
All API responses follow this format:
```json
{
    "success": true,
    "message": "Operation completed successfully",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "tokenType": "Bearer",
        "expiresIn": 900,
        "userId": 1,
        "email": "user@example.com",
        "roles": ["CUSTOMER"]
    }
}
```

## Database Schema

The system uses the following main tables:
- `users` - User account information
- `roles` - Available roles in the system
- `user_roles` - Many-to-many relationship between users and roles
- `sessions` - Active user sessions with refresh tokens
- `customers`, `agents`, `vendors` - Extended user profiles

## Mobile Integration

### Android Implementation
See `mobile-clients/android/SecureStorageManager.kt` for:
- Secure token storage using EncryptedSharedPreferences
- Android Keystore integration
- Biometric authentication support

### iOS Implementation
See `mobile-clients/ios/KeychainManager.swift` for:
- Keychain Services integration
- Secure token storage
- Touch ID/Face ID support

## Configuration

### Security Configuration
- JWT secret key (minimum 256 bits)
- Token expiration times
- Session timeout
- CORS settings

### Database Configuration
- Connection pooling settings
- Query optimization
- Index management

### Redis Configuration
- Session storage
- Cache expiration
- Connection pooling

## Deployment

### Development
```bash
./mvnw spring-boot:run
```

### Production
```bash
# Build production JAR
./mvnw clean package -Pprod

# Run with production profile
java -jar -Dspring.profiles.active=prod target/secure-auth-system-0.0.1-SNAPSHOT.jar
```

### Docker (Optional)
```bash
# Build Docker image
docker build -t secure-auth-system .

# Run with Docker Compose
docker-compose up -d
```

## Monitoring and Health Checks

- Health check endpoint: `GET /api/v1/auth/health`
- Actuator endpoints: `/actuator/health`, `/actuator/metrics`
- Application logs in structured format

## Security Best Practices

1. **Token Management**
   - Short-lived access tokens (15 minutes)
   - Long-lived refresh tokens (24 hours)
   - Automatic token rotation

2. **Password Security**
   - BCrypt hashing with salt
   - Password complexity requirements
   - Account lockout mechanisms

3. **Session Management**
   - Server-side session validation
   - Automatic session cleanup
   - Device-specific sessions

4. **Mobile Security**
   - Hardware-backed key storage
   - Biometric authentication
   - Certificate pinning (recommended)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue in the GitHub repository
- Contact the development team

## Changelog

### v1.0.0
- Initial release
- Basic authentication with JWT
- Role-based access control
- Mobile client support
- Redis session management
- PostgreSQL database integration
