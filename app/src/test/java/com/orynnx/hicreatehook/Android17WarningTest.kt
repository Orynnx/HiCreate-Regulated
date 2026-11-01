package com.orynnx.hicreatehook

import com.orynnx.hicreatehook.ui.shouldShowAndroid17Warning
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Android17WarningTest {

    @Test
    fun android16DoesNotWarn() {
        assertFalse(shouldShowAndroid17Warning(36))
    }

    @Test
    fun android17Warns() {
        assertTrue(shouldShowAndroid17Warning(37))
    }
}
