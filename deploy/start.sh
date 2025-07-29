#!/bin/bash

# Source the config for ports
if [ -f "./config.sh" ]; then
    source ./config.sh
else
    echo "Warning: config.sh not found, using default ports"
    # Default values from centralized config
    BACKEND_PORT=6565
    DATABASE_PORT=5432
    FRONTEND_PORT=3000
fi

# Update the nginx.conf file with proper port values if it exists
if [ -f "./nginx.conf.template" ]; then
    echo "Configuring nginx with port settings..."
    cat "./nginx.conf.template" | sed "s/BACKEND_PORT/$BACKEND_PORT/g" > "./nginx.conf"
    echo "Created nginx.conf with backend port $BACKEND_PORT"
fi

# Start Backend
echo "Starting Spring Boot Backend on port $BACKEND_PORT..."
nohup java -jar surveyor-tracking-backend.jar > backend.log 2>&1 &
echo $! > backend.pid

# Serve Frontend with nginx if installed, otherwise use serve
if command -v nginx >/dev/null 2>&1; then
    echo "Nginx detected. Copy nginx.conf to your nginx config directory."
else
    echo "Installing serve..."
    npm install -g serve
    echo "Starting Frontend server on port $FRONTEND_PORT..."
    nohup serve -s frontend -l $FRONTEND_PORT > frontend.log 2>&1 &
    echo $! > frontend.pid
fi

echo "Deployment complete!"
echo "Backend running on port $BACKEND_PORT"
echo "Frontend running on port $FRONTEND_PORT"
