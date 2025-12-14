package com.example.a501_final_project.chores

import androidx.core.net.toUri
import com.example.a501_final_project.IRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for ChoresViewModel
 * These run on the JVM without Android dependencies - FAST!
 * Place in: test/java/com/example/a501_final_project/chores/
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChoresViewModelUnitTest {

    @Mock
    private lateinit var mockRepository: IRepository

    private lateinit var viewModel: ChoresViewModel

    private val testDispatcher = StandardTestDispatcher()

    /**
     * Helper function to set a private property on any object using reflection.
     * USE ONLY IN TESTS.
     */
    fun <T> Any.setPrivateProperty(name: String, value: T) {
        val field = this::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }


    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Default stub to prevent crashes
        whenever(mockRepository.getCurrentUserId()).thenReturn("TEST_USER")
//        coEvery { mockRepository.getUserSuspend(any<String>())} returns mapOf("name" to "TEST_USER")
//        coEvery { mockRepository.updateChoreAssignmentsSuspend(any<List<Chore>>())  } just Runs


        viewModel = ChoresViewModel(mockRepository)

        // 1. Define and set the private `roommates` list.
        val roommatesList = listOf("ALICE", "BOB")

        val roommates = viewModel::class.java.getDeclaredField("_roommates").apply {
            isAccessible = true
        }.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<List<String>>
        roommates.value = roommatesList

        // 2. Define and set the private `_choresList` StateFlow.
        val recurringChores = listOf(
            RecurringChore(recurringChoreId = "0", name = "Chore 1", description = "Test", cycleFrequency = 7),
            RecurringChore(recurringChoreId = "1", name = "Chore 2", description = "Test", cycleFrequency = 7),
            RecurringChore(recurringChoreId = "2", name = "Chore 3", description = "Test", cycleFrequency = 7)
        )
        // Note: We need to get the private `_choresList` MutableStateFlow
        viewModel.setPrivateProperty("recurringChoresList", recurringChores)

    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isChoreOverdue returns true for past dates`() {
        // Arrange
        val overdueChore = Chore(
            choreID = "C1",
            householdID = "H1",
            name = "Old Chore",
            description = "Very old",
            dueDate = "January 1, 2000",
            dateCompleted = null,
            assignedToId = "USER1",
            assignedToName = "Alice",
            completed = false,
            instanceOf = "1"
        )

        // Act
        val result = viewModel.isChoreOverdue(overdueChore)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isChoreOverdue returns false for future dates`() {
        // Arrange
        val futureChore = Chore(
            choreID = "C1",
            householdID = "H1",
            name = "Future Chore",
            description = "Not due yet",
            dueDate = "December 31, 2099",
            dateCompleted = null,
            assignedToId = "USER1",
            assignedToName = "Alice",
            completed = false,
            instanceOf = "0"
        )

        // Act
        val result = viewModel.isChoreOverdue(futureChore)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isChoreOverdue returns false for null chore`() {
        // Act
        val result = viewModel.isChoreOverdue(null)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `getChoresFor returns only chores assigned to specific person`() {
        // Arrange
        val aliceChore = Chore(
            choreID = "C1",
            householdID = "H1",
            name = "Alice's Chore",
            description = "Do dishes",
            dueDate = "December 31, 2099",
            dateCompleted = null,
            assignedToId = "ALICE",
            assignedToName = "Alice",
            completed = false,
            instanceOf = "0"
        )
        val bobChore = Chore(
            choreID = "C2",
            householdID = "H1",
            name = "Bob's Chore",
            description = "Take out trash",
            dueDate = "December 31, 2099",
            dateCompleted = null,
            assignedToId = "BOB",
            assignedToName = "Bob",
            completed = false,
            instanceOf = "1"
        )

        viewModel.addChores(aliceChore)
        viewModel.addChores(bobChore)

        // Act
        val aliceChores = viewModel.getChoresFor("ALICE")
        val bobChores = viewModel.getChoresFor("BOB")

        // Assert
        assertEquals(1, aliceChores.size)
        assertEquals("Alice's Chore", aliceChores[0].name)
        assertEquals(1, bobChores.size)
        assertEquals("Bob's Chore", bobChores[0].name)
    }

    @Test
    fun `toggleShowPrevChores switches state from false to true`() {
        // Arrange - initial state is false
        assertFalse(viewModel.showPrevChores.value)

        // Act
        viewModel.toggleShowPrevChores()

        // Assert
        assertTrue(viewModel.showPrevChores.value)
    }

    @Test
    fun `toggleShowPrevChores switches state from true to false`() {
        // Arrange - set to true first
        viewModel.toggleShowPrevChores()
        assertTrue(viewModel.showPrevChores.value)

        // Act
        viewModel.toggleShowPrevChores()

        // Assert
        assertFalse(viewModel.showPrevChores.value)
    }

    @Test
    fun `completeChore calls repository to mark chore as completed`() = runTest {
        // Arrange
        val chore = Chore(
            choreID = "C1",
            householdID = "H1",
            name = "Test Chore",
            description = "Test",
            dueDate = "December 31, 2099",
            dateCompleted = null,
            assignedToId = "USER1",
            assignedToName = "Alice",
            completed = false,
            instanceOf = "1"
        )
        viewModel.addChores(chore)

        whenever(mockRepository.markChoreAsCompletedSuspend(any(), any())).thenReturn(Unit)
        whenever(mockRepository.updateChoreAssignmentsSuspend(any())).thenReturn(Unit)

        // Act
        viewModel.completeChore(chore)
        advanceUntilIdle() // Wait for coroutine to complete

        // Assert
        verify(mockRepository).markChoreAsCompletedSuspend("C1", "H1")

        // Verify the chore is marked as completed in the list
        val completedChore = viewModel.choresList.value.find { it.choreID == "C1" }
        assertTrue(completedChore?.completed == true)
    }


    @Test
    fun `getUpcomingChores filters out overdue and complete chores`() {
        // Arrange
        val overdueChore = Chore(
            choreID = "C1",
            householdID = "H1",
            name = "Overdue",
            description = "Old",
            dueDate = "January 1, 2000",
            dateCompleted = "January 3, 2000",
            assignedToId = "USER1",
            assignedToName = "Alice",
            completed = true,
            instanceOf = "1"
        )
        val futureChore = Chore(
            choreID = "C2",
            householdID = "H1",
            name = "Future",
            description = "New",
            dueDate = "December 31, 2099",
            dateCompleted = null,
            assignedToId = "USER1",
            assignedToName = "Alice",
            completed = false,
            instanceOf = "1"
        )

        // Act
        val upcomingChores = viewModel.getUpcomingChores(listOf(overdueChore, futureChore))

        // Assert
        assertEquals(1, upcomingChores.size)
        assertEquals("Future", upcomingChores[0].name)
    }

//    @Test
//    fun `assignChores distributes chores evenly among roommates`() {
//        runTest {
//            // Act: Run the function under test.
//            viewModel.assignChores()
//        }
//
//        // Assert: Verify the distribution logic.
//        val resultingChores = viewModel.choresList.value
//        assertEquals("ALICE", resultingChores.find { it.choreID == "C1" }?.assignedToId)
//        assertEquals("BOB", resultingChores.find { it.choreID == "C2" }?.assignedToId)
//        assertEquals("ALICE", resultingChores.find { it.choreID == "C3" }?.assignedToId)
//    }


    @Test
    fun `addChores adds chore to the list`() {
        // Arrange
        val initialSize = viewModel.choresList.value.size
        val newChore = Chore(
            choreID = "C_NEW",
            householdID = "H1",
            name = "New Chore",
            description = "Brand new",
            dueDate = "December 31, 2099",
            dateCompleted = null,
            assignedToId = "USER1",
            assignedToName = "Alice",
            completed = false,
            instanceOf = "1"
        )

        // Act
        viewModel.addChores(newChore)

        // Assert
        assertEquals(initialSize + 1, viewModel.choresList.value.size)
        assertTrue(viewModel.choresList.value.contains(newChore))
    }

    @Test
    fun `reset clears all state`() = runTest {

        viewModel.reset()

        assertTrue(viewModel.roommates.value.isEmpty())
        assertTrue(viewModel.choresList.value.isEmpty())
        assertFalse(viewModel.showPrevChores.value)
        assertTrue(viewModel.choreImageUris.value.isEmpty())
        assertNull(viewModel.tempImageUri.value)
        assertFalse(viewModel.isChoresDataLoaded.value)
        assertFalse(viewModel.isLoading.value)
    }

}