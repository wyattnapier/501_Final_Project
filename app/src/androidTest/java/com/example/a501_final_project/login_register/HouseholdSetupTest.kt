package com.example.a501_final_project.login_register

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.a501_final_project.FirestoreRepository
import com.example.a501_final_project.ui.theme._501_Final_ProjectTheme
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class HouseholdSetupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockRepository: FirestoreRepository

    @Mock
    private lateinit var mockNavController: NavController

    private lateinit var viewModel: HouseholdViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Mock repository methods
        whenever(mockRepository.getCurrentUserId()).thenReturn("test-user-id")

        // Create ViewModel with mocked repository
        viewModel = HouseholdViewModel(mockRepository)
        viewModel.loadCurrentUserId()
    }

    // NewHouseholdName Tests
    @Test
    fun newHouseholdName_displaysCorrectTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdName(viewModel, androidx.compose.ui.Modifier)
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
                NewHouseholdName(viewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Household Name")
            .assertIsDisplayed()
    }

    @Test
    fun newHouseholdName_showsPlaceholder() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdName(viewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("e.g., Downtown Apartment")
            .assertExists()
    }

    @Test
    fun newHouseholdName_acceptsTextInput() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdName(viewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Household Name")
            .performTextInput("My Test Home")

        assert(viewModel.householdName == "My Test Home")
    }

    @Test
    fun newHouseholdName_showsErrorWhenEmptyAndSubmitted() {
        viewModel.incrementStep() // Trigger validation

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdName(viewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Household name cannot be empty")
            .assertIsDisplayed()
    }

    // NewHouseholdChore Tests
    @Test
    fun newHouseholdChore_displaysCorrectTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdChore(viewModel, androidx.compose.ui.Modifier)
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
                NewHouseholdChore(viewModel, androidx.compose.ui.Modifier)
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
                NewHouseholdChore(viewModel, androidx.compose.ui.Modifier)
            }
        }

        val initialCount = viewModel.choreInputs.size

        composeTestRule
            .onNodeWithText("Add Another Chore")
            .performClick()

        assert(viewModel.choreInputs.size == initialCount + 1)
    }

    @Test
    fun newHouseholdChore_displaysChoreCard() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdChore(viewModel, androidx.compose.ui.Modifier)
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
                    hasAttemptedSubmit = false
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
                    hasAttemptedSubmit = true
                )
            }
        }

        composeTestRule
            .onNodeWithText("Chore name cannot be empty")
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
                    hasAttemptedSubmit = true
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
                    hasAttemptedSubmit = false
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
                NewHouseholdPayment(viewModel, androidx.compose.ui.Modifier)
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
                NewHouseholdPayment(viewModel, androidx.compose.ui.Modifier)
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
                NewHouseholdPayment(viewModel, androidx.compose.ui.Modifier)
            }
        }

        val initialCount = viewModel.paymentInputs.size

        composeTestRule
            .onNodeWithText("Add Another Payment")
            .performClick()

        assert(viewModel.paymentInputs.size == initialCount + 1)
    }

    @Test
    fun paymentSection_hasAllRequiredFields() {
        val testPayment = PaymentInput("", 0, 0, 0, false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PaymentSection(
                    payment = testPayment,
                    onPaymentChanged = {},
                    hasAttemptedSubmit = false
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
                    hasAttemptedSubmit = false
                )
            }
        }

        composeTestRule
            .onNodeWithText("You pay this bill:")
            .assertIsDisplayed()
    }

    @Test
    fun paymentSection_showsErrorsWhenFieldsInvalid() {
        val testPayment = PaymentInput("", 0, 0, 0, false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PaymentSection(
                    payment = testPayment,
                    onPaymentChanged = {},
                    hasAttemptedSubmit = true
                )
            }
        }

        composeTestRule
            .onNodeWithText("Payment name cannot be empty")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Amount must be greater than 0")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Split must be between 1 and 99%")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cycle must be greater than 0")
            .assertIsDisplayed()
    }

    @Test
    fun paymentSection_showsErrorWhenSplitTooHigh() {
        val testPayment = PaymentInput("Rent", 1000, 100, 30, false)

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                PaymentSection(
                    payment = testPayment,
                    onPaymentChanged = {},
                    hasAttemptedSubmit = true
                )
            }
        }

        composeTestRule
            .onNodeWithText("Split must be between 1 and 99%")
            .assertIsDisplayed()
    }

    // NewHouseholdCalendar Tests
    @Test
    fun newHouseholdCalendar_displaysCorrectTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdCalendar(viewModel, androidx.compose.ui.Modifier)
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
                NewHouseholdCalendar(viewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Calendar Name")
            .assertIsDisplayed()
    }

    @Test
    fun newHouseholdCalendar_showsErrorWhenEmpty() {
        viewModel.incrementStep() // Skip to calendar step
        viewModel.incrementStep()
        viewModel.incrementStep()
        viewModel.incrementStep()

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                NewHouseholdCalendar(viewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Calendar name cannot be empty")
            .assertIsDisplayed()
    }

    // ReviewHouseholdDetails Tests
    @Test
    fun reviewHouseholdDetails_displaysTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewHouseholdDetails(viewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("Review Your Household")
            .assertIsDisplayed()
    }

    @Test
    fun reviewHouseholdDetails_showsHouseholdName() {
        viewModel.updateName("Test Household")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewHouseholdDetails(viewModel, androidx.compose.ui.Modifier)
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
        viewModel.choreInputs.clear()

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewHouseholdDetails(viewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("No chores added.")
            .assertIsDisplayed()
    }

    @Test
    fun reviewHouseholdDetails_showsNoPaymentsMessage() {
        viewModel.paymentInputs.clear()

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                ReviewHouseholdDetails(viewModel, androidx.compose.ui.Modifier)
            }
        }

        composeTestRule
            .onNodeWithText("No payments added.")
            .assertIsDisplayed()
    }

    // HouseholdCreated Tests
    @Test
    fun householdCreated_displaysSuccessMessage() {
        viewModel.updateID("12345")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                HouseholdCreated(viewModel, androidx.compose.ui.Modifier, mockNavController)
            }
        }

        composeTestRule
            .onNodeWithText("Household Created!")
            .assertIsDisplayed()
    }

    @Test
    fun householdCreated_displaysHouseholdId() {
        viewModel.updateID("TEST123")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                HouseholdCreated(viewModel, androidx.compose.ui.Modifier, mockNavController)
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
        viewModel.updateID("12345")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                HouseholdCreated(viewModel, androidx.compose.ui.Modifier, mockNavController)
            }
        }

        composeTestRule
            .onNodeWithText("Proceed to App")
            .assertIsDisplayed()
    }

    @Test
    fun householdCreated_navigatesOnProceedClick() {
        viewModel.updateID("12345")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                HouseholdCreated(viewModel, androidx.compose.ui.Modifier, mockNavController)
            }
        }

        composeTestRule
            .onNodeWithText("Proceed to App")
            .performClick()

        verify(mockNavController).navigate("Home")
    }

    // FindHousehold Tests
    @Test
    fun findHousehold_displaysTitle() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                FindHousehold(viewModel, androidx.compose.ui.Modifier, onBack = {})
            }
        }

        composeTestRule
            .onNodeWithText("Join Household")
            .assertIsDisplayed()
    }

    @Test
    fun findHousehold_hasHouseholdIdField() {
        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                FindHousehold(viewModel, androidx.compose.ui.Modifier, onBack = {})
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
                FindHousehold(viewModel, androidx.compose.ui.Modifier, onBack = {})
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
                FindHousehold(viewModel, androidx.compose.ui.Modifier, onBack = {})
            }
        }

        composeTestRule
            .onNodeWithText("Search for Household")
            .assertIsNotEnabled()
    }

    // JoinHousehold Tests
    @Test
    fun joinHousehold_displaysHouseholdName() {
        viewModel.updateName("Test Home")

        composeTestRule.setContent {
            _501_Final_ProjectTheme {
                JoinHousehold(viewModel, androidx.compose.ui.Modifier, onBack = {})
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
                JoinHousehold(viewModel, androidx.compose.ui.Modifier, onBack = {})
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
        assert(viewModel.setupStep == 0)

        // Try to increment without filling name
        viewModel.incrementStep()

        // Should still be on step 0 because validation failed
        assert(viewModel.setupStep == 0)
        assert(viewModel.hasAttemptedSubmit)

        // Fill name and try again
        viewModel.updateName("Test Home")
        viewModel.incrementStep()

        // Should now be on step 1
        assert(viewModel.setupStep == 1)
    }

    @Test
    fun viewModel_decrementStepWorks() {
        viewModel.updateName("Test")
        viewModel.incrementStep()
        assert(viewModel.setupStep == 1)

        viewModel.decrementStep()
        assert(viewModel.setupStep == 0)
    }

    @Test
    fun viewModel_addRemoveChoreWorks() {
        val initialSize = viewModel.choreInputs.size

        viewModel.addChore()
        assert(viewModel.choreInputs.size == initialSize + 1)

        viewModel.removeChore(0)
        assert(viewModel.choreInputs.size == initialSize)
    }

    @Test
    fun viewModel_addRemovePaymentWorks() {
        val initialSize = viewModel.paymentInputs.size

        viewModel.addPayment()
        assert(viewModel.paymentInputs.size == initialSize + 1)

        viewModel.removePayment(0)
        assert(viewModel.paymentInputs.size == initialSize)
    }
}