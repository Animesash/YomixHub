package com.yomixhub.android.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.yomixhub.android.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The signed-in user as shown in the UI. */
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
)

/**
 * Firebase Authentication + Google Sign-In.
 *
 *  * the current user is exposed as a [StateFlow] that survives screens
 *    (observed by `AuthViewModel` and the settings screen);
 *  * Google sign-in uses the classic `GoogleSignIn` intent flow: the UI
 *    launches [googleSignInIntent] via `rememberLauncherForActivityResult`
 *    and hands the returned ID token to [signInWithGoogleIdToken], which
 *    exchanges it for Firebase credentials;
 *  * auth changes trigger [BookmarkRepository] cloud sync.
 */
object AuthRepository {

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private var attached = false

    /**
     * Starts listening to auth state. Call once from
     * `YomixApplication.onCreate()`; must be called AFTER `AppState.attach`
     * so cloud sync never races the local bookmark restore.
     */
    fun attach(context: Context) {
        if (attached) return
        attached = true
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            _currentUser.value = user?.toAuthUser()
            _authError.value = null
            if (user != null) {
                BookmarkRepository.onUserSignedIn(user.uid)
            } else {
                BookmarkRepository.onUserSignedOut()
            }
        }
    }

    /** Sign-in intent for `rememberLauncherForActivityResult`. */
    fun googleSignInIntent(context: Context): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // The Web client id (oauth client_type 3) comes from
            // google-services.json via the google-services plugin.
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options).signInIntent
    }

    /** Exchanges a Google ID token for a Firebase session. */
    fun signInWithGoogleIdToken(idToken: String) {
        _authError.value = null
        FirebaseAuth.getInstance()
            .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .addOnFailureListener { error ->
                _authError.value = error.message ?: "unknown"
            }
    }

    /** Surface a failed Google account pick (e.g. user cancelled / error). */
    fun reportSignInError(message: String?) {
        _authError.value = message ?: "unknown"
    }

    fun clearError() {
        _authError.value = null
    }

    fun signOut(context: Context) {
        _authError.value = null
        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        FirebaseAuth.getInstance().signOut()
    }

    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString(),
    )
}
