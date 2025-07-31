import React, { useState } from 'react';
import { 
  TextField, Button, Paper, Typography, Box, 
  InputAdornment, IconButton, Alert, CircularProgress 
} from '@mui/material';
import { Visibility, VisibilityOff, Person, Lock, Place } from '@mui/icons-material';

const Login = ({ onLogin }) => {
  const [credentials, setCredentials] = useState({ username: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setCredentials(prev => ({ ...prev, [name]: value }));
    if (error) setError('');
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!credentials.username || !credentials.password) {
      setError('Please enter both username and password');
      return;
    }

    setLoading(true);
    
    // Static login validation
    if (credentials.username === 'admin' && credentials.password === 'admin123') {
      onLogin({ username: 'admin', admin: true });
    } else if (credentials.username === 'user' && credentials.password === 'user123') {
      onLogin({ username: 'user', admin: false });
    } else {
      setError('Invalid credentials');
    }
    
    setLoading(false);
  };

  return (
    <Box sx={{
      height: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #4a6fa5 0%, #166088 100%)',
      p: 2
    }}>
      <Paper sx={{
        width: '100%',
        maxWidth: 380,
        p: 3,
        borderRadius: 3,
        bgcolor: 'background.paper',
        boxShadow: '0 10px 30px rgba(0,0,0,0.15)',
        position: 'relative',
        overflow: 'hidden',
        '&::before': {
          content: '""',
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          height: 4,
          background: 'linear-gradient(90deg, #4a6fa5, #166088)'
        }
      }}>
        <Box textAlign="center" mb={3}>
          <Place sx={{ 
            fontSize: 50, 
            color: '#166088',
            mb: 1,
            filter: 'drop-shadow(0 2px 4px rgba(22,96,136,0.3))'
          }} />
          <Typography variant="h5" sx={{ 
            fontWeight: 700,
            color: '#166088',
            mb: 0.5
          }}>
            Surveyor Track
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Surveyor Tracking Management System
          </Typography>
        </Box>
        
        {error && (
          <Alert severity="error" sx={{ mb: 2, borderRadius: 1 }}>
            {error}
          </Alert>
        )}
        
        <Box component="form" onSubmit={handleSubmit}>
          <TextField
            fullWidth
            size="small"
            label="Username"
            name="username"
            value={credentials.username}
            onChange={handleChange}
            disabled={loading}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Person fontSize="small" sx={{ color: '#4a6fa5' }} />
                </InputAdornment>
              ),
            }}
            sx={{ mb: 2 }}
          />
          
          <TextField
            fullWidth
            size="small"
            label="Password"
            name="password"
            type={showPassword ? "text" : "password"}
            value={credentials.password}
            onChange={handleChange}
            disabled={loading}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Lock fontSize="small" sx={{ color: '#4a6fa5' }} />
                </InputAdornment>
              ),
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton 
                    onClick={() => setShowPassword(!showPassword)}
                    edge="end"
                    size="small"
                    sx={{ color: '#4a6fa5' }}
                  >
                    {showPassword ? <VisibilityOff fontSize="small" /> : <Visibility fontSize="small" />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
          
          <Button
            fullWidth
            type="submit"
            variant="contained"
            size="medium"
            disabled={loading}
            sx={{
              mt: 3,
              py: 1,
              borderRadius: 1,
              bgcolor: '#166088',
              '&:hover': {
                bgcolor: '#4a6fa5',
                transform: 'translateY(-1px)'
              },
              transition: 'all 0.2s ease'
            }}
          >
            {loading ? <CircularProgress size={22} /> : 'Sign In'}
          </Button>
        </Box>

        <Typography variant="caption" color="text.secondary" display="block" textAlign="center" mt={3}>
          © NeoGeoInfo Technologies Private Limited
        </Typography>
      </Paper>
    </Box>
  );
};

export default Login;
