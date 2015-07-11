package mi.rssKoelbl;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.text.format.DateUtils;
import android.util.Log;


public class PrefsFragmet extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	private Context context;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
		// Load settings from xml resource
		addPreferencesFromResource(R.xml.preferences);
		updateListPrefSummary_PREF_LIST();
	}
	
	 @Override
	 public void onResume() {
	  super.onResume();
	  getPreferenceScreen().getSharedPreferences()
	   .registerOnSharedPreferenceChangeListener(this);
	 }
	  
	 @Override
	 public void onPause() {
	  super.onPause();
	  getPreferenceScreen().getSharedPreferences()
	   .unregisterOnSharedPreferenceChangeListener(this);
	 }
	 
	 private void updateListPrefSummary_PREF_LIST() {
	  ListPreference preference = (ListPreference)findPreference("updatePeriod");
	  CharSequence entry = preference.getEntry();
	  preference.setSummary("Momentane Auswahl: " + entry);
	  
	 }
	 
	 @Override
	 public void onSharedPreferenceChanged(
		 SharedPreferences sharedPreferences, String key) {
		 if(key.equals("updatePeriod")) {
		 	updateListPrefSummary_PREF_LIST();
	   
		 	Intent intent = new Intent(getActivity(), UpdateArticlesService.class);
		 	PendingIntent pintent = PendingIntent.getService(getActivity(), 0, intent, 0);
		 	String updatePeriod = sharedPreferences.getString("updatePeriod", "5");
			 assert updatePeriod != null;
			 if (!updatePeriod.equals("aus") && !updatePeriod.equals("off")) {
			   	int timeInMinInterval = Integer.valueOf(updatePeriod);
				long interval = DateUtils.MINUTE_IN_MILLIS * timeInMinInterval;
			
				AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
				alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pintent);
				Log.v("PrefsFragment", "AlarmManager gesetzt");
	    	}
		 }
	 }
}
