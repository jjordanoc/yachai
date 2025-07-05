package com.jjordanoc.yachai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jjordanoc.yachai.R
import com.jjordanoc.yachai.ui.Routes

@Composable
fun MainScreen(navController: NavController) {
    var useGpu by remember { mutableStateOf(true) }

    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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
        ) { Text(text = stringResource(R.string.tomar_foto_del_problema)) }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
                onClick = { navController.navigate(Routes.PRACTICE_SCREEN) },
                modifier = Modifier.fillMaxWidth()
        ) { Text(text = stringResource(R.string.practicar)) }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
                onClick = {
                    val route = "${Routes.WHITEBOARD_SCREEN}?useGpu=$useGpu"
                    navController.navigate(route)
                },
                modifier = Modifier.fillMaxWidth()
        ) { Text(text = "Pizarra Interactiva") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
                onClick = { navController.navigate(Routes.CHAT_SCREEN) },
                modifier = Modifier.fillMaxWidth()
        ) { Text(text = "Chat de Pruebas") }
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Usar aceleración de GPU", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = useGpu,
                onCheckedChange = { useGpu = it }
            )
        }
    }
}
