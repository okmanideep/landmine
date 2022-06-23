package me.okmanideep.landmine.compass.core

import java.util.*

/**
 * StackInternal is a simple wrapper around a [LinkedList]
 * This is used to maintain navigation stack inside [me.okmanideep.landmine.compass.stack.StackNavHost] and
 * [me.okmanideep.landmine.compass.tab.TabNavHost]. The purpose of this wrapper is to provide a meaningful
 * API when looking from the navigation-stack perspective.
 *
 * @param T
 */
internal class StackInternal<T> {
    private val entries = LinkedList<T>()

    /**
     * To add a new entry to the [LinkedList]
     */
    fun add(entry: T) {
        entries.add(entry)
    }

    /**
     * To remove the last entry and add the new entry in the same position
     *
     * @param entry
     * @throws IllegalStateException if there are no elements in the stack
     * @return removed entry
     */
    fun replace(entry: T): T {
        check(entries.isNotEmpty()) { "No items in the stack. Can't replace" }

        return entries.removeLast().also {
            entries.add(entry)
        }
    }

    /**
     * To check if there's any element in the stack
     *
     * @return true if the size > 1, false otherwise
     */
    fun canPop(): Boolean {
        return entries.size > 1
    }

    /**
     * To remove the last entry from the stack
     *
     * @throws IllegalStateException if the stack is empty
     * @return popped entry
     */
    fun pop(): T {
        check(canPop()) { "${entries.size} items in the stack. Can't pop" }

        return entries.removeLast()
    }

    /**
     * To remove entries until (exclusive) the predicate is true
     */
    fun popUpTo(predicate: (T) -> Boolean) {
        while (canPop()) {
            if (peek()?.let(predicate) == true) {
                return
            }
            entries.removeLast()
        }
    }

    /**
     * To remove the first entry that passes the test
     */
    fun remove(predicate: (index: Int, item: T) -> Boolean): T? {
        val entryToRemove = entries.withIndex().find {
            predicate(it.index, it.value)
        }?.value

        if (entryToRemove != null) entries.remove(entryToRemove)

        return entryToRemove
    }

    /**
     * To remove all elements from the stack
     */
    fun clear() {
        entries.clear()
    }

    /**
     * To get last element of the stack
     */
    fun peek(): T? {
        return entries.lastOrNull()
    }

    /**
     * To check if the given element exist in the stack
     */
    fun contains(entry: T): Boolean {
        return entries.contains(entry)
    }

    /**
     * To move the entry to the front.
     */
    fun bringToFront(entry: T) {
        require(entries.contains(entry))

        entries.remove(entry)
        entries.add(entry)
    }

    /**
     * To get stack [LinkedList] as [List]
     */
    fun toList(): List<T> {
        return entries.toList()
    }

    /**
     * Returns the index of the last item in the stack or -1 if the stack is empty.
     */
    val lastIndex: Int
        get() = entries.lastIndex
}
