package net.morainet.widget.dsl

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import net.morainet.widget.core.WidgetActions
import net.morainet.widget.state.WidgetUiState

/**
 * 将 [WidgetBlueprint] 渲染为 Glance Composable。
 */
@Composable
fun BlueprintRenderer(
    blueprint: WidgetBlueprint,
    state: WidgetUiState<Map<String, String>> = WidgetUiState.Loading,
    modifier: GlanceModifier = GlanceModifier,
) {
    val resolvedComponents = resolveComponents(blueprint, state)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .background(ColorProvider(android.graphics.Color.TRANSPARENT)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            is WidgetUiState.Loading -> {
                Text(text = "Loading...", style = TextStyle(fontWeight = FontWeight.Medium))
            }
            is WidgetUiState.Error -> {
                Text(text = state.message, style = TextStyle(fontWeight = FontWeight.Medium))
                if (state.retryable) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "Tap to retry",
                        modifier = GlanceModifier.clickable(
                            actionSendBroadcast(
                                android.content.Intent(WidgetActions.ACTION_RETRY),
                            ),
                        ),
                    )
                }
            }
            is WidgetUiState.Success -> {
                when (blueprint.layout) {
                    WidgetLayout.LIST_4X2 -> {
                        resolvedComponents.forEach { component ->
                            RenderComponent(component)
                            Spacer(modifier = GlanceModifier.height(4.dp))
                        }
                    }
                    else -> {
                        resolvedComponents.forEach { component ->
                            RenderComponent(component)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderComponent(component: WidgetComponent) {
    when (component.type) {
        ComponentType.TEXT -> {
            val text = component.props["text"] ?: component.id
            val style = when (component.props["style"]) {
                "headline" -> TextStyle(fontWeight = FontWeight.Bold)
                "caption" -> TextStyle(fontWeight = FontWeight.Normal)
                else -> TextStyle(fontWeight = FontWeight.Medium)
            }
            Text(text = text, style = style)
        }
        ComponentType.IMAGE -> {
            val src = component.props["src"] ?: "@android:drawable/ic_menu_gallery"
            val resId = resolveDrawableRes(src)
            if (resId != 0) {
                Image(
                    provider = ImageProvider(resId),
                    contentDescription = component.id,
                )
            } else {
                Text(text = "[image:${component.id}]")
            }
        }
        ComponentType.BUTTON -> {
            Text(
                text = component.props["text"] ?: component.id,
                style = TextStyle(fontWeight = FontWeight.Bold),
            )
        }
        ComponentType.PROGRESS -> {
            Text(text = component.props["text"] ?: "Progress")
        }
        ComponentType.LIST, ComponentType.CHART -> {
            Row {
                Text(text = component.props["text"] ?: component.type.name)
            }
        }
    }
}

private fun resolveComponents(
    blueprint: WidgetBlueprint,
    state: WidgetUiState<Map<String, String>>,
): List<WidgetComponent> {
    if (state !is WidgetUiState.Success) return blueprint.components
    return blueprint.components.map { component ->
        val overrideText = state.data[component.id]
        if (overrideText != null && component.type == ComponentType.TEXT) {
            component.copy(props = component.props + ("text" to overrideText))
        } else {
            component
        }
    }
}

private fun resolveDrawableRes(src: String): Int {
    return when {
        src.startsWith("@drawable/") -> {
            when (src.removePrefix("@drawable/")) {
                "ic_sunny" -> android.R.drawable.ic_menu_day
                else -> android.R.drawable.ic_menu_gallery
            }
        }
        src.startsWith("@android:drawable/") -> {
            when (src.removePrefix("@android:drawable/")) {
                "ic_menu_day" -> android.R.drawable.ic_menu_day
                else -> android.R.drawable.ic_menu_gallery
            }
        }
        else -> android.R.drawable.ic_menu_gallery
    }
}
