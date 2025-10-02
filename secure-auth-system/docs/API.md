# API Documentation

## Base URL
```
http://localhost:8080/api/v1
```

## Authentication
All protected endpoints require a Bearer token in the Authorization header:
```
Authorization: Bearer <access_token>
```

## Error Handling
All endpoints return standardized error responses:

```json
{
    "success": false,
    "message": "Error description",
    "timestamp": "2023-10-02T10:15:30Z",
    "path": "/api/v1/auth/login"
}
```

## HTTP Status Codes
- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Access denied
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists
- `500 Internal Server Error` - Server error

## Endpoints

### Authentication

#### POST /auth/register
Register a new user account.

**Request Body:**
```json
{
    "email": "user@example.com",
    "password": "securePassword123",
    "phone": "+1234567890"
}
```

**Response:**
```json
{
    "success": true,
    "message": "User registered successfully",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIs...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
        "tokenType": "Bearer",
        "expiresIn": 900,
        "userId": 1,
        "email": "user@example.com",
        "roles": ["CUSTOMER"]
    }
}
```

#### POST /auth/login
Authenticate user with email and password.

**Request Body:**
```json
{
    "email": "user@example.com",
    "password": "securePassword123"
}
```

**Response:** Same as register response.

#### POST /auth/send-otp
Send OTP to user's phone number.

**Request Body:**
```json
{
    "phoneNumber": "+1234567890"
}
```

**Response:**
```json
{
    "success": true,
    "message": "OTP sent successfully"
}
```

#### POST /auth/verify-otp
Verify OTP and authenticate user.

**Request Body:**
```json
{
    "phoneNumber": "+1234567890",
    "otp": "123456"
}
```

**Response:** Same as login response.

#### POST /auth/refresh-token
Refresh access token using refresh token.

**Request Body:**
```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:** Same as login response.

#### POST /auth/logout
Logout user and revoke refresh token.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request Body:**
```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:**
```json
{
    "success": true,
    "message": "User logged out successfully"
}
```

#### GET /auth/health
Health check endpoint.

**Response:**
```json
{
    "success": true,
    "message": "Authentication service is running",
    "data": "OK"
}
```

## Rate Limiting

### OTP Endpoints
- Maximum 3 OTP requests per phone number per 15 minutes
- OTP expires in 5 minutes

### Login Attempts
- Maximum 5 failed login attempts per email per hour
- Account lockout for 30 minutes after exceeding limit

## Request Examples

### cURL Examples

**Register User:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123",
    "phone": "+1234567890"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123"
  }'
```

**Access Protected Resource:**
```bash
curl -X GET http://localhost:8080/api/v1/protected-endpoint \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

### JavaScript Examples

**Register User:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/auth/register', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
    },
    body: JSON.stringify({
        email: 'user@example.com',
        password: 'securePassword123',
        phone: '+1234567890'
    })
});

const data = await response.json();
console.log(data);
```

**Login:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/auth/login', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
    },
    body: JSON.stringify({
        email: 'user@example.com',
        password: 'securePassword123'
    })
});

const data = await response.json();
localStorage.setItem('accessToken', data.data.accessToken);
localStorage.setItem('refreshToken', data.data.refreshToken);
```

## Token Management

### Access Token
- **Lifetime:** 15 minutes
- **Usage:** Include in Authorization header for API requests
- **Format:** Bearer token
- **Claims:** userId, roles, expiration

### Refresh Token
- **Lifetime:** 24 hours
- **Usage:** Use to obtain new access tokens
- **Storage:** Secure storage (Keychain/Keystore)
- **Rotation:** New refresh token issued on each refresh

### Token Refresh Flow
1. Access token expires (401 response)
2. Use refresh token to get new access token
3. Update stored access token
4. Retry original request

```javascript
// Automatic token refresh interceptor
axios.interceptors.response.use(
    (response) => response,
    async (error) => {
        if (error.response?.status === 401) {
            const refreshToken = localStorage.getItem('refreshToken');
            if (refreshToken) {
                try {
                    const response = await fetch('/api/v1/auth/refresh-token', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ refreshToken })
                    });

                    const data = await response.json();
                    localStorage.setItem('accessToken', data.data.accessToken);

                    // Retry original request
                    error.config.headers.Authorization = `Bearer ${data.data.accessToken}`;
                    return axios.request(error.config);
                } catch (refreshError) {
                    // Redirect to login
                    window.location.href = '/login';
                }
            }
        }
        return Promise.reject(error);
    }
);
```
