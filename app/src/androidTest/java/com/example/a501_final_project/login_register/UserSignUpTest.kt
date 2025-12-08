package com.example.a501_final_project.login_register

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class UserSignUpTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockNavController: NavController

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser

    @Mock
    private lateinit var mockFirestore: FirebaseFirestore

    @Mock
    private lateinit var mockGoogleSignInClient: GoogleSignInClient

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Setup Firebase mocks
        whenever(mockFirebaseAuth.currentUser).thenReturn(null)
    }

    // GetUserInfo Tests (Pure UI - no ViewModel needed)
    @Test
    fun getUserInfo_displaysCorrectTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                GetUserInfo(
                    name = "",
                    onNameChange = {},
                    venmoUsername = "",
                    onVenmoUsernameChange = {},
                    onNext = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Tell Us About Yourself")
            .assertIsDisplayed()
    }

    @Test
    fun getUserInfo_showsErrorWhenNameIsEmpty() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                var name by remember { mutableStateOf("") }
                var venmo by remember { mutableStateOf("testuser") }
                GetUserInfo(
                    name = name,
                    onNameChange = { name = it },
                    venmoUsername = venmo,
                    onVenmoUsernameChange = { venmo = it },
                    onNext = {}
                )
            }
        }

        // Click Next without entering data
        composeTestRule
            .onNodeWithText("Next")
            .performClick()

        // Verify error message appears
        composeTestRule
            .onNodeWithText("Name must be between 1 and 25 characters")
            .assertIsDisplayed()
    }

    @Test
    fun getUserInfo_callsOnNextWhenBothFieldsFilled() {
        var onNextCalled = false

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                var name by remember { mutableStateOf("John Doe") }
                var venmo by remember { mutableStateOf("@johndoe") }
                GetUserInfo(
                    name = name,
                    onNameChange = { name = it },
                    venmoUsername = venmo,
                    onVenmoUsernameChange = { venmo = it },
                    onNext = { onNextCalled = true }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Next")
            .performClick()

        assert(onNextCalled)
    }

    @Test
    fun getUserInfo_nameFieldExistsAndIsEmpty() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                GetUserInfo(
                    name = "",
                    onNameChange = {},
                    venmoUsername = "",
                    onVenmoUsernameChange = {},
                    onNext = {}
                )
            }
        }

        // Find the "Your Name" text field and assert it's displayed and has empty text
        composeTestRule
            .onNodeWithText("Your Name")
            .assertIsDisplayed()
            .assert(hasText(""))
    }

    @Test
    fun getUserInfo_venmoFieldExistsAndIsEmpty() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                GetUserInfo(
                    name = "",
                    onNameChange = {},
                    venmoUsername = "",
                    onVenmoUsernameChange = {},
                    onNext = {}
                )
            }
        }

        // Find the "Venmo Username" text field and assert it's displayed and has empty text
        composeTestRule
            .onNodeWithText("Venmo Username")
            .assertIsDisplayed()
            .assert(hasText(""))
    }

    @Test
    fun getUserInfo_nextButtonIsDisplayedAndEnabled() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                GetUserInfo(
                    name = "test",
                    onNameChange = {},
                    venmoUsername = "test",
                    onVenmoUsernameChange = {},
                    onNext = {}
                )
            }
        }

        // Find the "Next" button and assert it's displayed and enabled
        composeTestRule
            .onNodeWithText("Next")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun getUserInfo_onNameChangeIsCalledWhenTyping() {
        var changedValue = ""
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                GetUserInfo(
                    name = "",
                    onNameChange = { changedValue = it }, // Capture the change
                    venmoUsername = "",
                    onVenmoUsernameChange = {},
                    onNext = {}
                )
            }
        }

        val testInput = "Wyatt"
        // Find the name field and type text into it
        composeTestRule
            .onNodeWithText("Your Name")
            .performTextInput(testInput)

        // Assert that the onNameChange lambda was called with the correct value
        assert(changedValue == testInput)
    }

    @Test
    fun getUserInfo_doesNotShowErrorInitially() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                GetUserInfo(
                    name = "",
                    onNameChange = {},
                    venmoUsername = "",
                    onVenmoUsernameChange = {},
                    onNext = {}
                )
            }
        }

        // Verify that initially, no error messages are displayed
        composeTestRule
            .onNodeWithText("Name must be between 1 and 25 characters")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("Venmo username must be between 1 and 25 characters")
            .assertDoesNotExist()
    }

    // ------ ReviewInfo Tests - These need a mock ViewModel ------

    @Test
    fun reviewInfo_displaysCorrectDataAsText() {
        val realViewModel = LoginViewModel()
        realViewModel.displayName = "Wyatt Napier"
        realViewModel.venmoUsername = "wyatt-n"

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewInfo(realViewModel, mockNavController, onBack = {})
            }
        }

        // Verify the data is displayed as simple text, not in text fields
        composeTestRule.onNodeWithText("Wyatt Napier").assertIsDisplayed()
        composeTestRule.onNodeWithText("wyatt-n").assertIsDisplayed()
        // Ensure there are no editable text fields with these values
        composeTestRule.onNode(hasSetTextAction() and hasText("Wyatt Napier")).assertDoesNotExist()
    }

    @Test
    fun reviewInfo_hasCreateHouseholdButton() {
        // Arrange
        val realViewModel = LoginViewModel()
        realViewModel.displayName = "Test"
        realViewModel.venmoUsername = "@test"

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewInfo(realViewModel, mockNavController, onBack = {})
            }
        }

        // Assert that the "Create New Household" button is displayed and enabled
        composeTestRule
            .onNodeWithText("Create New Household")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun reviewInfo_hasJoinHouseholdButton() {
        // Arrange
        val realViewModel = LoginViewModel()
        realViewModel.displayName = "Test"
        realViewModel.venmoUsername = "@test"

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewInfo(realViewModel, mockNavController, onBack = {})
            }
        }

        // Assert that the "Join Existing Household" button is displayed and enabled
        composeTestRule
            .onNodeWithText("Join Existing Household")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun reviewInfo_hasBackButton() {
        val realViewModel = LoginViewModel()
        realViewModel.displayName = "Test"
        realViewModel.venmoUsername = "@test"

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewInfo(realViewModel, mockNavController, onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Go Back & Edit").assertIsDisplayed()
    }

    @Test
    fun reviewInfo_callsOnBackWhenBackButtonIsClicked() {
        var onBackCalled = false
        val realViewModel = LoginViewModel()
        realViewModel.displayName = "Test"
        realViewModel.venmoUsername = "@test"

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewInfo(
                    loginViewModel = realViewModel,
                    navController = mockNavController,
                    onBack = { onBackCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Go Back & Edit").performClick()

        assert(onBackCalled)
    }
}