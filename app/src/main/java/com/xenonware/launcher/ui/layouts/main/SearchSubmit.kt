package com.xenonware.launcher.ui.layouts.main

import com.xenonware.launcher.model.SearchResult

/**
 * What pressing Enter in the search bar should do, decided purely from the query, the active search
 * category and the results it narrowed down to.
 */
sealed interface SearchSubmitAction {

    /** Open [result]: an app, a contact or a file. */
    data class Open(val result: SearchResult) : SearchSubmitAction

    /** The query is already a link, so it is opened as-is instead of being searched for. */
    data class OpenUrl(val url: String) : SearchSubmitAction

    /** Nothing matched: hand the query to the browser's search engine instead of doing nothing. */
    data class WebSearch(val query: String) : SearchSubmitAction

    /** Leave it to the caller: keep the current highlight, or do nothing for a blank query. */
    data object None : SearchSubmitAction
}

/** `scheme://rest`, e.g. `https://`, `ftp://`, `mailto:`-style explicit schemes. */
private val UrlSchemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*://\\S+$")

/** Just the scheme part, for deciding whether a URL still needs `https://` added. */
private val BareSchemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*://")

/**
 * A bare host such as `example.com`, `www.google.com` or `youtube.com/watch?v=abc`, with an optional
 * port, path, query and fragment.
 *
 * The TLD has to be alphabetic (two or more characters, or a punycode label), which keeps decimal
 * numbers like `3.14` from being mistaken for a host.
 */
private val BareHostRegex = Regex(
    "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+" + // one or more host labels
            "(?:[a-zA-Z]{2,}|xn--[a-zA-Z0-9-]+)" +        // the TLD
            "(?::\\d{1,5})?" +                            // optional port
            "(?:[/?#]\\S*)?$"                              // optional path / query / fragment
)

/** A dotted IPv4 address, with the same optional port / path tail. */
private val Ipv4Regex = Regex(
    "^(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}" +
            "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
            "(?::\\d{1,5})?(?:[/?#]\\S*)?$"
)

/**
 * True when [query] should be treated as a link rather than as something to search the web for.
 *
 * Anything containing whitespace is a search phrase and never a URL, so `weather tomorrow` is
 * searched for while `github.com` and `https://news.ycombinator.com` are opened directly.
 */
fun looksLikeUrl(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return false
    return UrlSchemeRegex.matches(trimmed) ||
            BareHostRegex.matches(trimmed) ||
            Ipv4Regex.matches(trimmed)
}

/**
 * Turns [url] into an absolute URL, adding `https://` only when it does not carry a scheme of its
 * own - so a typed `ftp://…` or `http://…` is left exactly as the user wrote it.
 */
fun withDefaultScheme(url: String): String =
    if (BareSchemeRegex.containsMatchIn(url)) url else "https://$url"

/**
 * Decides what Enter means for the search bar.
 *
 * The intent is that Enter always does the obvious thing for whatever is currently on screen:
 *  - **Web**: the query itself is the result. A link is opened as a website, anything else is
 *    handed to the browser to search for.
 *  - **Apps**: a single match is launched, so "cal" + Enter opens the calculator without reaching
 *    for the result row. Several matches are left to the caller so the highlighted cell is never
 *    overridden, which is also what makes a touch user's Enter pick the visible selection.
 *  - **Contacts / Files**: there is no highlighted row to defer to, so a single match and the top
 *    hit of several matches are both opened.
 *  - Everywhere: if nothing matched, the query goes to web search so Enter never does nothing, and
 *    an empty query is [SearchSubmitAction.None] because searching for an empty string would only
 *    open a blank results page.
 *
 * [results] must be the results visible in [category] itself; query-derived web rows are never
 * candidates, since counting them would break the "exactly one match" rule.
 *
 * This is a pure function so every branch can be unit tested without a device.
 */
fun resolveSearchSubmit(
    query: String,
    category: SearchType,
    results: List<SearchResult>,
): SearchSubmitAction {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return SearchSubmitAction.None

    // The web tab has no result list to choose from: the query is the only candidate.
    if (category == SearchType.Web) {
        return if (looksLikeUrl(trimmed)) SearchSubmitAction.OpenUrl(trimmed)
        else SearchSubmitAction.WebSearch(trimmed)
    }

    if (results.isEmpty()) return SearchSubmitAction.WebSearch(trimmed)

    // Apps keep the highlighted cell as the tie-breaker when the query is still ambiguous.
    if (category == SearchType.Apps && results.size > 1) return SearchSubmitAction.None

    return SearchSubmitAction.Open(results.first())
}
