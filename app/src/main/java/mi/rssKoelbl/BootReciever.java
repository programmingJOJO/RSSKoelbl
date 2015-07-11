package mi.rssKoelbl;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

public class BootReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.v("Reciever", " Boot komplett.");
            
            Intent serviceIntent = new Intent(context, UpdateArticlesService.class);
            PendingIntent pintent = PendingIntent.getService(context, 0, serviceIntent, 0);
 
            SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        	int timeInMinInterval = Integer.valueOf(mySharedPreferences.getString("updatePeriod", "5"));
            long interval = DateUtils.MINUTE_IN_MILLIS * timeInMinInterval;
            long firstStart = System.currentTimeMillis() + interval;
 
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, firstStart, interval, pintent);
 
            Log.v("Reciever", "AlarmManager gesetzt");
        }
	}

}
