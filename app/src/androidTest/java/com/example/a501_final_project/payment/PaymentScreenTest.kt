package com.example.a501_final_project.payment

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.test.platform.app.InstrumentationRegistry
import com.example.a501_final_project.IRepository
import com.example.a501_final_project.MainViewModel
import com.example.a501_final_project.models.LocalResident
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

class PaymentScreenTest {
    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockRepository: IRepository

    private lateinit var mainViewModel: MainViewModel
    private lateinit var paymentViewModel: PaymentViewModel

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        mainViewModel = MainViewModel(mockRepository)
        paymentViewModel = PaymentViewModel(mockRepository)

        val roommatesList = listOf(
            LocalResident(id = "USER1", name = "Alice", venmoUsername = "alice123"),
            LocalResident(id = "USER2", name = "Bob", venmoUsername = "bob123")
        )

        // Set the residents as LocalResident objects
        val roommates = mainViewModel::class.java.getDeclaredField("_residents").apply {
            isAccessible = true
        }.get(mainViewModel) as MutableStateFlow<List<LocalResident>>
        roommates.value = roommatesList

        val userId = mainViewModel::class.java.getDeclaredField("_userId").apply {
            isAccessible = true
        }.get(mainViewModel) as MutableStateFlow<String?>
        userId.value = "TEST"
    }

    @Test
    fun venmoScreen_showsTitle() {
        composeTestRule.setContent {
            VenmoPaymentScreen(
                paymentViewModel = paymentViewModel,
                mainViewModel = mainViewModel
            )
        }

        composeTestRule.onNodeWithText("Pay with Venmo")
            .assertIsDisplayed()
    }

    @Test
    fun addPaymentButton_enabledWhenMultipleResidents() {
        composeTestRule.setContent {
            VenmoPaymentScreen(
                paymentViewModel = paymentViewModel,
                mainViewModel = mainViewModel
            )
        }

        composeTestRule.onNodeWithContentDescription("Add Payment")
            .assertIsEnabled()
    }

    @Test
    fun unpaidPayment_showsPayViaVenmoButton() {
        val payment = Payment(
            id = "1",
            payFromId = "TEST",
            payFromName = "Alice",
            payToId = "USER2",
            payToName = "Bob",
            payToVenmoUsername = "bob123",
            amount = 25.0,
            memo = "Groceries",
            dueDate = null,
            datePaid = null,
            paid = false,
            recurring = false
        )

        paymentViewModel.reset()
        val paymentsFlow = paymentViewModel::class.java.getDeclaredField("_paymentsList").apply {
            isAccessible = true
        }.get(paymentViewModel) as MutableStateFlow<List<Payment>>
        paymentsFlow.value = listOf(payment)

        composeTestRule.setContent {
            VenmoPaymentScreen(paymentViewModel = paymentViewModel, mainViewModel = mainViewModel)
        }

        composeTestRule.onNodeWithText("Pay via Venmo")
            .assertIsDisplayed()
    }

    @Test
    fun paidPayment_rendersPaidText() {
        val payment = Payment(
            id = "1",
            payFromId = "TEST",
            payFromName = "Alice",
            payToId = "USER2",
            payToName = "Bob",
            payToVenmoUsername = "bob123",
            amount = 25.0,
            memo = "Groceries",
            dueDate = null,
            datePaid = null,
            paid = true,
            recurring = false
        )

        paymentViewModel.reset()
        // Use reflection to get the private _pastPayments MutableStateFlow and set its value
        val pastPaymentsFlow = paymentViewModel::class.java.getDeclaredField("_pastPayments").apply {
            isAccessible = true
        }.get(paymentViewModel) as MutableStateFlow<List<Payment>>
        pastPaymentsFlow.value = listOf(payment)
        paymentViewModel.toggleShowPastPayments()


        composeTestRule.setContent {
            VenmoPaymentScreen(paymentViewModel = paymentViewModel, mainViewModel = mainViewModel)
        }

        composeTestRule.onNodeWithText("Paid: Yes")
            .assertExists()
    }


    @Test
    fun addPaymentButton_opensDialog() {
        composeTestRule.setContent {
            VenmoPaymentScreen(
                paymentViewModel = paymentViewModel,
                mainViewModel = mainViewModel
            )
        }

        // Click the add payment button
        composeTestRule.onNodeWithContentDescription("Add Payment")
            .performClick()

        // Verify dialog appears
        composeTestRule.onNodeWithText("Add New Payment Task")
            .assertIsDisplayed()
    }

    @Test
    fun addPaymentDialog_showsAllFields() {
        composeTestRule.setContent {
            VenmoPaymentScreen(
                paymentViewModel = paymentViewModel,
                mainViewModel = mainViewModel
            )
        }

        // Open the dialog
        composeTestRule.onNodeWithContentDescription("Add Payment")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify all expected fields are present
        composeTestRule.onAllNodesWithText("Pay To").assertCountEquals(2)
        composeTestRule.onNodeWithText("Request From").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select a person").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amount").assertIsDisplayed()
        composeTestRule.onNodeWithText("Memo (e.g., Groceries)").assertIsDisplayed()
        composeTestRule.onNodeWithText("No due date set").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun addPaymentDialog_cancelButtonDismissesDialog() {
        composeTestRule.setContent {
            VenmoPaymentScreen(
                paymentViewModel = paymentViewModel,
                mainViewModel = mainViewModel
            )
        }

        // Open the dialog
        composeTestRule.onNodeWithContentDescription("Add Payment")
            .performClick()

        composeTestRule.waitForIdle()

        // Click cancel
        composeTestRule.onNodeWithText("Cancel")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify dialog is dismissed
        composeTestRule.onNodeWithText("Add New Payment Task")
            .assertDoesNotExist()
    }

    @Test
    fun addPaymentDialog_addButtonDisabledInitially() {
        composeTestRule.setContent {
            VenmoPaymentScreen(
                paymentViewModel = paymentViewModel,
                mainViewModel = mainViewModel
            )
        }

        // Open the dialog
        composeTestRule.onNodeWithContentDescription("Add Payment")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify Add button is disabled initially
        composeTestRule.onNodeWithText("Add")
            .assertIsNotEnabled()
    }

    @Test
    fun addPaymentDialog_fillFormAndSubmit() {
        composeTestRule.setContent {
            VenmoPaymentScreen(
                paymentViewModel = paymentViewModel,
                mainViewModel = mainViewModel
            )
        }

        // Open the dialog
        composeTestRule.onNodeWithContentDescription("Add Payment")
            .performClick()

        composeTestRule.waitForIdle()

        // Select a person from dropdown
        composeTestRule.onNodeWithText("Select a person")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Bob")
            .performClick()

        composeTestRule.waitForIdle()

        // Enter amount
        composeTestRule.onNodeWithText("Amount")
            .performTextInput("50.00")

        composeTestRule.waitForIdle()

        // Enter memo
        composeTestRule.onNodeWithText("Memo (e.g., Groceries)")
            .performTextInput("Rent")

        composeTestRule.waitForIdle()

        // Verify Add button is now enabled
        composeTestRule.onNodeWithText("Add")
            .assertIsEnabled()

        // Click Add button
        composeTestRule.onNodeWithText("Add")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify dialog is dismissed
        composeTestRule.onNodeWithText("Add New Payment Task")
            .assertDoesNotExist()
    }

    @Test
    fun addPaymentDialog_switchToRequestMode() {
        composeTestRule.setContent {
            VenmoPaymentScreen(
                paymentViewModel = paymentViewModel,
                mainViewModel = mainViewModel
            )
        }

        // Open the dialog
        composeTestRule.onNodeWithContentDescription("Add Payment")
            .performClick()

        composeTestRule.waitForIdle()

        // Click "Request From" button
        composeTestRule.onNodeWithText("Request From")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify the dropdown label changed
        composeTestRule.onAllNodesWithText("Request From")
            .assertCountEquals(2)
    }

    @Test
    fun addPaymentDialog_setDueDate() {
        composeTestRule.setContent {
            VenmoPaymentScreen(
                paymentViewModel = paymentViewModel,
                mainViewModel = mainViewModel
            )
        }

        // Open the dialog
        composeTestRule.onNodeWithContentDescription("Add Payment")
            .performClick()

        composeTestRule.waitForIdle()

        // Click "Set Date" button
        composeTestRule.onNodeWithText("Set Date")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify date picker dialog appears
        // Note: The actual date picker is a Material3 component
        // You may need to interact with it differently depending on your setup
        composeTestRule.onNodeWithText("OK")
            .assertIsDisplayed()

        composeTestRule.onAllNodesWithText("Cancel")
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun addPaymentDialog_validatesAmountField() {
        composeTestRule.setContent {
            VenmoPaymentScreen(
                paymentViewModel = paymentViewModel,
                mainViewModel = mainViewModel
            )
        }

        // Open the dialog
        composeTestRule.onNodeWithContentDescription("Add Payment")
            .performClick()

        composeTestRule.waitForIdle()

        // Select a person
        composeTestRule.onNodeWithText("Select a person")
            .performClick()
        composeTestRule.onNodeWithText("Bob")
            .performClick()

        // Enter memo
        composeTestRule.onNodeWithText("Memo (e.g., Groceries)")
            .performTextInput("Test")

        // Try to enter invalid amount (0 or negative)
        composeTestRule.onNodeWithText("Amount")
            .performTextInput("0")

        composeTestRule.waitForIdle()

        // Add button should still be disabled
        composeTestRule.onNodeWithText("Add")
            .assertIsNotEnabled()

        // Clear and enter valid amount
        composeTestRule.onNodeWithText("Amount")
            .performTextClearance()

        composeTestRule.onNodeWithText("Amount")
            .performTextInput("25.50")

        composeTestRule.waitForIdle()

        // Now Add button should be enabled
        composeTestRule.onNodeWithText("Add")
            .assertIsEnabled()
    }



}