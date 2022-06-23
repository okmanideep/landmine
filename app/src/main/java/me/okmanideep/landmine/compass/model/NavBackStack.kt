package me.okmanideep.landmine.compass.model

import me.okmanideep.landmine.compass.NavEntry

/**
 * To represent the current stack.
 * For example, if navigate from A -> B -> C, then the [entries] will have 3 elements.
 *
 * @param entries
 */
data class NavBackStack(
    val entries: List<NavEntry> = emptyList()
)

/**
 * To check if the given entry exist in the stack
 *
 * @param entry
 * @return `true` if exists, `false` otherwise
 */
internal fun NavBackStack.hasEntry(entry: NavEntry): Boolean {
    return entries.contains(entry)
}
