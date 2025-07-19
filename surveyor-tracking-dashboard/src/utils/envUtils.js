// Environment utilities for dynamic configuration
export const getEnvironmentVariable = (key) => {
  // Check for React environment variables
  const reactKey = `REACT_APP_${key}`;
  if (process.env[reactKey]) {
    return process.env[reactKey];
  }
  
  // Check for direct environment variable
  if (process.env[key]) {
    return process.env[key];
  }
  
  // Check for custom configuration
  if (process.env.REACT_APP_PORT_CONFIG) {
    try {
      const config = JSON.parse(process.env.REACT_APP_PORT_CONFIG);
      return config[key];
    } catch (e) {
      console.warn('Could not parse port configuration from environment');
    }
  }
  
  return null;
};

export const isDevelopment = () => {
  return process.env.NODE_ENV === 'development';
};

export const isProduction = () => {
  return process.env.NODE_ENV === 'production';
};

export const isTest = () => {
  return process.env.NODE_ENV === 'test';
};

export const getApiUrl = () => {
  const customApiUrl = getEnvironmentVariable('API_URL');
  if (customApiUrl) {
    return customApiUrl;
  }
  
  if (isDevelopment()) {
    return 'http://localhost:6565';
  }
  
  return 'http://183.82.114.29:6565';
};

export const getWebSocketUrl = () => {
  const customWsUrl = getEnvironmentVariable('WS_URL');
  if (customWsUrl) {
    return customWsUrl;
  }
  
  const apiUrl = getApiUrl();
  return apiUrl.replace('http', 'ws') + '/ws/location';
}; 