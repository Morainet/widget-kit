# widget-ai consumer rules
# Prompt → WidgetBlueprint 生成管线，保持序列化类型不混淆
-keep class com.morainet.widget.dsl.** { *; }
-keep class com.morainet.widget.ai.** { *; }
