package com.example.a501_final_project.login_register

import android.content.Context
import com.example.a501_final_project.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HouseholdViewModelTest {

    // Use mock() from mockito-kotlin for cleaner syntax
    private val mockRepository: FirestoreRepository = mock()
    private lateinit var viewModel: HouseholdViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // Set main dispatcher for testing
        whenever(mockRepository.getCurrentUserId()).thenReturn("test-user-id")

        // Create ViewModel with mocked repository
        viewModel = HouseholdViewModel(mockRepository)
        viewModel.loadCurrentUserId()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // Reset main dispatcher
    }

    @Test
    fun test_household_creation_with_mocked_repository() = runTest {
        val context = mock<Context>()
        // Arrange
        val mockHouseholdId = "test-household-123"
        // Use suspend version of whenever for suspend functions
        whenever(mockRepository.createHouseholdSuspend(any()))
            .thenReturn(mockHouseholdId)

        viewModel.updateName("Test Home")
        viewModel.updateCalendarName("Test Calendar")

        // Act
        viewModel.createHousehold(context)

        // Wait for coroutines launched in viewModelScope to complete
        advanceUntilIdle()

        // Assert
        assert(viewModel.householdID == mockHouseholdId)
        assert(viewModel.householdCreated)
        verify(mockRepository).createHouseholdSuspend(any())
        verify(mockRepository).updateUserHouseholdIdSuspend("test-user-id", mockHouseholdId)
    }
}
