package com.example.a501_final_project.payment

import com.example.a501_final_project.IRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PaymentUnitTests {
    @Mock
    private lateinit var mockRepository: IRepository

    private lateinit var viewModel: PaymentViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)
        viewModel = PaymentViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleShowPastPayments_flipsState() {
        assertFalse(viewModel.showPastPayments.value)

        viewModel.toggleShowPastPayments()

        assertTrue(viewModel.showPastPayments.value)
    }

    // Helper function to more easily create sample payment objects
    private fun samplePayment(
        id: String = "1",
        payFrom: String = "X",
        payTo: String = "Y",
        paid: Boolean = false
    ) = Payment(
        id = id,
        payFromId = payFrom,
        payFromName = "From",
        payToId = payTo,
        payToName = "To",
        payToVenmoUsername = "venmo",
        amount = 10.0,
        memo = "Test",
        dueDate = null,
        datePaid = null,
        paid = paid,
        recurring = false
    )


    @Test
    fun getPaymentsFor_filtersCorrectly() {
        val payments = listOf(
            samplePayment(payTo = "A"),
            samplePayment(payTo = "B")
        )

        val result = viewModel.getPaymentsFor("A", payments)

        assertEquals(1, result.size)
        assertEquals("A", result.first().payToId)
    }

    @Test
    fun completePayment_movesPaymentToPast() = runTest {
        val payment = samplePayment()

        viewModel.reset()
        viewModel.completePayment(payment)

        advanceUntilIdle()

        assertTrue(viewModel.pastPayments.value.any { it.id == payment.id })
    }


}