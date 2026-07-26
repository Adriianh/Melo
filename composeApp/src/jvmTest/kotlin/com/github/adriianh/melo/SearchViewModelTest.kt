package com.github.adriianh.melo

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.MusicRepository
import com.github.adriianh.melo.ui.search.SearchUiState
import com.github.adriianh.melo.ui.search.SearchViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val musicRepository = mockk<MusicRepository>(relaxed = true)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SearchViewModel(musicRepository)

    @Test
    fun `initial state is empty`() = runTest {
        val vm = createViewModel()
        val state = vm.uiState.value

        assertEquals("", state.query)
        assertTrue(state.results.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `onQueryChange updates query in state`() = runTest {
        val vm = createViewModel()

        vm.onQueryChange("Bad Bunny")

        assertEquals("Bad Bunny", vm.uiState.value.query)
    }

    @Test
    fun `performSearch with blank query does nothing`() = runTest {
        val vm = createViewModel()
        vm.onQueryChange("")

        vm.performSearch()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.results.isEmpty())
    }

    @Test
    fun `performSearch sets loading and then results`() = runTest {
        val tracks = listOf(
            Track("1", "Title 1", "Artist 1", "Album", 180_000, emptyList(), null, null)
        )
        coEvery { musicRepository.search("query") } returns tracks

        val vm = createViewModel()
        vm.onQueryChange("query")
        vm.performSearch()

        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(1, vm.uiState.value.results.size)
        assertEquals("Title 1", vm.uiState.value.results[0].title)
    }

    @Test
    fun `performSearch on error sets error message`() = runTest {
        coEvery { musicRepository.search(any()) } throws RuntimeException("Network error")

        val vm = createViewModel()
        vm.onQueryChange("test")
        vm.performSearch()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Network error", vm.uiState.value.error)
        assertTrue(vm.uiState.value.results.isEmpty())
    }

    @Test
    fun `performSearch clears previous error on success`() = runTest {
        coEvery { musicRepository.search("fail") } throws RuntimeException("fail")
        coEvery { musicRepository.search("ok") } returns emptyList()

        val vm = createViewModel()

        // First search fails
        vm.onQueryChange("fail")
        vm.performSearch()
        advanceUntilIdle()
        assertEquals("fail", vm.uiState.value.error)

        // Second search succeeds
        vm.onQueryChange("ok")
        vm.performSearch()
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `performSearch calls repository with correct query`() = runTest {
        coEvery { musicRepository.search(any()) } returns emptyList()

        val vm = createViewModel()
        vm.onQueryChange("specific query")
        vm.performSearch()
        advanceUntilIdle()

        coVerify { musicRepository.search("specific query") }
    }
}