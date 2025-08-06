package com.jjordanoc.yachai.ui.screens.whiteboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jjordanoc.yachai.ui.theme.TutorialTeal
import com.jjordanoc.yachai.ui.theme.White

@Composable
fun SuccessModal(
    isVisible: Boolean,
    viewModel: TutorialViewModel,
    onResolverOtroProblema: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = { /* Cannot be dismissed */ },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            // Semi-transparent black background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                SuccessModalContent(
                    viewModel = viewModel,
                    onResolverOtroProblema = onResolverOtroProblema,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun SuccessModalContent(
    viewModel: TutorialViewModel,
    onResolverOtroProblema: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(30.dp, 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success message
        Text(
            text = "¡Excelente trabajo!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )
        
        // Congratulatory message
        Text(
            text = "Has completado el problema exitosamente. ¿Qué te gustaría hacer ahora?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        )
        
        // Action buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // "Resolver otro problema" button
            Button(
                onClick = {
                    viewModel.resetForNewProblem()
                    viewModel.hideSuccessModal()
                    onResolverOtroProblema() // Navigate to ProblemInputScreen
                },
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TutorialTeal,
                    contentColor = White
                )
            ) {
                Text(
                    text = "Resolver otro problema",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            
            // "Repetir explicación" button
            OutlinedButton(
                onClick = {
                    viewModel.restartExplanation()
                    viewModel.hideSuccessModal()
                },
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
            ) {
                Text(
                    text = "Repetir explicación",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
} 