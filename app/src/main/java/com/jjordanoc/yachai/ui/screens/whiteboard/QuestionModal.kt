package com.jjordanoc.yachai.ui.screens.whiteboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jjordanoc.yachai.ui.theme.TutorialTeal
import com.jjordanoc.yachai.ui.theme.White
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.*

@Composable
fun QuestionModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    viewModel: TutorialViewModel,
    modifier: Modifier = Modifier
) {
    // Clear chat when modal is dismissed
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            viewModel.clearQuestionModalChat()
        }
    }
    
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            // Semi-transparent black background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                QuestionModalContent(
                    onDismiss = onDismiss,
                    viewModel = viewModel,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun QuestionModalContent(
    onDismiss: () -> Unit,
    viewModel: TutorialViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var textInput by remember { mutableStateOf("") }
    
    // LazyListState for auto-scrolling
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when messages change
    LaunchedEffect(uiState.questionModalMessages.size) {
        if (uiState.questionModalMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.questionModalMessages.size - 1)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(30.dp, 15.dp)
    ) {
        // Header
        Text(
            text = "¿Cuál es tu pregunta?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp)
        )
        
        // Chat messages section
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.questionModalMessages) { message ->
                ChatBubble(
                    message = message,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Show thinking bubble when processing
            if (uiState.isQuestionModalProcessing) {
                item {
                    ThinkingBubble(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Input section using ProblemInputBar
        ProblemInputBar(
            textInput = textInput,
            onTextInputChanged = { textInput = it },
            onSendText = {
                if (textInput.isNotBlank()) {
                    viewModel.sendQuestionModalMessage(textInput)
                    textInput = ""
                }
            },
            onImageSelected = { /* No image support in question modal */ },
            selectedImageUri = null,
            isProcessing = uiState.isQuestionModalProcessing,
            placeholder = "Escribe tu pregunta...",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (message.isFromUser) {
        Color(0xFFF1F1F1) // Light gray for user messages
    } else {
        TutorialTeal // Teal for AI messages
    }
    
    val textColor = if (message.isFromUser) {
        Color.Black
    } else {
        Color.Black
    }
    
    val alignment = if (message.isFromUser) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }
    
    Box(
        modifier = modifier,
        contentAlignment = alignment
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
        ) {
            // Empty space for user messages (left side)
            if (message.isFromUser) {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Speech bubble triangle for AI messages (left side)
            if (!message.isFromUser) {
                Box(
                    modifier = Modifier
                        .size(0.dp, 0.dp)
                        .background(
                            color = backgroundColor,
                            shape = androidx.compose.ui.graphics.RectangleShape
                        )
                        .clip(
                            androidx.compose.ui.graphics.RectangleShape
                        )
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .size(12.dp, 12.dp)
                            .offset(x = (-6).dp, y = 20.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(12f, 6f)
                            lineTo(0f, 12f)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = backgroundColor
                        )
                    }
                }
            }
            
            // Main chat bubble (same width for both AI and user)
            Card(
                modifier = Modifier.weight(2f), // Both AI and user bubbles take 2/3 of available space
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                )
            ) {
                Text(
                    text = message.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    modifier = Modifier.padding(20.dp),
                    textAlign = if (message.isFromUser) TextAlign.End else TextAlign.Start
                )
            }
            
            // Speech bubble triangle for user messages (right side)
            if (message.isFromUser) {
                Box(
                    modifier = Modifier
                        .size(0.dp, 0.dp)
                        .background(
                            color = backgroundColor,
                            shape = androidx.compose.ui.graphics.RectangleShape
                        )
                        .clip(
                            androidx.compose.ui.graphics.RectangleShape
                        )
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .size(12.dp, 12.dp)
                            .offset(x = 6.dp, y = 20.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(12f, 0f)
                            lineTo(0f, 6f)
                            lineTo(12f, 12f)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = backgroundColor
                        )
                    }
                }
            }
            
            // Empty space for AI messages (right side)
            if (!message.isFromUser) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
} 

@Composable
private fun ThinkingBubble(
    modifier: Modifier = Modifier
) {
    // Animated dots for thinking
    val infiniteTransition = rememberInfiniteTransition(label = "thinkingDots")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            // Speech bubble triangle for AI messages (left side)
            Box(
                modifier = Modifier
                    .size(0.dp, 0.dp)
                    .background(
                        color = TutorialTeal,
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
                    .clip(
                        androidx.compose.ui.graphics.RectangleShape
                    )
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(12.dp, 12.dp)
                        .offset(x = (-6).dp, y = 20.dp)
                ) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(12f, 6f)
                        lineTo(0f, 12f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = TutorialTeal
                    )
                }
            }
            
            // Main thinking bubble with flex layout
            Card(
                modifier = Modifier.weight(2f), // Take 2/3 of available space
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TutorialTeal
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pensando",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    
                    // Animated dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = Color.Black.copy(alpha = dot1Alpha),
                                    shape = CircleShape
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = Color.Black.copy(alpha = dot2Alpha),
                                    shape = CircleShape
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = Color.Black.copy(alpha = dot3Alpha),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
            
            // Empty space (1/3 of available space)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
} 