package com.xenonware.launcher.ui.layouts.main

import com.xenonware.launcher.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [resolveSearchSubmit], the rule that decides what Enter does in the search bar.
 */
class SearchSubmitTest {

    private fun app(label: String) = AppInfo(
        name = label,
        packageName = "com.example.${label.lowercase()}",
    )

    private val calculator = app("Calculator")
    private val camera = app("Camera")

    @Test
    fun `blank query does nothing`() {
        assertEquals(
            SearchSubmitAction.None,
            resolveSearchSubmit(query = "   ", matchingApps = emptyList())
        )
    }

    @Test
    fun `single match launches that app`() {
        assertEquals(
            SearchSubmitAction.LaunchApp(calculator),
            resolveSearchSubmit(query = "calc", matchingApps = listOf(calculator))
        )
    }

    @Test
    fun `no match falls back to web search with the trimmed query`() {
        assertEquals(
            SearchSubmitAction.WebSearch("weather tomorrow"),
            resolveSearchSubmit(query = "  weather tomorrow  ", matchingApps = emptyList())
        )
    }

    @Test
    fun `several matches are left to the caller`() {
        assertEquals(
            SearchSubmitAction.None,
            resolveSearchSubmit(query = "ca", matchingApps = listOf(calculator, camera))
        )
    }
}
