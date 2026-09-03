package com.yomixhub.android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yomixhub.android.BuildConfig
import com.yomixhub.android.R
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.yomixhub.android.data.GitHubUpdater
import com.yomixhub.android.ui.AuthViewModel
import com.yomixhub.android.ui.YomixIcons
import com.yomixhub.android.ui.components.ActiveSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Settings overlay (opened from the top-bar gear icon):
 *  * "Аккаунт" – Google sign-in (Firebase Auth) with bookmark cloud sync;
 *  * "Проверить обновления" – queries the GitHub releases API
 *    (`Animesash/YomixHub`) and offers the APK of a newer release;
 *  * "Источник каталога" – the catalogue source picker (MangaLib /
 *    RanobeLib / HentaiLib), previously behind the top-bar tune icon.
 */
@Composable
fun SettingsScreen(
    activeSource: ActiveSource,
    onBack: () -> Unit,
    onOpenSourcePicker: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var newRelease by remember { mutableStateOf<GitHubUpdater.Release?>(null) }

    fun checkForUpdates() {
        if (checking) return
        checking = true
        statusMessage = context.getString(R.string.settings_checking)
        newRelease = null
        scope.launch {
            runCatching { GitHubUpdater.fetchLatestRelease() }
                .onSuccess { release ->
                    if (GitHubUpdater.isNewer(release.tag, BuildConfig.VERSION_NAME)) {
                        newRelease = release
                    } else {
                        statusMessage = context.getString(R.string.settings_no_updates)
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    statusMessage = context.getString(R.string.settings_update_error) +
                        ": " + (error.message ?: "")
                }
            checking = false
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(end = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = YomixIcons.ArrowBack,
                    contentDescription = stringResource(R.string.cd_navigate_back),
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            AccountCard()

            SettingsItem(
                title = stringResource(R.string.settings_check_updates),
                subtitle = stringResource(R.string.settings_current_version, BuildConfig.VERSION_NAME),
                onClick = ::checkForUpdates,
                trailing = {
                    if (checking) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )

            statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp, bottom = 12.dp),
                )
            } ?: Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 16.dp),
            )

            SettingsItem(
                title = stringResource(R.string.settings_source_catalog),
                subtitle = activeSourceLabel(activeSource),
                onClick = onOpenSourcePicker,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }

    newRelease?.let { release ->
        UpdateDialog(
            release = release,
            currentVersion = BuildConfig.VERSION_NAME,
            onDownload = {
                openInBrowser(context, release.apkUrl ?: release.pageUrl)
                newRelease = null
            },
            onDismiss = { newRelease = null },
        )
    }
}

/**
 * Google sign-in / account card: offers "Войти через Google" while signed
 * out and the user's identity + sign-out action while signed in. Bookmark
 * cloud sync is active only while signed in.
 */
@Composable
private fun AccountCard() {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel()
    val user by viewModel.currentUser.collectAsState()
    val authError by viewModel.authError.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        GoogleSignIn.getSignedInAccountFromIntent(result.data)
            .addOnSuccessListener { account ->
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogleIdToken(idToken)
                } else {
                    viewModel.reportSignInError("no id token returned")
                }
            }
            .addOnFailureListener { error ->
                // Cancelling the account picker is a user action, not a failure.
                if (error is ApiException &&
                    error.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED
                ) {
                    viewModel.clearAuthError()
                } else {
                    viewModel.reportSignInError(error.message)
                }
            }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.auth_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val signedIn = user
            if (signedIn == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.clearAuthError()
                        signInLauncher.launch(viewModel.googleSignInIntent(context))
                    },
                ) {
                    Text(text = stringResource(R.string.auth_sign_in_google))
                }
                authError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.auth_error_prefix, error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(photoUrl = signedIn.photoUrl, displayName = signedIn.displayName)
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = signedIn.displayName
                                ?: stringResource(R.string.auth_sign_in_google),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = signedIn.email
                                ?: stringResource(R.string.auth_email_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.auth_sync_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { viewModel.signOut(context) }) {
                    Text(text = stringResource(R.string.auth_sign_out))
                }
            }
        }
    }
}

/** 40dp round avatar: the Google photo or a letter fallback. */
@Composable
private fun UserAvatar(photoUrl: String?, displayName: String?) {
    if (photoUrl != null) {
        AsyncImage(
            model = photoUrl,
            contentDescription = stringResource(R.string.cd_user_avatar),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
        )
    } else {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = displayName
                        ?.trim()
                        ?.take(1)
                        ?.uppercase()
                        ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
    )
}

@Composable
private fun activeSourceLabel(source: ActiveSource): String = when (source) {
    ActiveSource.ALL -> stringResource(R.string.source_all) +
        " · " + stringResource(R.string.source_all_subtitle)

    ActiveSource.MANGALIB -> "MangaLib"
    ActiveSource.RANOBELIB -> "RanobeLib"
    ActiveSource.HENTAILIB -> "HentaiLib · " + stringResource(R.string.badge_adult)
}

/** "Доступно обновление" prompt with a browser download of the APK. */
@Composable
private fun UpdateDialog(
    release: GitHubUpdater.Release,
    currentVersion: String,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_available_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.update_available_text,
                        release.tag.trimStart('v', 'V'),
                        currentVersion,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                release.notes?.let { notes ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text(stringResource(R.string.update_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_later))
            }
        },
    )
}

/** Opens the URL in the browser (the APK download happens outside the app). */
private fun openInBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
