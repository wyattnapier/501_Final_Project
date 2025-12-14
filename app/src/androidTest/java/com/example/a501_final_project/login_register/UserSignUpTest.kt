package com.example.a501_final_project.login_register

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import androidx.test.platform.app.InstrumentationRegistry
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class UserSignUpTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockNavController: NavController

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
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
                    onNext = {},
                    onBack = {}
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
                    onNext = {},
                    onBack = {}
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
    fun getUserInfo_showsErrorWhenNameExceedsMaxLength() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                var name by remember { mutableStateOf("I am way too long to be a name especially for this little household app") }
                var venmo by remember { mutableStateOf("testuser") }
                GetUserInfo(
                    name = name,
                    onNameChange = { name = it },
                    venmoUsername = venmo,
                    onVenmoUsernameChange = { venmo = it },
                    onNext = {},
                    onBack = {}
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
    fun getUserInfo_showsErrorWhenVenmoIsEmpty() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                var name by remember { mutableStateOf("testuser") }
                var venmo by remember { mutableStateOf("") }
                GetUserInfo(
                    name = name,
                    onNameChange = { name = it },
                    venmoUsername = venmo,
                    onVenmoUsernameChange = { venmo = it },
                    onNext = {},
                    onBack = {}
                )
            }
        }

        // Click Next without entering data
        composeTestRule
            .onNodeWithText("Next")
            .performClick()

        // Verify error message appears
        composeTestRule
            .onNodeWithText("Venmo username must be between 5 and 30 characters")
            .assertIsDisplayed()
    }

    @Test
    fun getUserInfo_showsErrorWhenVenmoExceedsMaxLength() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                var name by remember { mutableStateOf("testuser") }
                var venmo by remember { mutableStateOf("This is way too long to be a venmo username, unless they're trying to do something bad") }
                GetUserInfo(
                    name = name,
                    onNameChange = { name = it },
                    venmoUsername = venmo,
                    onVenmoUsernameChange = { venmo = it },
                    onNext = {},
                    onBack = {}
                )
            }
        }

        // Click Next without entering data
        composeTestRule
            .onNodeWithText("Next")
            .performClick()

        // Verify error message appears
        composeTestRule
            .onNodeWithText("Venmo username must be between 5 and 30 characters")
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
                    onNext = { onNextCalled = true },
                    onBack = {}
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
                    onNext = {},
                    onBack = {}
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
                    onNext = {},
                    onBack = {}
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
                    onNext = {},
                    onBack = {}
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
                    onNext = {},
                    onBack = {}
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
                    onNext = {},
                    onBack = {}
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

    @Test
    fun backButton_fromReviewToGetUserInfo_preservesState() {
        // 1. Hoist the state for the test, just like SignUpScreen does for the real app.
        var name by mutableStateOf("")
        var venmo by mutableStateOf("")
        var currentStep by mutableStateOf(SignUpSteps.USER_INFO) // Start at the USER_INFO step

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                // 2. Use a 'when' block to simulate the navigation controlled by the test.
                when (currentStep) {
                    SignUpSteps.USER_INFO -> {
                        GetUserInfo(
                            name = name,
                            onNameChange = { name = it },
                            venmoUsername = venmo,
                            onVenmoUsernameChange = { venmo = it },
                            onNext = {
                                // When "Next" is clicked, change the step to REVIEW
                                currentStep = SignUpSteps.REVIEW
                            },
                            onBack = {}
                        )
                    }
                    SignUpSteps.REVIEW -> {
                        // Create a dummy ViewModel for the preview
                        val reviewViewModel = LoginViewModel().apply {
                            displayName = name
                            venmoUsername = venmo
                        }
                        ReviewInfo(
                            loginViewModel = reviewViewModel,
                            navController = mockNavController,
                            onBack = {
                                // When "Go Back" is clicked, change the step back to USER_INFO
                                currentStep = SignUpSteps.USER_INFO
                            }
                        )
                    }
                    else -> {
                        // Other steps are not relevant for this test
                    }
                }
            }
        }

        // --- Step 1: Simulate user entering data on the GetUserInfo screen ---
        val testName = "Test User"
        val testVenmo = "testing123"
        // Enter text in GetUserInfo. This is now guaranteed to be on the screen.
        composeTestRule.onNodeWithText("Your Name").performTextInput(testName)
        composeTestRule.onNodeWithText("Venmo Username").performTextInput(testVenmo)
        // Click "Next" to navigate to ReviewInfo
        composeTestRule.onNodeWithText("Next").performClick()

        // --- Step 2: Verify we are on the ReviewInfo screen and data is present ---
        composeTestRule.onNodeWithText("Review Your Info").assertIsDisplayed()
        composeTestRule.onNodeWithText(testName).assertIsDisplayed()
        composeTestRule.onNodeWithText(testVenmo).assertIsDisplayed()

        // --- Step 3: Click the back button on the ReviewInfo screen ---
        composeTestRule.onNodeWithText("Go Back & Edit").performClick()

        // --- Step 4: Verify we are back on GetUserInfo and the data is still there ---
        composeTestRule.onNodeWithText("Tell Us About Yourself").assertIsDisplayed()
        // Check that the text fields still contain the original input.
        composeTestRule.onNodeWithText("Your Name").assert(hasText(testName))
        composeTestRule.onNodeWithText("Venmo Username").assert(hasText(testVenmo))
    }
}