package mi.rssKoelbl;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.util.Log;

class RssDB {
	private static final String FEEDS_TABLE = "feeds";
	private static final String ARTICLES_TABLE = "articles";
	
	private final Context context;
	
	// Constructor
	public RssDB(Context ctx) {		
		this.context = ctx;
	}
	
	// Feeds
	public void addFeed(Feed feed) {
		ContentValues values = new ContentValues();
		values.put("title", feed.title);
		values.put("rss_url", feed.rss_url);
		values.put("notify", feed.notify);
		values.put("widgetable", feed.widgetable);
		
		Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, FEEDS_TABLE);
		if (!doesFeedExists(contentUri, "rss_url", feed.rss_url)) {
			context.getContentResolver().insert(contentUri, values);
		} else {
			Log.e("RssDB", "Feed already exists!");
		}
	}
	
	private boolean doesFeedExists(Uri contentUri, String query, String value) {
        Cursor c = context.getContentResolver().query(
        		contentUri, new String[] {"feed_id"}, 
        		query+" = ?", new String[] { String.valueOf(value) }, null);
        boolean exists = (c.getCount() > 0);
        c.close();
        return exists;
    }
	
	public void updateFeed(Feed feed) {
		ContentValues values = new ContentValues();
		values.put("title", feed.title);
		values.put("notify", feed.notify);
		values.put("widgetable", feed.widgetable);
		values.put("rss_url", feed.rss_url);
		
		Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, FEEDS_TABLE);
		if (doesFeedExists(contentUri, "feed_id", String.valueOf(feed.feed_id))) {
			context.getContentResolver().update(contentUri, values, "feed_id = ?", 
					new String[] { String.valueOf(feed.feed_id) });
		}
	}
	
	public Feed getFeed(int id) {
		String selection = null;
		String[] selectionArgs = null;
		if (id >= 0) {
			selection = "feed_id = ?";
			selectionArgs = new String[] { String.valueOf(id) };
		}
		Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, FEEDS_TABLE);
		Cursor c = context.getContentResolver().query(
				contentUri, new String[] {"feed_id", "title", "rss_url", "widgetable", "notify" },
				selection, selectionArgs, null);
		Feed feed = new Feed();
		if(c.moveToFirst())
		{
			feed.feed_id = c.getInt(0);
			feed.title = c.getString(1);
			feed.rss_url = c.getString(2);
			feed.widgetable = c.getInt(3);
			feed.notify = c.getInt(4);
		}
		c.close();
		return feed;
	}
	
	public int getFeedIdByURL(String url) {
		Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, FEEDS_TABLE);
		Cursor c = context.getContentResolver().query(
				contentUri, new String[] {"feed_id"},
				"rss_url = ?", new String[] { String.valueOf(url) }, null);
		int feed_id = -1;
		if(c.moveToFirst())
		{
			feed_id = c.getInt(0);
		}
		c.close();
		return feed_id;
	}
	
	public Feed getWidgetFeed() {
		Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, FEEDS_TABLE);
		Cursor c = context.getContentResolver().query(
				contentUri, new String[] {"feed_id", "title", "rss_url", "widgetable"},
				"widgetable = 1", null, null);
		Feed feed = new Feed();
		if(c.moveToFirst())
		{
			feed.feed_id = c.getInt(0);
			feed.title = c.getString(1);
			feed.rss_url = c.getString(2);
			feed.widgetable = c.getInt(3);
		}
		c.close();
		return feed;
	}
	
	public void deleteFeed(Integer feedId) {
		Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, FEEDS_TABLE);
		context.getContentResolver().delete(contentUri, "feed_id=" + feedId.toString(), null);
	}
	
	public List<Feed> getFeeds() {
		List<Feed> feedList = new ArrayList<Feed>();
		try {
			Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, FEEDS_TABLE);
			Cursor c = context.getContentResolver().query(
					contentUri, new String[] {"feed_id", "title", "rss_url",  "widgetable", "notify" },
					null, null, "feed_id DESC");
			int numRows = c.getCount();
			c.moveToFirst();
			for (int i = 0; i < numRows; i++) {
				Feed feed = new Feed();
				feed.feed_id = c.getInt(0);
				feed.title = c.getString(1);
				feed.rss_url = c.getString(2);
				feed.widgetable = c.getInt(3);
				feed.notify = c.getInt(4);
				feedList.add(feed);
				c.moveToNext();
			}
			c.close();
		} catch (SQLException e) {
			Log.e("GET_FEEDS", e.toString());
		}
		
		return feedList;
	}
	
	// Articles
	public void addArticle(int feedId, Article a) {
		ContentValues values = new ContentValues();
		values.put("feed_id", feedId);
		values.put("title", a.title);
		values.put("published_at", a.published_at);
		values.put("url", a.url);
		values.put("already_read", false);
		values.put("guid", a.guid);
		Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, ARTICLES_TABLE);
		context.getContentResolver().insert(contentUri, values);
	}

	public void deleteArticles(Integer feedId) {
		Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, ARTICLES_TABLE);
		context.getContentResolver().delete(contentUri, "feed_id=" + feedId.toString(), null);
	}
	
	public Article getArticleByWidgetFeed(int f_id) {
		Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, ARTICLES_TABLE);
		Cursor c = context.getContentResolver().query(
				contentUri, new String[] {"article_id", "feed_id", "title" , "url", "published_at", "already_read", "guid"},
				"feed_id = ?", new String[] { String.valueOf(f_id) }, "published_at DESC");
		c.moveToFirst();
		Article article = new Article();
		article.article_id = c.getInt(0);
		article.feed_id = c.getInt(1);
		article.title = c.getString(2);
		article.url = c.getString(3);
		article.published_at = c.getString(4);
		article.already_read = c.getInt(5);
		article.guid = c.getString(6);
		c.close();
		return article;
	}
	
	public Article getArticle(int a_id, int f_id) {
		Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, ARTICLES_TABLE);
		Cursor c = context.getContentResolver().query(
				contentUri, new String[] {"article_id", "feed_id", "title" , "url", "published_at", "already_read", "guid"},
				"feed_id = ? AND article_id = ?", new String[] { String.valueOf(f_id), String.valueOf(a_id) }, "published_at DESC");
		Article article = new Article();
		if (c.moveToFirst()) {
			article.article_id = c.getInt(0);
			article.feed_id = c.getInt(1);
			article.title = c.getString(2);
			article.url = c.getString(3);
			article.published_at = c.getString(4);
			article.already_read = c.getInt(5);
			article.guid = c.getString(6);
		}
		c.close();
		return article;
	}
	
    public List<Article> getArticles(int feedId) {
        ArrayList<Article> articles = new ArrayList<Article>();
        try {
        		Uri contentUri = Uri.withAppendedPath(ContentProviderDb.CONTENT_URI, ARTICLES_TABLE);
                Cursor c = context.getContentResolver().query(contentUri, 
                				new String[] { "article_id", "feed_id", "title", "url", "published_at", "already_read", "guid" },
                				"feed_id = ?", new String[] { String.valueOf(feedId) }, "published_at DESC");
 
                int numRows = c.getCount();
                c.moveToFirst();
                for (int i = 0; i < numRows; ++i) {
                        Article article = new Article();
                        article.article_id = c.getInt(0);
                        article.feed_id = c.getInt(1);
                        article.title = c.getString(2);
                        article.url = c.getString(3);
                        article.published_at = c.getString(4);
                        article.already_read = c.getInt(5);
                        article.guid = c.getString(6);
                        articles.add(article);
                        c.moveToNext();
                }
                c.close();
        } catch (SQLException e) {
                Log.e("Rss", e.toString());
        }
        return articles;
    }
}
