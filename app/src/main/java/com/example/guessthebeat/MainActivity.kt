package com.example.guessthebeat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guessthebeat.ui.theme.GuessTheBeatTheme
import com.example.guessthebeat.ui.theme.NeonBlue
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val musicPlayer = MusicPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuessTheBeatTheme {
                val topColor = Color(0xFF070707)
                val bottomNeon = NeonBlue.copy(alpha = 0.62f)

                val backgroundGradient = Brush.verticalGradient(
                    colors = listOf(topColor, bottomNeon)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundGradient),
                    color = Color.Transparent
                ) {
                    MainContainer(musicPlayer = musicPlayer)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        musicPlayer.stop()
    }
}

@Composable
fun MainContainer(
    musicPlayer: MusicPlayer,
    viewModel: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    val statsRepository = remember { StatsRepository() }
    val biometricAuthManager = remember(context) { BiometricAuthManager(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUserEmail by remember { mutableStateOf(authRepository.currentUserEmail) }
    var playerStats by remember { mutableStateOf(PlayerStats()) }

    LaunchedEffect(authRepository.currentUserId) {
        val uid = authRepository.currentUserId
        if (uid != null) {
            runCatching { statsRepository.getStats(uid) }
                .onSuccess { playerStats = it }
        } else {
            playerStats = PlayerStats()
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(viewModel = viewModel, musicPlayer = musicPlayer)
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = "GuessTheBeat",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )

            when {
                viewModel.currentScreen == "login" -> {
                    LoginScreen(
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        currentUserEmail = currentUserEmail,
                        isBiometricAvailable = biometricAuthManager.isBiometricAvailable(),
                        onLogin = { email, password ->
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                authRepository.login(email, password)
                                    .onSuccess {
                                        biometricAuthManager.saveCredentials(email, password)
                                        currentUserEmail = authRepository.currentUserEmail
                                        viewModel.currentScreen = "menu"
                                    }
                                    .onFailure { e ->
                                        val msg = e.localizedMessage ?: e.message ?: "Błąd logowania"
                                        errorMessage = when {
                                            msg.contains("CONFIGURATION_NOT_FOUND") ->
                                                "Błąd konfiguracji Firebase (CONFIGURATION_NOT_FOUND). Wyłącz reCAPTCHA w Firebase Console (Authentication -> Settings -> User actions) lub dodaj plik google-services.json."
                                            msg.contains("INVALID_LOGIN_CREDENTIALS") || msg.contains("user-not-found") || msg.contains("wrong-password") ->
                                                "Nieprawidłowy e-mail lub hasło."
                                            msg.contains("email-already-in-use") ->
                                                "Konto o podanym adresie e-mail już istnieje."
                                            else -> msg
                                        }
                                    }
                                isLoading = false
                            }
                        },
                        onRegister = { email, password ->
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                authRepository.register(email, password)
                                    .onSuccess {
                                        biometricAuthManager.saveCredentials(email, password)
                                        currentUserEmail = authRepository.currentUserEmail
                                        viewModel.currentScreen = "menu"
                                    }
                                    .onFailure { e ->
                                        val msg = e.localizedMessage ?: e.message ?: "Błąd rejestracji"
                                        errorMessage = when {
                                            msg.contains("CONFIGURATION_NOT_FOUND") ->
                                                "Błąd konfiguracji Firebase (CONFIGURATION_NOT_FOUND). Wyłącz reCAPTCHA w Firebase Console (Authentication -> Settings -> User actions) lub dodaj plik google-services.json."
                                            msg.contains("email-already-in-use") ->
                                                "Konto o podanym adresie e-mail już istnieje."
                                            msg.contains("weak-password") ->
                                                "Hasło jest zbyt słabe (wymagane min. 6 znaków)."
                                            else -> msg
                                        }
                                    }
                                isLoading = false
                            }
                        },
                        onGoogleLogin = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                authRepository.loginWithGoogle(context)
                                    .onSuccess {
                                        currentUserEmail = authRepository.currentUserEmail
                                        viewModel.currentScreen = "menu"
                                    }
                                    .onFailure { e ->
                                        errorMessage = e.localizedMessage ?: e.message ?: "Błąd logowania Google"
                                    }
                                isLoading = false
                            }
                        },
                        onBiometricLogin = {
                            val activity = context.findFragmentActivity()
                            if (activity == null) {
                                errorMessage = "Brak okna dla logowania biometrycznego."
                            } else {
                                errorMessage = null
                                biometricAuthManager.showBiometricPrompt(
                                    activity = activity,
                                    onSuccess = {
                                        scope.launch {
                                            isLoading = true
                                            if (authRepository.currentUserEmail != null) {
                                                currentUserEmail = authRepository.currentUserEmail
                                                viewModel.currentScreen = "menu"
                                            } else {
                                                val saved = biometricAuthManager.getSavedCredentials()
                                                if (saved != null) {
                                                    authRepository.login(saved.first, saved.second)
                                                        .onSuccess {
                                                            currentUserEmail = authRepository.currentUserEmail
                                                            viewModel.currentScreen = "menu"
                                                        }
                                                        .onFailure { e ->
                                                            errorMessage = "Błąd automatycznego logowania: ${e.localizedMessage}"
                                                        }
                                                } else {
                                                    errorMessage = "Zaloguj się najpierw e-mailem i hasłem, aby aktywować biometrię."
                                                }
                                            }
                                            isLoading = false
                                        }
                                    },
                                    onError = { err ->
                                        errorMessage = err
                                    }
                                )
                            }
                        },
                        onLogout = {
                            authRepository.logout()
                            currentUserEmail = null
                            playerStats = PlayerStats()
                            errorMessage = null
                        },
                        onBackToMenu = {
                            errorMessage = null
                            viewModel.currentScreen = "menu"
                        }
                    )
                }
                viewModel.selectedPlaylist == null -> {
                    MainMenuScreen(viewModel = viewModel, musicPlayer = musicPlayer)
                }
                else -> {
                    GameScreen(musicPlayer = musicPlayer, viewModel = viewModel)
                }
            }

            // 💡 Pusty odstęp na końcu, żeby ostatni element na liście dało się przewinąć ponad pasek
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}