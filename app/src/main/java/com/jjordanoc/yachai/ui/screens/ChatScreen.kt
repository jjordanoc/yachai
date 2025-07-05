package com.jjordanoc.yachai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(chatViewModel: ChatViewModel = viewModel()) {
    val uiState by chatViewModel.uiState.collectAsState()

    val photoPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { chatViewModel.processImage(it) }
        }

    Scaffold(
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Upload Image")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ChatUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ChatUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        reverseLayout = true
                    ) {
                        items(state.messages.reversed()) { message ->
                            MessageView(message)
                        }
                    }
                }
                is ChatUiState.Error -> {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun MessageView(message: ChatMessage) {
    val horizontalAlignment = when (message) {
        is ChatMessage.UserMessage -> Alignment.End
        else -> Alignment.Start
    }
    
    val cardColors = CardDefaults.cardColors(
        containerColor = when (message) {
            is ChatMessage.UserMessage -> MaterialTheme.colorScheme.primaryContainer
            is ChatMessage.ModelMessage -> MaterialTheme.colorScheme.surfaceVariant
            is ChatMessage.ErrorMessage -> MaterialTheme.colorScheme.errorContainer
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = horizontalAlignment)
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = cardColors
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when (message) {
                    is ChatMessage.UserMessage -> {
                        if (message.image != null) {
                            Image(
                                bitmap = message.image.asImageBitmap(),
                                contentDescription = "User uploaded image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (message.text.isNotEmpty()) {
                            Text(message.text)
                        }
                    }
                    is ChatMessage.ModelMessage -> Text(message.text)
                    is ChatMessage.ErrorMessage -> Text(message.text)
                }
            }
        }
    }
} 