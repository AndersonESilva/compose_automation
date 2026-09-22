@file:OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)

package com.seuapp.debug

private data class ScanItem(val id: String, val bounds: Rect)

private fun SemanticsNode.scanPrefix(): String? {
    val role = config.getOrNull(SemanticsProperties.Role)
    return when {
        SemanticsActions.SetText in config -> "input"
        role == Role.Checkbox -> "check"
        role == Role.Switch -> "switch"
        role == Role.RadioButton -> "radio"
        role == Role.Tab -> "tab"
        role == Role.Button || SemanticsActions.OnClick in config -> "btn"
        else -> null
    }
}

private fun View.scanUntagged(): List<ScanItem> {
    val root = this as? RootForTest ?: return emptyList()
    val counters = mutableMapOf<String, Int>()
    return root.semanticsOwner
        .getAllSemanticsNodes(mergingEnabled = true)
        .filter { SemanticsProperties.TestTag !in it.config }
        .mapNotNull { node -> node.scanPrefix()?.let { node to it } }
        .sortedWith(compareBy({ it.first.boundsInRoot.top }, { it.first.boundsInRoot.left }))
        .map { (node, prefix) ->
            val n = (counters[prefix] ?: 0) + 1
            counters[prefix] = n
            ScanItem("${prefix}_$n", node.boundsInRoot)
        }
}

@Composable
fun AutomationTagRoot(
    intervalMs: Long = 500,
    showBadges: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    var items by remember { mutableStateOf(emptyList<ScanItem>()) }

    LaunchedEffect(view) {
        while (isActive) {
            val found = view.scanUntagged()
            if (found != items) items = found   // data class: só recompõe se mudou
            delay(intervalMs)
        }
    }

    Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
        content()

        items.forEach { item ->
            Box(
                Modifier
                    .offset { IntOffset(item.bounds.left.roundToInt(), item.bounds.top.roundToInt()) }
                    .size(
                        with(density) { item.bounds.width.toDp() },
                        with(density) { item.bounds.height.toDp() }
                    )
                    .semantics { testTag = item.id }
            ) {
                if (showBadges) {
                    Text(
                        text = item.id,
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .background(Color.Red, CircleShape)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
