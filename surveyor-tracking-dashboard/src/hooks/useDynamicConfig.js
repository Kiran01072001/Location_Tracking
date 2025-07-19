import { useState, useEffect, useCallback } from 'react';
import config from '../config';

export const useDynamicConfig = () => {
  const [dropdownOptions, setDropdownOptions] = useState({
    cities: [],
    projects: [],
    statuses: [],
    roles: []
  });
  
  const [systemConfig, setSystemConfig] = useState({
    map: {},
    features: {},
    ui: {}
  });
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Fetch all dropdown options from backend
  const fetchDropdownOptions = useCallback(async () => {
    try {
      const response = await fetch(`${config.backendHost}/api/config/dropdowns`);
      if (!response.ok) throw new Error('Failed to fetch dropdown options');
      
      const data = await response.json();
      setDropdownOptions(data);
    } catch (err) {
      console.error('Error fetching dropdown options:', err);
      setError(err.message);
      // Fallback to default options
      setDropdownOptions({
        cities: ['Hyderabad', 'Mumbai', 'Delhi', 'Bangalore', 'Chennai'],
        projects: ['PTMS', 'Survey', 'Mapping', 'Inspection', 'Construction'],
        statuses: ['Online', 'Offline', 'Busy', 'Available', 'On Break'],
        roles: ['Surveyor', 'Supervisor', 'Manager', 'Coordinator', 'Technician']
      });
    }
  }, []);

  // Fetch system configuration
  const fetchSystemConfig = useCallback(async () => {
    try {
      const response = await fetch(`${config.backendHost}/api/config/system`);
      if (!response.ok) throw new Error('Failed to fetch system config');
      
      const data = await response.json();
      setSystemConfig(data);
    } catch (err) {
      console.error('Error fetching system config:', err);
      // Fallback to default config
      setSystemConfig({
        map: {
          defaultCenter: [17.3850, 78.4867],
          defaultZoom: 10,
          trackingInterval: 5000,
          refreshInterval: 30000
        },
        features: {
          realTimeTracking: true,
          historicalRoutes: true,
          geofencing: false,
          notifications: false,
          darkMode: true,
          customThemes: true
        },
        ui: {
          animations: true,
          transitions: true,
          loadingSpinners: true,
          responsive: true
        }
      });
    }
  }, []);

  // Add new city
  const addCity = useCallback(async (city) => {
    try {
      const response = await fetch(`${config.backendHost}/api/config/cities`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(city)
      });
      
      if (!response.ok) throw new Error('Failed to add city');
      
      // Refresh dropdown options
      await fetchDropdownOptions();
      return true;
    } catch (err) {
      console.error('Error adding city:', err);
      return false;
    }
  }, [fetchDropdownOptions]);

  // Add new project
  const addProject = useCallback(async (project) => {
    try {
      const response = await fetch(`${config.backendHost}/api/config/projects`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(project)
      });
      
      if (!response.ok) throw new Error('Failed to add project');
      
      // Refresh dropdown options
      await fetchDropdownOptions();
      return true;
    } catch (err) {
      console.error('Error adding project:', err);
      return false;
    }
  }, [fetchDropdownOptions]);

  // Update system configuration
  const updateSystemConfig = useCallback(async (newConfig) => {
    try {
      const response = await fetch(`${config.backendHost}/api/config/system`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newConfig)
      });
      
      if (!response.ok) throw new Error('Failed to update system config');
      
      setSystemConfig(newConfig);
      return true;
    } catch (err) {
      console.error('Error updating system config:', err);
      return false;
    }
  }, []);

  // Initialize configuration
  useEffect(() => {
    const initializeConfig = async () => {
      setLoading(true);
      setError(null);
      
      try {
        await Promise.all([
          fetchDropdownOptions(),
          fetchSystemConfig()
        ]);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    initializeConfig();
  }, [fetchDropdownOptions, fetchSystemConfig]);

  // Check if feature is enabled
  const isFeatureEnabled = useCallback((featureName) => {
    return systemConfig.features?.[featureName] || false;
  }, [systemConfig]);

  // Get dropdown options by type
  const getDropdownOptions = useCallback((type) => {
    return dropdownOptions[type] || [];
  }, [dropdownOptions]);

  // Get map configuration
  const getMapConfig = useCallback(() => {
    return systemConfig.map || {};
  }, [systemConfig]);

  // Get UI configuration
  const getUIConfig = useCallback(() => {
    return systemConfig.ui || {};
  }, [systemConfig]);

  return {
    // State
    dropdownOptions,
    systemConfig,
    loading,
    error,
    
    // Actions
    addCity,
    addProject,
    updateSystemConfig,
    fetchDropdownOptions,
    fetchSystemConfig,
    
    // Getters
    isFeatureEnabled,
    getDropdownOptions,
    getMapConfig,
    getUIConfig
  };
}; 