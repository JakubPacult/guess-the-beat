package com.example.guessthebeat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    currentUserEmail: String?,
    isBiometricAvailable: Boolean,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onBiometricLogin: () -> Unit,
    onLogout: () -> Unit,
    onBackToMenu: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Ikona logowania",
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (currentUserEmail != null) "Zalogowano" else "Konto gracza",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (currentUserEmail != null) {
            Text(text = "Konto: $currentUserEmail")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onLogout, enabled = !isLoading) {
                Text("Wyloguj")
            }
        } else {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Hasło (min. 6 znaków)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onLogin(email, password) },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zaloguj się")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { onRegister(email, password) },
                enabled = !isLoading && email.isNotBlank() && password.length >= 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zarejestruj konto")
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            ElevatedButton(
                onClick = onGoogleLogin,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zaloguj się przez Google")
            }

            if (isBiometricAvailable) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onBiometricLogin,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometria",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zaloguj się biometrycznie")
                }
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onBackToMenu) {
            Text("Powrót do menu")
        }
    }
}