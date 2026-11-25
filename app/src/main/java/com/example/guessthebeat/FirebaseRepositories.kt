package com.example.guessthebeat

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

data class PlayerStats(
    val gamesPlayed: Int = 0,
    val songsGuessed: Int = 0,
    val songsMissed: Int = 0,
    val totalPoints: Int = 0,
    val bestScore: Int = 0
)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    init {
        // Disable reCAPTCHA / App Verification during testing/development if Firebase Console is not configured for reCAPTCHA
        runCatching {
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        }
    }
    val currentUserEmail: String?
        get() = auth.currentUser?.email

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun loginWithGoogle(context: Context): Result<Unit> = runCatching {
        val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val webClientId = if (webClientIdResId != 0) {
            context.getString(webClientIdResId)
        } else {
            // Fallback from google-services.json if resource hasn't been generated yet
            "782578499357-fbt5i81u7b3ilf99057j23qb7t68i8p1.apps.googleusercontent.com"
        }

        val activity = context.findActivity() ?: error("Brak kontekstu Activity dla logowania Google.")
        val credentialManager = CredentialManager.create(activity)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = try {
            credentialManager.getCredential(context = activity, request = request)
        } catch (e: GetCredentialCancellationException) {
            return@runCatching
        } catch (e: NoCredentialException) {
            error("Brak dostępnych kont Google na urządzeniu. Dodaj konto Google w ustawieniach telefonu/emulatora.")
        } catch (e: GetCredentialException) {
            error("Błąd pobierania konta Google (${e.type}): ${e.message}")
        }

        val credential = result.credential

        if (credential is androidx.credentials.CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            auth.signInWithCredential(firebaseCredential).await()
        } else {
            error("Nieobsługiwany typ poświadczeń Google.")
        }
    }

    fun logout() {
        auth.signOut()
    }
}

class StatsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getStats(uid: String): PlayerStats = runCatching {
        val snapshot = db.collection("users").document(uid).get().await()
        snapshot.toObject(PlayerStats::class.java) ?: PlayerStats()
    }.getOrDefault(PlayerStats())

    suspend fun recordGameResult(
        uid: String,
        guessedInSession: Int,
        missedInSession: Int,
        sessionScore: Int
    ): PlayerStats {
        val current = getStats(uid)
        val updated = PlayerStats(
            gamesPlayed = current.gamesPlayed + 1,
            songsGuessed = current.songsGuessed + guessedInSession,
            songsMissed = current.songsMissed + missedInSession,
            totalPoints = current.totalPoints + sessionScore,
            bestScore = maxOf(current.bestScore, sessionScore)
        )
        runCatching {
            db.collection("users")
                .document(uid)
                .set(updated, SetOptions.merge())
                .await()
        }
        return updated
    }
}