package com.embychapter.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.embychapter.MainActivity

/**
 * 桌面小组件 - 显示最近播放进度
 * 使用 Jetpack Glance，用 Compose 风格 API 构建 RemoteViews
 */
class EmbyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            EmbyWidgetContent(context)
        }
    }
}

@Composable
fun EmbyWidgetContent(context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(12.dp)
            .cornerRadius(16.dp)
            .background(
                ColorProvider(android.graphics.Color.parseColor("#13222B"))
            )
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = VerticalAlignment.Center
        ) {
            Text(
                text = "\uD83C\uDFAC",
                style = TextStyle(fontSize = 18.sp)
            )
            Text(
                text = "Emby 工具箱",
                style = TextStyle(
                    color = ColorProvider(android.graphics.Color.parseColor("#F3BF7D")),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.padding(start = 8.dp)
            )
        }

        // Quick status
        Text(
            text = "点击打开章节管理",
            style = TextStyle(
                color = ColorProvider(android.graphics.Color.parseColor("#9AB0B6")),
                fontSize = 12.sp
            ),
            modifier = GlanceModifier.padding(top = 8.dp)
        )

        // Action button
        Button(
            text = "打开App",
            onClick = actionStartActivity(Intent(context, MainActivity::class.java)),
            modifier = GlanceModifier.padding(top = 12.dp),
            colors = androidx.glance.ButtonDefaults.buttonColors(
                backgroundColor = ColorProvider(android.graphics.Color.parseColor("#E19B58")),
                textColor = ColorProvider(android.graphics.Color.parseColor("#1C1A18"))
            )
        )
    }
}

/**
 * 小程序 Receiver - 用于 Launcher 发现并渲染 Widget
 */
class EmbyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EmbyWidget()
}
