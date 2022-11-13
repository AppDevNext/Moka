package com.sample.app

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.moka.lib.assertions.MatchOperator
import com.moka.lib.assertions.WaitingAssertion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecyclerViewWaitTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testRecyclerViewWaitItemCount() {
        WaitingAssertion.assertAdapterMinimumItemsCount(R.id.recyclerTest, 2, 500)
    }

    @Test
    fun testRecyclerViewWaitItemCountEqual() {
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.recyclerTest, 3, MatchOperator.IS, 500)
    }

    @Test
    fun testRecyclerViewWaitItemCountLessEqual() {
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.recyclerTest, 3, MatchOperator.LESS_EQUAL, 500)
    }

    @Test
    fun testRecyclerViewWaitItemCountLess() {
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.recyclerTest, 4, MatchOperator.LESS, 500)
    }

    @Test
    fun testRecyclerViewWaitItemCountGreater() {
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.recyclerTest, 2, MatchOperator.GREATER, 500)
    }

    @Test
    fun testRecyclerViewWaitItemCountGreaterEqual() {
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.recyclerTest, 3, MatchOperator.GREATER_EQUAL, 500)
    }
}
