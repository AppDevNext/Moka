package com.moka.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.moka.lib.assertions.MatchOperator
import com.moka.lib.assertions.RecyclerViewItemCountAssertion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecyclerViewTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testRecyclerViewItemCountEqual() {
        onView(ViewMatchers.withId(R.id.recyclerTest))
            .check(RecyclerViewItemCountAssertion(3, MatchOperator.IS))
    }

    @Test
    fun testRecyclerViewItemCountLessEqual() {
        onView(ViewMatchers.withId(R.id.recyclerTest))
            .check(RecyclerViewItemCountAssertion(3, MatchOperator.LESS_EQUAL))
    }

    @Test
    fun testRecyclerViewItemCountLess() {
        onView(ViewMatchers.withId(R.id.recyclerTest))
            .check(RecyclerViewItemCountAssertion(4, MatchOperator.LESS))
    }

    @Test
    fun testRecyclerViewItemCountGreater() {
        onView(ViewMatchers.withId(R.id.recyclerTest))
            .check(RecyclerViewItemCountAssertion(2, MatchOperator.GREATER))
    }

    @Test
    fun testRecyclerViewItemCountGreaterEqual() {
        onView(ViewMatchers.withId(R.id.recyclerTest))
            .check(RecyclerViewItemCountAssertion(3, MatchOperator.GREATER_EQUAL))
    }
}
