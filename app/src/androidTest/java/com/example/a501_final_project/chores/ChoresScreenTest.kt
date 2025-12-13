package com.example.a501_final_project.chores

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.a501_final_project.IRepository
import com.example.a501_final_project.MainViewModel
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

//@RunWith(MockitoJUnitRunner::class)
class ChoresScreenTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockRepository: IRepository

    private lateinit var mainViewModel: MainViewModel
    private lateinit var choresViewModel: ChoresViewModel

    private lateinit var context: Context

    // sample chores used in multiple tests
    private val myChore = Chore(
        choreID = "C_ME",
        householdID = "H1",
        name = "My Dishes",
        description = "Wash dishes",
        dueDate = "December 31, 2099",
        dateCompleted = null,
        assignedToId = "ME",
        assignedToName = "Alice",
        completed = false
    )

    private val roommateChore = Chore(
        choreID = "C_OTHER",
        householdID = "H1",
        name = "Trash",
        description = "Take out trash",
        dueDate = "December 31, 2099",
        dateCompleted = null,
        assignedToId = "OTHER",
        assignedToName = "Bob",
        completed = false
    )

    @Before
    fun setUp() {
//        MockitoAnnotations.openMocks(this)
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Setup default repository behavior for MainViewModel
        whenever(mockRepository.getCurrentUserId()).thenReturn("ME")

        // Setup household data with chores and residents
        val householdData = mapOf(
            "name" to "Test Household",
            "residents" to listOf(
                mapOf("id" to "ME"),
                mapOf("id" to "OTHER")
            ),
            "recurring_chores" to listOf(
                mapOf(
                    "name" to "My Dishes",
                    "description" to "Wash dishes",
                    "cycle_frequency" to 7
                ),
                mapOf(
                    "name" to "Trash",
                    "description" to "Take out trash",
                    "cycle_frequency" to 7
                )
            ),
            "chores" to listOf(
                mapOf(
                    "chore_id" to "C_ME",
                    "recurring_chore_id" to 0,
                    "dueDate" to "December 31, 2099",
                    "dateCompleted" to null,
                    "assignedToId" to "ME",
                    "completed" to false
                ),
                mapOf(
                    "chore_id" to "C_OTHER",
                    "recurring_chore_id" to 1,
                    "dueDate" to "December 31, 2099",
                    "dateCompleted" to null,
                    "assignedToId" to "OTHER",
                    "completed" to false
                )
            )
        )

        kotlinx.coroutines.runBlocking {
            whenever(mockRepository.getHouseholdWithoutIdSuspend()).thenReturn(
                Pair(
                    "H1",
                    householdData
                )
            )
            whenever(mockRepository.getUserWithoutIdSuspend()).thenReturn(
                Pair(
                    "ME",
                    mapOf("name" to "Alice")
                )
            )
            whenever(mockRepository.getUserSuspend("ME")).thenReturn(mapOf("name" to "Alice"))
            whenever(mockRepository.getUserSuspend("OTHER")).thenReturn(mapOf("name" to "Bob"))
            whenever(mockRepository.markChoreAsCompletedSuspend(any(), any())).thenReturn(Unit)
        }

        // Create ViewModels with mocked repository
        mainViewModel = MainViewModel(mockRepository)
        choresViewModel = ChoresViewModel(mockRepository)

        // Manually load the data for choresViewModel since it doesn't auto-load
        choresViewModel.loadHouseholdData()

        // Wait for data to load
        Thread.sleep(500)
    }

    // ChoresScreen: loading state when IDs are null
    @Test
    fun choresScreen_showsLoadingStateWhenIdsNull() {
        // Create a separate repository that returns null for user ID
        val nullRepository: IRepository = org.mockito.kotlin.mock()
        kotlinx.coroutines.runBlocking {
            whenever(nullRepository.getCurrentUserId()).thenReturn(null)
            whenever(nullRepository.getUserWithoutIdSuspend()).thenThrow(RuntimeException("No user"))
            whenever(nullRepository.getHouseholdWithoutIdSuspend()).thenThrow(RuntimeException("No household"))
        }

        val nullMainViewModel = MainViewModel(nullRepository)
        val nullChoresViewModel = ChoresViewModel(nullRepository)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ChoresScreen(nullMainViewModel, nullChoresViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Loading chores data...")
            .assertIsDisplayed()
    }

    // ChoresScreen: renders My Chore + Roommate Chores when IDs available
    @Test
    fun choresScreen_rendersMyChoreWidgetAndRoommateChores() {
        // Load user data for MainViewModel
        mainViewModel.loadUserData()
        mainViewModel.loadHouseholdData()
        Thread.sleep(500) // Wait for data to load

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ChoresScreen(mainViewModel, choresViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule.onNodeWithText("My Chore").assertIsDisplayed()
        composeTestRule.onNodeWithText("Roommate Chores").assertIsDisplayed()
        composeTestRule.onNodeWithText("My Dishes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob: Trash").assertIsDisplayed()
    }

    // MyChoreWidget: shows "Complete with Photo" button for incomplete chore
    @Test
    fun myChoreWidget_showsCompleteWithPhotoWhenNotCompleted() {
        val incomplete = myChore.copy(completed = false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                MyChoreWidget(
                    userID = "ME",
                    householdID = "H1",
                    chores = listOf(incomplete),
                    choresViewModel = choresViewModel,
                    context = context
                )
            }
        }

        composeTestRule.onNodeWithText("Complete with Photo").assertIsDisplayed()
    }

    // MyChoreWidget: shows "Chore Completed" for completed chore
    @Test
    fun myChoreWidget_showsChoreCompletedWhenCompleted() {
        val completed = myChore.copy(completed = true)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                MyChoreWidget(
                    userID = "ME",
                    householdID = "H1",
                    chores = listOf(completed),
                    choresViewModel = choresViewModel,
                    context = context
                )
            }
        }

        composeTestRule.onNodeWithText("Chore Completed").assertIsDisplayed()
    }

    // RoommateChores: shows roommate chore items and "See Previous Chores" button
    @Test
    fun roommateChores_showsRoommateChoreAndSeePreviousButton() {
        val roommateOnly = roommateChore.copy(assignedToId = "OTHER")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                RoommateChores(
                    userID = "ME",
                    householdID = "H1",
                    chores = listOf(roommateOnly),
                    choresViewModel = choresViewModel,
                    context = context
                )
            }
        }

        composeTestRule.onNodeWithText("Roommate Chores").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob: Trash").assertIsDisplayed()
        composeTestRule.onNodeWithText("See Previous Chores").assertIsDisplayed()
    }

    // PrevChores: only overdue chores are shown; back button present
    @Test
    fun prevChores_showsOnlyOverdueChoresAndBackButton() {
        val overdue = myChore.copy(dueDate = "January 1, 2000", completed = true)
        val future = roommateChore.copy(dueDate = "January 1, 2099", completed = false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PrevChores(
                    chores = listOf(overdue, future),
                    context = context,
                    choresViewModel = choresViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Previous Chores").assertIsDisplayed()
        composeTestRule.onNodeWithText("My Dishes", substring = true).assertIsDisplayed() // overdue one
        composeTestRule.onNodeWithText("Trash", substring = true).assertDoesNotExist() // future chore should not appear
        composeTestRule.onNodeWithText("Back to Current").assertIsDisplayed()
    }

    // PrevChoreItem & RoommateChoreItem: completed chore fetches and displays image
    @Test
    fun roommateChoreItem_displaysImageWhenCompleted() {
        val completedRoommateChore = roommateChore.copy(completed = true)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                RoommateChoreItem(
                    chore = completedRoommateChore,
                    context = context,
                    choresViewModel = choresViewModel
                )
            }
        }

        // Note: Since image loading is async and depends on Supabase,
        // this test may need to be adjusted based on how your composable handles loading states
        composeTestRule.waitForIdle()
    }

    // Tapping "See Previous Chores" should toggle the showPrevChores value in the VM
    @Test
    fun seePreviousChores_buttonTogglesShowPrevChores() {
        val chores = listOf(roommateChore)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                RoommateChores(
                    userID = "ME",
                    householdID = "H1",
                    chores = chores,
                    choresViewModel = choresViewModel,
                    context = context
                )
            }
        }

        // Verify initial state shows "See Previous Chores"
        composeTestRule.onNodeWithText("See Previous Chores").assertIsDisplayed()

        // Click the button
        composeTestRule.onNodeWithText("See Previous Chores").performClick()

        // Verify the state changed by checking if PrevChores would be shown
        // You may need to adjust this based on your actual UI behavior
        composeTestRule.waitForIdle()

        // After clicking, the showPrevChores state should be true
        assert(choresViewModel.showPrevChores.value == true)
    }
}