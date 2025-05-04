package com.example.nimbus.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nimbus.data.model.local.SavedLocation
import com.example.nimbus.data.repository.WeatherRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val repository = remember { WeatherRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val locations by repository.savedLocations.collectAsState(initial = emptyList())
    val selectedLocationId by repository.selectedLocationId.collectAsState(initial = "")
    
    var showAddLocationDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Locations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (!isDarkTheme) Color(0xFFE6E1E8) else MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddLocationDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Location")
            }
        }
    ) { paddingValues -> 
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = 80.dp,
                    start = 16.dp,
                    end = 16.dp
                )
            ) {
                items(locations) { location -> 
                    LocationItem(
                        location = location,
                        isSelected = location.id == selectedLocationId,
                        onSelectLocation = {
                            coroutineScope.launch {
                                repository.setSelectedLocation(location.id)
                                onNavigateBack()
                            }
                        },
                        onDeleteLocation = {
                            if (!location.isCurrent) {
                                coroutineScope.launch {
                                    repository.removeLocation(location.id)
                                }
                            }
                        }
                    )
                }
            }
            
            if (showAddLocationDialog) {
                AddLocationDialog(
                    onDismiss = { showAddLocationDialog = false },
                    onAddLocation = { locationName, locationQuery -> 
                        coroutineScope.launch {
                            val newLocation = SavedLocation(
                                name = locationName,
                                query = locationQuery
                            )
                            repository.addLocation(newLocation)
                            repository.setSelectedLocation(newLocation.id)
                            showAddLocationDialog = false
                            onNavigateBack()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LocationItem(
    location: SavedLocation,
    isSelected: Boolean,
    onSelectLocation: () -> Unit,
    onDeleteLocation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectLocation),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location icon - using LocationOn for both, but with a different color for current location
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (location.isCurrent) Color(0xFF1E88E5) else MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Location name and details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!location.isCurrent && location.query.isNotEmpty()) {
                    Text(
                        text = location.query,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Selected indicator - we'll use CheckCircle for selected and a custom circle for unselected
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)  // Size of the circle
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)  // Border around the circle
                        .clip(CircleShape)  // Ensures it's a circle
                )
            }
            
            // Delete button (not shown for Current Location)
            if (!location.isCurrent) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDeleteLocation,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLocationDialog(
    onDismiss: () -> Unit,
    onAddLocation: (String, String) -> Unit
) {
    var locationName by remember { mutableStateOf("") }
    var locationQuery by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Location") },
        text = {
            Column {
                Text(
                    "Enter a city name or coordinates",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = locationQuery,
                    onValueChange = { locationQuery = it },
                    label = { Text("City Name or Coordinates") },
                    placeholder = { Text("e.g., London or 51.5072,-0.1276") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddLocation(locationName, locationQuery) },
                enabled = locationName.isNotBlank() && locationQuery.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}