package com.example.nimbus.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.nimbus.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

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
    isDarkTheme: Boolean
) {
    // Get API key from BuildConfig
    val apiKey = BuildConfig.GEMINI_API_KEY
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State for the selected image
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // State for analysis
    var analysisResult by remember { mutableStateOf<SkyAnalysisResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Function to scale down bitmap to a reasonable size
    fun scaleBitmap(originalBitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        
        // If the bitmap is already smaller than maxDimension, return it unchanged
        if (width <= maxDimension && height <= maxDimension) {
            return originalBitmap
        }
        
        // Calculate scale factor
        val scaleFactor = when {
            width > height -> maxDimension.toFloat() / width
            else -> maxDimension.toFloat() / height
        }
        
        // Create scaled bitmap
        return Bitmap.createScaledBitmap(
            originalBitmap,
            (width * scaleFactor).toInt(),
            (height * scaleFactor).toInt(),
            true
        )
    }
    
    // Process uri to bitmap when selectedImageUri changes
    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let {
            try {
                val originalBitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                        // Set lower allocation limit to prevent OOM
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        // Use RGB_565 to reduce memory usage (loses alpha channel but fine for sky images)
                        decoder.setTargetSampleSize(2) // Sample 1 in every 2 pixels
                    }
                }
                
                // Scale down the bitmap to prevent "Canvas: trying to draw too large bitmap" error
                bitmap = scaleBitmap(originalBitmap)
                
                // Recycle the original bitmap if it's different from the scaled one
                if (originalBitmap != bitmap) {
                    originalBitmap.recycle()
                }
            } catch (e: Exception) {
                errorMessage = "Error loading image: ${e.message}"
            }
        }
    }
    
    // Setup image picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            // Reset previous analysis when new image is selected
            analysisResult = null
            errorMessage = null
        }
    }
    
    // Function to parse the AI response into a structured format
    fun parseAiResponse(response: String): SkyAnalysisResult {
        // Default values for each section
        var weatherConditions = "No weather conditions identified"
        var cloudTypes = "No cloud types identified"
        var forecast = "No forecast available"
        var phenomena = "No notable atmospheric phenomena"
        
        // Simple parsing: split by numbered points (1., 2., 3., 4.)
        val points = response.split(Regex("\\d+\\."))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        // Assign points to the appropriate sections if available
        if (points.size >= 1) weatherConditions = points[0]
        if (points.size >= 2) cloudTypes = points[1]
        if (points.size >= 3) forecast = points[2]
        if (points.size >= 4) phenomena = points[3]
        
        return SkyAnalysisResult(
            weatherConditions = weatherConditions,
            cloudTypes = cloudTypes,
            forecast = forecast,
            phenomena = phenomena
        )
    }
    
    // Function to perform AI analysis of sky image
    val analyzeSkyImage: () -> Unit = {
        bitmap?.let { image ->
            isAnalyzing = true
            errorMessage = null
            analysisResult = null
            
            scope.launch {
                try {
                    // Create the Gemini model
                    val model = GenerativeModel(
                        modelName = "gemini-2.0-flash",  // Use standard supported model
                        apiKey = apiKey
                    )
                    
                    // Prepare the prompt for sky/weather analysis
                    val prompt = "Analyze this sky image and provide JUST these 4 numbered points without section headers or any other text:\n" +
                        "1. Current weather conditions visible\n" +
                        "2. Description of clouds in simple, everyday language (avoid technical terms like cirrus, stratus, etc.)\n" +
                        "3. Weather forecast prediction based on sky appearance\n" +
                        "4. Any interesting atmospheric phenomena visible\n\n" +
                        "Keep each point brief (10-15 words maximum) and use everyday language that non-experts can understand."
                    
                    // Send the image to Gemini for analysis
                    val response = model.generateContent(
                        content {
                            text(prompt)
                            image(image)
                        }
                    )
                    
                    // Parse the response into a structured format - safely handle nullable text
                    val responseText = response.text ?: "No analysis could be generated."
                    analysisResult = parseAiResponse(responseText)
                } catch (e: Exception) {
                    errorMessage = "Analysis failed: ${e.message}"
                } finally {
                    isAnalyzing = false
                }
            }
        } ?: run {
            errorMessage = "Please select an image first"
        }
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
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Selected sky image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "No image selected",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Image selection button
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(text = "Select Image from Gallery")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Analyze button
            Button(
                onClick = analyzeSkyImage,
                modifier = Modifier.fillMaxWidth(0.8f),
                enabled = bitmap != null && !isAnalyzing
            ) {
                if (isAnalyzing) {
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
            
            // Analysis results card
            if (isAnalyzing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing sky image...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else if (analysisResult != null) {
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
                        
                        // Weather conditions section
                        AnalysisSection(
                            title = "Current Weather",
                            content = analysisResult!!.weatherConditions,
                            icon = Icons.Default.WbSunny
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        // Cloud types section
                        AnalysisSection(
                            title = "Cloud Types",
                            content = analysisResult!!.cloudTypes,
                            icon = Icons.Default.Cloud
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        // Forecast section
                        AnalysisSection(
                            title = "Forecast Prediction",
                            content = analysisResult!!.forecast,
                            icon = Icons.Default.Info
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        // Phenomena section
                        AnalysisSection(
                            title = "Atmospheric Phenomena",
                            content = analysisResult!!.phenomena,
                            icon = Icons.Default.Visibility
                        )
                    }
                }
            } else if (errorMessage != null) {
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
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
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