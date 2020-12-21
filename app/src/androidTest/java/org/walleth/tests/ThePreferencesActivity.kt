package org.BigDefi.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskIntentRule
import org.BigDefi.R
import org.BigDefi.preferences.PreferenceActivity
import org.BigDefi.preferences.reference.SelectReferenceActivity
import org.BigDefi.tokens.SelectTokenActivity


class ThePreferencesActivity {

    @get:Rule
    var rule = TruleskIntentRule(PreferenceActivity::class.java)

    @Test
    fun preferencesShow() {
        onView(withText(R.string.day_or_night_summary)).check(matches(isDisplayed()))

        rule.screenShot("preferences")
    }

    @Test
    fun whenClickOnSelectFiatWeGetToSelectFiat() {

        onView(withText(R.string.select_fiat_reference)).perform(click())

        intended(hasComponent(SelectReferenceActivity::class.java.name))

    }


    @Test
    fun whenClickOnTokenSelectWeGetToTokenSelect() {

        onView(withText(R.string.select_token)).perform(click())

        intended(hasComponent(SelectTokenActivity::class.java.name))

    }
}
