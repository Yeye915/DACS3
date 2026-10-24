package com.example.dacs3

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationUiTest {
    @Test
    fun exploreFromHomeOpensRoomSearch() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btnExploreRooms)).perform(scrollTo(), click())
            onView(withId(R.id.edtSearch)).check(matches(isDisplayed()))
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun emptyLoginShowsFieldErrorsAndKeepsSubmitAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.btnLogin)).perform(scrollTo(), click())
            onView(withId(R.id.etUsername)).check(matches(hasErrorText(context.getString(R.string.auth_required_email))))
            onView(withId(R.id.etPassword)).check(matches(hasErrorText(context.getString(R.string.auth_required_password))))
            onView(withId(R.id.btnLogin)).check(matches(isEnabled()))
        }
    }
}
