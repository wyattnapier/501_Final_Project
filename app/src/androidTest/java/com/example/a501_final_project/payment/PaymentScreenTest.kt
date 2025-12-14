package com.example.a501_final_project.payment

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.a501_final_project.IRepository
import com.example.a501_final_project.MainViewModel
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

        val roommatesList = listOf("ALICE", "BOB")

        val roommates = mainViewModel::class.java.getDeclaredField("_residents").apply {
            isAccessible = true
        }.get(mainViewModel) as kotlinx.coroutines.flow.MutableStateFlow<List<String>>
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




}