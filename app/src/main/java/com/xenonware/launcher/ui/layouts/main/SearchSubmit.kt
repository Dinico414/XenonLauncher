package com.xenonware.launcher.ui.layouts.main

import com.xenonware.launcher.model.AppInfo

/**
 * What pressing Enter in the search bar should do, decided purely from the query and the apps it
 * narrowed the list down to.
 */
sealed interface SearchSubmitAction {

    /** Exactly one app matched: launch it. */
    data class LaunchApp(val app: AppInfo) : SearchSubmitAction

    /** No app matched: search the web for the query instead of dead-ending on "no apps found". */
    data class WebSearch(val query: String) : SearchSubmitAction

    /** Zero or several apps matched in a way that is not ours to decide, or the query is empty. */
    data object None : SearchSubmitAction
}

/**
 * Decides what Enter means while the app search is on screen.
 *
 * The intent is that Enter always does the obvious thing for the current query:
 *  - the query narrowed the list down to a single app -> open it, so "cal" + Enter launches the
 *    calculator without the user having to reach for the result row (this also covers touch users,
 *    who have no highlighted row to select);
 *  - nothing matched at all -> run the query through web search, so Enter never does nothing;
 *  - several apps still match -> return [SearchSubmitAction.None] and let the caller keep the
 *    existing "open the highlighted app" behaviour, so the highlight is never overridden.
 *
 * A blank query is also [SearchSubmitAction.None]: there is nothing to open and searching the web
 * for an empty string would just open a blank results page.
 *
 * This is a pure function so every branch can be unit tested without a device.
 */
fun resolveSearchSubmit(
    query: String,
    matchingApps: List<AppInfo>,
): SearchSubmitAction {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return SearchSubmitAction.None

    return when (matchingApps.size) {
        1 -> SearchSubmitAction.LaunchApp(matchingApps.first())
        0 -> SearchSubmitAction.WebSearch(trimmed)
        else -> SearchSubmitAction.None
    }
}
