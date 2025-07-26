#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Import config.sh if it exists, otherwise set defaults
if [ -f "./deploy/config.sh" ]; then
    echo "Loading port configuration from config.sh"
    source ./deploy/config.sh
else
    echo "Using default port configuration"
    BACKEND_PORT=9511
    FRONTEND_PORT=3000
    DATABASE_PORT=9503
fi

echo -e "${GREEN}Starting deployment process...${NC}"
echo "Using backend port: $BACKEND_PORT"
echo "Using frontend port: $FRONTEND_PORT"

# Build Backend
echo -e "\n${GREEN}Building Spring Boot Backend...${NC}"
cd SurveyorTrackingBackend
./mvnw clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}Backend build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}Backend built successfully!${NC}"

# Build Frontend
echo -e "\n${GREEN}Building React Frontend...${NC}"
cd ../surveyor-tracking-dashboard
npm install
if [ $? -ne 0 ]; then
    echo -e "${RED}Frontend npm install failed!${NC}"
    exit 1
fi

# Use the custom build script that incorporates port configuration
npm run build:config
if [ $? -ne 0 ]; then
    echo -e "${RED}Frontend build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}Frontend built successfully!${NC}"

# Create deployment directory
echo -e "\n${GREEN}Preparing deployment files...${NC}"
cd ..
mkdir -p deploy
rm -rf deploy/*

# Copy backend jar
cp SurveyorTrackingBackend/target/*.jar deploy/surveyor-tracking-backend.jar

# Copy frontend build
cp -r surveyor-tracking-dashboard/build deploy/frontend

# Create startup script
echo -e "\n${GREEN}Creating startup script...${NC}"
cat > deploy/start.sh << 'EOL'
#!/bin/bash

# Start Backend
echo "Starting Spring Boot Backend..."
nohup java -jar surveyor-tracking-backend.jar > backend.log 2>&1 &
echo $! > backend.pid

# Serve Frontend with nginx if installed, otherwise use serve
if command -v nginx >/dev/null 2>&1; then
    echo "Nginx detected, please configure nginx.conf"
else
    echo "Installing serve..."
    npm install -g serve
    echo "Starting Frontend server..."
    nohup serve -s frontend -l $FRONTEND_PORT > frontend.log 2>&1 &
    echo $! > frontend.pid
fi

echo "Deployment complete!"
echo "Backend running on port $BACKEND_PORT"
echo "Frontend running on port $FRONTEND_PORT"
EOL

# Create stop script
cat > deploy/stop.sh << 'EOL'
#!/bin/bash

# Stop Backend
if [ -f backend.pid ]; then
    echo "Stopping backend..."
    kill $(cat backend.pid)
    rm backend.pid
fi

# Stop Frontend
if [ -f frontend.pid ]; then
    echo "Stopping frontend..."
    kill $(cat frontend.pid)
    rm frontend.pid
fi
EOL

# Create nginx configuration template
cat > deploy/nginx.conf.template << 'EOL'
server {
    listen 80;
    server_name localhost;

    # Frontend
    location / {
        root /path/to/frontend;
        try_files $uri $uri/ /index.html;
        index index.html;
    }

    # Backend API
    location /api {
        proxy_pass http://localhost:$BACKEND_PORT;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # WebSocket endpoint
    location /ws {
        proxy_pass http://localhost:$BACKEND_PORT;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
    }
}
EOL

# Make scripts executable
chmod +x deploy/start.sh deploy/stop.sh

echo -e "${GREEN}Deployment package created successfully!${NC}"
echo -e "\nDeployment instructions:"
echo "1. Copy the 'deploy' directory to your production server"
echo "2. Configure application.properties in the JAR if needed"
echo "3. Configure nginx.conf.template with your domain"
echo "4. Run ./start.sh to start the application"
echo "5. Use ./stop.sh to stop the application"