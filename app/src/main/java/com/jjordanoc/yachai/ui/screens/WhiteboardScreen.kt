package com.jjordanoc.yachai.ui.screens

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions

@Composable
fun WhiteboardScreen(useGpu: Boolean) {
    val factory = WhiteboardViewModelFactory(application = LocalContext.current.applicationContext as Application, useGpu = useGpu)
    val whiteboardViewModel: WhiteboardViewModel = viewModel(factory = factory)

    val uiState by whiteboardViewModel.uiState.collectAsState()
    var currentStepIndex by remember { mutableIntStateOf(0) }

    val cropImageLauncher =
            rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
              if (result.isSuccessful) {
                result.uriContent?.let { whiteboardViewModel.runInference(it) }
              }
            }

    val photoPickerLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) {
                    uri: Uri? ->
              uri?.let {
                val cropOptions = CropImageContractOptions(it, CropImageOptions())
                cropImageLauncher.launch(cropOptions)
              }
            }

    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
      when (val state = uiState) {
        is WhiteboardUiState.ModelNotDownloaded -> {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              Icons.Outlined.CloudDownload, 
              contentDescription = "Download Model",
              modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
            )
            Text("Modelo de IA requerido", style = MaterialTheme.typography.headlineSmall)
            Text(
              "Necesitas descargar el modelo de IA para usar esta función",
              style = MaterialTheme.typography.bodyMedium,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
              "Tamaño: ${whiteboardViewModel.formatBytes(3_000_000_000L)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
              onClick = { whiteboardViewModel.downloadModel() },
              modifier = Modifier.fillMaxWidth(0.8f)
            ) {
              Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Descargar Modelo")
            }
          }
        }
        
        is WhiteboardUiState.Downloading -> {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp).padding(bottom = 16.dp))
            Text("Descargando modelo de IA...", style = MaterialTheme.typography.headlineSmall)
            
            val progress = state.progress
            if (progress.totalBytes > 0) {
              val progressPercentage = (progress.receivedBytes.toFloat() / progress.totalBytes.toFloat())
              
              LinearProgressIndicator(
                progress = { progressPercentage },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
              )
              
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("${whiteboardViewModel.formatBytes(progress.receivedBytes)} / ${whiteboardViewModel.formatBytes(progress.totalBytes)}")
                if (progress.bytesPerSecond > 0) {
                  Text("${whiteboardViewModel.formatBytes(progress.bytesPerSecond)}/s")
                }
              }
              
              if (progress.remainingMs > 0) {
                Text(
                  "${whiteboardViewModel.formatTime(progress.remainingMs)} restante",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
        
        is WhiteboardUiState.Initializing -> {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
            Text("Inicializando modelo de IA...", style = MaterialTheme.typography.bodyLarge)
            Text("Por favor espera un momento", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        
        is WhiteboardUiState.Idle -> {
          IdleStateContent(
            onStartClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            }
          )
        }
        is WhiteboardUiState.Loading -> {
          CircularProgressIndicator(modifier = Modifier.padding(top = 64.dp))
          Text("Analizando el problema...", modifier = Modifier.padding(top = 16.dp))
        }
        is WhiteboardUiState.Error -> {
          Text(state.message, color = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.height(16.dp))
          Button(
                  onClick = {
                    photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  }
          ) { Text("Intentar con otra foto") }
        }
        is WhiteboardUiState.Success -> {
          val steps = state.steps
          Column(
                  modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.SpaceBetween
          ) {
            WhiteboardContent(steps = steps, currentStepIndex = currentStepIndex)

            Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              Button(
                      onClick = { currentStepIndex = (currentStepIndex - 1).coerceAtLeast(0) },
                      enabled = currentStepIndex > 0
              ) { Text("Anterior") }
              Button(
                      onClick = {
                        currentStepIndex = (currentStepIndex + 1).coerceAtMost(steps.lastIndex)
                      },
                      enabled = currentStepIndex < steps.lastIndex
              ) { Text("Siguiente") }
            }
          }
        }
      }
    }
}

@Composable
fun IdleStateContent(
  onStartClick: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text("Pizarra Interactiva", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      "Toma una foto de un problema de matemáticas y te lo explicaré paso a paso.",
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
      onClick = onStartClick,
      modifier = Modifier.fillMaxWidth(0.8f)
    ) {
      Text("Analizar Problema de la Pizarra")
    }
  }
}

@Composable
fun WhiteboardContent(steps: List<WhiteboardStep>, currentStepIndex: Int) {
  val step = steps[currentStepIndex]

  Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
            "Paso ${currentStepIndex + 1} de ${steps.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(32.dp))

    // We use a key to force recomposition and trigger animation
    AnimatedContent(targetState = step.equation, label = "Equation Animation") { targetEquation ->
      Text(
              text = targetEquation,
              style = MaterialTheme.typography.displaySmall,
              textAlign = TextAlign.Center
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    AnimatedContent(targetState = step.prose, label = "Prose Animation") { targetProse ->
      Text(
              text = targetProse,
              style = MaterialTheme.typography.bodyLarge,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 16.dp)
      )
    }
  }
}
