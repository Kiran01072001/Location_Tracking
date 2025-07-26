#!/bin/bash
# Script to build the React application with properly configured port values from config.js

# Function to extract a port from config.js
extract_port() {
  local port_name=$1
  local config_file="../src/config.js"
  local port=$(grep -Eo "${port_name}:\s*([0-9]+)" "$config_file" | grep -Eo '[0-9]+')
  
  if [[ -z "$port" ]]; then
    echo "Warning: Couldn't extract $port_name from config.js, using default"
    if [[ "$port_name" == "BACKEND_API" ]]; then
      echo "9511"
    elif [[ "$port_name" == "FRONTEND" ]]; then
      echo "3000"
    else
      echo "Unknown port type"
      exit 1
    fi
  else
    echo "$port"
  fi
}

# Change to script directory
cd "$(dirname "$0")"

# Extract ports from config
BACKEND_PORT=$(extract_port "BACKEND_API")
FRONTEND_PORT=$(extract_port "FRONTEND")

echo "Using Backend Port: $BACKEND_PORT"
echo "Using Frontend Port: $FRONTEND_PORT"

# Create a temporary .env.production file with proper values
cat > ../.env.production.temp << EOF
# Automatically generated from config.js
REACT_APP_API_URL=http://183.82.114.29:${BACKEND_PORT}
REACT_APP_WS_URL=ws://183.82.114.29:${BACKEND_PORT}/ws
EOF

# Replace the original .env.production file
mv ../.env.production.temp ../.env.production

echo "Updated .env.production with correct port values"

# Run the build script
cd ..
npm run build

echo "Build completed with configured port values"
