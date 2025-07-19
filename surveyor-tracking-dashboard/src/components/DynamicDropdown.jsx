import React, { useState, useEffect, useMemo } from 'react';
import { 
  FormControl, 
  InputLabel, 
  Select, 
  MenuItem, 
  TextField,
  Box,
  Chip,
  OutlinedInput,
  Checkbox,
  ListItemText,
  Typography,
  IconButton,
  Tooltip
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';
import AddIcon from '@mui/icons-material/Add';
import config from '../config';

const DynamicDropdown = ({
  type = 'cities',
  label = 'Select Option',
  value = '',
  onChange,
  multiple = false,
  searchable = true,
  addable = false,
  placeholder = 'Select...',
  disabled = false,
  required = false,
  error = false,
  helperText = '',
  style = {},
  variant = 'outlined',
  size = 'medium',
  fullWidth = true,
  customOptions = null,
  onAddOption = null,
  showCount = false,
  groupBy = null,
  renderOption = null,
  maxHeight = 300
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [localOptions, setLocalOptions] = useState([]);

  // Get options from config or use custom options
  const options = useMemo(() => {
    if (customOptions) {
      return customOptions;
    }
    return config.getDropdownOptions(type) || [];
  }, [type, customOptions]);

  // Filter options based on search term
  const filteredOptions = useMemo(() => {
    if (!searchTerm) return options;
    
    return options.filter(option => {
      const optionText = typeof option === 'string' ? option : option.label || option.value || option;
      return optionText.toLowerCase().includes(searchTerm.toLowerCase());
    });
  }, [options, searchTerm]);

  // Group options if groupBy is specified
  const groupedOptions = useMemo(() => {
    if (!groupBy) return filteredOptions;
    
    const groups = {};
    filteredOptions.forEach(option => {
      const group = typeof option === 'object' ? option[groupBy] : 'Other';
      if (!groups[group]) groups[group] = [];
      groups[group].push(option);
    });
    return groups;
  }, [filteredOptions, groupBy]);

  // Handle option addition
  const handleAddOption = () => {
    if (searchTerm && onAddOption) {
      onAddOption(searchTerm);
      setSearchTerm('');
    }
  };

  // Render option based on type
  const renderOptionItem = (option, index) => {
    if (renderOption) {
      return renderOption(option, index);
    }

    if (typeof option === 'string') {
      return (
        <MenuItem key={option} value={option}>
          {option}
        </MenuItem>
      );
    }

    if (typeof option === 'object') {
      return (
        <MenuItem key={option.value || option.id || index} value={option.value || option.id}>
          <ListItemText 
            primary={option.label || option.name || option.value} 
            secondary={option.description || option.subtitle}
          />
        </MenuItem>
      );
    }

    return null;
  };

  // Render grouped options
  const renderGroupedOptions = () => {
    return Object.entries(groupedOptions).map(([group, groupOptions]) => (
      <div key={group}>
        <Typography 
          variant="subtitle2" 
          sx={{ 
            px: 2, 
            py: 1, 
            fontWeight: 600, 
            color: 'text.secondary',
            backgroundColor: 'action.hover'
          }}
        >
          {group}
        </Typography>
        {groupOptions.map((option, index) => renderOptionItem(option, index))}
      </div>
    ));
  };

  return (
    <FormControl 
      fullWidth={fullWidth} 
      variant={variant} 
      size={size}
      error={error}
      required={required}
      disabled={disabled}
      style={style}
    >
      <InputLabel>{label}</InputLabel>
      <Select
        value={value}
        onChange={onChange}
        input={<OutlinedInput label={label} />}
        multiple={multiple}
        open={isOpen}
        onOpen={() => setIsOpen(true)}
        onClose={() => setIsOpen(false)}
        renderValue={(selected) => {
          if (multiple) {
            return (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {selected.map((value) => (
                  <Chip key={value} label={value} size="small" />
                ))}
              </Box>
            );
          }
          return selected;
        }}
        MenuProps={{
          PaperProps: {
            style: { maxHeight }
          }
        }}
      >
        {/* Search Input */}
        {searchable && (
          <Box sx={{ p: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
            <TextField
              size="small"
              placeholder={`Search ${label.toLowerCase()}...`}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              InputProps={{
                startAdornment: <SearchIcon sx={{ color: 'action.active', mr: 1 }} />,
                endAdornment: searchTerm && (
                  <IconButton size="small" onClick={() => setSearchTerm('')}>
                    <ClearIcon />
                  </IconButton>
                )
              }}
              fullWidth
              variant="outlined"
            />
          </Box>
        )}

        {/* Add Option Button */}
        {addable && searchTerm && onAddOption && (
          <MenuItem onClick={handleAddOption}>
            <AddIcon sx={{ mr: 1 }} />
            Add "{searchTerm}"
          </MenuItem>
        )}

        {/* Options Count */}
        {showCount && (
          <Box sx={{ px: 2, py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography variant="caption" color="text.secondary">
              {filteredOptions.length} option{filteredOptions.length !== 1 ? 's' : ''}
            </Typography>
          </Box>
        )}

        {/* Options */}
        {groupBy ? renderGroupedOptions() : filteredOptions.map((option, index) => renderOptionItem(option, index))}

        {/* No Options Message */}
        {filteredOptions.length === 0 && (
          <MenuItem disabled>
            <Typography variant="body2" color="text.secondary">
              No options found
            </Typography>
          </MenuItem>
        )}
      </Select>
    </FormControl>
  );
};

export default DynamicDropdown; 