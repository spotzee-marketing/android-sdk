package com.spotzee.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingLinkTest {
    @Test
    fun acceptsExactHttpsClickPathOnCustomDomain() {
        assertTrue(
            isSpotzeeTrackingLink(
                "https://click.customer.example/c?r=https%3A%2F%2Fexample.com%2Foffer",
            ),
        )
    }

    @Test
    fun rejectsInsecureAndLookalikeClickPaths() {
        assertFalse(
            isSpotzeeTrackingLink(
                "http://click.customer.example/c?r=https%3A%2F%2Fexample.com",
            ),
        )
        assertFalse(
            isSpotzeeTrackingLink(
                "https://click.customer.example/c/extra?r=https%3A%2F%2Fexample.com",
            ),
        )
        assertFalse(
            isSpotzeeTrackingLink(
                "https://click.customer.example/not-c?r=https%3A%2F%2Fexample.com",
            ),
        )
        assertFalse(isSpotzeeTrackingLink("https://click.customer.example/c"))
    }
}
