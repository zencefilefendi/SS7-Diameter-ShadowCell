package com.shadowcell.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.shadowcell.R
import com.shadowcell.ShadowCellApp
import com.shadowcell.scoring.RiskLevel
import com.shadowcell.ui.MainActivity

class RiskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val app = context.applicationContext as? ShadowCellApp ?: return
            val scorer = app.scorer
            val snapshot = scorer.currentRisk()

            val views = RemoteViews(context.packageName, R.layout.widget_risk)
            
            views.setTextViewText(R.id.widget_score, snapshot.totalScore.toString())
            views.setTextViewText(R.id.widget_level, snapshot.level.name)

            val color = when (snapshot.level) {
                RiskLevel.SAFE -> Color.GREEN
                RiskLevel.MEDIUM -> Color.YELLOW
                RiskLevel.HIGH -> Color.parseColor("#FFA500") // Orange
                RiskLevel.CRITICAL -> Color.RED
            }
            views.setTextColor(R.id.widget_score, color)
            views.setTextColor(R.id.widget_level, color)

            // Create an Intent to launch MainActivity when clicked
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_score, pendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}