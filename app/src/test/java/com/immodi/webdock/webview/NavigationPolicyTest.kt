package com.immodi.webdock.webview

import com.immodi.webdock.domain.AppSettings
import com.immodi.webdock.domain.LinkPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPolicyTest {

    private val startUrl = "https://trade-journel.pages.dev/"

    private fun policy(settings: AppSettings = AppSettings()) = NavigationPolicy(startUrl, settings)

    @Test
    fun sameHostStaysInApp() {
        assertTrue(policy().isInApp("https://trade-journel.pages.dev/reports?month=1"))
    }

    @Test
    fun differentHostOpensExternally() {
        assertEquals(
            NavigationDecision.External("https://example.com/"),
            policy().decide("https://example.com/"),
        )
    }

    @Test
    fun allowedHostsStayInApp() {
        val settings = AppSettings(allowedHosts = listOf("auth.example.com"))
        assertTrue(policy(settings).isInApp("https://auth.example.com/login"))
    }

    @Test
    fun subdomainsAreDifferentHosts() {
        assertEquals(
            NavigationDecision.External("https://app.trade-journel.pages.dev/"),
            policy().decide("https://app.trade-journel.pages.dev/"),
        )
    }

    @Test
    fun siblingPagesDevHostsAreNotTheSameSite() {
        assertEquals(
            NavigationDecision.External("https://futures-playbook.pages.dev/"),
            policy().decide("https://futures-playbook.pages.dev/"),
        )
    }

    @Test
    fun allInAppPolicyKeepsEveryHttpUrlInApp() {
        val settings = AppSettings(linkPolicy = LinkPolicy.ALL_IN_APP)
        assertTrue(policy(settings).isInApp("https://example.com/anything"))
    }

    @Test
    fun allExternalPolicySendsEvenTheStartHostOut() {
        val settings = AppSettings(linkPolicy = LinkPolicy.ALL_EXTERNAL)
        assertEquals(
            NavigationDecision.External("$startUrl?x=1"),
            policy(settings).decide("$startUrl?x=1"),
        )
    }

    @Test
    fun nonHttpSchemesGoToAnotherApp() {
        val policy = policy()
        assertTrue(policy.decide("mailto:hello@example.com") is NavigationDecision.ExternalApp)
        assertTrue(policy.decide("tel:+123456") is NavigationDecision.ExternalApp)
        assertTrue(policy.decide("geo:0,0?q=test") is NavigationDecision.ExternalApp)
        assertTrue(policy.decide("intent://scan/#Intent;scheme=zxing;end") is NavigationDecision.ExternalApp)
    }

    @Test
    fun dangerousSchemesAreBlocked() {
        val policy = policy()
        assertEquals(NavigationDecision.Blocked, policy.decide("javascript:alert(1)"))
        assertEquals(NavigationDecision.Blocked, policy.decide("file:///sdcard/index.html"))
        assertEquals(NavigationDecision.Blocked, policy.decide("data:text/html,hello"))
    }

    @Test
    fun invalidUrlsAreBlocked() {
        assertEquals(NavigationDecision.Blocked, policy().decide("not a url"))
        assertEquals(NavigationDecision.Blocked, policy().decide(""))
    }

    @Test
    fun httpIsAccepted() {
        assertFalse(policy().decide("http://trade-journel.pages.dev/") is NavigationDecision.Blocked)
    }
}
