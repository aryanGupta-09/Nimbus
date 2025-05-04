package com.example.nimbus.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nimbus.BuildConfig

// Data class to hold structured sky analysis data
data class SkyAnalysisResult(
    val weatherConditions: String = "",
    val cloudTypes: String = "",
    val forecast: String = "",
    val phenomena: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkyAnalysisScreen(
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean,
    viewModel: SkyAnalysisViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // State to control image source dialog
    var showImageSourceDialog by remember { mutableStateOf(false) }
    
    // Create temporary Uri for camera
    val imageUri = remember {
        ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Sky Image")
            put(MediaStore.Images.Media.DESCRIPTION, "Taken from Nimbus app")
        }.let { contentValues ->
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }
    }
    
    // Setup camera capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            viewModel.processImageUri(imageUri, context)
        }
    }
    
    // Camera permission state
    val cameraPermissionState = remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == 
            PackageManager.PERMISSION_GRANTED
    ) }
    
    // Request camera permission
    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionState.value = isGranted
        if (isGranted) {
            // Only launch the camera if imageUri is not null
            imageUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        }
    }
    
    // Setup image picker from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.processImageUri(uri, context)
    }
    
    // Image source selection dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Select Image Source") },
            text = { Text("Choose where to get the sky image from") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Gallery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        if (cameraPermissionState.value) {
                            // Only launch the camera if imageUri is not null
                            imageUri?.let { uri ->
                                cameraLauncher.launch(uri)
                            }
                        } else {
                            requestCameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Camera")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sky Analysis") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (!isDarkTheme) Color(0xFFE6E1E8) else MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Sky Weather Analysis",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Upload an image of the sky and AI will analyze current weather conditions",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Image selection area
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.bitmap != null) {
                    // Image with clickable modifier to show full screen
                    Image(
                        bitmap = viewModel.bitmap!!.asImageBitmap(),
                        contentDescription = "Selected sky image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { viewModel.setFullscreenImage(true) },
                        contentScale = ContentScale.Crop
                    )
                    
                    // Custom delete button with better proportions and positioning
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp) // More padding to prevent cutting off
                            .size(28.dp) // Small circle
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { 
                                viewModel.clearImage() 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete image",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp) // Proper icon size
                        )
                    }
                } else {
                    // Make the empty placeholder clickable to open the image source dialog
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showImageSourceDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to add sky image",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Analyze button
            Button(
                onClick = { viewModel.analyzeSkyImage(context) },
                modifier = Modifier.fillMaxWidth(0.8f),
                enabled = viewModel.bitmap != null && !viewModel.isAnalyzing
            ) {
                if (viewModel.isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(text = "Analyze Sky")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Analysis results card - remove the separate loading card
            if (viewModel.analysisResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Sky Analysis Results",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Create a local copy of the analysis result to avoid smart cast issues
                        val analysis = viewModel.analysisResult
                        if (analysis != null) {
                            // Weather conditions section
                            AnalysisSection(
                                title = "Current Weather",
                                content = analysis.weatherConditions,
                                icon = Icons.Default.WbSunny
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                            
                            // Cloud types section
                            AnalysisSection(
                                title = "Cloud Types",
                                content = analysis.cloudTypes,
                                icon = Icons.Default.Cloud
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                            
                            // Forecast section
                            AnalysisSection(
                                title = "Forecast Prediction",
                                content = analysis.forecast,
                                icon = Icons.Default.Info
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                            
                            // Phenomena section
                            AnalysisSection(
                                title = "Atmospheric Phenomena",
                                content = analysis.phenomena,
                                icon = Icons.Default.Visibility
                            )
                        }
                    }
                }
            } else if (viewModel.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        
        // Fullscreen image viewer without animation
        if (viewModel.showFullscreenImage && viewModel.bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Removes the ripple/tap effect
                    ) { 
                        viewModel.setFullscreenImage(false) 
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = viewModel.bitmap!!.asImageBitmap(),
                    contentDescription = "Sky image fullscreen view",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentScale = ContentScale.Fit
                )
                
                // Close icon in top-right corner with additional padding to avoid top bar overlap
                IconButton(
                    onClick = { viewModel.setFullscreenImage(false) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = paddingValues.calculateTopPadding() + 16.dp, end = 16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close fullscreen view",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisSection(
    title: String,
    content: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 12.dp, top = 2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}