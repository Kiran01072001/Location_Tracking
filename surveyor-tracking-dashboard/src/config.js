// Dynamic configuration system for Surveyor Tracking
import { getEnvironmentVariable } from './utils/envUtils';

// Environment-based port configuration
const getEnvPort = (defaultPort) => {
  const envPort = getEnvironmentVariable('PORT');
  return envPort ? parseInt(envPort) : defaultPort;
};

// Centralized port configuration
const PORTS = {
  // Backend API port
  BACKEND_API: getEnvPort(9511),
  // Frontend port
  FRONTEND: getEnvPort(3000),
  // Database port if needed
  DATABASE: getEnvPort(9503)
};

// Dynamic System Configuration
const DYNAMIC_CONFIG = {
  // Theme Configuration
  themes: {
    light: {
      primary: '#667eea',
      secondary: '#764ba2',
      background: 'linear-gradient(135deg, #f8fafc 0%, #e0e7ff 100%)',
      card: 'rgba(255, 255, 255, 0.95)',
      text: '#1f2937',
      border: '#e5e7eb'
    },
    dark: {
      primary: '#1a1a2e',
      secondary: '#16213e',
      background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
      card: 'rgba(30, 41, 59, 0.95)',
      text: '#f1f5f9',
      border: '#334155'
    },
    custom: {
      primary: '#10b981',
      secondary: '#059669',
      background: 'linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%)',
      card: 'rgba(255, 255, 255, 0.95)',
      text: '#1f2937',
      border: '#d1fae5'
    }
  },

  // Dynamic Dropdown Options
  dropdowns: {
    cities: [
      'Hyderabad', 'Mumbai', 'Delhi', 'Bangalore', 'Chennai', 
      'Kolkata', 'Pune', 'Ahmedabad', 'Jaipur', 'Lucknow',
      'Kanpur', 'Nagpur', 'Indore', 'Thane', 'Bhopal',
      'Visakhapatnam', 'Pimpri-Chinchwad', 'Patna', 'Vadodara', 'Ghaziabad'
    ],
    projects: [
      'PTMS', 'Survey', 'Mapping', 'Inspection', 'Construction',
      'Infrastructure', 'Urban Planning', 'Environmental', 'Transportation',
      'Real Estate', 'Agriculture', 'Mining', 'Oil & Gas', 'Telecom',
      'Healthcare', 'Education', 'Retail', 'Manufacturing', 'Logistics'
    ],
    statuses: [
      'Online', 'Offline', 'Busy', 'Available', 'On Break',
      'In Meeting', 'Traveling', 'On Site', 'Office', 'Field Work'
    ],
    roles: [
      'Surveyor', 'Supervisor', 'Manager', 'Coordinator', 'Technician',
      'Engineer', 'Analyst', 'Consultant', 'Inspector', 'Planner'
    ]
  },

  // Map Configuration
  map: {
    providers: {
      openstreetmap: {
        url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
        attribution: '© OpenStreetMap contributors'
      },
      google: {
        url: 'https://maps.googleapis.com/maps/api/js?key=YOUR_API_KEY',
        attribution: '© Google Maps'
      },
      bing: {
        url: 'https://ecn.t3.tiles.virtualearth.net/tiles/a{q}.jpeg?g=1',
        attribution: '© Bing Maps'
      }
    },
    defaultCenter: [17.3850, 78.4867], // Hyderabad
    defaultZoom: 10,
    trackingInterval: 5000, // 5 seconds
    refreshInterval: 30000, // 30 seconds
    maxMarkers: 100,
    enableClustering: true,
    enableHeatmap: false
  },

  // Feature Flags
  features: {
    realTimeTracking: true,
    historicalRoutes: true,
    geofencing: false,
    notifications: false,
    offlineMode: false,
    analytics: false,
    reporting: false,
    multiLanguage: false,
    darkMode: true,
    customThemes: true,
    exportData: true,
    bulkOperations: false,
    advancedFilters: true,
    searchFunctionality: true,
    userManagement: true,
    roleBasedAccess: false,
    auditLogs: false,
    backupRestore: false
  },

  // API Configuration
  api: {
    timeout: 30000,
    retryAttempts: 3,
    retryDelay: 1000,
    enableCaching: true,
    cacheExpiry: 300000, // 5 minutes
    enableCompression: true,
    enableLogging: true
  },

  // UI Configuration
  ui: {
    animations: true,
    transitions: true,
    loadingSpinners: true,
    progressBars: true,
    tooltips: true,
    confirmations: true,
    autoSave: true,
    autoRefresh: true,
    responsive: true,
    accessibility: true,
    keyboardShortcuts: false,
    dragAndDrop: false,
    infiniteScroll: false,
    virtualScrolling: false
  },

  // Performance Configuration
  performance: {
    enableLazyLoading: true,
    enableCodeSplitting: true,
    enableServiceWorker: false,
    enablePWA: false,
    enableOptimization: true,
    enableCompression: true,
    enableMinification: true,
    enableCaching: true,
    maxConcurrentRequests: 5,
    requestQueueSize: 10
  }
};

const config = {
  // Base URLs constructed using the port configuration
  backendHost: `http://183.82.114.29:${PORTS.BACKEND_API}`,
  frontendHost: `http://183.82.114.29:${PORTS.FRONTEND}`,
  
  // WebSocket related URLs - SockJS expects HTTP/HTTPS, not ws://
  webSocketUrl: `http://183.82.114.29:${PORTS.BACKEND_API}/ws/location`,
  
  // Export ports for direct access when needed
  ports: PORTS,
  
  // Dynamic configuration
  dynamic: DYNAMIC_CONFIG,
  
  // Error handling helper function
  handleFetchError: (error, endpoint) => {
    console.error(`Error fetching from ${endpoint}:`, error);
    // Add more detailed logging for network issues
    if (error.name === 'TypeError' && error.message === 'Failed to fetch') {
      console.error('Network error - Check if backend server is running and accessible');
      console.error('Backend URL:', config.backendHost);
    }
    return error;
  },

  // Dynamic configuration getters
  getDropdownOptions: (type) => {
    return DYNAMIC_CONFIG.dropdowns[type] || [];
  },

  getTheme: (themeName = 'light') => {
    return DYNAMIC_CONFIG.themes[themeName] || DYNAMIC_CONFIG.themes.light;
  },

  isFeatureEnabled: (featureName) => {
    return DYNAMIC_CONFIG.features[featureName] || false;
  },

  getMapProvider: (providerName = 'openstreetmap') => {
    return DYNAMIC_CONFIG.map.providers[providerName];
  },

  // Configuration update methods
  updateDropdownOptions: (type, options) => {
    if (DYNAMIC_CONFIG.dropdowns[type]) {
      DYNAMIC_CONFIG.dropdowns[type] = options;
    }
  },

  addDropdownOption: (type, option) => {
    if (DYNAMIC_CONFIG.dropdowns[type]) {
      DYNAMIC_CONFIG.dropdowns[type].push(option);
    }
  },

  enableFeature: (featureName) => {
    DYNAMIC_CONFIG.features[featureName] = true;
  },

  disableFeature: (featureName) => {
    DYNAMIC_CONFIG.features[featureName] = false;
  }
};

export default config;