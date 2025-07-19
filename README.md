# 🚀 Surveyor Tracking Dashboard

> **Real-time location tracking system for surveyors with dynamic database-driven configuration**

[![React](https://img.shields.io/badge/React-18.2.0-blue.svg)](https://reactjs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.0-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![WebSocket](https://img.shields.io/badge/WebSocket-Real--time-orange.svg)](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Table of Contents

- [✨ Features](#-features)
- [🏗️ Architecture](#️-architecture)
- [🚀 Quick Start](#-quick-start)
- [📱 Screenshots](#-screenshots)
- [🔧 Configuration](#-configuration)
- [📊 API Documentation](#-api-documentation)
- [🛠️ Development](#️-development)
- [📦 Deployment](#-deployment)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)

## ✨ Features

### 🎯 **Core Features**
- **Real-time Location Tracking** - Live GPS tracking with WebSocket
- **Historical Route Analysis** - View past surveyor movements
- **Dynamic Database-Driven UI** - Dropdowns populated from database
- **Surveyor Management** - Add, edit, delete surveyors
- **Interactive Maps** - Leaflet.js with custom markers
- **Responsive Design** - Works on desktop and mobile

### 🔄 **Dynamic System**
- **Database-Driven Dropdowns** - Cities and projects from surveyor data
- **Feature Flags** - Enable/disable features dynamically
- **Dynamic Configuration** - System settings from backend
- **Real-time Updates** - UI updates when database changes
- **Fallback System** - Graceful degradation when backend unavailable

### 🎨 **Modern UI/UX**
- **Glass Morphism Design** - Modern glass-like effects
- **Gradient Backgrounds** - Beautiful color schemes
- **Smooth Animations** - CSS transitions and animations
- **Dark/Light Themes** - Dynamic theme switching
- **Mobile Responsive** - Optimized for all devices

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React)                        │
├─────────────────────────────────────────────────────────────┤
│  📱 Surveyor Dashboard    │  🗺️ Live Tracking    │  📊 Reports │
│  • Login/Auth            │  • Real-time Maps    │  • Analytics │
│  • Surveyor Management   │  • Historical Routes │  • Export    │
│  • Dynamic Dropdowns     │  • WebSocket Updates │  • Charts    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Backend (Spring Boot)                    │
├─────────────────────────────────────────────────────────────┤
│  🔌 REST APIs             │  📡 WebSocket Server │  🗄️ Database │
│  • Surveyor CRUD          │  • Live Location     │  • PostgreSQL │
│  • Dynamic Config         │  • Real-time Updates │  • JPA/Hibernate │
│  • Authentication         │  • Status Tracking   │  • Flyway Migrations │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Mobile App (Android)                    │
├─────────────────────────────────────────────────────────────┤
│  📍 GPS Tracking          │  🔐 Authentication   │  📤 Data Sync │
│  • Background Location    │  • Secure Login      │  • Offline Support │
│  • Battery Optimized      │  • Session Management│  • Auto Sync      │
│  • Real-time Updates      │  • Biometric Auth    │  • Error Handling  │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- **Java 17+** - Backend runtime
- **Node.js 16+** - Frontend development
- **PostgreSQL 15+** - Database
- **Android Studio** - Mobile app development

### 1. Clone Repository
```bash
git clone https://github.com/Kiran01072001/Surveyor_Tracking_Dashboard.git
cd Surveyor_Tracking_Dashboard
```

### 2. Backend Setup
```bash
cd SurveyorTrackingBackend

# Configure database
# Edit src/main/resources/application.properties

# Run with Maven
mvn spring-boot:run

# Or build JAR
mvn clean package
java -jar target/SurveyorTrackingBackend-0.0.1-SNAPSHOT.jar
```

### 3. Frontend Setup
```bash
cd surveyor-tracking-dashboard

# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build
```

### 4. Mobile App Setup
```bash
cd SurveyorMobileApp

# Open in Android Studio
# Build and run on device/emulator
```

## 📱 Screenshots

### 🖥️ Dashboard
![Dashboard](https://via.placeholder.com/800x400/2563eb/ffffff?text=Surveyor+Dashboard)

### 📍 Live Tracking
![Live Tracking](https://via.placeholder.com/800x400/10b981/ffffff?text=Live+Location+Tracking)

### 📊 Reports
![Reports](https://via.placeholder.com/800x400/8b5cf6/ffffff?text=Analytics+%26+Reports)

## 🔧 Configuration

### Environment Variables
```bash
# Backend Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/surveyor_tracking
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
SERVER_PORT=6565

# Frontend Configuration
REACT_APP_BACKEND_URL=http://localhost:6565
REACT_APP_WEBSOCKET_URL=ws://localhost:6565/ws
```

### Database Schema
```sql
-- Surveyors table
CREATE TABLE surveyor (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255),
    project_name VARCHAR(255),
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255)
);

-- Location tracking table
CREATE TABLE location_track (
    id BIGSERIAL PRIMARY KEY,
    surveyor_id VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    timestamp TIMESTAMP,
    status VARCHAR(50)
);
```

## 📊 API Documentation

### 🔌 REST APIs

#### Surveyor Management
```http
GET    /api/surveyors              # Get all surveyors
POST   /api/surveyors              # Create surveyor
PUT    /api/surveyors/{id}         # Update surveyor
DELETE /api/surveyors/{id}         # Delete surveyor
GET    /api/surveyors/status       # Get surveyor status
```

#### Dynamic Configuration
```http
GET    /api/config/cities          # Get all cities
GET    /api/config/projects        # Get all projects
GET    /api/config/statuses        # Get all statuses
GET    /api/config/roles           # Get all roles
GET    /api/config/dropdowns       # Get all dropdown options
GET    /api/config/system          # Get system configuration
POST   /api/config/cities          # Add new city
POST   /api/config/projects        # Add new project
PUT    /api/config/system          # Update system config
```

#### Location Tracking
```http
GET    /api/location/historical    # Get historical routes
POST   /api/location/update        # Update location
GET    /api/location/live/{id}     # Get live location
```

### 📡 WebSocket APIs
```javascript
// Subscribe to live location updates
stompClient.subscribe('/topic/location/{surveyorId}', (message) => {
    const location = JSON.parse(message.body);
    // Handle location update
});

// Send location update
stompClient.send('/app/location/update', {}, JSON.stringify({
    surveyorId: 'SUR001',
    latitude: 17.4010007,
    longitude: 78.5643879,
    timestamp: new Date()
}));
```

## 🛠️ Development

### Project Structure
```
Surveyor_Tracking_Dashboard/
├── 📁 surveyor-tracking-dashboard/     # React Frontend
│   ├── 📁 src/
│   │   ├── 📁 components/             # React components
│   │   ├── 📁 hooks/                  # Custom hooks
│   │   ├── 📁 pages/                  # Page components
│   │   ├── 📁 utils/                  # Utility functions
│   │   └── 📁 config.js               # Configuration
│   └── 📁 public/                     # Static assets
├── 📁 SurveyorTrackingBackend/         # Spring Boot Backend
│   ├── 📁 src/main/java/
│   │   ├── 📁 controller/             # REST controllers
│   │   ├── 📁 service/                # Business logic
│   │   ├── 📁 model/                  # Data models
│   │   ├── 📁 repository/             # Data access
│   │   └── 📁 config/                 # Configuration
│   └── 📁 src/main/resources/         # Configuration files
├── 📁 SurveyorMobileApp/              # Android Mobile App
│   ├── 📁 app/src/main/
│   │   ├── 📁 java/                   # Kotlin/Java code
│   │   ├── 📁 res/                    # Resources
│   │   └── 📁 AndroidManifest.xml     # App manifest
│   └── 📁 gradle/                     # Build configuration
└── 📁 deploy/                         # Deployment scripts
```

### Development Commands
```bash
# Backend Development
cd SurveyorTrackingBackend
mvn spring-boot:run                    # Run with hot reload
mvn test                               # Run tests
mvn clean package                      # Build JAR

# Frontend Development
cd surveyor-tracking-dashboard
npm start                              # Start dev server
npm test                               # Run tests
npm run build                          # Build for production
npm run eject                          # Eject from CRA

# Mobile App Development
cd SurveyorMobileApp
./gradlew assembleDebug                 # Build debug APK
./gradlew installDebug                  # Install on device
```

## 📦 Deployment

### Docker Deployment
```dockerfile
# Backend Dockerfile
FROM openjdk:17-jdk-slim
COPY target/SurveyorTrackingBackend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 6565
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```dockerfile
# Frontend Dockerfile
FROM node:16-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
FROM nginx:alpine
COPY --from=0 /app/build /usr/share/nginx/html
EXPOSE 80
```

### Production Deployment
```bash
# Backend Deployment
mvn clean package
java -jar target/SurveyorTrackingBackend-0.0.1-SNAPSHOT.jar

# Frontend Deployment
npm run build
serve -s build -l 3000

# Database Setup
psql -U postgres -d surveyor_tracking -f schema.sql
```

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### Development Guidelines
- Follow existing code style
- Add tests for new features
- Update documentation
- Ensure backward compatibility

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **React** - Frontend framework
- **Spring Boot** - Backend framework
- **PostgreSQL** - Database
- **Leaflet.js** - Interactive maps
- **Material-UI** - UI components
- **WebSocket** - Real-time communication

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Kiran01072001/Surveyor_Tracking_Dashboard/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Kiran01072001/Surveyor_Tracking_Dashboard/discussions)
- **Email**: support@surveyortracking.com

---

<div align="center">

**Made with ❤️ by the Surveyor Tracking Team**

[![GitHub stars](https://img.shields.io/github/stars/Kiran01072001/Surveyor_Tracking_Dashboard)](https://github.com/Kiran01072001/Surveyor_Tracking_Dashboard/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/Kiran01072001/Surveyor_Tracking_Dashboard)](https://github.com/Kiran01072001/Surveyor_Tracking_Dashboard/network)
[![GitHub issues](https://img.shields.io/github/issues/Kiran01072001/Surveyor_Tracking_Dashboard)](https://github.com/Kiran01072001/Surveyor_Tracking_Dashboard/issues)

</div>

