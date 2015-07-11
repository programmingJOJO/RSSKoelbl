package mi.rssKoelbl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NotificationReceiverActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		startActivity(intent);
	}
}
