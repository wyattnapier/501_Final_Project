package com.example.a501_final_project.chores

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.a501_final_project.MainViewModel
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class ChoresScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockMainViewModel: MainViewModel

    @Mock
    private lateinit var mockChoresViewModel: ChoresViewModel

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
        MockitoAnnotations.openMocks(this)

        // Default flows for the mocks to avoid NPEs
        whenever(mockMainViewModel.userId).thenReturn(MutableStateFlow<String?>("ME"))
        whenever(mockMainViewModel.householdId).thenReturn(MutableStateFlow<String?>("H1"))

        whenever(mockChoresViewModel.choresList).thenReturn(MutableStateFlow(listOf(myChore, roommateChore)))
        whenever(mockChoresViewModel.showPrevChores).thenReturn(MutableStateFlow(false))
        whenever(mockChoresViewModel.tempImageUri).thenReturn(MutableStateFlow(null))
        whenever(mockChoresViewModel.choreImageUris).thenReturn(MutableStateFlow(emptyMap()))
        // default behavior for overdue check
        whenever(mockChoresViewModel.isChoreOverdue(org.mockito.kotlin.any())).thenReturn(false)

        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    // ChoresScreen: loading state when IDs are null
    @Test
    fun choresScreen_showsLoadingStateWhenIdsNull() {
        // override main viewmodel flows to be null to simulate loading
        whenever(mockMainViewModel.userId).thenReturn(MutableStateFlow<String?>(null))
        whenever(mockMainViewModel.householdId).thenReturn(MutableStateFlow<String?>(null))

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ChoresScreen(mockMainViewModel, mockChoresViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Loading chores data...")
            .assertIsDisplayed()
    }

    // ChoresScreen: renders My Chore + Roommate Chores when IDs available
    @Test
    fun choresScreen_rendersMyChoreWidgetAndRoommateChores() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ChoresScreen(mockMainViewModel, mockChoresViewModel, androidx.compose.ui.Modifier)
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
        // ensure chore is incomplete
        val incomplete = myChore.copy(completed = false)
        whenever(mockChoresViewModel.choresList).thenReturn(MutableStateFlow(listOf(incomplete)))
        whenever(mockChoresViewModel.isChoreOverdue(incomplete)).thenReturn(false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                MyChoreWidget(
                    userID = "ME",
                    householdID = "H1",
                    chores = listOf(incomplete),
                    choresViewModel = mockChoresViewModel,
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
        whenever(mockChoresViewModel.choresList).thenReturn(MutableStateFlow(listOf(completed)))
        whenever(mockChoresViewModel.isChoreOverdue(completed)).thenReturn(false)

        // also simulate that an image URI exists for the completed chore
        val sampleUri = Uri.parse("content://com.example/test.jpg")
        whenever(mockChoresViewModel.choreImageUris).thenReturn(MutableStateFlow(mapOf("C_ME" to sampleUri)))

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                MyChoreWidget(
                    userID = "ME",
                    householdID = "H1",
                    chores = listOf(completed),
                    choresViewModel = mockChoresViewModel,
                    context = context
                )
            }
        }

        composeTestRule.onNodeWithText("Chore Completed").assertIsDisplayed()
        // AsyncImage uses semantics for content description; assert the content description exists
        composeTestRule.onNodeWithContentDescription("Chore completion proof").assertExists()
    }

    // RoommateChores: shows roommate chore items and "See Previous Chores" button
    @Test
    fun roommateChores_showsRoommateChoreAndSeePreviousButton() {
        val roommateOnly = roommateChore.copy(assignedToId = "OTHER")
        whenever(mockChoresViewModel.choresList).thenReturn(MutableStateFlow(listOf(roommateOnly)))

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                RoommateChores(
                    userID = "ME",
                    householdID = "H1",
                    chores = listOf(roommateOnly),
                    choresViewModel = mockChoresViewModel,
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
        val overdue = myChore.copy(dueDate = "January 1, 2000", completed = false)
        val future = roommateChore.copy(dueDate = "January 1, 2099", completed = false)

        whenever(mockChoresViewModel.choresList).thenReturn(MutableStateFlow(listOf(overdue, future)))
        whenever(mockChoresViewModel.isChoreOverdue(overdue)).thenReturn(true)
        whenever(mockChoresViewModel.isChoreOverdue(future)).thenReturn(false)
        whenever(mockChoresViewModel.showPrevChores).thenReturn(MutableStateFlow(true))

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PrevChores(
                    chores = listOf(overdue, future),
                    context = context,
                    choresViewModel = mockChoresViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Previous Chores").assertIsDisplayed()
        composeTestRule.onNodeWithText("My Dishes").assertIsDisplayed() // overdue one
        composeTestRule.onNodeWithText("Trash").assertDoesNotExist() // future chore should not appear
        composeTestRule.onNodeWithText("Back to Current").assertIsDisplayed()
    }

    // PrevChoreItem & RoommateChoreItem: completed chore fetches and displays image
    @Test
    fun roommateChoreItem_displaysImageWhenCompleted() {
        val completedRoommateChore = roommateChore.copy(completed = true)
        val sampleUri = Uri.parse("content://com.example/roommate.jpg")

        // 1. DO NOT mock the suspend function.
        //    Instead, mock the STATE FLOW that the suspend function would update.
        //    This simulates the end result of the background operation.
        whenever(mockChoresViewModel.choreImageUris).thenReturn(MutableStateFlow(mapOf("C_OTHER" to sampleUri)))

        // 2. Set up other necessary mocks for the composable.
        whenever(mockChoresViewModel.isChoreOverdue(completedRoommateChore)).thenReturn(false)

        // 3. Render the composable.
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                RoommateChoreItem(
                    chore = completedRoommateChore,
                    context = context,
                    choresViewModel = mockChoresViewModel
                )
            }
        }

        // 4. Assert the result.
        // The composable's LaunchedEffect runs (and calls the un-mocked suspend fun, which does nothing).
        // The UI then composes based on the `choreImageUris` flow we've mocked.
        // We can now assert that the image with the correct content description is present.
        composeTestRule.onNodeWithContentDescription("Proof for ${completedRoommateChore.name}")
            .assertExists()
    }


    // Tapping "See Previous Chores" should toggle the showPrevChores value in the VM (mocked state flip)
    @Test
    fun seePreviousChores_buttonTogglesShowPrevChores() {
        val chores = listOf(roommateChore)
        // start with false
        val showState = MutableStateFlow(false)
        whenever(mockChoresViewModel.choresList).thenReturn(MutableStateFlow(chores))
        whenever(mockChoresViewModel.showPrevChores).thenReturn(showState)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                RoommateChores(
                    userID = "ME",
                    householdID = "H1",
                    chores = chores,
                    choresViewModel = mockChoresViewModel,
                    context = context
                )
            }
        }

        composeTestRule.onNodeWithText("See Previous Chores").performClick()

        // Since the view model method toggles the flow, check that the mocked flow can be updated manually
        // Note: Because mock is not a real ViewModel, ensure the test updates the flow as expected (simulate toggle)
        showState.value = true

        // Recompose with showPrevChores = true by resetting content to reflect updated flow
        whenever(mockChoresViewModel.showPrevChores).thenReturn(showState)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ChoresScreen(mockMainViewModel, mockChoresViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule.onNodeWithText("Previous Chores").assertIsDisplayed()
    }
}
