package com.sample.app

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.moka.lib.assertions.MatchOperator
import com.moka.lib.assertions.WaitingAssertion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecyclerViewTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testRecyclerViewItemCount() {
        WaitingAssertion.assertAdapterMinimumItemsCount(R.id.recyclerTest, 2, 500)
    }

    @Test
    fun testRecyclerViewItemCountEqual() {
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.recyclerTest, 3, MatchOperator.IS, 500)
    }

    @Test
    fun testRecyclerViewItemCountLessEqual() {
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.recyclerTest, 3, MatchOperator.LESS_EQUAL, 500)
    }

    @Test
    fun testRecyclerViewItemCountLess() {
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.recyclerTest, 4, MatchOperator.LESS, 500)
    }

    @Test
    fun testRecyclerViewItemCountGreater() {
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.recyclerTest, 2, MatchOperator.GRATER, 500)
    }

    @Test
    fun testRecyclerViewItemCountGreaterEqual() {
        WaitingAssertion.assertRecyclerAdapterItemsCount(R.id.recyclerTest, 3, MatchOperator.GRATER_EQUAL, 500)
    }
}
