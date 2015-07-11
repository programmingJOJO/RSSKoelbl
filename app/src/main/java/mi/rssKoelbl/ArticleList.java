package mi.rssKoelbl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ArticleList extends ListActivity {

	// Progress dialog
	private ProgressDialog pDialog;
	
	// Array list for list view
    private final ArrayList<HashMap<String, String>> articleList = new ArrayList<HashMap<String,String>>();
 
    private final RSSParser rssParser = new RSSParser();
    private final RssDB rssDB = new RssDB(this);
     
    private List<Article> articles = new ArrayList<Article>();
    
    private Feed feed;

    private static final String TAG_ID = "article_id";
    private static final String TAG_FEED_ID = "feed_id";
    private static final String TAG_TITLE = "title";
    private static final String TAG_LINK = "link";
    private static final String TAG_DATE = "pubDate";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.article_list);
        
        // Action bar home button
    	getActionBar().setDisplayHomeAsUpEnabled(true);
        
        Intent i = getIntent();
         
        // Getting row id
        int feed_id = Integer.parseInt(i.getStringExtra("id"));
        
		feed = rssDB.getFeed(feed_id);
        String feed_rss_url = feed.rss_url;
		
		// Background handling
        new loadFeedArticles().execute(feed_id);
         
        // selecting single ListView item
        ListView lv = getListView();
  
        // Launching new screen on selecting single article
        lv.setOnItemClickListener(new OnItemClickListener() {
  
            @Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), DisplayWebsite.class);
                 
                // getting page url
                String page_url = ((TextView) view.findViewById(R.id.page_url)).getText().toString();
                Toast.makeText(getApplicationContext(), page_url, Toast.LENGTH_SHORT).show();
                intent.putExtra("page_url", page_url);
                startActivity(intent);
                
                // Set article to already read
                TextView a_id = ((TextView) view.findViewById(R.id.article_id));
                int article_id = Integer.parseInt(a_id.getText().toString());
                
                TextView f_id = ((TextView) view.findViewById(R.id.feed_id));
                int feed_id = Integer.parseInt(f_id.getText().toString());
                
                //TODO: change field already_read and save article
            }
		});
    }
    
    // ---------- Action bar ----------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        menu.findItem(R.id.action_add_feed).setVisible(false);
        menu.findItem(R.id.action_about).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        switch (item.getItemId()) {
            case R.id.action_refresh_articles:
            	// Update articles
            	new refreshArticles().execute(feed);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    // ------------------------ ----------------------------------------
    
    class refreshArticles extends AsyncTask<Feed, String, String> {
        boolean isListRefreshable = false;
    	
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(ArticleList.this);
            pDialog.setMessage("Aktualisiere ...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

		// Getting all recent articles and showing them in list view
        @Override
        protected String doInBackground(Feed... args) {
        	Feed feed = args[0];
        	
        	List<Article> pulledArticles;
        	pulledArticles = rssParser.getArticles(feed.rss_url);
        	articles = rssDB.getArticles(feed.feed_id);
            
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
            	
                // adding new article into db
                rssDB.addArticle(feed.feed_id, pulledArticle);
                isListRefreshable = true;
            }
			return null;
        }
 
        @Override
		protected void onPostExecute(String args) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();
            if (isListRefreshable) {
            	new loadFeedArticles().execute(feed.feed_id);
            }
        }
    }
    
    private class loadFeedArticles extends AsyncTask<Integer, String, String> {
    	 
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(ArticleList.this);
            pDialog.setMessage("Lade Artikel ...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

		// Getting all recent articles and showing them in list view
        @Override
        protected String doInBackground(Integer... args) {
        	int feed_id = args[0];
        	
        	articles = rssDB.getArticles(feed_id);
        	articleList.clear();
        	
            // looping through each item
            for(Article article : articles){
                // creating new HashMap
                HashMap<String, String> map = new HashMap<String, String>();
                 
                // adding each child node to HashMap key => value
                map.put(TAG_ID, String.valueOf(article.article_id));
                map.put(TAG_FEED_ID, String.valueOf(article.feed_id));
                map.put(TAG_TITLE, article.title);
                map.put(TAG_LINK, String.valueOf(article.url));
                map.put(TAG_DATE, String.valueOf(article.published_at));
            	
                // adding HashList to ArrayList
                articleList.add(map);
            }
			 // updating UI from Background Thread
			runOnUiThread(new Runnable() {
			    @Override
				public void run() {
			    	ListAdapter adapter = new SimpleAdapter(
					        ArticleList.this,
					        articleList, R.layout.article_row,
					        new String[] { TAG_ID, TAG_FEED_ID, TAG_LINK, TAG_TITLE, TAG_DATE },
					        new int[] { R.id.article_id, R.id.feed_id, R.id.page_url, R.id.title, R.id.pub_date });
					 
					// Updating list view
			        setListAdapter(adapter);
			    }
			});
			return null;
        }
 
        @Override
		protected void onPostExecute(String args) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();
        }
    }
}
