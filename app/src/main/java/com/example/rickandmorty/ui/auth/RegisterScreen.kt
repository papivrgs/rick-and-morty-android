package com.example.rickandmorty.ui.auth

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rickandmorty.R
import com.example.rickandmorty.ui.auth.components.AuthErrorMessage
import com.example.rickandmorty.ui.auth.components.AuthPasswordField
import com.example.rickandmorty.ui.auth.components.AuthScreenLayout
import com.example.rickandmorty.ui.auth.components.authFieldErrorText
import com.example.rickandmorty.ui.theme.RickAndMortyAppTheme

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RegisterContent(
        uiState = uiState,
        onUsernameChanged = viewModel::onUsernameChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onPasswordConfirmationChanged = viewModel::onPasswordConfirmationChanged,
        onSubmit = viewModel::submit,
        onBackToLogin = onBackToLogin,
        modifier = modifier,
    )
}

@Composable
fun RegisterContent(
    uiState: RegisterUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordConfirmationChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    AuthScreenLayout(
        title = stringResource(R.string.register_title),
        subtitle = stringResource(R.string.register_subtitle),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.register_form_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.register_form_supporting_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val usernameError = authFieldErrorText(uiState.usernameError)
        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting,
            singleLine = true,
            label = { Text(stringResource(R.string.auth_username)) },
            isError = usernameError != null,
            supportingText = usernameError?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        val passwordError = authFieldErrorText(uiState.passwordError)
        AuthPasswordField(
            value = uiState.password,
            onValueChange = onPasswordChanged,
            label = stringResource(R.string.auth_password),
            showPasswordDescription = stringResource(R.string.auth_show_password),
            hidePasswordDescription = stringResource(R.string.auth_hide_password),
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting,
            errorText = passwordError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
        )
        val confirmationError = authFieldErrorText(uiState.passwordConfirmationError)
        AuthPasswordField(
            value = uiState.passwordConfirmation,
            onValueChange = onPasswordConfirmationChanged,
            label = stringResource(R.string.auth_password_confirmation),
            showPasswordDescription = stringResource(R.string.auth_show_password_confirmation),
            hidePasswordDescription = stringResource(R.string.auth_hide_password_confirmation),
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting,
            errorText = confirmationError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onSubmit()
                },
            ),
        )
        uiState.authError?.let { AuthErrorMessage(it) }
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !uiState.isSubmitting,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    text = stringResource(
                        if (uiState.isSubmitting) R.string.register_loading else R.string.register_action,
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.register_existing_user_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.height(48.dp),
                enabled = !uiState.isSubmitting,
            ) {
                Text(stringResource(R.string.register_back_to_login))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun RegisterPreview() {
    RickAndMortyAppTheme(dynamicColor = false) {
        RegisterContent(
            uiState = RegisterUiState(),
            onUsernameChanged = {},
            onPasswordChanged = {},
            onPasswordConfirmationChanged = {},
            onSubmit = {},
            onBackToLogin = {},
        )
    }
}

@Preview(
    name = "Register dark with validation",
    showBackground = true,
    widthDp = 390,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RegisterDarkValidationPreview() {
    RickAndMortyAppTheme(darkTheme = true, dynamicColor = false) {
        RegisterContent(
            uiState = RegisterUiState(
                username = "rick",
                password = "portal",
                passwordConfirmation = "portal-2",
                passwordError = AuthFieldError.PasswordTooShort,
                passwordConfirmationError = AuthFieldError.PasswordsDoNotMatch,
            ),
            onUsernameChanged = {},
            onPasswordChanged = {},
            onPasswordConfirmationChanged = {},
            onSubmit = {},
            onBackToLogin = {},
        )
    }
}
