package mi.rssKoelbl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class UpdateArticlesService extends Service {

	// Array list for list view
	ArrayList<HashMap<String, String>> articleList = new ArrayList<HashMap<String,String>>();

	private final RSSParser rssParser = new RSSParser();
	private final RssDB rssDB = new RssDB(this);

	@Override
	public void onCreate() {

		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.v("SERVICE", System.currentTimeMillis() + ": Service started");

		refreshArticles longTask2 = new refreshArticles();
		longTask2.execute();

		return Service.START_NOT_STICKY;
	}

	private void createNotification(int feed_id, String feed_title, String article_title, String page_url) {
		// Prepare intent which is triggered if the notification is selected
		Intent intent = new Intent(this, NotificationReceiverActivity.class);

		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

		// Build notification
		Notification noti = new Notification.Builder(this)
				.setContentTitle(feed_title)
				.setContentText(article_title)
				.setSmallIcon(R.drawable.ic_rss)
				.setContentIntent(pIntent).build();
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Vibration
		noti.defaults |= Notification.DEFAULT_VIBRATE;

		notificationManager.notify(feed_id, noti);
	}

	private class refreshArticles extends AsyncTask<String, String, String> {

		// Getting all recent articles and showing them in listview
		@Override
		protected String doInBackground(String... args) {

			List<Feed> feeds = rssDB.getFeeds();

			for(Feed feed : feeds) {

				// Update Notification
				boolean atLeastOneArticleInFeedIsNew = false;
				String articleTitle = null, articleUrl = null;

				List<Article> pulledArticles;
				pulledArticles = rssParser.getArticles(feed.rss_url);
				List<Article> articles = rssDB.getArticles(feed.feed_id);

				// looping through each item
				for(Article pulledArticle : pulledArticles) {
					boolean articleIsNew = true;
					for(Article article : articles) {
						if (pulledArticle.guid.equals(article.guid)) {
							articleIsNew = false;
							break;
						}
					}
					if (!articleIsNew) {
						continue;
					}
					atLeastOneArticleInFeedIsNew = true;
					articleTitle = pulledArticle.title;
					articleUrl = pulledArticle.url;

					// Adding new article into db
					rssDB.addArticle(feed.feed_id, pulledArticle);

				}
				if (atLeastOneArticleInFeedIsNew && feed.notify == 1) {
					createNotification(feed.feed_id, feed.title, articleTitle, articleUrl);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String args) {
			// Stop service
			stopSelf();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// Irrelevant
		return null;
	}

	@Override
	public void onDestroy() {
		// The service is no longer used and is being destroyed
		Log.v("SERVICE", System.currentTimeMillis() + ": Service terminated");
	}
}
