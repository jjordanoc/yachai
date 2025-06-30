package com.jjordanoc.yachai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jjordanoc.yachai.R
import com.jjordanoc.yachai.ui.Routes

@Composable
fun MainScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.bienvenido_a_yachai),
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.tu_ayudante_de_matematicas),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(64.dp))
        Button(
            onClick = { navController.navigate(Routes.CAMERA_SCREEN) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.tomar_foto_del_problema))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate(Routes.PRACTICE_SCREEN) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.practicar))
        }
    }
} 