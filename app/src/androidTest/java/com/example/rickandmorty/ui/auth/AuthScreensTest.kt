package com.example.rickandmorty.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.rickandmorty.ui.theme.RickAndMortyAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginRendersErrorsAndInvokesAccountCreation() {
        var createAccountClicked = false
        composeRule.setContent {
            RickAndMortyAppTheme(dynamicColor = false) {
                LoginContent(
                    uiState = LoginUiState(
                        usernameError = AuthFieldError.Required,
                        passwordError = AuthFieldError.Required,
                        authError = AuthUiError.InvalidCredentials,
                    ),
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onCreateAccount = { createAccountClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Bienvenido").assertIsDisplayed()
        composeRule.onAllNodesWithText("Este campo es obligatorio.", useUnmergedTree = true)
            .assertCountEquals(2)
        composeRule.onNodeWithText("El usuario o la contraseña son incorrectos.")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Mostrar contraseña").performClick()
        composeRule.onNodeWithContentDescription("Ocultar contraseña").assertIsDisplayed()
        composeRule.onNodeWithText("Crear una cuenta").performClick()

        assertTrue(createAccountClicked)
    }

    @Test
    fun registerRendersPasswordRequirementsAndInvokesBack() {
        var backClicked = false
        composeRule.setContent {
            RickAndMortyAppTheme(dynamicColor = false) {
                RegisterContent(
                    uiState = RegisterUiState(
                        passwordError = AuthFieldError.PasswordTooShort,
                        passwordConfirmationError = AuthFieldError.PasswordsDoNotMatch,
                    ),
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onPasswordConfirmationChanged = {},
                    onSubmit = {},
                    onBackToLogin = { backClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Crear cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("La contraseña debe tener al menos 8 caracteres.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Las contraseñas no coinciden.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Mostrar contraseña").performClick()
        composeRule.onNodeWithContentDescription("Ocultar contraseña").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Mostrar confirmación de contraseña").performClick()
        composeRule.onNodeWithContentDescription("Ocultar confirmación de contraseña")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Volver a iniciar sesión")
            .performScrollTo()
            .performClick()

        assertTrue(backClicked)
    }

    @Test
    fun loginShowsAccessibleLoadingStateAndDisablesSubmission() {
        composeRule.setContent {
            RickAndMortyAppTheme(dynamicColor = false) {
                LoginContent(
                    uiState = LoginUiState(isSubmitting = true),
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onCreateAccount = {},
                )
            }
        }

        composeRule.onNodeWithText("Iniciando sesión…")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
}
