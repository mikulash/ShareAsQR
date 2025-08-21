package mikulash.shareasqr

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the intent to extract any shared text
        val sharedText = handleIncomingIntent(intent)

        // Pass the shared text to the composable UI
        setContent {
            QRCodeApp(sharedText, onShare = { qrCodeBitmap ->
                shareQRCode(qrCodeBitmap)
            })
        }
    }

    // Override to handle new intents while the app is running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Handle the new intent
        val sharedText = handleIncomingIntent(intent)

        // Update the content if necessary
        setContent {
            QRCodeApp(sharedText, onShare = { qrCodeBitmap ->
                shareQRCode(qrCodeBitmap)
            })
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

    // Function to share the QR code as an image
    private fun shareQRCode(qrCodeBitmap: Bitmap?) {
        if (qrCodeBitmap != null) {
            try {
                // Save the bitmap to a file in the cache directory
                val cachePath = File(cacheDir, "images")
                cachePath.mkdirs() // Create the directory if it doesn't exist
                val file = File(cachePath, "qr_code.png")
                val fileOutputStream = FileOutputStream(file)
                qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.close()

                // Get the URI using FileProvider
                val qrImageUri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", file)

                // Create the share intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, qrImageUri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                // Start the share activity
                startActivity(Intent.createChooser(shareIntent, "Share QR Code"))

            } catch (e: IOException) {
                Log.e("MainActivity", "Error sharing QR code image", e)
            }
        } else {
            Log.e("MainActivity", "QR code bitmap is null, cannot share")
        }
    }
}

@Composable
fun QRCodeApp(sharedText: String? = null, onShare: (Bitmap?) -> Unit) {
    var inputText by remember { mutableStateOf(sharedText ?: "") }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val textFieldScrollState = rememberScrollState()

    // Automatically generate the QR code whenever the input text changes
    LaunchedEffect(inputText) {
        if (inputText.isNotEmpty()) {
            qrCodeBitmap = generateQRCode(inputText)
            if (qrCodeBitmap == null) {
                Log.e("com.github.mikulash.shareasqr", "Failed to generate QR Code.")
            }
        } else {
            qrCodeBitmap = null // Clear QR code when text is empty
        }
    }

    // Auto-scroll to bottom when text changes (to follow cursor)
    LaunchedEffect(inputText) {
        textFieldScrollState.animateScrollTo(textFieldScrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Make the entire column scrollable
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Changed from Center to Top
    ) {
        // Add some top spacing to center content when it's small
        Spacer(modifier = Modifier.height(32.dp))

        // Input Field with height constraint
        BasicTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp, max = 200.dp) // Constrain height with min and max
                .padding(horizontal = 16.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(textFieldScrollState), // Use dedicated scroll state
                    contentAlignment = Alignment.TopStart // Changed from Center to TopStart for better text alignment
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Enter text to generate QR Code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Display the QR code if generated
        qrCodeBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Reduced from 0.9f for better margins
                    .aspectRatio(1f) // Maintain a 1:1 aspect ratio
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Share button
            Button(
                onClick = { onShare(bitmap) },
                modifier = Modifier.fillMaxWidth(0.6f) // Give button consistent width
            ) {
                Text(text = "Share QR Code")
            }
        }

        // Add bottom spacing
        Spacer(modifier = Modifier.height(32.dp))
    }
}

fun generateQRCode(text: String): Bitmap? {
    return try {
        val barcodeEncoder = BarcodeEncoder()
        barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400)
    } catch (e: Exception) {
        Log.e("com.github.mikulash.shareasqr", "Error generating QR Code", e)
        null
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    QRCodeApp(onShare = {})
}