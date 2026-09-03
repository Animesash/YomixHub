package com.yomixhub.android.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.yomixhub.android.data.AuthRepository
import com.yomixhub.android.data.AuthUser
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin ViewModel over [AuthRepository]: exposes the signed-in user and
 * sign-in / sign-out actions to the settings screen.
 */
class AuthViewModel : ViewModel() {

    val currentUser: StateFlow<AuthUser?> = AuthRepository.currentUser

    val authError: StateFlow<String?> = AuthRepository.authError

    /** Google sign-in intent to launch via `ActivityResultContracts`. */
    fun googleSignInIntent(context: Context): Intent =
        AuthRepository.googleSignInIntent(context)

    fun signInWithGoogleIdToken(idToken: String) =
        AuthRepository.signInWithGoogleIdToken(idToken)

    fun reportSignInError(message: String?) =
        AuthRepository.reportSignInError(message)

    fun clearAuthError() = AuthRepository.clearError()

    fun signOut(context: Context) = AuthRepository.signOut(context)
}
