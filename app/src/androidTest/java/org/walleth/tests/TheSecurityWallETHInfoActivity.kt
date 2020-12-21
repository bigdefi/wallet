package org.BigDefi.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.BigDefi.R
import org.BigDefi.securityinfo.SecurityInfoActivity

class TheSecurityBigDefiInfoActivity {

    @get:Rule
    var rule = TruleskActivityRule(SecurityInfoActivity::class.java)

    @Test
    fun infoShows() {
        onView(withText(R.string.security_info)).check(matches(isDisplayed()))

        rule.screenShot("security_info")
    }

}
