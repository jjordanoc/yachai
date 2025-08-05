package com.jjordanoc.yachai.ui.screens.whiteboard

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.jjordanoc.yachai.R
import com.jjordanoc.yachai.ui.theme.White

@Composable
fun ProblemLoadingScreen(
    navController: NavController,
    viewModel: TutorialViewModel = viewModel(
        factory = TutorialViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Use data from ViewModel if available, otherwise show defaults
    val problemText = if (uiState.textInput.isNotBlank()) uiState.textInput else "Texto del problema"
    val problemImageUri = uiState.selectedImageUri
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Alpaca with analysis message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Alpaca image
                Image(
                    painter = painterResource(id = R.drawable.alpakey),
                    contentDescription = "Alpaca tutor analyzing",
                    modifier = Modifier
                        .width(250.dp)
                        .height(166.dp),
                    contentScale = ContentScale.Fit
                )
                
                // Analysis message
                Text(
                    text = "Estoy analizando tu problema…",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
            
            // Right side - Problem display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(15.dp),
                modifier = Modifier.padding(vertical = 15.dp)
            ) {
                // "Tu problema:" label
                Text(
                    text = "Tu problema:",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                
                // Image placeholder or actual image
                Box(
                    modifier = Modifier
                        .width(312.dp)
                        .height(92.dp)
                        .background(
                            color = Color(0xFFD9D9D9),
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    problemImageUri?.let { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "Problem image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                // Problem text
                Text(
                    text = if (uiState.textInput.isNotBlank()) "\"$problemText\"" else "\"Texto del problema\"",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(312.dp)
                )
            }
        }
    }
}