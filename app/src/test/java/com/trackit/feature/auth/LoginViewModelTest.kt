package com.trackit.feature.auth

import com.trackit.data.model.User
import com.trackit.data.model.UserRole
import com.trackit.fakes.FakeAuthRepository
import com.trackit.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()

    @Test
    fun login_withBlankCredentials_showsValidationError() = runTest {
        val viewModel = LoginViewModel(authRepository)

        viewModel.login()

        assertEquals("Ingresá email y contraseña.", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.loggedInUser)
    }

    @Test
    fun login_withInvalidCredentials_showsError() = runTest {
        authRepository.loginResult = null
        val viewModel = LoginViewModel(authRepository)
        viewModel.onEmailChange("user@test.com")
        viewModel.onPasswordChange("wrong")

        viewModel.login()

        assertEquals("Credenciales inválidas.", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.loggedInUser)
    }

    @Test
    fun login_withValidCredentials_setsLoggedInUser() = runTest {
        val user = User(
            id = "u1",
            email = "user@test.com",
            displayName = "Usuario Test",
            role = UserRole.WAREHOUSE
        )
        authRepository.loginResult = user
        val viewModel = LoginViewModel(authRepository)
        viewModel.onEmailChange(user.email)
        viewModel.onPasswordChange("secret")

        viewModel.login()

        assertEquals(user, viewModel.uiState.value.loggedInUser)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
