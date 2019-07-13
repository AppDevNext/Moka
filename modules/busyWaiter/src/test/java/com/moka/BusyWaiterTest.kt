package com.moka

import com.moka.waiter.BusyWaiter
import com.moka.waiter.BusyWaiter.Category.NETWORK
import com.moka.waiter.BusyWaiter.ExecutionThread.Companion.IMMEDIATE
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BusyWaiterTest {

    private lateinit var busyWaiter: BusyWaiter

    @Before
    @Throws(Exception::class)
    fun setUp() {
        busyWaiter = BusyWaiter.withOperationsCompletedOn(IMMEDIATE)
    }

    @Test
    @Throws(Exception::class)
    fun whenBusy_thenIsNotBusyReturnsFalse() {
        busyWaiter.busyWith(this)
        assertIsBusy(busyWaiter)
    }

    @Test
    @Throws(Exception::class)
    fun whenNetworkBusyAndNotTrackingNetwork_thenIsNotBusy() {
        busyWaiter.busyWith(this, NETWORK)
        busyWaiter.ignoreCategory(NETWORK)

        // should not be busy because we aren't tracking network requests
        assertNotBusy(busyWaiter)
    }

    @Test
    @Throws(Exception::class)
    fun whenNetworkBusyAndPayAttention_thenIsBusy() {
        busyWaiter.busyWith(this, NETWORK)
        busyWaiter.ignoreCategory(NETWORK)
        busyWaiter.payAttentionToCategory(NETWORK)

        assertIsBusy(busyWaiter)
    }

    @Test
    @Throws(Exception::class)
    fun whenNetworkBusyAndCompletedNetwork_thenIsNotBusy() {
        busyWaiter.busyWith(this, NETWORK)
        busyWaiter.completedEverythingInCategory(NETWORK)

        assertNotBusy(busyWaiter)
    }


    @Test
    @Throws(Exception::class)
    fun whenCompletedNetwork_thenIsNotBusy() {
        busyWaiter.completedEverythingInCategory(NETWORK)

        assertNotBusy(busyWaiter)
    }

    @Test
    @Throws(Exception::class)
    fun whenNetworkBusy_thenIsNotBusyReturnsFalse() {
        busyWaiter.busyWith(this, NETWORK)

        assertIsBusy(busyWaiter)
    }

    @Test
    @Throws(Exception::class)
    fun whenCompleted_thenIsNotBusyReturnsTrue() {
        busyWaiter.busyWith(this)
        busyWaiter.completed(this)

        assertNotBusy(busyWaiter)
    }

    @Test
    @Throws(Exception::class)
    fun whenBusyWithSomething_thenNameIncludesSomething() {
        busyWaiter.busyWith("some thing")
        assertThat(busyWaiter.name).contains("some thing")
    }

    @Test
    @Throws(Exception::class)
    fun whenCompletedEverything_thenIsNotBusyReturnsTrue() {
        busyWaiter.busyWith(this)
        busyWaiter.busyWith(Any())
        busyWaiter.busyWith(Any())
        busyWaiter.completedEverything()
        assertNotBusy(busyWaiter)
    }

    @Test
    @Throws(Exception::class)
    fun whenCompletedOnlySome_thenStillBusy() {
        val o1 = Any()
        val o2 = Any()
        assertNotBusy(busyWaiter)
        busyWaiter.busyWith(o1)
        assertIsBusy(busyWaiter)
        busyWaiter.busyWith(o2)
        assertIsBusy(busyWaiter)
        busyWaiter.completed(o2)
        assertIsBusy(busyWaiter)
        busyWaiter.completed(o1)
        assertNotBusy(busyWaiter)
    }

    private fun assertNotBusy(busyWaiter: BusyWaiter) {
        assertTrue(busyWaiter.isNotBusy)
    }

    private fun assertIsBusy(busyWaiter: BusyWaiter) {
        assertFalse(busyWaiter.isNotBusy)
    }
}
