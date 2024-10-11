package com.example.shareasqr

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import android.util.Log

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the intent to extract any shared text
        val sharedText = handleIncomingIntent(intent)

        // Pass the shared text to the composable UI
        setContent {
            QRCodeApp(sharedText)
        }
    }

    // Override to handle new intents while the app is running
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Handle the new intent
        val sharedText = handleIncomingIntent(intent)

        // Update the content if necessary
        setContent {
            QRCodeApp(sharedText)
        }
    }

    // Function to handle the shared intent and extract the shared text
    private fun handleIncomingIntent(intent: Intent?): String? {
        if (intent != null && Intent.ACTION_SEND == intent.action && intent.type != null) {
            if ("text/plain" == intent.type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

                // Log shared text for debugging
                Log.d("MainActivity", "Shared Text: $sharedText")

                return sharedText
            } else {
                Log.e("MainActivity", "Unhandled intent type: ${intent.type}")
            }
        } else {
            Log.e("MainActivity", "Unhandled intent action or null intent")
        }
        return null // No shared text or invalid intent
    }
}

@Composable
fun QRCodeApp(sharedText: String? = null) {
    var inputText by remember { mutableStateOf(sharedText ?: "") }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Automatically generate the QR code whenever the input text changes
    LaunchedEffect(inputText) {
        if (inputText.isNotEmpty()) {
            qrCodeBitmap = generateQRCode(inputText)
            if (qrCodeBitmap == null) {
                Log.e("com.example.shareasqr.QRCodeApp", "Failed to generate QR Code.")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Input Field
        BasicTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (inputText.isEmpty()) {
                        Text(text = "Enter text to generate QR Code", style = MaterialTheme.typography.bodySmall)
                    }
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Display the QR code if generated
        qrCodeBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier
                    .fillMaxWidth(0.9f) // 90% of the screen width
                    .aspectRatio(1f) // Maintain a 1:1 aspect ratio
                    .padding(horizontal = 25.dp) // Additional padding if needed
            )
        }
    }
}

fun generateQRCode(text: String): Bitmap? {
    return try {
        val barcodeEncoder = BarcodeEncoder()
        barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400)
    } catch (e: Exception) {
        Log.e("com.example.shareasqr.QRCodeApp", "Error generating QR Code", e)
        null
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    QRCodeApp()
}
