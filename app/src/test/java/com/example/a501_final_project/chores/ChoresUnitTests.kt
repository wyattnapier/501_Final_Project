package com.example.a501_final_project.chores

import com.example.a501_final_project.IRepository
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

        viewModel = ChoresViewModel(mockRepository)
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
            completed = false
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
            completed = false
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
            completed = false
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
            completed = false
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
            completed = false
        )
        viewModel.addChores(chore)

        whenever(mockRepository.markChoreAsCompletedSuspend(any(), any())).thenReturn(Unit)

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
            completed = true
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
            completed = false
        )

        // Act
        val upcomingChores = viewModel.getUpcomingChores(listOf(overdueChore, futureChore))

        // Assert
        assertEquals(1, upcomingChores.size)
        assertEquals("Future", upcomingChores[0].name)
    }

//    @Test
//    fun `assignChores distributes chores evenly among roommates`() {
//        // Arrange
//        val chore1 = Chore("C1", "H1", "Chore 1", null, "Dec 31, 2099", null, "", null, false)
//        val chore2 = Chore("C2", "H1", "Chore 2", null, "Dec 31, 2099", null, "", null, false)
//        val chore3 = Chore("C3", "H1", "Chore 3", null, "Dec 31, 2099", null, "", null, false)
//
//        viewModel.addChores(chore1)
//        viewModel.addChores(chore2)
//        viewModel.addChores(chore3)
//
//        // Mock roommates data
//        val householdData = mapOf(
//            "residents" to listOf(
//                mapOf("id" to "ALICE"),
//                mapOf("id" to "BOB")
//            ),
//            "chores" to emptyList<Map<String, Any>>(),
//            "recurring_chores" to listOf(
//                mapOf("name" to "Take out trash", "cycle" to 7),
//                mapOf("name" to "Clean bathroom", "cycle" to 14),
//                mapOf("name" to "Do dishes", "cycle" to 3)
//            )
//        )
//
//
//        runTest {
//            whenever(mockRepository.getHouseholdWithoutIdSuspend()).thenReturn(
//                Pair(
//                    "H1",
//                    householdData
//                )
//            )
//            whenever(mockRepository.getUserSuspend(any())).thenReturn(mapOf("name" to "Test"))
//
//
//            // Load the roommates
//            viewModel.loadHouseholdData()
//            advanceUntilIdle()
//
//            // Act
//            viewModel.assignChores()
//
//            // Assert
//            val choresList = viewModel.choresList.value
//            // First chore to ALICE, second to BOB, third to ALICE
//            assertEquals("ALICE", choresList[0].assignedToId)
//            assertEquals("BOB", choresList[1].assignedToId)
//            assertEquals("ALICE", choresList[2].assignedToId)
//        }
//    }

    @Test
    fun `assignChores distributes chores evenly among roommates`() {
        // 1. Define and set the private `roommates` list.
        val roommatesList = listOf("ALICE", "BOB")

        val roommates = viewModel::class.java.getDeclaredField("_roommates").apply {
            isAccessible = true
        }.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<List<String>>
        roommates.value = roommatesList


        // 2. Define and set the private `_choresList` StateFlow.
        val recurringChores = listOf(
            RecurringChore(recurringChoreId = "C1", name = "Chore 1", description = "Test", cycleFrequency = 7),
            RecurringChore(recurringChoreId = "C2", name = "Chore 2", description = "Test", cycleFrequency = 7),
            RecurringChore(recurringChoreId = "C3", name = "Chore 3", description = "Test", cycleFrequency = 7)
        )
        // Note: We need to get the private `_choresList` MutableStateFlow
        viewModel.setPrivateProperty("recurringChoresList", recurringChores)

        // Act: Run the function under test.
        viewModel.assignChores()

        // Assert: Verify the distribution logic.
        val resultingChores = viewModel.choresList.value
        assertEquals("ALICE", resultingChores.find { it.choreID == "C1" }?.assignedToId)
        assertEquals("BOB", resultingChores.find { it.choreID == "C2" }?.assignedToId)
        assertEquals("ALICE", resultingChores.find { it.choreID == "C3" }?.assignedToId)
    }


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
            completed = false
        )

        // Act
        viewModel.addChores(newChore)

        // Assert
        assertEquals(initialSize + 1, viewModel.choresList.value.size)
        assertTrue(viewModel.choresList.value.contains(newChore))
    }
}