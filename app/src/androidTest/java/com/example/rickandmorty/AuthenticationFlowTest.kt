package com.example.rickandmorty

import android.content.Context
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationFlowTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var username: String
    private val password = "password-123"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("rick_and_morty.db")
        File(context.filesDir, "datastore/session.preferences_pb").delete()
        username = "user-${System.nanoTime()}"
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun registerPersistsSessionThenLogoutAndLogin() {
        composeRule.waitForText("Bienvenido")
        composeRule.onNodeWithText("Crear una cuenta").performClick()
        composeRule.waitForText("Crear cuenta")

        composeRule.textField("Nombre de usuario").performTextInput(username)
        composeRule.textField("Contraseña").performTextInput(password)
        composeRule.textField("Confirmar contraseña").performTextInput(password)
        composeRule.onNodeWithText("Registrarme").performClick()

        composeRule.waitForText("Rick and Morty")
        scenario.recreate()
        composeRule.waitForText("Rick and Morty")

        composeRule.onNodeWithText("Cerrar sesión").performClick()
        composeRule.waitForText("Bienvenido")

        composeRule.textField("Nombre de usuario")
            .performTextInput(username.uppercase(Locale.ROOT))
        composeRule.textField("Contraseña").performTextInput(password)
        composeRule.onNodeWithText("Iniciar sesión").performClick()

        composeRule.waitForText("Rick and Morty")
        composeRule.onNodeWithText("Cerrar sesión").performClick()
        composeRule.waitForText("Bienvenido")
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.textField(label: String) =
        onNode(hasText(label) and hasSetTextAction())

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.waitForText(text: String) {
        waitUntil(timeoutMillis = 60_000) {
            runCatching {
                onNodeWithText(text).fetchSemanticsNode()
            }.isSuccess
        }
    }
}
