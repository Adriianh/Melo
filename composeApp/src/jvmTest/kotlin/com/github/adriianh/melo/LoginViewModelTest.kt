package com.github.adriianh.melo

import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.usecase.login.SetSessionCookiesUseCase
import com.github.adriianh.core.domain.usecase.login.VerifySessionUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.melo.ui.login.LoginViewModel
import com.github.adriianh.melo.ui.login.importExistingBrowserCookies
import com.github.adriianh.melo.ui.login.launchAutomatedBrowserLogin
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getSettings = mockk<GetSettingsUseCase>(relaxed = true)
    private val updateSettings = mockk<UpdateSettingsUseCase>(relaxed = true)
    private val setSessionCookies = mockk<SetSessionCookiesUseCase>(relaxed = true)
    private val verifySession = mockk<VerifySessionUseCase>(relaxed = true)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getSettings.getSnapshot() } returns Settings()
        mockkStatic("com.github.adriianh.melo.ui.login.InAppSignIn_jvmKt")
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm() = LoginViewModel(
        getSettings,
        updateSettings,
        setSessionCookies,
        verifySession
    )

    @Test
    fun `init marks logged in when stored cookies exist`() = runTest {
        coEvery { getSettings.getSnapshot() } returns Settings(sessionCookies = "SAPISID=x")

        val vm = createVm()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isLoggedIn)
    }

    @Test
    fun `init stays logged out without stored cookies`() = runTest {
        val vm = createVm()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoggedIn)
    }

    @Test
    fun `saveSessionCookies stores session and marks logged in on success`() = runTest {
        coEvery { verifySession() } returns "Adrian"

        val vm = createVm()
        advanceUntilIdle()
        vm.saveSessionCookies("\tSAPISID=x\t")
        advanceUntilIdle()

        verify { setSessionCookies("SAPISID=x") }
        assertTrue(vm.uiState.value.isLoggedIn)
        assertEquals("Adrian", vm.uiState.value.accountName)
        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isVerifying)
    }

    @Test
    fun `saveSessionCookies surfaces error on invalid cookies`() = runTest {
        coEvery { verifySession() } returns null

        val vm = createVm()
        advanceUntilIdle()
        vm.saveSessionCookies("SAPISID=bad")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoggedIn)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `saveSessionCookies ignores blank input`() = runTest {
        val vm = createVm()
        advanceUntilIdle()
        vm.saveSessionCookies("   ")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isVerifying)
        assertFalse(vm.uiState.value.isLoggedIn)
        verify(exactly = 0) { setSessionCookies(any()) }
    }

    @Test
    fun `automated browser login waits for capture and saves cookies`() = runTest {
        coEvery { verifySession() } returns "Adrian"
        coEvery { launchAutomatedBrowserLogin() } returns "SAPISID=captured"

        val vm = createVm()
        advanceUntilIdle()
        vm.startAutomatedBrowserLogin()

        assertTrue(vm.uiState.value.isWaitingBrowserAuth)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isWaitingBrowserAuth)
        verify { setSessionCookies("SAPISID=captured") }
        assertTrue(vm.uiState.value.isLoggedIn)
    }

    @Test
    fun `automated browser login exits waiting state when nothing captured`() = runTest {
        coEvery { launchAutomatedBrowserLogin() } returns null

        val vm = createVm()
        advanceUntilIdle()
        vm.startAutomatedBrowserLogin()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isWaitingBrowserAuth)
        assertFalse(vm.uiState.value.isLoggedIn)
    }

    @Test
    fun `quick import falls back to automated browser login`() = runTest {
        coEvery { verifySession() } returns "Adrian"
        coEvery { importExistingBrowserCookies() } returns null
        coEvery { launchAutomatedBrowserLogin() } returns "SAPISID=auto"

        val vm = createVm()
        advanceUntilIdle()
        vm.startQuickImport()
        advanceUntilIdle()

        coVerify { importExistingBrowserCookies() }
        coVerify { launchAutomatedBrowserLogin() }
        assertFalse(vm.uiState.value.isWaitingBrowserAuth)
        assertTrue(vm.uiState.value.isLoggedIn)
    }

    @Test
    fun `cancel waiting browser auth clears waiting state`() = runTest {
        val vm = createVm()
        advanceUntilIdle()
        vm.startAutomatedBrowserLogin()
        assertTrue(vm.uiState.value.isWaitingBrowserAuth)

        vm.cancelWaitingBrowserAuth()

        assertFalse(vm.uiState.value.isWaitingBrowserAuth)
    }

    @Test
    fun `logout clears session`() = runTest {
        coEvery { getSettings.getSnapshot() } returns Settings(sessionCookies = "SAPISID=x")

        val vm = createVm()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLoggedIn)

        vm.logout()
        advanceUntilIdle()

        verify { setSessionCookies(null) }
        assertFalse(vm.uiState.value.isLoggedIn)
        assertNull(vm.uiState.value.accountName)
    }
}