package mi.rssKoelbl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;


public class MainActivity extends Activity {

	// Progress Dialog
	private ProgressDialog pDialog;

	// Array list for listView
	private ArrayList<HashMap<String, String>> feedList;

	RSSParser rssParser = new RSSParser();

	Feed rssFeed;
	ImageButton btnAddFeed;

	private String[] sqliteIds;

	private static final String TAG_ID = "id";
	private static final String TAG_TITLE = "title";
	private static final String TAG_LINK = "link";
	public static String TAG_DATE = "pub_date";

	// List view
	private ListView lv;

	private static final int RESULT_SETTINGS = 1;
	Intent intentService;

	private Dialog dialog_about;
	private Dialog dialog_rename;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feed_list);

		// Hashmap for ListView
		feedList = new ArrayList<HashMap<String, String>>();

		// Action bar home button
		getActionBar().setDisplayHomeAsUpEnabled(false);

		// Background thread
		new loadFeeds().execute();

		// about dialog
		dialog_about = new Dialog(this);
		dialog_about.setContentView(R.layout.about_dialog);
		dialog_about.setTitle("Information");
		// set the about dialog components - text and button
		TextView text = (TextView) dialog_about.findViewById(R.id.text);
		text.setText("Diese Anwendung wurde im Rahmen einer Seminars erstellt.");
		Button dialogButton_about = (Button) dialog_about.findViewById(R.id.dialogButtonOK);
		dialogButton_about.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog_about.dismiss();
			}
		});

		// Rename dialog
		dialog_rename = new Dialog(this);
		dialog_rename.setContentView(R.layout.rename_dialog);
		dialog_rename.setTitle("Bearbeiten");
		Button dialogButtonRenameCancel = (Button) dialog_rename.findViewById(R.id.btnRenameCancel);
		dialogButtonRenameCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog_rename.dismiss();
			}
		});

		Intent intent = new Intent(this, UpdateArticlesService.class);
		PendingIntent pintent = PendingIntent.getService(this, 0, intent, 0);

		SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String updatePeriod = mySharedPreferences.getString("updatePeriod", "5");
		if (!updatePeriod.equals("aus")) {
			int timeInMinInterval = Integer.valueOf(updatePeriod);
			long interval = DateUtils.MINUTE_IN_MILLIS * timeInMinInterval;

			AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pintent);
		}

		// Display article list activity
		lv = (ListView) findViewById(R.id.list);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// Get values from selected feed
				String feed_id = ((TextView) view.findViewById(R.id.feedId)).getText().toString();
				// Start new Intent
				Intent intent = new Intent(getApplicationContext(), ArticleList.class);

				// Passing feed row id
				intent.putExtra(TAG_ID, feed_id);
				startActivity(intent);
			}
		});

		TextView empty = (TextView) findViewById(android.R.id.empty);
		lv.setEmptyView(empty);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	// ---------- Action bar ----------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		menu.findItem(R.id.action_refresh_articles).setVisible(false);
		return super.onCreateOptionsMenu(menu);
	}

	private void openAddFeedActivity() {
		Intent intent = new Intent(getApplicationContext(), AddNewFeed.class);
		// starting new activity and expecting some response back
		// depending on the result will decide whether new feed is
		// Added to database or not
		startActivityForResult(intent, 100);
	}

	private void openPreferenceActivity() {
		Intent intent = new Intent(getApplicationContext(), SetPreferenceActivity.class);
		startActivityForResult(intent, RESULT_SETTINGS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.action_add_feed:
				// Display activity for adding new feed
				openAddFeedActivity();
				break;
			case R.id.action_settings:
				openPreferenceActivity();
				break;
			case R.id.action_about:
				dialog_about.show();
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	// ------------------------ ----------------------------------------

	/*
     * Response from AddNewSiteActivity.java
     * if response is 100 means new feed is added to db
     * reload this activity again to show
     * newly added feed in list view
     */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == 100) {
			// Reload activity to display current feeds
			Intent intent = getIntent();
			finish();
			startActivity(intent);
		}
	}

	// Background Async Task to get RSS data from URL
	private class loadFeeds extends AsyncTask<String, String, String> {

		// Before starting background thread Show Progress Dialog
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(
					MainActivity.this);
			pDialog.setMessage("Lade feeds ...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					RssDB rssDb = new RssDB(getApplicationContext());

					// listing all feeds from db
					List<Feed> feeds = rssDb.getFeeds();

					sqliteIds = new String[feeds.size()];
					feedList.clear();

					// loop through each feed
					for (int i = 0; i < feeds.size(); i++) {

						Feed f = feeds.get(i);

						// creating new HashMap
						HashMap<String, String> map = new HashMap<String, String>();

						// adding each child node to HashMap key => value
						map.put(TAG_ID, String.valueOf(f.feed_id));
						map.put(TAG_TITLE, f.title);
						map.put(TAG_LINK, f.rss_url);

						// adding HashList to ArrayList
						feedList.add(map);

						// Add db id to array
						// Used when deleting a feed from db
						sqliteIds[i] = String.valueOf(f.feed_id);
					}

					// Updating list view with feeds
					ListAdapter adapter = new SimpleAdapter(
							MainActivity.this,
							feedList, R.layout.feed_row,
							new String[] { TAG_ID, TAG_TITLE, TAG_LINK },
							new int[] { R.id.feedId, R.id.title, R.id.url });
					// Updating list view
					lv.setAdapter(adapter);
					registerForContextMenu(lv);
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

	// ---------- Context menu for editing/deleting feeds ----------------------------------------

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		if (view.getId() == R.id.list) {
			menu.setHeaderTitle("Eigenschaften");
			menu.add(Menu.NONE, 0, 0, "Löschen");
			menu.add(Menu.NONE, 1, 0, "Bearbeiten");
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		final RssDB rssDb = new RssDB(getApplicationContext());
		int feedId;
		final Feed originalFeed;
		final CheckBox notifyBox;
		final CheckBox widgetableBox;

		switch (item.getItemId()) {
			case 0:
				// Delete the feed
				feedId = Integer.parseInt(sqliteIds[info.position]);
				rssDb.deleteFeed(feedId);

				// Reload activity to display current feeds
				Intent intent = getIntent();
				finish();
				startActivity(intent);
				break;
			case 1:
				// Edit Feed
				feedId = Integer.parseInt(sqliteIds[info.position]);
				originalFeed = rssDb.getFeed(feedId);

				// Find elements
				notifyBox = (CheckBox) dialog_rename.findViewById(R.id.notifyBox);
				widgetableBox = (CheckBox) dialog_rename.findViewById(R.id.widgetableBox);

				boolean notified = originalFeed.notify > 0;
				boolean widgetabled = originalFeed.widgetable > 0;

				// Display data from db
				notifyBox.setChecked(notified);
				widgetableBox.setChecked(widgetabled);

				dialog_rename.show();

				Button dialogButtonRenameSubmit = (Button) dialog_rename.findViewById(R.id.btnRenameSubmit);
				dialogButtonRenameSubmit.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						CheckBox notifyBox = (CheckBox) dialog_rename.findViewById(R.id.notifyBox);
						CheckBox widgetableBox = (CheckBox) dialog_rename.findViewById(R.id.widgetableBox);

						if ((notifyBox.isChecked()) != (originalFeed.notify > 0) ||
								(widgetableBox.isChecked()) != (originalFeed.widgetable > 0)) {

							if (widgetableBox.isChecked() != (originalFeed.widgetable > 0) && widgetableBox.isChecked()) {
								// Listing all feeds from db
								List<Feed> feeds = rssDb.getFeeds();

								// Loop through each feed
								for (int i = 0; i < feeds.size(); i++) {
									Feed f = feeds.get(i);
									if (originalFeed.feed_id != f.feed_id && f.widgetable > 0) {
										f.widgetable = 0;
										rssDb.updateFeed(f);
									}
								}
							}
							originalFeed.widgetable = widgetableBox.isChecked() ? 1 : 0;
							originalFeed.notify = notifyBox.isChecked() ? 1 : 0;
							rssDb.updateFeed(originalFeed);

							// Immediate update widget
							Intent updateWidgetIntent = new Intent(getBaseContext(), WidgetProvider.class);
							getBaseContext().sendBroadcast(updateWidgetIntent);

							// Background thread
							new loadFeeds().execute();

							dialog_rename.dismiss();
						}
					}
				});
				break;
		}
		return true;
	}
}
