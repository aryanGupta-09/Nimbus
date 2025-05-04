package com.example.nimbus.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimbus.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import android.content.Context

class SkyAnalysisViewModel : ViewModel() {
    
    // State for the selected image
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set
    
    var bitmap by mutableStateOf<Bitmap?>(null)
        private set
    
    // State for analysis
    var analysisResult by mutableStateOf<SkyAnalysisResult?>(null)
        private set
    
    var isAnalyzing by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var showFullscreenImage by mutableStateOf(false)
        private set
    
    fun setFullscreenImage(show: Boolean) {
        showFullscreenImage = show
    }
    
    fun clearImage() {
        bitmap = null
        selectedImageUri = null
        analysisResult = null
        errorMessage = null
    }
    
    // Function to scale down bitmap to a reasonable size
    private fun scaleBitmap(originalBitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
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
    
    // Process uri to bitmap
    fun processImageUri(uri: Uri?, context: Context) {
        selectedImageUri = uri
        
        // Reset analysis when selecting a new image
        analysisResult = null
        errorMessage = null
        
        uri?.let {
            try {
                val originalBitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.setTargetSampleSize(2)
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
                Log.e("SkyAnalysisViewModel", "Error loading image", e)
            }
        }
    }
    
    // Function to parse the AI response into a structured format
    private fun parseAiResponse(response: String): SkyAnalysisResult {
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
    fun analyzeSkyImage(context: Context) {
        val image = bitmap ?: run {
            errorMessage = "Please select an image first"
            return
        }
        
        isAnalyzing = true
        errorMessage = null
        analysisResult = null
        
        viewModelScope.launch {
            try {
                // Get API key from BuildConfig
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                // Create the Gemini model
                val model = GenerativeModel(
                    modelName = "gemini-2.0-flash",
                    apiKey = apiKey
                )
                
                // Prepare the prompt for sky/weather analysis with sky detection
                val prompt = "First, determine if this is a sky image suitable for weather analysis. " +
                    "If it is NOT a sky image, respond ONLY with: 'NOT_SKY: [brief reason why it's not a suitable sky image]' " +
                    "If it IS a sky image, analyze it and provide JUST these 4 numbered points:\n" +
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
                
                // Parse the response - check if it's a non-sky image first
                val responseText = response.text ?: "No analysis could be generated."
                
                if (responseText.startsWith("NOT_SKY:", ignoreCase = true)) {
                    // This is not a sky image, show the error message
                    errorMessage = responseText.substringAfter("NOT_SKY:").trim()
                } else {
                    // This is a sky image, parse the analysis
                    analysisResult = parseAiResponse(responseText)
                }
            } catch (e: Exception) {
                errorMessage = "Analysis failed: ${e.message}"
                Log.e("SkyAnalysisViewModel", "Analysis failed", e)
            } finally {
                isAnalyzing = false
            }
        }
    }
}