package com.example.a501_final_project.login_register

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import com.example.a501_final_project.IRepository
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class HouseholdSetupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockRepository: IRepository
    @Mock
    private lateinit var mockNavController: NavController

    private lateinit var householdViewModel: HouseholdViewModel
    private lateinit var loginViewModel: LoginViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Mock repository methods
        whenever(mockRepository.getCurrentUserId()).thenReturn("test-user-id")

        // Create ViewModel with mocked repository
        householdViewModel = HouseholdViewModel(mockRepository)
        householdViewModel.loadCurrentUserId()
        loginViewModel = LoginViewModel(mockRepository)
    }

    // NewHouseholdName Tests
    @Test
    fun newHouseholdName_displaysCorrectTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdName(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Name Your Household")
            .assertIsDisplayed()
    }

    @Test
    fun newHouseholdName_hasHouseholdNameField() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdName(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Household Name")
            .assertIsDisplayed()
    }

    @Test
    fun newHouseholdName_acceptsTextInput() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdName(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Household Name")
            .performTextInput("My Test Home")

        assert(householdViewModel.householdName == "My Test Home")
    }

    @Test
    fun newHouseholdName_showsErrorWhenEmptyAndSubmitted() {
        householdViewModel.incrementStep() // Trigger validation

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdName(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Household name must be between 1 and 25 characters")
            .assertIsDisplayed()
    }

    // NewHouseholdChore Tests
    @Test
    fun newHouseholdChore_displaysCorrectTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdChore(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Create Chores")
            .assertIsDisplayed()
    }

    @Test
    fun newHouseholdChore_displaysAddButton() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdChore(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Add Another Chore")
            .assertIsDisplayed()
    }

    @Test
    fun newHouseholdChore_addsChoreWhenButtonClicked() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdChore(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        val initialCount = householdViewModel.choreInputs.size

        composeTestRule
            .onNodeWithText("Add Another Chore")
            .performClick()

        assert(householdViewModel.choreInputs.size == initialCount + 1)
    }

    @Test
    fun newHouseholdChore_displaysChoreCard() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdChore(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Chore 1")
            .assertIsDisplayed()
    }

    @Test
    fun choreSection_hasAllRequiredFields() {
        val testChore = ChoreInput("", "", 0)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ChoreSection(
                    chore = testChore,
                    onChoreChanged = {},
                    hasAttemptedSubmit = false,
                    viewModel = householdViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithText("Chore Name")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Description (Optional)")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cycle (days)")
            .assertIsDisplayed()
    }

    @Test
    fun choreSection_showsErrorWhenNameEmpty() {
        val testChore = ChoreInput("", "", 0)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ChoreSection(
                    chore = testChore,
                    onChoreChanged = {},
                    hasAttemptedSubmit = true,
                    viewModel = householdViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithText("Chore name must be between 1 and 25 characters")
            .assertIsDisplayed()
    }

    @Test
    fun choreSection_showsErrorWhenCycleInvalid() {
        val testChore = ChoreInput("Dishes", "Wash dishes", 0)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ChoreSection(
                    chore = testChore,
                    onChoreChanged = {},
                    hasAttemptedSubmit = true,
                    viewModel = householdViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithText("Cycle must be greater than 0")
            .assertIsDisplayed()
    }

    @Test
    fun choreSection_updatesChoreOnInput() {
        var updatedChore: ChoreInput? = null
        val testChore = ChoreInput("", "", 0)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ChoreSection(
                    chore = testChore,
                    onChoreChanged = { updatedChore = it },
                    hasAttemptedSubmit = false,
                    viewModel = householdViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithText("Chore Name")
            .performTextInput("Test Chore")

        assert(updatedChore?.name == "Test Chore")
    }

    // NewHouseholdPayment Tests
    @Test
    fun newHouseholdPayment_displaysCorrectTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdPayment(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Recurring Payments")
            .assertIsDisplayed()
    }

    @Test
    fun newHouseholdPayment_displaysAddButton() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdPayment(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Add Another Payment")
            .assertIsDisplayed()
    }

    @Test
    fun newHouseholdPayment_addsPaymentWhenButtonClicked() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdPayment(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        val initialCount = householdViewModel.paymentInputs.size

        composeTestRule
            .onNodeWithText("Add Another Payment")
            .performClick()

        assert(householdViewModel.paymentInputs.size == initialCount + 1)
    }

    @Test
    fun paymentSection_hasAllRequiredFields() {
        val testPayment = PaymentInput("", 0, 0, 0, false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PaymentSection(
                    payment = testPayment,
                    onPaymentChanged = {},
                    hasAttemptedSubmit = false,
                    viewModel = householdViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithText("Payment Name")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Amount ($)")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Your Split (%)")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cycle (days)")
            .assertIsDisplayed()
    }

    @Test
    fun paymentSection_hasToggleSwitchForYouPay() {
        val testPayment = PaymentInput("Rent", 1000, 50, 30, false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PaymentSection(
                    payment = testPayment,
                    onPaymentChanged = {},
                    hasAttemptedSubmit = false,
                    viewModel = householdViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithText("You pay this bill:")
            .assertIsDisplayed()
    }

    @Test
    fun paymentSection_showsErrorsWhenFieldsInvalid() {
        val testPayment = PaymentInput("", 0, 101, 0, false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PaymentSection(
                    payment = testPayment,
                    onPaymentChanged = {},
                    hasAttemptedSubmit = true,
                    viewModel = householdViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithText("Payment name must be between 1 and 25 characters")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Amount must be greater than 0")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Split must be between 0 and 100%")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cycle must be greater than 0")
            .assertIsDisplayed()
    }

    @Test
    fun paymentSection_showsErrorWhenSplitTooHigh() {
        val testPayment = PaymentInput("Rent", 1000, 101, 30, false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PaymentSection(
                    payment = testPayment,
                    onPaymentChanged = {},
                    hasAttemptedSubmit = true,
                    viewModel = householdViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithText("Split must be between 0 and 100%")
            .assertIsDisplayed()
    }

    // NewHouseholdCalendar Tests
    @Test
    fun newHouseholdCalendar_displaysCorrectTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdCalendar(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Shared Calendar")
            .assertIsDisplayed()
    }

    @Test
    fun newHouseholdCalendar_hasCalendarNameField() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdCalendar(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Calendar Name")
            .assertIsDisplayed()
    }

    @Test
    fun newHouseholdCalendar_showsErrorWhenEmpty() {
        householdViewModel.updateName("test")
        householdViewModel.choreInputs[0] = ChoreInput("test", "test", 1)
        householdViewModel.paymentInputs[0] = PaymentInput("test", 1, 1, 1, false)
        householdViewModel.incrementStep() // -> 1
        householdViewModel.incrementStep() // -> 2
        householdViewModel.incrementStep() // -> 3
        householdViewModel.incrementStep() // -> 4 (triggers validation for step 3)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdCalendar(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Calendar name must be between 1 and 25 characters")
            .assertIsDisplayed()
    }

    // ReviewHouseholdDetails Tests
    @Test
    fun reviewHouseholdDetails_displaysTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewHouseholdDetails(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Review Your Household")
            .assertIsDisplayed()
    }

    @Test
    fun reviewHouseholdDetails_showsHouseholdName() {
        householdViewModel.updateName("Test Household")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewHouseholdDetails(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Household Name")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Test Household")
            .assertIsDisplayed()
    }

    @Test
    fun reviewHouseholdDetails_showsNoChoresMessage() {
        householdViewModel.choreInputs.clear()

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewHouseholdDetails(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("No chores added.")
            .assertIsDisplayed()
    }

    @Test
    fun reviewHouseholdDetails_showsNoPaymentsMessage() {
        householdViewModel.paymentInputs.clear()

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewHouseholdDetails(householdViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("No payments added.")
            .assertIsDisplayed()
    }

    // HouseholdCreated Tests
    @Test
    fun householdCreated_displaysSuccessMessage() {
        householdViewModel.updateID("12345")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                HouseholdCreated(householdViewModel, loginViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Household Created!")
            .assertIsDisplayed()
    }

    @Test
    fun householdCreated_displaysHouseholdId() {
        householdViewModel.updateID("TEST123")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                HouseholdCreated(householdViewModel, loginViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Household ID")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("TEST123")
            .assertIsDisplayed()
    }

    @Test
    fun householdCreated_hasProceedButton() {
        householdViewModel.updateID("12345")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                HouseholdCreated(householdViewModel, loginViewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Proceed to App")
            .assertIsDisplayed()
    }

    // FindHousehold Tests
    @Test
    fun findHousehold_displaysTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                FindHousehold(householdViewModel, onBack = {})
            }
        }

        composeTestRule
            .onNodeWithText("Join a Household")
            .assertIsDisplayed()
    }

    @Test
    fun findHousehold_hasHouseholdIdField() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                FindHousehold(householdViewModel, onBack = {})
            }
        }

        composeTestRule
            .onNodeWithText("Household ID")
            .assertIsDisplayed()
    }

    @Test
    fun findHousehold_hasSearchButton() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                FindHousehold(householdViewModel, onBack = {})
            }
        }

        composeTestRule
            .onNodeWithText("Search for Household")
            .assertIsDisplayed()
    }

    @Test
    fun findHousehold_searchButtonDisabledWhenEmpty() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                FindHousehold(householdViewModel, onBack = {})
            }
        }

        composeTestRule
            .onNodeWithText("Search for Household")
            .assertIsNotEnabled()
    }

    // JoinHousehold Tests
    @Test
    fun joinHousehold_displaysHouseholdName() {
        householdViewModel.updateName("Test Home")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                JoinHousehold(householdViewModel, onBack = {})
            }
        }

        composeTestRule
            .onNodeWithText("Test Home")
            .assertIsDisplayed()
    }

    @Test
    fun joinHousehold_hasConfirmButton() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                JoinHousehold(householdViewModel, onBack = {})
            }
        }

        composeTestRule
            .onNodeWithText("Confirm & Join Household")
            .assertIsDisplayed()
    }

    // PaymentItem Tests
    @Test
    fun paymentItem_displaysPaymentDetails() {
        val payment = PaymentDB("Rent", 1000, 30, "", 30, 50, false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PaymentItem(payment = payment, onPaymentChanged = {})
            }
        }

        composeTestRule
            .onNodeWithText("Rent")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Occupied split: 30%")
            .assertIsDisplayed()
    }

    @Test
    fun paymentItem_showsErrorWhenSplitExceeds100() {
        val payment = PaymentDB("Rent", 1000, 30, "", 50, 60, false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PaymentItem(payment = payment, onPaymentChanged = {})
            }
        }

        composeTestRule
            .onNodeWithText("Total split exceeds 100%")
            .assertIsDisplayed()
    }

    @Test
    fun paymentItem_showsToggleWhenNoPayer() {
        val payment = PaymentDB("Rent", 1000, 30, "", 30, 50, false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PaymentItem(payment = payment, onPaymentChanged = {})
            }
        }

        composeTestRule
            .onNodeWithText("You pay this bill:")
            .assertIsDisplayed()
    }

    @Test
    fun paymentItem_hidesToggleWhenPayerExists() {
        val payment = PaymentDB("Rent", 1000, 30, "user123", 30, 50, false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PaymentItem(payment = payment, onPaymentChanged = {})
            }
        }

        composeTestRule
            .onNodeWithText("You pay this bill:")
            .assertDoesNotExist()
    }

    // ViewModel Logic Tests
    @Test
    fun viewModel_incrementStepValidatesInput() {
        assert(householdViewModel.setupStep == 0)

        // Try to increment without filling name
        householdViewModel.incrementStep()

        // Should still be on step 0 because validation failed
        assert(householdViewModel.setupStep == 0)
        assert(householdViewModel.hasAttemptedSubmit)

        // Fill name and try again
        householdViewModel.updateName("Test Home")
        householdViewModel.incrementStep()

        // Should now be on step 1
        assert(householdViewModel.setupStep == 1)
    }

    @Test
    fun viewModel_decrementStepWorks() {
        householdViewModel.updateName("Test")
        householdViewModel.choreInputs[0] = ChoreInput("test", "", 1)
        householdViewModel.incrementStep()
        assert(householdViewModel.setupStep == 1)

        householdViewModel.decrementStep()
        assert(householdViewModel.setupStep == 0)
    }

    @Test
    fun viewModel_addRemoveChoreWorks() {
        val initialSize = householdViewModel.choreInputs.size

        householdViewModel.addChore()
        assert(householdViewModel.choreInputs.size == initialSize + 1)

        householdViewModel.removeChore(0)
        assert(householdViewModel.choreInputs.size == initialSize)
    }

    @Test
    fun viewModel_addRemovePaymentWorks() {
        val initialSize = householdViewModel.paymentInputs.size

        householdViewModel.addPayment()
        assert(householdViewModel.paymentInputs.size == initialSize + 1)

        householdViewModel.removePayment(0)
        assert(householdViewModel.paymentInputs.size == initialSize)
    }
}