package me.okmanideep.landmine.compass.tab

import android.os.Parcelable
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import me.okmanideep.landmine.compass.LocalOwnersProvider
import me.okmanideep.landmine.compass.NavEntry

/**
 * To hold a tab's rendering data
 *
 * @param content Tab's content @Composable
 */
internal class TabDestination(
    val content: @Composable (NavEntry) -> Unit,
)

/**
 * [TabGraph] is a collection of [TabDestination] nodes.
 * A [TabGraph] serves as a 'virtual' destination: while the [TabGraph] itself will not appear
 * on the back stack, navigating to the [TabGraph] will cause the
 * [starting destination][defaultTab] to be added to the back stack.
 *
 * @property destinationByType A map containing the tab's type as the key and [TabDestination]
 * as the value
 */
@ExperimentalAnimationApi
internal class TabGraph(
    val defaultTab: String,
    val defaultTabArgs: Parcelable?,
    val startTransition: ContentTransform,
    val endTransition: ContentTransform,
    val tabs: List<String>,
    private val destinationByType: Map<String, TabDestination>,
) {

    /**
     * To get [TabDestination] for the given pageType
     * @param pageType
     * @return [TabDestination] if exist
     * @throws IllegalArgumentException if doesn't exist
     */
    fun destinationFor(pageType: String): TabDestination {
        return destinationByType[pageType]
            ?: throw IllegalArgumentException("No destination for $pageType")
    }

    /**
     * To check if the given pageType exists in the destination map
     * @param pageType
     * @return if exists true, false otherwise.
     */
    fun hasPageType(pageType: String): Boolean {
        return destinationByType.containsKey(pageType)
    }

    fun key(): String {
        return destinationByType.keys.joinToString(",")
    }
}

/**
 * DSL for constructing a new [TabGraph]
 *
 * @property destinationByType
 */
@ExperimentalAnimationApi
class TabGraphBuilder
internal constructor(
    private val destinationByType: HashMap<String, TabDestination> = HashMap(),
    private val tabs: ArrayList<String> = ArrayList(),
) {
    private lateinit var defaultTab: String
    private var defaultTabArgs: Parcelable? = null

    /**
     * To create a new tab destination
     *
     * @param type Unique page type. Eg. home, profile, settings etc
     * @param isDefault If set to true, this tab will be the first tab in the stack
     * @param defaultTabArgs arguments of the tab
     * @param content the content @Composable of the tab
     */
    fun tab(
        type: String,
        isDefault: Boolean = false,
        defaultTabArgs: Parcelable? = null,
        content: @Composable (NavEntry) -> Unit,
    ) {
        require(tabs.none { it == type }) { "Tab of type: $type already exists" }

        if (isDefault || !this::defaultTab.isInitialized) {
            defaultTab = type
            this.defaultTabArgs = defaultTabArgs
        }
        tabs.add(type)
        destinationByType[type] = TabDestination(content)
    }

    internal fun build(
        startTransition: ContentTransform,
        endTransition: ContentTransform,
    ): TabGraph {
        if (!this::defaultTab.isInitialized) {
            error("TabNavHost with no tabs can't be created.")
        }
        return TabGraph(
            defaultTab,
            defaultTabArgs,
            startTransition,
            endTransition,
            tabs,
            destinationByType
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun NavEntry.Render(saveableStateHolder: SaveableStateHolder, graph: TabGraph) {
    LocalOwnersProvider(saveableStateHolder) {
        graph.destinationFor(pageType).content(this)
    }
}
