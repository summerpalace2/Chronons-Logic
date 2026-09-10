package com.chronotask.pages.stats.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsRequestGateTest {
    @Test
    fun onlyNewestRequestMayPublishStatsState() {
        val gate = StatsRequestGate()
        val firstRequest = gate.beginRequest()
        val secondRequest = gate.beginRequest()

        assertFalse(gate.isLatest(firstRequest))
        assertTrue(gate.isLatest(secondRequest))
    }
}
