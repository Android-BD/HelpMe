package pwr.project.getrawdata;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ToggleButton;

public class GetRawData extends Activity implements OnClickListener {

	private SharedPreferences prefs;
	static float maximumSafeDistance = 1000;
	static final float threshold = 3.5f;
	private ToggleButton butStart;
	ImageView view;
	static boolean isRunning = false;
	final static String messageTextPreference = "textMessage";
	final static String sendPreference = "sendMessage";
	final static String telephonNumberPreference = "telephoneNumber";
	final static String maxDistPreference = "safeDistance";
	static boolean isSendingMessage;
	private AlarmReciver alarms;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		setContentView(R.layout.main);
		butStart = (ToggleButton) findViewById(R.id.butStart);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		butStart.setOnClickListener(this);
		view = (ImageView) findViewById(R.id.imageView1);
		isRunning = true;

	}
	


	@Override
	protected void onPause() {
		super.onPause();
		if (alarms != null)
			unregisterReceiver(alarms);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopService(new Intent(this, MonitoringService.class));
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		isRunning = false;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (isMyServiceRunning()) {
			view.setImageDrawable(getResources().getDrawable(R.drawable.ok));
			if (!butStart.isChecked()) {
				butStart.setChecked(true);
			}

		}
		isSendingMessage = prefs.getBoolean(sendPreference, false);
		String maxDist = prefs.getString(maxDistPreference, "1000");
		maximumSafeDistance = Float.parseFloat(maxDist);

		if (alarms == null)
			alarms = new AlarmReciver();
		IntentFilter intentFilter = new IntentFilter(MonitoringService.ALARM);
		registerReceiver(alarms, intentFilter);
	}

	@Override
	public void onClick(View v) {
		if (v == butStart) {
			if (butStart.isChecked()) {
				view.setImageDrawable(getResources().getDrawable(R.drawable.ok));
				Intent i = new Intent(this, MonitoringService.class);
				startService(i);
			} else {
				adjustUserInterface(false);
				stopService(new Intent(this, MonitoringService.class));
			}
		}
	}



	void adjustUserInterface(boolean isTriggerd) {
		if (isTriggerd) {
			view.setImageDrawable(getResources().getDrawable(R.drawable.error));
			if(!butStart.isChecked()){
				butStart.setChecked(true);
			}
		} else {
			view.setImageDrawable(getResources().getDrawable(
					R.drawable.klepsydra));

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(this).inflate(R.menu.get_raw_data, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.settings:
			i = new Intent(this, Preferences.class);
			startActivity(i);
			return true;
		case R.id.about:
			i = new Intent(this, AboutActivity.class);
			startActivity(i);
			return true;
		}
		return super.onOptionsItemSelected(item);

	}


	private class AlarmReciver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(MonitoringService.ALARM)) {
				adjustUserInterface(true);
			}
		}
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (MonitoringService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		adjustUserInterface(true);
		
	}
}