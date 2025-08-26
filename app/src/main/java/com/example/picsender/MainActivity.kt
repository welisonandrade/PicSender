package com.example.picsender

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.picsender.ui.theme.PicSenderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PicSenderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PicSenderScreen()
                }
            }
        }
    }
}

@Composable
fun PicSenderScreen() {
    val ctx = LocalContext.current
    var ip by remember { mutableStateOf("192.168.0.10") }
    var port by remember { mutableStateOf("5001") }
    var status by remember { mutableStateOf("Status: pronto.") }

    // Estados de captura/preview
    var photoFile by remember { mutableStateOf<File?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Launcher nativo para tirar foto
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            // carregar preview leve
            photoFile?.let { file ->
                previewBitmap = FileInputStream(file).use { fis ->
                    BitmapFactory.decodeStream(fis)
                }
                showPreview = true
                status = "Pré-visualização pronta. Enviar ou Descartar?"
            }
        } else {
            status = "Captura cancelada."
            cleanupTemp { photoFile = null; previewBitmap = null; showPreview = false }
        }
    }

    // Função para iniciar o processo de captura
    val startCaptureProcess = {
        // cria arquivo temp e Uri via FileProvider
        val imagesDir = File(ctx.cacheDir, "images").apply { mkdirs() }
        val file = File(imagesDir, "capture_${System.currentTimeMillis()}.jpg")
        val uri: Uri = FileProvider.getUriForFile(
            ctx, ctx.packageName + ".fileprovider", file
        )
        photoFile = file
        takePictureLauncher.launch(uri)
    }

    // Permissão de câmera
    val askCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCaptureProcess()
        } else {
            status = "Permissão da câmera negada."
        }
    }

    fun ensureCameraThenCapture() {
        val granted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startCaptureProcess()
        } else {
            askCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun doSend() {
        val file = photoFile ?: run {
            status = "Nenhuma imagem para enviar."
            return
        }
        status = "Enviando..."
        scope.launch(Dispatchers.IO) {
            try {
                val jpeg = prepareJpegForSend(file, 1280, 80)
                sendBytesOverSocket(ip.trim(), port.trim().toInt(), jpeg)
                status = "Foto enviada ✅"
            } catch (e: Exception) {
                status = "Falha ao enviar: ${e.message}"
            }
        }
    }

    fun doDiscard() {
        cleanupTemp {
            photoFile = null
            previewBitmap = null
            showPreview = false
            status = "Imagem descartada. Pronto para nova captura."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = ip, onValueChange = { ip = it },
            label = { Text("IP do servidor (ex.: 192.168.0.10)") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = port, onValueChange = { port = it },
            label = { Text("Porta") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { ensureCameraThenCapture() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Tirar foto") }

        if (showPreview && previewBitmap != null) {
            Spacer(Modifier.height(16.dp))
            Image(
                bitmap = previewBitmap!!.asImageBitmap(),
                contentDescription = "Pré-visualização",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { doDiscard() },
                    modifier = Modifier.weight(1f)
                ) { Text("Descartar") }
                Button(
                    onClick = { doSend() },
                    modifier = Modifier.weight(1f)
                ) { Text("Enviar") }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(status)
    }
}

/* ===== Helpers ===== */

private fun prepareJpegForSend(file: File, maxWidth: Int, quality: Int): ByteArray {
    val original = FileInputStream(file).use { fis ->
        BitmapFactory.decodeStream(fis)!!
    }
    val resized = if (original.width > maxWidth) {
        val ratio = maxWidth.toFloat() / original.width.toFloat()
        val newH = (original.height * ratio).roundToInt()
        Bitmap.createScaledBitmap(original, maxWidth, newH, true)
    } else original

    val baos = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, quality, baos)
    if (resized !== original) original.recycle()
    resized.recycle()
    return baos.toByteArray()
}

private fun sendBytesOverSocket(ip: String, port: Int, payload: ByteArray) {
    Socket(ip, port).use { s ->
        s.getOutputStream().use { os ->
            val len = ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(payload.size)
                .array()
            os.write(len)
            os.write(payload)
            os.flush()
        }
    }
}

private inline fun cleanupTemp(after: () -> Unit) {
    after() // aqui poderíamos deletar arquivo se quisermos: file.delete()
}
