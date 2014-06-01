package pwr.project.getrawdata;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class GetRawData extends Activity implements OnClickListener {

	Button butShowLog;
	Button butClearLog;
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
	private static ArrayList<String> LogList;
	private AlarmReciver alarms;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		setContentView(R.layout.main);
		butStart = (ToggleButton) findViewById(R.id.butStart);
		butShowLog = (Button) findViewById(R.id.butLog);
		butClearLog = (Button) findViewById(R.id.butClearLog);
		LogList = new ArrayList<String>();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		butStart.setOnClickListener(this);
		butClearLog.setOnClickListener(this);
		butShowLog.setOnClickListener(this);
		view = (ImageView) findViewById(R.id.imageView1);
		isRunning = true;
		Intent i = getIntent();
		boolean isAlarm = i.getBooleanExtra(MonitoringService.ALARM, false);
		adjustUserInterface(isAlarm);
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

		} else if (v == butShowLog) {
			final Dialog dial = createAlertDialog();
			dial.show();

		} else if (v == butClearLog) {
			LogList.clear();
			Toast.makeText(this, "Log cleared", Toast.LENGTH_SHORT).show();
		}

	}

	private Dialog createAlertDialog() {
		AlertDialog.Builder logDialog = new AlertDialog.Builder(this);
		logDialog.setTitle("Log akcelerometru");
		ListView logListView = new ListView(this);
		ArrayAdapter<String> logListAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1,
				LogList);
		logListView.setAdapter(logListAdapter);
		logDialog.setView(logListView);
		return logDialog.create();
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
		}
		return super.onOptionsItemSelected(item);

	}

	static void log(String s) {
		LogList.add(s);
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
}