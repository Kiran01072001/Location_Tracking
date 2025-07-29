import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import 'leaflet/dist/leaflet.css';

import SurveyorTable from '../components/SurveyorTable';
import SurveyorFormModal from '../components/SurveyorFormModal';
// Material-UI components are used in inline styles and JSX
import { Snackbar, Alert, Dialog, DialogContent, DialogTitle, DialogActions, Button } from '@mui/material';
import { useDynamicConfig } from '../hooks/useDynamicConfig';

// Configuration matching your backend exactly
const config = {
  backendHost: 'http://183.82.114.29:6565',
  webSocketUrl: 'http://183.82.114.29:6565/ws/location',
  refreshInterval: 30000, // 30 seconds
  liveUpdateInterval: 5000, // 5 seconds for live tracking
  handleFetchError: (error, endpoint) => {
    console.error(`Error fetching from ${endpoint}:`, error);
    if (error.name === 'TypeError' && error.message === 'Failed to fetch') {
      console.error('Network error - Check if backend server is running and accessible');
      console.error('Backend URL:', config.backendHost);
    }
    return error;
  }
};

// Fix for default markers in react-leaflet
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// ✅ NEW: Utility functions for distance calculation and time formatting
const calculateDistance = (lat1, lon1, lat2, lon2) => {
  const R = 6371; // Radius of Earth in kilometers
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c; // Distance in kilometers
};

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleTimeString('en-US', { 
    hour12: true, 
    hour: 'numeric', 
    minute: '2-digit' 
  });
};

// Custom icons for different states
const liveIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const startIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const endIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

// Component to auto-fit map bounds
const MapBounds = ({ positions }) => {
  const map = useMap();
  
  useEffect(() => {
    if (positions && positions.length > 0) {
      const bounds = L.latLngBounds(positions);
      map.fitBounds(bounds, { padding: [20, 20] });
    }
  }, [positions, map]);
  
  return null;
};

// OSRM Route component for historical routes
const OSRMRoute = ({ coordinates, color = '#6366f1' }) => {
  const [routeCoordinates, setRouteCoordinates] = useState([]);
  
  useEffect(() => {
    if (!coordinates || coordinates.length < 2) return;
    
    const fetchRoute = async () => {
      try {
        const start = coordinates[0];
        const end = coordinates[coordinates.length - 1];
        
        const osrmUrl = `https://router.project-osrm.org/route/v1/walking/${start[1]},${start[0]};${end[1]},${end[0]}?overview=full&geometries=geojson`;
        
        const response = await fetch(osrmUrl);
        const data = await response.json();
        
        if (data.routes && data.routes[0]) {
          const routeGeometry = data.routes[0].geometry.coordinates;
          const leafletCoords = routeGeometry.map(coord => [coord[1], coord[0]]);
          setRouteCoordinates(leafletCoords);
        }
      } catch (error) {
        console.error('Failed to fetch OSRM route:', error);
        setRouteCoordinates(coordinates);
      }
    };
    
    fetchRoute();
  }, [coordinates]);

  if (routeCoordinates.length === 0) return null;
  
  return (
    <Polyline
      positions={routeCoordinates}
      color={color}
      weight={4}
      opacity={0.8}
    />
  );
};

const LiveTrackingPage = () => {
  // Dynamic configuration hook
  const dynamicConfig = useDynamicConfig();
  
  // State management
  const [surveyors, setSurveyors] = useState([]);
  const [statusMap, setStatusMap] = useState({});
  const [selectedSurveyor, setSelectedSurveyor] = useState('');
  const [fromDate, setFromDate] = useState(null);
  const [toDate, setToDate] = useState(null);

  const [isLiveTracking, setIsLiveTracking] = useState(false);
  const [historicalRoute, setHistoricalRoute] = useState([]);
  const [liveLocation, setLiveLocation] = useState(null);
  const [liveTrail, setLiveTrail] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [connectionStatus, setConnectionStatus] = useState('Disconnected');
  const [city, setCity] = useState('');
  const [project, setProject] = useState('');
  const [surveyorSearch, setSurveyorSearch] = useState('');

  // Error state for showing messages when buttons are clicked
  const [showSurveyorError, setShowSurveyorError] = useState(false);
  const [showDateError, setShowDateError] = useState(false);
  const [showNoDataError, setShowNoDataError] = useState(false);

  // Function to hide error messages after a delay
  const hideErrorMessages = useCallback(() => {
    setTimeout(() => {
      setShowSurveyorError(false);
      setShowDateError(false);
      setShowNoDataError(false);
    }, 3000); // Hide after 3 seconds
  }, []);

  // Surveyor Management state
  const [modalOpen, setModalOpen] = useState(false);
  const [editingSurveyor, setEditingSurveyor] = useState(null);

  // Remove mode toggle and use modal overlay instead
  const [surveyorManagementOpen, setSurveyorManagementOpen] = useState(false);

  const handleAddClick = () => {
    setEditingSurveyor(null);
    setModalOpen(true);
  };

  const handleEditClick = (surveyor) => {
    setEditingSurveyor(surveyor);
    setModalOpen(true);
  };

  const handleDeleteClick = async (surveyorId) => {
    if (window.confirm('Are you sure you want to delete this surveyor?')) {
      try {
        const config = await import('../config').then(module => module.default);
        const response = await fetch(`${config.backendHost}/api/surveyors/${surveyorId}`, {
          method: 'DELETE',
        });
        if (!response.ok) throw new Error('Failed to delete');
        setSnackbarMsg('✅ Surveyor deleted successfully!');
        setSnackbarOpen(true);
        loadSurveyors(); // Refresh the list
      } catch (error) {
        console.error('Failed to delete surveyor:', error);
        setSnackbarMsg('Failed to delete surveyor.');
        setSnackbarOpen(true);
      }
    }
  };

  const handleSaveSuccess = () => {
    setModalOpen(false);
    loadSurveyors();
  };

  const handleRowClick = (surveyor) => {
    console.log('Surveyor selected:', surveyor);
  };

  const openSurveyorManagement = () => {
    setSurveyorManagementOpen(true);
  };

  const closeSurveyorManagement = () => {
    setSurveyorManagementOpen(false);
  };


  // Add this memoized filtered surveyors list before the groupedSurveyors memo
const filteredSurveyors = useMemo(() => {
  return surveyors.filter(surveyor => {
    const searchTerm = surveyorSearch.toLowerCase();
    return (
      (surveyor.id && surveyor.id.toLowerCase().includes(searchTerm)) ||
      (surveyor.name && surveyor.name.toLowerCase().includes(searchTerm)) ||
      (surveyor.username && surveyor.username.toLowerCase().includes(searchTerm)) ||
      (surveyor.city && surveyor.city.toLowerCase().includes(searchTerm)) ||
      (surveyor.projectName && surveyor.projectName.toLowerCase().includes(searchTerm))
    );
  });
}, [surveyors, surveyorSearch]);

// Then keep the existing groupedSurveyors memo

const groupedSurveyors = useMemo(() => {
  const groups = {};
  filteredSurveyors.forEach(surveyor => {
    const project = surveyor.projectName || 'Other';
    if (!groups[project]) groups[project] = [];
    groups[project].push(surveyor);
  });
  return groups;
}, [filteredSurveyors]);
  
  // Polling refs (WebSocket removed - using REST API only)
  const statusIntervalRef = useRef(null);
  const liveLocationIntervalRef = useRef(null);
  
  // API helper function
  const apiCall = useCallback(async (endpoint, options = {}) => {
    try {

      const url = `${config.backendHost}${endpoint}`;
      // --- THIS IS THE FIX ---
      //const url = endpoint; // Use a relative path so the proxy can work

      console.log(`Making API call to: ${url}`);
      
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          ...options.headers,
        },
        mode: 'cors',
        ...options,
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      console.log(`API response from ${endpoint}:`, data);
      return data;
    } catch (error) {
      console.error(`API call failed for ${endpoint}:`, error);
      config.handleFetchError(error, endpoint);
      throw error;
    }
  }, []);

  // Load surveyors - exclude admin accounts
  const loadSurveyors = useCallback(async () => {
    try {
      console.log('Loading surveyors...');
      setLoading(true);
      setError('');
      
      let endpoint = '/api/surveyors';
      
      // Use backend filter endpoint if filters are applied
      if (city || project) {
        const params = new URLSearchParams();
        if (city) params.append('city', city);
        if (project) params.append('project', project);
        endpoint = `/api/surveyors/filter?${params.toString()}`;
      }
      
      const data = await apiCall(endpoint);
      
      // Filter out admin accounts - only show actual surveyors
      const filteredSurveyors = data.filter(surveyor => 
        surveyor.id && 
        !surveyor.id.toLowerCase().includes('admin') &&
        !surveyor.username?.toLowerCase().includes('admin')
      );
      
      setSurveyors(filteredSurveyors);
      console.log('Filtered surveyors (excluding admin):', filteredSurveyors);
      
    } catch (err) {
      console.error('Failed to load surveyors:', err);
      
      // Fallback to demo data with dynamic cities and projects
      const cities = dynamicConfig.getDropdownOptions('cities');
      const projects = dynamicConfig.getDropdownOptions('projects');
      
      const mockSurveyors = [
        {
          id: 'SUR009',
          name: 'Kiran',
          city: cities[0] || 'Hyderabad',
          projectName: projects[0] || 'PTMS',
          username: 'kiran_sur'
        },
        {
          id: 'SUR010',
          name: 'Rajesh Kumar',
          city: cities[1] || 'Mumbai',
          projectName: projects[1] || 'Survey',
          username: 'rajesh_kumar'
        },
        {
          id: 'SUR011',
          name: 'Priya Sharma',
          city: cities[2] || 'Delhi',
          projectName: projects[2] || 'Mapping',
          username: 'priya_sharma'
        }
      ];
      
      setSurveyors(mockSurveyors);
      setError('Using demo data - Backend not available');
      
      // Also set mock status immediately
      const mockStatus = {
        'SUR009': 'Online',
        'SUR010': 'Offline',
        'SUR011': 'Offline'
      };
      setStatusMap(mockStatus);
    } finally {
      setLoading(false);
    }
  }, [city, project, apiCall]);

  // Load surveyor status - only for actual surveyors
  const loadStatus = useCallback(async () => {
    try {
      console.log('Fetching surveyor status...');
      const data = await apiCall('/api/surveyors/status');
      
      // Filter status to only include actual surveyors (not admin)
      const filteredStatus = {};
      Object.keys(data).forEach(surveyorId => {
        if (surveyorId.startsWith('SUR') && !surveyorId.toLowerCase().includes('admin')) {
          filteredStatus[surveyorId] = data[surveyorId];
        }
      });
      
      setStatusMap(filteredStatus);
      console.log('Filtered status map:', filteredStatus);
    } catch (err) {
      console.error('Failed to load status:', err);
      
      // Mock status for actual surveyors only
      const mockStatus = {};
      surveyors.forEach(surveyor => {
        if (surveyor.id.startsWith('SUR')) {
          // SUR009 should be online based on your data
          mockStatus[surveyor.id] = surveyor.id === 'SUR009' ? 'Online' : 'Offline';
        }
      });
      setStatusMap(mockStatus);
    }
  }, [surveyors, apiCall]);

  // Load surveyors on component mount
  useEffect(() => {
    loadSurveyors();
  }, [loadSurveyors]);

  // Set up status checking interval
  useEffect(() => {
    if (surveyors.length > 0) {
      loadStatus();
      
      if (statusIntervalRef.current) {
        clearInterval(statusIntervalRef.current);
      }
      
      statusIntervalRef.current = setInterval(() => {
        loadStatus();
      }, config.refreshInterval);
    }
    
    return () => {
      if (statusIntervalRef.current) {
        clearInterval(statusIntervalRef.current);
      }
    };
  }, [surveyors, loadStatus]);

  // ✅ NEW: Polling for latest location every 10 minutes
  const startLocationPolling = useCallback((surveyorId) => {
    console.log('🔄 Starting location polling for surveyor:', surveyorId);
    setConnectionStatus('Polling');
    setError('');
    
    // Clear any existing interval
    if (liveLocationIntervalRef.current) {
      clearInterval(liveLocationIntervalRef.current);
    }
    
    // Function to fetch latest location
    const fetchLatestLocation = async () => {
      try {
        console.log('📍 Fetching latest location for:', surveyorId);
        const data = await apiCall(`/api/location/${surveyorId}/latest`);
        
        if (data && data.latitude && data.longitude) {
          const newLocation = {
            lat: data.latitude,
            lng: data.longitude,
            timestamp: new Date(data.timestamp),
            surveyorId: data.surveyorId
          };
          
          console.log('✅ Latest location received:', newLocation);
          setLiveLocation(newLocation);
          setConnectionStatus('Connected');
          
          // ❌ REMOVED: No trail for live tracking - only moving pin
        } else {
          console.log('⚠️ No location data found for surveyor:', surveyorId);
          setAlertDialogTitle('⚠️ No Data Found');
          setAlertDialogMessage(`No location data found for surveyor: ${surveyorId}`);
          setAlertDialogOpen(true);
        }
      } catch (err) {
        console.error('❌ Error fetching latest location:', err);
        setError('Failed to fetch latest location data');
      }
    };
    
    // Fetch immediately
    fetchLatestLocation();
    
    // ✅ OPTIMIZED: Fetch every 30 seconds to catch mobile app's 10-minute updates quickly
    liveLocationIntervalRef.current = setInterval(fetchLatestLocation, 30000);
    
  }, [apiCall]);

  // ✅ NEW: Stop location polling
  const stopLocationPolling = useCallback(() => {
    console.log('🛑 Stopping location polling');
    
    if (liveLocationIntervalRef.current) {
      clearInterval(liveLocationIntervalRef.current);
      liveLocationIntervalRef.current = null;
    }
    
    setConnectionStatus('Disconnected');
    setLiveLocation(null);
  }, []);

  // ✅ NEW: Toggle live tracking with polling
  const toggleLiveTracking = useCallback(() => {
    // Reset all error states
    setShowSurveyorError(false);
    setShowDateError(false);
    setShowNoDataError(false);

    if (!selectedSurveyor) {
      setAlertDialogTitle('⚠️ Validation Error');
      setAlertDialogMessage('Please select a surveyor first');
      setAlertDialogOpen(true);
      return;
    }

    if (isLiveTracking) {
      // Stop live tracking
      console.log('🛑 Stopping live tracking for:', selectedSurveyor);
      setIsLiveTracking(false);
      stopLocationPolling();
      setError('');
    } else {
      // Start live tracking
      console.log('🟢 Starting live tracking for:', selectedSurveyor);
      setIsLiveTracking(true);
      setError('');
      setLiveLocation(null);
      setHistoricalRoute([]);
      startLocationPolling(selectedSurveyor);
    }
  }, [selectedSurveyor, isLiveTracking, stopLocationPolling, startLocationPolling]);

  // Fetch historical route with proper date formatting
  
    // Fetch Historical

const fetchHistoricalRoute = useCallback(async () => {
  // Reset all error states
  setShowSurveyorError(false);
  setShowDateError(false);
  setShowNoDataError(false);

  if (!selectedSurveyor) {
    setAlertDialogTitle('⚠️ Validation Error');
    setAlertDialogMessage('Please select a surveyor first');
    setAlertDialogOpen(true);
    return;
  }
  
  setLoading(true);
  setError('');
  setHistoricalRoute([]);
  setLiveLocation(null);
  setLiveTrail([]);

  try {
    // Convert dates to UTC and format as ISO strings
    if (!fromDate || !toDate) {
      setAlertDialogTitle('⚠️ Validation Error');
      setAlertDialogMessage('Please select both start and end dates');
      setAlertDialogOpen(true);
      setLoading(false);
      return;
    }
    
    const startUTC = new Date(fromDate.getTime() - (fromDate.getTimezoneOffset() * 60000));
    const endUTC = new Date(toDate.getTime() - (toDate.getTimezoneOffset() * 60000));
    
    const startFormatted = startUTC.toISOString();
    const endFormatted = endUTC.toISOString();
    
    console.log('Fetching historical data for:', {
      surveyor: selectedSurveyor,
      start: startFormatted,
      end: endFormatted
    });
    
    const url = `/api/location/${selectedSurveyor}/track?start=${encodeURIComponent(startFormatted)}&end=${encodeURIComponent(endFormatted)}`;
    console.log('API URL:', url);
    
    const response = await apiCall(url);
    console.log('Raw API response:', response);

    // Handle both response formats:
    // 1. Direct array (localhost) 
    // 2. Spring Data REST paginated { content: [...] } (production)
    const data = Array.isArray(response) ? response : response?.content || [];
    
    console.log('Processed data:', data);
    
    if (data.length > 0) {
      const routePoints = data.map(point => ({
        lat: point.latitude,
        lng: point.longitude,
        timestamp: new Date(point.timestamp) // Convert to Date object
      }));
      
      setHistoricalRoute(routePoints);
      console.log('Route points processed:', routePoints.length);
      
      // ✅ IMPROVED: Enhanced Google Maps URL with route info and distance
      if (routePoints.length > 1) {
        const start = routePoints[0];
        const end = routePoints[routePoints.length - 1];
        
        // Calculate approximate distance for display
        const distance = calculateDistance(start.lat, start.lng, end.lat, end.lng);
        console.log(`📏 Calculated route distance: ${distance.toFixed(2)} km`);
        
        // Enhanced Google Maps URL with driving directions
        const googleMapsUrl = `https://www.google.com/maps/dir/${start.lat},${start.lng}/${end.lat},${end.lng}/?travelmode=driving&dir_action=navigate`;
        window.open(googleMapsUrl, '_blank', 'noopener,noreferrer');
        
        // Show user-friendly message with route info
        const routeInfo = `📍 Route: ${routePoints.length} points • ~${distance.toFixed(1)} km • ${formatTime(routePoints[0].timestamp)} to ${formatTime(end.timestamp)}`;
        console.log(routeInfo);
      }
    } else {
      setError('');
      setAlertDialogTitle('⚠️ No Data Found');
      setAlertDialogMessage('No location data found for the selected time range');
      setAlertDialogOpen(true);
    }
  } catch (error) {
    console.error('Failed to fetch track data:', error);
    
    // Fallback demo data with proper timestamps
    const demoRoute = Array.from({ length: 5 }, (_, i) => {
      const timeOffset = (toDate - fromDate) * (i / 4);
      return {
        lat: 17.4010007 + (Math.random() - 0.5) * 0.01,
        lng: 78.5643879 + (Math.random() - 0.5) * 0.01,
        timestamp: new Date(fromDate.getTime() + timeOffset)
      };
    });
    
    setHistoricalRoute(demoRoute);
    setError('Demo mode: Showing simulated historical route');
  } finally {
    setLoading(false);
  }
}, [selectedSurveyor, fromDate, toDate, apiCall]);







  // Get map center and positions for bounds
  const getMapData = () => {
    if (liveLocation) {
      return {
        center: [liveLocation.lat, liveLocation.lng],
        positions: [[liveLocation.lat, liveLocation.lng]] // ✅ FIXED: Only current position, no trail
      };
    } else if (historicalRoute.length > 0) {
      const routePositions = historicalRoute.map(point => [point.lat, point.lng]);
      return {
        center: [historicalRoute[0].lat, historicalRoute[0].lng],
        positions: routePositions
      };
    }
    
    // Default center from dynamic configuration
    const mapConfig = dynamicConfig.getMapConfig();
    return {
      center: mapConfig.defaultCenter || [17.4010007, 78.5643879], // Hyderabad default
      positions: []
    };
  };

  const { center, positions } = getMapData();

  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMsg, setSnackbarMsg] = useState('');
  const [warningOpen, setWarningOpen] = useState(false);
  
  // Alert Dialog state for validation messages
  const [alertDialogOpen, setAlertDialogOpen] = useState(false);
  const [alertDialogMessage, setAlertDialogMessage] = useState('');
  const [alertDialogTitle, setAlertDialogTitle] = useState('');

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
      background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
      fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
    }}>
      {/* Header Section (Top Bar) */}
      <div style={{
        background: 'linear-gradient(135deg, #1e40af 0%, #3b82f6 100%)',
        color: '#fff',
        padding: '1rem 2rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        boxShadow: '0 8px 32px rgba(30, 64, 175, 0.15)',
        position: 'relative',
        minHeight: '80px',
        flexShrink: 0,
      }}>
        {/* Left: Socket and Total Surveyors */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', position: 'absolute', left: '2rem', top: 0, bottom: 0, height: '100%' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600, fontSize: '1rem' }}>
            <span>{connectionStatus === 'Connected' || connectionStatus === 'Polling' ? '🟢' : '🔴'}</span>
            <span>Status: {connectionStatus}</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600, fontSize: '1rem' }}>
            <span>📊</span>
            <span>Total Surveyors: {surveyors.length}</span>
          </div>
        </div>
        {/* Center: Dashboard Title */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', position: 'absolute', left: 0, right: 0, top: 0, bottom: 0, margin: 'auto', justifyContent: 'center', height: '100%', pointerEvents: 'none' }}>
          {/* <span style={{ fontSize: '2rem', lineHeight: 1 }}>📍</span> */}
          <span style={{ fontWeight: 700, fontSize: '2rem', letterSpacing: '0.5px' }}>
            Live Tracking Dashboard
          </span>
        </div>
        {/* Right: Surveyors Button */}
        <div style={{ position: 'absolute', right: '2rem', top: 0, bottom: 0, height: '100%', display: 'flex', alignItems: 'center' }}>
          <button
            onClick={openSurveyorManagement}
            style={{
              background: 'linear-gradient(90deg, #10b981 0%, #3b82f6 100%)',
              color: '#fff',
              border: '2px solid #fff',
              borderRadius: '10px',
              padding: '0.5rem 1.3rem',
              fontWeight: '700',
              fontSize: '1.05rem',
              cursor: 'pointer',
              boxShadow: '0 0 12px 2px rgba(16,185,129,0.25), 0 4px 16px rgba(59,130,246,0.18)',
              transition: 'all 0.3s cubic-bezier(.4,2,.3,1)',
              marginLeft: 'auto',
              letterSpacing: '0.5px',
            }}
          >
            🧑‍💼 Surveyors
          </button>
        </div>
      </div>

      {/* Filter & Control Panel (Attached to Map) */}
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '1rem',
        padding: '0.75rem 2rem',
        background: '#ffffff',
        borderBottom: '1px solid #e2e8f0',
        boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
        alignItems: 'center',
        justifyContent: 'left',
        zIndex: 200,
        flexShrink: 0,
      }}>
        {/* Filter by City */}
        <div style={{ minWidth: '150px', background: 'linear-gradient(135deg, #e0f7fa 0%, #f0f4ff 100%)', borderRadius: '10px', boxShadow: '0 4px 16px rgba(6,182,212,0.10)', border: '1.5px solid #06b6d4', transition: 'box-shadow 0.2s, border 0.2s' }}>
          <select
            value={city}
            onChange={e => setCity(e.target.value)}
            style={{
              padding: '0.5rem 1rem',
              borderRadius: '8px',
              border: '1.5px solid #e5e7eb',
              width: '100%',
              fontSize: '1.05rem',
              background: '#ffffff',
              cursor: 'pointer',
              fontWeight: 500,
            }}
          >
            <option value="">Filter by City</option>
            {dynamicConfig.getDropdownOptions('cities').map(cityOption => (
              <option key={cityOption} value={cityOption}>
                {cityOption}
              </option>
            ))}
          </select>
        </div>
        {/* Filter by Project */}
        <div style={{ minWidth: '150px', background: 'linear-gradient(135deg, #e0ffe0 0%, #f0f4ff 100%)', borderRadius: '10px', boxShadow: '0 4px 16px rgba(16,185,129,0.10)', border: '1.5px solid #10b981', transition: 'box-shadow 0.2s, border 0.2s' }}>
          <select
            value={project}
            onChange={e => setProject(e.target.value)}
            style={{
              padding: '0.5rem 1rem',
              borderRadius: '8px',
              border: '1.5px solid #e5e7eb',
              width: '100%',
              fontSize: '1.05rem',
              background: '#ffffff',
              cursor: 'pointer',
              fontWeight: 500,
            }}
          >
            <option value="">Filter by Project</option>
            {dynamicConfig.getDropdownOptions('projects').map(projectOption => (
              <option key={projectOption} value={projectOption}>
                {projectOption}
              </option>
            ))}
          </select>
        </div>
        {/* Tracking Surveyor */}
        <div style={{ minWidth: '220px', background: 'linear-gradient(135deg, #f0f4ff 0%, #e0f7fa 100%)', borderRadius: '10px', boxShadow: '0 4px 16px rgba(59,130,246,0.10)', border: '1.5px solid #3b82f6', transition: 'box-shadow 0.2s, border 0.2s' }}>
          <select
            value={selectedSurveyor}
            onChange={e => {
              const value = e.target.value;
              setSelectedSurveyor(value);
              // Hide surveyor error when user selects a surveyor
              if (value) {
                setShowSurveyorError(false);
              }
              const found = surveyors.find(s => s.id === value);
              if (found) {
                setCity(found.city || '');
                setProject(found.projectName || '');
              }
            }}
            style={{
              padding: '0.5rem 1rem',
              borderRadius: '8px',
              border: '1.5px solid #e5e7eb',
              width: '100%',
              fontSize: '1.05rem',
              background: '#ffffff',
              cursor: 'pointer',
              fontWeight: 500,
            }}
          >
            <option value="">Select a Surveyor</option>
            {surveyors.map(surveyor => (
              <option key={surveyor.id} value={surveyor.id}>
                {surveyor.name} ({surveyor.id}) {statusMap[surveyor.id] === 'Online' ? '🟢' : '🔴'}
              </option>
            ))}
          </select>
        </div>
        {/* From Date */}
        <div style={{ minWidth: '140px', background: 'linear-gradient(135deg, #f3e8ff 0%, #e0f2fe 100%)', borderRadius: '10px', boxShadow: '0 4px 16px rgba(99,102,241,0.10)', border: '1.5px solid #6366f1', transition: 'box-shadow 0.2s, border 0.2s' }}>
          <DatePicker
            selected={fromDate}
            onChange={date => setFromDate(date)}
            showTimeSelect
            dateFormat="MMM d, yyyy h:mm aa"
            placeholderText="From Date & Time"
            className="date-picker"
            isClearable
            popperPlacement="bottom-end"
          />
        </div>
        {/* To Date */}
        <div style={{ minWidth: '140px', background: 'linear-gradient(135deg, #f3e8ff 0%, #e0f2fe 100%)', borderRadius: '10px', boxShadow: '0 4px 16px rgba(99,102,241,0.10)', border: '1.5px solid #6366f1', transition: 'box-shadow 0.2s, border 0.2s' }}>
          <DatePicker
            selected={toDate}
            onChange={date => setToDate(date)}
            showTimeSelect
            dateFormat="MMM d, yyyy h:mm aa"
            placeholderText="To Date & Time"
            className="date-picker"
            isClearable
            popperPlacement="bottom-end"
          />
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '1rem' }}>
          <button
            onClick={toggleLiveTracking}
            style={{
              background: isLiveTracking ? '#ef4444' : '#10b981',
              color: '#fff',
              border: 'none',
              borderRadius: '9px',
              padding: '0.55rem 1.5rem',
              fontWeight: 700,
              fontSize: '1.13rem',
              cursor: 'pointer',
              boxShadow: isLiveTracking ? '0 2px 8px rgba(239,68,68,0.18)' : '0 2px 8px rgba(16,185,129,0.13)',
              transition: 'background 0.2s',
            }}
          >
            {isLiveTracking ? 'Stop Live' : 'Live'}
          </button>
          <button
            onClick={fetchHistoricalRoute}
            style={{
              background: '#6366f1',
              color: '#fff',
              border: 'none',
              borderRadius: '9px',
              padding: '0.55rem 1.5rem',
              fontWeight: 700,
              fontSize: '1.13rem',
              cursor: 'pointer',
              boxShadow: '0 2px 8px rgba(99,102,241,0.13)',
              transition: 'background 0.2s',
            }}
          >
            Historical
          </button>
          {/* Alert messages displayed at top center of screen */}
          <div style={{ position: 'fixed', top: '20px', left: '50%', transform: 'translateX(-50%)', zIndex: 2000, width: '100%', display: 'flex', justifyContent: 'center' }}>
            {showSurveyorError && (
              <div style={{
                position: 'relative',
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                padding: '1rem 1.5rem',
                borderRadius: '8px',
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                color: '#dc2626',
                fontWeight: 500,
                fontSize: '0.9rem',
                border: '1px solid #dc2626',
                marginBottom: '10px',
                maxWidth: '400px',
                textAlign: 'center',
              }}>
                ⚠️ Please select a surveyor first
              </div>
            )}
            {showDateError && (
              <div style={{
                position: 'relative',
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                padding: '1rem 1.5rem',
                borderRadius: '8px',
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                color: '#dc2626',
                fontWeight: 500,
                fontSize: '0.9rem',
                border: '1px solid #dc2626',
                marginBottom: '10px',
                maxWidth: '400px',
                textAlign: 'center',
              }}>
                ⚠️ Please select both start and end dates
              </div>
            )}
            {showNoDataError && (
              <div style={{
                position: 'relative',
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                padding: '1rem 1.5rem',
                borderRadius: '8px',
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                color: '#dc2626',
                fontWeight: 500,
                fontSize: '0.9rem',
                border: '1px solid #dc2626',
                marginBottom: '10px',
                maxWidth: '400px',
                textAlign: 'center',
              }}>
                ⚠️ No location data found for surveyor: {selectedSurveyor}
              </div>
            )}
          </div>
        </div>
      </div>
      {/* Error Display - Floating on Left Side */}
      {error && error !== 'No location data found for the selected time range' && (
        <div style={{
          position: 'absolute',
          bottom: '20px',
          left: '20px',
          background: error.includes('Demo') ? 'rgba(59, 130, 246, 0.1)' : 'rgba(239, 68, 68, 0.1)',
          padding: '1rem',
          border: `1px solid ${error.includes('Demo') ? 'rgba(59, 130, 246, 0.2)' : 'rgba(239, 68, 68, 0.2)'}`,
          color: error.includes('Demo') ? '#1d4ed8' : '#dc2626',
          fontSize: '0.9rem',
          borderRadius: 8,
          zIndex: 1000,
          maxWidth: '300px'
        }}>
          {error.includes('Demo') ? '💡' : '⚠️'} {error}
        </div>
      )}

      {/* 🗺️ Main Map Area (Leaflet Map) */}
      <div style={{
        position: 'relative',
        flex: 1, // This makes the map area fill the remaining vertical space
        width: '100%',
        overflow: 'hidden',
        zIndex: 1,
      }}>
        <MapContainer
          center={center}
          zoom={13}
          style={{ height: '100%', width: '100%' }}
          zoomControl={true}
          attributionControl={true}
        >
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          />

          {/* 🟢 Live Location Marker */}
          {liveLocation && (
            <Marker position={[liveLocation.lat, liveLocation.lng]} icon={liveIcon}>
              <Popup>
                <div style={{ textAlign: 'center' }}>
                  <h3 style={{ margin: '0 0 0.5rem 0', color: '#596690' }}>🟢 Live Location</h3>
                  <p style={{ margin: '0.25em 0', fontSize: '0.9rem' }}>
                    <strong>Surveyor:</strong> {selectedSurveyor}
                  </p>
                  <p style={{ margin: '0.25em 0', fontSize: '0.9rem' }}>
                    <strong>Coordinates:</strong> {liveLocation.lat.toFixed(6)}, {liveLocation.lng.toFixed(6)}
                  </p>
                  <p style={{ margin: '0.25em 0', fontSize: '0.9rem' }}>
                    <strong>Time:</strong> {liveLocation.timestamp ? formatTime(liveLocation.timestamp) : 'Unknown'}
                  </p>
                </div>
              </Popup>
            </Marker>
          )}

          {/* ❌ REMOVED: No lines for live tracking - only moving pin */}

          {/* 🔵 Blue Route - Full Route Line (Historical) */}
          {historicalRoute.length > 1&& (
            <OSRMRoute coordinates={historicalRoute.map(point => [point.lat, point.lng])} color="#3b82f6" />
          )}

          {/* 🟢 Start Marker - First Point */}
          {historicalRoute.length > 0&& (
            <Marker position={[historicalRoute[0].lat, historicalRoute[0].lng]} icon={startIcon}>
              <Popup>
                <div style={{ textAlign: 'center' }}>
                  <h3 style={{ margin: '0 0 0.5rem 0', color: '#10b981' }}>🟢 Start Point</h3>
                  <p style={{ margin: '0.25em 0', fontSize: '0.9rem' }}>
                    <strong>Time:</strong> {new Date(historicalRoute[0].timestamp).toLocaleString()}
                  </p>
                </div>
              </Popup>
            </Marker>
          )}

          {/* 🔴 End Marker - Latest Point */}
          {historicalRoute.length > 0&& (
            <Marker position={[historicalRoute[historicalRoute.length - 1].lat, historicalRoute[historicalRoute.length - 1].lng]} icon={endIcon}>
              <Popup>
                <div style={{ textAlign: 'center' }}>
                  <h3 style={{ margin: '0 0 0.5rem 0', color: '#dc2626' }}>🔴 End Point</h3>
                  <p style={{ margin: '0.25em 0', fontSize: '0.9rem' }}>
                    <strong>Time:</strong> {new Date(historicalRoute[historicalRoute.length - 1].timestamp).toLocaleString()}
                  </p>
                </div>
              </Popup>
            </Marker>
          )}

          {/* Auto-fit bounds */}
          <MapBounds positions={positions} />

          {/* 💡 Ready to Track Overlay */}
          {!selectedSurveyor && (
            <div style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              background: 'rgba(255, 255, 255, 0.95)',
              padding: '2rem',
              borderRadius: '16px',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.15)',
              textAlign: 'center',
              zIndex: 5
            }}>
              <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🗺️</div>
              <h2 style={{ margin: '0 0 0.5rem 0', color: '#374151', fontWeight: 700 }}>
                Ready to Track
              </h2>
              <p style={{ margin: '0 0 0 0', color: '#64748b', fontSize: '1rem' }}>
                Select a surveyor and choose tracking mode
              </p>
            </div>
          )}
        </MapContainer>
        {/*  Surveyor Details Panel (Floating Right Panel on Map) - now absolutely positioned in map area */}
        {selectedSurveyor && (() => {
          const selectedSurveyorData = surveyors.find(s => s.id === selectedSurveyor);
          const name = selectedSurveyorData?.name || selectedSurveyor;
          const isOnline = statusMap[selectedSurveyor] === 'Online';
          return (
            <div style={{
              position: 'absolute',
              top: '24px',
              right: '24px',
              background: 'rgba(255,255,255,0.35)',
              borderRadius: '14px',
              padding: '1.2rem 1.5rem',
              border: '1.5px solid rgba(59,130,246,0.18)',
              boxShadow: '0 4px 24px rgba(30,64,175,0.10)',
              minWidth: '260px',
              zIndex: 1000, // CHANGED: Increased z-index to ensure this panel is on top of the map
              backdropFilter: 'blur(8px)',
              color: '#222',
              fontWeight: 500,
              fontSize: '1.05rem',
              pointerEvents: 'auto'
            }}>
              <h3 style={{ margin: '0 0 1rem 0', color: '#374151', fontWeight: 700 }}>
                Surveyor Details
              </h3>
              <div style={{ marginBottom: '0.5rem' }}>
                <strong>Name / ID:</strong> {name} / {selectedSurveyor}
              </div>
              <div style={{ marginBottom: '0.5rem' }}>
                <strong>Live Status:</strong> {isOnline ? '🟢 Online' : '🔴 Offline'}
              </div>
              <div style={{ marginBottom: '0.5rem' }}>
                <strong>Project:</strong> {selectedSurveyorData?.projectName || 'N/A'}
              </div>
              <div style={{ marginBottom: '0.5rem' }}>
                <strong>City:</strong> {selectedSurveyorData?.city || 'N/A'}
              </div>
              {liveLocation && (
                <>
                  <div style={{ marginBottom: '0.5rem' }}>
                    <strong>Last Update:</strong> 0 secs ago
                  </div>
                  <div style={{ marginBottom: '0.5rem' }}>
                    <strong>Coordinates:</strong> {liveLocation.lat.toFixed(5)}, {liveLocation.lng.toFixed(5)}
                  </div>
                </>
              )}
            </div>
          );
        })()}
      </div>
      {/* Custom CSS */}
      <style>{`
        /* --- REPLACED: Updated and comprehensive styles for the DatePicker --- */
        .date-picker { width: 100% !important; box-sizing: border-box; padding: 0.5rem 1rem !important; border-radius: 8px !important; border: 1.5px solid #e5e7eb !important; font-size: 1.05rem !important; font-weight: 500 !important; background: #ffffff !important; cursor: pointer; }
        .date-picker::placeholder { color: #6b7280; }
        .date-picker:focus { border-color: #3b82f6 !important; outline: none !important; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important; }
        .react-datepicker { font-family: 'Inter', sans-serif !important; border: 1px solid #e2e8f0 !important; border-radius: 12px !important; background-color: #ffffff !important; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1) !important; padding: 8px; }
        .react-datepicker-popper { z-index: 300 !important; }
        .react-datepicker__triangle { display: none !important; }
        .react-datepicker__header { background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%) !important; border-radius: 8px 8px 0 0 !important; padding-top: 12px !important; padding-bottom: 4px !important; border-bottom: none !important; }
        .react-datepicker__current-month { color: #ffffff !important; font-weight: 600 !important; font-size: 1.1rem !important; }
        .react-datepicker__navigation { top: 12px !important; }
        .react-datepicker__navigation-icon::before { border-color: #ffffff !important; border-width: 2px 2px 0 0 !important; height: 8px !important; width: 8px !important; }
        .react-datepicker__navigation:hover .react-datepicker__navigation-icon::before { border-color: #c7d2fe !important; }
        .react-datepicker__day-name { color: #475569 !important; font-weight: 600 !important; margin: 0.25rem; }
        .react-datepicker__day { color: #334155 !important; margin: 0.25rem; width: 2.2rem; height: 2.2rem; line-height: 2.2rem; border-radius: 50% !important; transition: all 0.2s ease-in-out; }
        .react-datepicker__day:hover { background-color: #e0f2fe !important; color: #0c4a6e !important; }
        .react-datepicker__day--selected { background-color: #3b82f6 !important; color: #ffffff !important; font-weight: 700 !important; }
        .react-datepicker__day--keyboard-selected { background-color: #dbeafe !important; color: #1e40af !important; }
        .react-datepicker__day--today { font-weight: 700 !important; border: 1px solid #3b82f6; }
        .react-datepicker__day--outside-month { opacity: 0.4; }
        .react-datepicker__time-container { border-left: 1px solid #e2e8f0 !important; }
        .react-datepicker__time-box { width: 100% !important; }
        .react-datepicker__time-list-item { transition: all 0.2s ease-in-out; border-radius: 6px !important; margin: 2px 4px !important; padding: 5px 10px !important; }
        .react-datepicker__time-list-item:hover { background-color: #f1f5f9 !important; }
        .react-datepicker__time-list-item--selected { background-color: #3b82f6 !important; color: white !important; font-weight: bold !important; }
        .react-datepicker__time-list-item--selected:hover { background-color: #2563eb !important; }
      `}</style>
      <SurveyorFormModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSave={handleSaveSuccess}
        surveyor={editingSurveyor}
      />
      {surveyorManagementOpen && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0,0.5)',
          zIndex: 1000,
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: '2rem',
          backdropFilter: 'blur(4px)'
        }}>
          <div style={{
            background: 'linear-gradient(135deg, #ffffff0, #f8fafc 100%)',
            borderRadius: '20px',
            padding: '0',
            maxWidth: '95%',
            maxHeight: '90%',
            overflow: 'hidden',
            boxShadow: '0 25px 50px rgba(0,0,0,0.25)',
            position: 'relative',
            border: '1px solid rgba(59, 130, 2461)'
          }}>
            {/* Header */}
            <div style={{
              background: 'linear-gradient(135deg, #1e40af 0%, #3b82f6 100%)',
              color: '#fff',
              padding: '1.5rem 2rem',
              borderTopLeftRadius: '20px',
              borderTopRightRadius: '20px',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              borderBottom: '1px solid rgba(255, 255, 255,0.1)'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                {/* Search Bar replaces the title */}
                <div style={{ display: 'flex', alignItems: 'center', background: 'rgba(255,255,255,0.15)', borderRadius: '8px', padding: '0.2rem 0.8rem', boxShadow: '0 2px 8px rgba(30,64,175,0.08)' }}>
                  <span style={{ fontSize: '1.5rem', color: '#fff', marginRight: '0.5rem', filter: 'drop-shadow(0 0 2px #000)' }}>🔍</span>
                  <input
                    type="text"
                    placeholder="Search surveyors by name, ID, username, city, or project..."
                    value={surveyorSearch}
                    onChange={e => setSurveyorSearch(e.target.value)}
                    style={{
                      background: 'transparent',
                      border: 'none',
                      outline: 'none',
                      color: '#fff',
                      fontSize: '1.2rem',
                      width: '260px',
                      fontWeight: 500,
                    }}
                  />
                  <style>{`
                    input[type=\"text\"]::placeholder {
                      color: rgba(255,255,255,0.85);
                      opacity: 1;
                    }
                  `}</style>
                </div>
              </div>
              <div style={{ display: 'flex', gap: '1em', alignItems: 'center' }}>
                <button
                  onClick={handleAddClick}
                  style={{
                    background: 'linear-gradient(135deg, #10b981 0%, #1e40af 100%)',
                    color: '#fff', 
                    border: 'none',
                    borderRadius: '12px',
                    padding: '0.75rem 1.5rem',
                    fontWeight: '600',
                    fontSize: '0.9rem',
                    cursor: 'pointer',
                    boxShadow: '0 8px 25px rgba(16, 185, 129, 0.3)',
                    transition: 'all 0.3s ease',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}
                  onMouseOver={e => e.target.style.transform = 'translateY(-2px)'}
                  onMouseOut={e => e.target.style.transform = 'translateY(0)'}
                >
                  <span style={{ fontSize: '1.1rem' }}>➕</span>
                  ADD SURVEYOR
                </button>
                <button
                  onClick={closeSurveyorManagement}
                  style={{
                    background: 'rgba(255, 255, 255, 0.1)',
                    border: '1px solid rgba(255, 255, 255, 0.1)',
                    fontSize: '1.2rem',
                    cursor: 'pointer',
                    color: '#fff',
                    padding: '0.5rem',
                    borderRadius: '50%',
                    width: '40px',
                    height: '40px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    transition: 'all 0.3s ease'
                  }}
                  onMouseOver={e => e.target.style.backgroundColor = 'rgba(255, 255, 255, 0.2)'}
                  onMouseOut={e => e.target.style.backgroundColor = 'rgba(255, 255, 255, 0.1)'}
                >
                  ✕
                </button>
              </div>
            </div>
            {/* Content */}
            <div style={{
              padding: '2rem',
              maxHeight: 'calc(90vh - 120px)',
              overflow: 'auto'
            }}>
              {/* Stats Cards */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '1rem',
                marginBottom: '2rem'
              }}>
                <div style={{
                  background: 'linear-gradient(135deg, #3b82f60, #60a5fa 100%)',
                  color: '#fff',
                  padding: '1.5rem',
                  borderRadius: '12px',
                  textAlign: 'center',
                  boxShadow: '0 4px 12px rgba(59, 130, 246, 0.2)'
                }}>
                  <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>👤</div>
                  <div style={{ fontSize: '1.5em', fontWeight: '700' }}>{surveyors.length}</div>
                  <div style={{ fontSize: '0.9rem', opacity: 0.9 }}>Total Surveyors</div>
                </div>
                <div style={{
                  background: 'linear-gradient(135deg, #10810, #34d399 100%)',
                  color: '#fff',
                  padding: '1.5rem',
                  borderRadius: '12px',
                  textAlign: 'center',
                  boxShadow: '0 4px 12px rgba(16, 185, 129, 0.2)'
                }}>
                  <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>🟢</div>
                  <div style={{ fontSize: '1.5em', fontWeight: '700' }}>{surveyors.filter(s => statusMap[s.id] === 'Online').length}</div>
                  <div style={{ fontSize: '0.9rem', opacity: 0.9 }}>Online Surveyors</div>
                </div>
                <div style={{
                  background: 'linear-gradient(135deg, #f5900, #fbbf24 100%)',
                  color: '#fff',
                  padding: '1.5rem',
                  borderRadius: '12px',
                  textAlign: 'center',
                  boxShadow: '0 4px 12px rgba(245, 144, 0, 0.2)'
                }}>
                  <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>🏙️</div>
                  <div style={{ fontSize: '1.5em', fontWeight: '700' }}>{new Set(surveyors.map(s => s.city).filter(Boolean)).size}</div>
                  <div style={{ fontSize: '0.9rem', opacity: 0.9 }}>Unique Cities</div>
                </div>
              </div>
              {/* Surveyor Table */}
              <div style={{
                background: '#fff',
                borderRadius: '12px',
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
                overflow: 'hidden'
              }}>
                <SurveyorTable
                  surveyors={filteredSurveyors}
                  onEdit={handleEditClick}
                  onDelete={handleDeleteClick}
                  onRowClick={handleRowClick}
                />
              </div>
            </div>
          </div>
        </div>
      )}
      <Snackbar
        open={snackbarOpen}
        autoHideDuration={3000}
        onClose={() => setSnackbarOpen(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={() => setSnackbarOpen(false)} severity={snackbarMsg.startsWith('✅') ? 'success' : 'error'} sx={{ width: '100%' }}>
          {snackbarMsg}
        </Alert>
      </Snackbar>
      <Snackbar
        open={warningOpen}
        autoHideDuration={3000}
        onClose={() => setWarningOpen(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={() => setWarningOpen(false)} severity="warning" sx={{ width: '100%' }}>
          ⚠️ No location data found for the selected time range
        </Alert>
      </Snackbar>
      {/* Top Alert Dialog for Validation Messages */}
      <Dialog
        open={alertDialogOpen}
        onClose={() => setAlertDialogOpen(false)}
        maxWidth="xs"
        PaperProps={{
          style: {
            position: 'fixed',
            top: '20px',
            left: '50%',
            transform: 'translateX(-50%)',
            margin: 0,
            borderRadius: '12px',
            padding: '1rem 1.5rem',
            textAlign: 'center',
            boxShadow: '0 8px 25px rgba(0,0,0,0.15)',
            minWidth: '320px',
            maxWidth: '450px'
          }
        }}
        BackdropProps={{
          style: {
            backgroundColor: 'transparent'
          }
        }}
      >
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '1rem'
        }}>
          <div style={{
            fontSize: '1.1rem',
            color: '#dc2626',
            fontWeight: 600,
            flex: 1
          }}>
            {alertDialogMessage}
          </div>
          <Button
            onClick={() => setAlertDialogOpen(false)}
            variant="contained"
            size="small"
            sx={{
              background: '#3b82f6',
              color: '#fff',
              borderRadius: '8px',
              padding: '0.4rem 1rem',
              fontWeight: 600,
              fontSize: '0.9rem',
              textTransform: 'none',
              minWidth: 'auto',
              boxShadow: '0 2px 8px rgba(59, 130, 246, 0.3)',
              '&:hover': {
                background: '#2563eb',
                boxShadow: '0 4px 12px rgba(59, 130, 246, 0.4)'
              }
            }}
          >
            OK
          </Button>
        </div>
      </Dialog>
    </div>
  );
};

export default LiveTrackingPage;
