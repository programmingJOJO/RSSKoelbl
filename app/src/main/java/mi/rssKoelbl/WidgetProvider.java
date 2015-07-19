package mi.rssKoelbl;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		RssDB rssDb = new RssDB(context);
	    // Get all ids
		ComponentName thisWidget = new ComponentName(context,WidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		for (int widgetId : allWidgetIds) {
			String visibleText = "Bitte wähle zuerst einen Feed für das Widget aus.", visibleTitle = "Kein Feed ausgewählt";
			
			Feed feed = rssDb.getWidgetFeed();
			if (feed.title != null) {
				visibleTitle = feed.title;
				Article article = rssDb.getArticleByWidgetFeed(feed.feed_id);
				if (article.title != null) {
					visibleText = article.title;
				} else {
					visibleText = "Keine Artikel vorhanden.";
				}
			}
	
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			// Set the text
			remoteViews.setTextViewText(R.id.widget_title, visibleTitle);
			remoteViews.setTextViewText(R.id.widget_text, visibleText);
		
			// Register an onClickListener
			Intent intent = new Intent(context, WidgetProvider.class);
		
			intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
					0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
			appWidgetManager.updateAppWidget(widgetId, remoteViews);
		}
	}
	
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context); // AppWidgetManager wird instanziert
		int ids[] = appWidgetManager.getAppWidgetIds(new ComponentName(context.getPackageName(), getClass().getName())); // IDs werden ausgelesen
		onUpdate(context, appWidgetManager, ids); // onUpdate wird explizit aufgerufen
	}
} 