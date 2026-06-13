package net.morainet.widget.debugger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RemoteViews 节点快照，用于 Inspector 树形展示。
 */
data class RemoteViewNode(
    val viewId: Int,
    val viewType: String,
    val children: List<RemoteViewNode> = emptyList(),
    val actions: List<PendingIntentInfo> = emptyList(),
)

/**
 * PendingIntent 元信息。
 */
data class PendingIntentInfo(
    val action: String?,
    val requestCode: Int,
    val targetPackage: String?,
)

/**
 * Widget 调试快照，聚合 RemoteViews 树与更新元数据。
 */
data class WidgetDebugSnapshot(
    val widgetId: Int,
    val widgetClass: String,
    val remoteViewTree: RemoteViewNode?,
    val lastUpdatedAt: Long,
    val updateSource: String,
)

/**
 * 调试会话入口，收集并展示 Widget 运行时信息。
 */
object WidgetInspector {

    private val snapshots = mutableMapOf<Int, WidgetDebugSnapshot>()
    private val timeline = mutableListOf<WidgetUpdateEvent>()

    fun record(snapshot: WidgetDebugSnapshot) {
        snapshots[snapshot.widgetId] = snapshot
        timeline.add(
            WidgetUpdateEvent(
                widgetId = snapshot.widgetId,
                widgetClass = snapshot.widgetClass,
                source = snapshot.updateSource,
                timestamp = snapshot.lastUpdatedAt,
            ),
        )
        if (timeline.size > 200) {
            timeline.removeAt(0)
        }
    }

    fun getSnapshot(widgetId: Int): WidgetDebugSnapshot? = snapshots[widgetId]

    fun getAllSnapshots(): List<WidgetDebugSnapshot> = snapshots.values.toList()

    fun getTimeline(): List<WidgetUpdateEvent> = timeline.toList()

    fun clear() {
        snapshots.clear()
        timeline.clear()
    }
}

data class WidgetUpdateEvent(
    val widgetId: Int,
    val widgetClass: String,
    val source: String,
    val timestamp: Long,
)

/**
 * Widget Debugger Compose UI：Tree View + Action Inspector + Update Timeline。
 */
@Composable
fun WidgetDebuggerPanel(
    modifier: Modifier = Modifier,
    snapshots: List<WidgetDebugSnapshot> = WidgetInspector.getAllSnapshots(),
    timeline: List<WidgetUpdateEvent> = WidgetInspector.getTimeline(),
) {
    var selectedWidgetId by remember(snapshots) {
        mutableStateOf(snapshots.firstOrNull()?.widgetId)
    }
    val selected = snapshots.find { it.widgetId == selectedWidgetId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Widget Debugger",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        if (snapshots.isEmpty()) {
            Text(
                text = "No snapshots recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            snapshots.forEach { snapshot ->
                val selectedBg = if (snapshot.widgetId == selectedWidgetId) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedWidgetId = snapshot.widgetId }
                        .background(selectedBg)
                        .padding(8.dp),
                ) {
                    Text(
                        text = snapshot.widgetClass.substringAfterLast('.'),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        selected?.let { snapshot ->
            Text("RemoteViews Tree", style = MaterialTheme.typography.labelLarge)
            snapshot.remoteViewTree?.let { RemoteViewTree(node = it, depth = 0) }
                ?: Text("No RemoteViews tree", style = MaterialTheme.typography.bodySmall)

            Text("PendingIntents", style = MaterialTheme.typography.labelLarge)
            val actions = collectActions(snapshot.remoteViewTree)
            if (actions.isEmpty()) {
                Text("No actions", style = MaterialTheme.typography.bodySmall)
            } else {
                actions.forEach { action ->
                    Text(
                        text = "• ${action.action ?: "null"} (req=${action.requestCode})",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        Text("Update Timeline", style = MaterialTheme.typography.labelLarge)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(timeline.takeLast(20).reversed()) { event ->
                Text(
                    text = "${formatTime(event.timestamp)} #${event.widgetId} ${event.source}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun RemoteViewTree(node: RemoteViewNode, depth: Int) {
    val indent = "  ".repeat(depth)
    Text(
        text = "$indent└ ${node.viewType} (id=${node.viewId})",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
    )
    node.children.forEach { child ->
        RemoteViewTree(node = child, depth = depth + 1)
    }
}

private fun collectActions(node: RemoteViewNode?): List<PendingIntentInfo> {
    if (node == null) return emptyList()
    return node.actions + node.children.flatMap { collectActions(it) }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
