package com.EYP.dimdim.presentation

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.EYP.dimdim.MainActivity
import com.EYP.dimdim.data.util.ImageInfo
import com.EYP.dimdim.presentation.viewmodel.ShareReceiverViewModel
import com.EYP.dimdim.ui.theme.DimDimTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private val viewModel: ShareReceiverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            DimDimTheme {
                ShareReceiverScreen(
                    viewModel = viewModel,
                    onProcessSelected = { imageInfoList ->
                        // Navigate to main processing flow
                        val intent = Intent(this@ShareReceiverActivity, MainActivity::class.java).apply {
                            putExtra("PROCESS_IMAGES", true)
                            putParcelableArrayListExtra("IMAGE_URIS", ArrayList<Uri>(imageInfoList.map { it.uri }))
                        }
                        startActivity(intent)
                        finish()
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }

        // Process the shared intent
        handleSharedIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
                if (imageUri != null) {
                    viewModel.processImages(listOf(imageUri))
                } else {
                    viewModel.setError("No image received from share intent")
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val imageUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }
                if (!imageUris.isNullOrEmpty()) {
                    viewModel.processImages(imageUris)
                } else {
                    viewModel.setError("No images received from share intent")
                }
            }
            else -> {
                viewModel.setError("Unsupported share action: ${intent.action}")
            }
        }
    }
}

@Composable
fun ShareReceiverScreen(
    viewModel: ShareReceiverViewModel = viewModel(),
    onProcessSelected: (List<ImageInfo>) -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Process Receipt Images",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            uiState.isLoading -> {
                LoadingContent()
            }
            uiState.error != null -> {
                ErrorContent(
                    error = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.retry() },
                    onCancel = onCancel
                )
            }
            uiState.imageInfoList.isNotEmpty() -> {
                ImageListContent(
                    imageInfoList = uiState.imageInfoList,
                    onProcessSelected = onProcessSelected,
                    onCancel = onCancel
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Processing shared images...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Error Processing Images",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun ImageListContent(
    imageInfoList: List<ImageInfo>,
    onProcessSelected: (List<ImageInfo>) -> Unit,
    onCancel: () -> Unit
) {
    val validImages = imageInfoList.filter { it.isValid }
    val invalidImages = imageInfoList.filter { !it.isValid }

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (validImages.isNotEmpty()) {
            item {
                Text(
                    text = "Valid Images (${validImages.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            items(validImages) { imageInfo ->
                ImageInfoCard(imageInfo = imageInfo, isValid = true)
            }
        }

        if (invalidImages.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Invalid Images (${invalidImages.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            items(invalidImages) { imageInfo ->
                ImageInfoCard(imageInfo = imageInfo, isValid = false)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancel")
        }
        
        Button(
            onClick = { onProcessSelected(validImages) },
            enabled = validImages.isNotEmpty(),
            modifier = Modifier.weight(1f)
        ) {
            Text("Process ${validImages.size} Image${if (validImages.size != 1) "s" else ""}")
        }
    }
}

@Composable
private fun ImageInfoCard(
    imageInfo: ImageInfo,
    isValid: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isValid) 
                MaterialTheme.colorScheme.surfaceContainerLow 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (isValid) "Valid" else "Invalid",
                tint = if (isValid) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = imageInfo.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${imageInfo.mimeType} • ${formatFileSize(imageInfo.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!isValid && imageInfo.errorMessage != null) {
                    Text(
                        text = imageInfo.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
        bytes >= 1024 -> "${bytes / 1024}KB"
        else -> "${bytes}B"
    }
}