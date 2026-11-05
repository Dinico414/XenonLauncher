package com.xenonware.launcher.ui.layouts.main

import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [resolveSearchSubmit], [looksLikeUrl] and [withDefaultScheme] - the rules that
 * decide what Enter does in the search bar.
 */
class SearchSubmitTest {

    private fun app(label: String) = AppInfo(
        name = label,
        packageName = "com.example.${label.lowercase()}",
    )

    private val calculator = app("Calculator")
    private val camera = app("Camera")
    private val contact = SearchResult.Contact(
        id = "1",
        name = "Ada Lovelace",
        phoneNumber = "+15551234",
        photoUri = null,
    )

    // ---- The query itself -------------------------------------------------------------

    @Test
    fun `blank query does nothing in every category`() {
        SearchType.entries.forEach { category ->
            assertEquals(
                "category $category",
                SearchSubmitAction.None,
                resolveSearchSubmit(query = "   ", category = category, results = emptyList())
            )
        }
    }

    // ---- Apps -------------------------------------------------------------------------

    @Test
    fun `single app match launches that app`() {
        assertEquals(
            SearchSubmitAction.Open(SearchResult.App(calculator)),
            resolveSearchSubmit(
                query = "calc",
                category = SearchType.Apps,
                results = listOf(SearchResult.App(calculator)),
            )
        )
    }

    @Test
    fun `several app matches leave the highlight in charge`() {
        assertEquals(
            SearchSubmitAction.None,
            resolveSearchSubmit(
                query = "ca",
                category = SearchType.Apps,
                results = listOf(SearchResult.App(calculator), SearchResult.App(camera)),
            )
        )
    }

    @Test
    fun `no app match falls back to web search with the trimmed query`() {
        assertEquals(
            SearchSubmitAction.WebSearch("weather tomorrow"),
            resolveSearchSubmit(query = "  weather tomorrow  ", category = SearchType.Apps, results = emptyList())
        )
    }

    // ---- Contacts / Files -------------------------------------------------------------

    @Test
    fun `single contact match opens it`() {
        assertEquals(
            SearchSubmitAction.Open(contact),
            resolveSearchSubmit(
                query = "ada",
                category = SearchType.Contacts,
                results = listOf(contact),
            )
        )
    }

    @Test
    fun `several contact matches open the top hit`() {
        val other = contact.copy(id = "2", name = "Ada Byron")
        assertEquals(
            SearchSubmitAction.Open(contact),
            resolveSearchSubmit(
                query = "ada",
                category = SearchType.Contacts,
                results = listOf(contact, other),
            )
        )
    }

    @Test
    fun `no contact match falls back to web search`() {
        assertEquals(
            SearchSubmitAction.WebSearch("ada"),
            resolveSearchSubmit(query = "ada", category = SearchType.Contacts, results = emptyList())
        )
    }

    // The Files tab needs no dedicated case: SearchResult.File carries an android.net.Uri, which is a
    // stub in unit tests, so it cannot be instantiated here. resolveSearchSubmit treats Files exactly
    // like Contacts (only Apps and Web are special-cased), which the contact cases above cover.

    @Test
    fun `no file match falls back to web search`() {
        assertEquals(
            SearchSubmitAction.WebSearch("report"),
            resolveSearchSubmit(query = "report", category = SearchType.Files, results = emptyList())
        )
    }

    // ---- Web --------------------------------------------------------------------------

    @Test
    fun `web tab opens a link as a website`() {
        assertEquals(
            SearchSubmitAction.OpenUrl("https://github.com"),
            resolveSearchSubmit(query = "https://github.com", category = SearchType.Web, results = emptyList())
        )
    }

    @Test
    fun `web tab opens a bare host as a website`() {
        assertEquals(
            SearchSubmitAction.OpenUrl("example.com"),
            resolveSearchSubmit(query = "example.com", category = SearchType.Web, results = emptyList())
        )
    }

    @Test
    fun `web tab searches for a phrase`() {
        assertEquals(
            SearchSubmitAction.WebSearch("weather tomorrow"),
            resolveSearchSubmit(
                query = "weather tomorrow",
                category = SearchType.Web,
                // Web rows are query-derived, so the visible list never contains them; they must not
                // be mistaken for a match either way.
                results = emptyList(),
            )
        )
    }

    // ---- URL detection ----------------------------------------------------------------

    @Test
    fun `scheme urls are links`() {
        assertTrue(looksLikeUrl("https://news.ycombinator.com"))
        assertTrue(looksLikeUrl("http://localhost:8080/x?y=1#z"))
        assertTrue(looksLikeUrl("ftp://files.example.org/pub"))
        assertTrue(looksLikeUrl("spotify://track/123"))
    }

    @Test
    fun `bare hosts are links`() {
        assertTrue(looksLikeUrl("example.com"))
        assertTrue(looksLikeUrl("www.google.com"))
        assertTrue(looksLikeUrl("youtube.com/watch?v=abc123"))
        assertTrue(looksLikeUrl("sub.domain.co.uk/path"))
        assertTrue(looksLikeUrl("192.168.1.1:9090"))
        assertTrue(looksLikeUrl("something.xn--p1ai"))
    }

    @Test
    fun `phrases and unknown words are not links`() {
        assertFalse(looksLikeUrl("weather tomorrow"))
        assertFalse(looksLikeUrl("how do i reset my router"))
        assertFalse(looksLikeUrl("calc"))
        // A space anywhere disqualifies a candidate, even with a valid host in it.
        assertFalse(looksLikeUrl("example.com foo"))
        // A decimal number is not a host.
        assertFalse(looksLikeUrl("3.14"))
        // A scheme is required for the URL form, but a bare "foo:bar" is not a link either.
        assertFalse(looksLikeUrl("note:remember"))
    }

    @Test
    fun `default scheme is added only when missing`() {
        assertEquals("https://example.com", withDefaultScheme("example.com"))
        assertEquals("https://www.google.com/x", withDefaultScheme("www.google.com/x"))
        assertEquals("http://example.com", withDefaultScheme("http://example.com"))
        assertEquals("ftp://files.example.org", withDefaultScheme("ftp://files.example.org"))
    }
}
