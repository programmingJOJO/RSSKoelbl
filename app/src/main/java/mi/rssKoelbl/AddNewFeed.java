package mi.rssKoelbl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class AddNewFeed extends Activity {
	
	private static final String TAG_TITLE = "title";
    private static final String TAG_LINK = "link";

	private EditText textUrl;
	private TextView errorMessage;
	
	private final RSSParser rssParser = new RSSParser();

	// Array list for listView
	private ArrayList<HashMap<String, String>> feedList;
    
    // List view
	private ListView lv;
	
	// Progress dialog
	private ProgressDialog pDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_feed);
		
		// Action bar home button
    	getActionBar().setDisplayHomeAsUpEnabled(true);

		Button btnSubmit = (Button) findViewById(R.id.btnSubmit);
		Button btnCancel = (Button) findViewById(R.id.btnCancel);
		textUrl = (EditText) findViewById(R.id.txtUrl);
		errorMessage = (TextView) findViewById(R.id.errorMessage);
		
		feedList = new ArrayList<HashMap<String, String>>();
		new displayPredefinedFeeds().execute();
		textUrl.setText("http://");
		
		// Set rss url from predefined list to text field
		lv = (ListView) findViewById(R.id.predefined_list);
    	lv.setOnItemClickListener(new OnItemClickListener() { 		
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// Get values from selected feed
				String rss_url = ((TextView) view.findViewById(R.id.feed_rss_url)).getText().toString();
				textUrl.setText(rss_url);
			}
		});
		
		btnSubmit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = textUrl.getText().toString();
				// Validation URL
				if (url.length() > 0) {
					errorMessage.setText("");
					String urlPattern = "^http(s{0,1})://[a-zA-Z0-9_/\\-\\.]+\\.([A-Za-z/]{2,5})[a-zA-Z0-9_/\\&\\?\\=\\-\\.\\~\\%]*";
					if (url.matches(urlPattern)) {
						// Valid URL
						new LoadRSSFeed().execute(url);
					} else {
						errorMessage.setText("Url ist nicht valide.");
					}
				} else {
					errorMessage.setText("Bitte geben Sie eine Url ein.");
				}
			}
		});
		
		// Cancel button click event
        btnCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	// Background Async Task to display predefined list
	private class displayPredefinedFeeds extends AsyncTask<String, String, String> {
        
		@Override
		protected String doInBackground(String... args) {
			
			runOnUiThread(new Runnable() {
                @Override
				public void run() {
                	String preFeeds [][] = { 
                			{"Fakult�t Informatik", "http://www.th-nuernberg.de/institutionen/fakultaeten/informatik/startseite/rss2.xml"},
                			{"Spiegel Politik", "http://m.spiegel.de/politik/index.rss"},
                			{"Spiegel Wissenschaft", "http://m.spiegel.de/wissenschaft/index.rss"},
                			{"Spiegel Netzwelt", "http://m.spiegel.de/netzwelt/index.rss"},
                			{"Wetter", "http://www.wetter.com/wetter_rss/wetter.xml"},
                			{"Heise Jobs", "http://jobs.heise.de/rss.xml"},
                			{"Die Welt", "http://www.welt.de/?service=Rss"},
                			{"Die Welt: Politik", "http://www.welt.de/politik/?service=Rss"},
                			{"Die Welt: Wissenschaft", "http://www.welt.de/wissenschaft/?service=Rss"},
                			{"Google News", "http://news.google.de/news/feeds?output=rss"},
                			{"Test Feed f�r Kurs", "http://www.informatik.fh-nuernberg.de/professors/roth/SS2014/MobileInternet/rss/rss.xml"}
                	};
                    
                    // loop through each feed
                    for (int i = 0; i < preFeeds.length; i++) {
                        
                        // creating new HashMap
                        HashMap<String, String> map = new HashMap<String, String>();
 
                        // adding each child node to HashMap key => value
                        map.put(TAG_TITLE, preFeeds[i][0]);
                        map.put(TAG_LINK, preFeeds[i][1]);
                        
                        // adding HashList to ArrayList
                        feedList.add(map);
                    }

                    // Updating list view with feeds
                    ListAdapter adapter = new SimpleAdapter(
                            AddNewFeed.this,
                            feedList, R.layout.predefined_feed_row,
                            new String[] { TAG_LINK, TAG_TITLE },
                            new int[] { R.id.feed_rss_url, R.id.title });
                    // Updating list view
                    lv.setAdapter(adapter);
                    registerForContextMenu(lv);
                }
            });
			return null;
		}
    }
	
	private class LoadRSSFeed extends AsyncTask<String, String, String> {
		
		@Override
		protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(AddNewFeed.this);
            pDialog.setMessage("Lade RSS-Informationen ...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }
		
		@Override
		protected String doInBackground(String... args) {
			String rss_url = args[0];

			Feed feed = rssParser.getFeed(rss_url);
            
            if (feed != null) {
                Log.d("after URL", feed.title + " " + feed.rss_url);
                
                RssDB rssDb = new RssDB(getApplicationContext());
                rssDb.addFeed(feed);
                
                List<Article> articles = new ArrayList<Article>();
                articles = rssParser.getArticles(rss_url);
                
            	int feed_id = rssDb.getFeedIdByURL(rss_url);
            	if (feed_id > 0) {
            		// Add articles to db
                    for(Article article : articles) {
                        rssDb.addArticle(feed_id, article);
                    }
            	}
                
                Intent intent = getIntent();
                // send result code 100 to notify about product update
                setResult(100, intent);
                finish();
            } else {
                // updating UI from Background Thread
                runOnUiThread(new Runnable() {
                    @Override
					public void run() {
                        errorMessage.setText("Die URL wurde leider nicht gefunden. Versuchen Sie es nochmal");
                    }
                });
            }
            return null;
		}
		
		@Override
		protected void onPostExecute(String args) {
            pDialog.dismiss();
            // Update UI
            runOnUiThread(new Runnable() {
                @Override
				public void run() {}
            });
        }
	}
}
