package com.EYP.dimdim

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.EYP.dimdim.ui.theme.DimDimTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check if we're processing shared images
        val shouldProcessImages = intent.getBooleanExtra("PROCESS_IMAGES", false)
        val imageUris = intent.getParcelableArrayListExtra<Uri>("IMAGE_URIS")
        
        setContent {
            DimDimTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (shouldProcessImages && !imageUris.isNullOrEmpty()) {
                        Text(
                            text = "Processing ${imageUris.size} receipt image${if (imageUris.size != 1) "s" else ""}...",
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        Greeting(
                            name = "DimDim",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DimDimTheme {
        Greeting("Android")
    }
}