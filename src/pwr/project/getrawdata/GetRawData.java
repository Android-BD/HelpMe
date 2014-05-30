package pwr.project.getrawdata;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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

public class GetRawData extends Activity implements OnClickListener,
		SensorEventListener, LocationListener {

	Button butShowLog;
	Button butClearLog;
	private Sensor accelerometer;
	private Srednia<WynikiACC> sr;
	private SharedPreferences prefs;
	private SensorManager mSensorManager;
	private LocationManager mLocationManager;
	private Location middleOfSafeZone;
	private float maximumSafeDistance = 1000;
	private boolean isMonitoring = false;
	// private final float mAlpha = 0.8f;
	private static float threshold;
	private ToggleButton butStart;
	ImageView view;
	final static String thresholdPreference = "threshold";
	final static String sendPreference = "sendMessage";
	final static String telephonNumberPreference = "telephonNumber";
	final static String maxDistPreference = "safeDistance";
	private static boolean isSendingMessage;
	// final static String telephonNumber = "602780038";
	private ArrayList<String> LogList;
	final static String messageText = "RATUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUNKUUUUUUUUUUUU :OOOO";

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
		sr = new Srednia<WynikiACC>();
		mSensorManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (accelerometer == null) {
			Toast.makeText(this, "No accelerometer available",
					Toast.LENGTH_SHORT).show();
		}
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (mLocationManager == null) {
			Toast.makeText(this, "No GPS available", Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		mLocationManager.removeUpdates(this);

	}

	@Override
	protected void onResume() {
		super.onResume();
		assert (mLocationManager != null);
		isSendingMessage = prefs.getBoolean(sendPreference, false);
		threshold = Float.parseFloat(prefs
				.getString(thresholdPreference, "2.0"));
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				SensorManager.SENSOR_DELAY_UI, 0, this);
		middleOfSafeZone = mLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (middleOfSafeZone != null) {
			Log.i(getClass().getSimpleName(),
					"middleOfSafeZone = " + middleOfSafeZone.getLongitude()
							+ " " + middleOfSafeZone.getLatitude());
		}
		String maxDist = prefs.getString(maxDistPreference, "1000");
		maximumSafeDistance = Float.parseFloat(maxDist);

	}

	@Override
	public void onClick(View v) {
		if (v == butStart) {
			if (butStart.isChecked()) {
				isMonitoring = true;
				middleOfSafeZone = mLocationManager
						.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				startAccelerometerAndAdjustUI();
			} else {
				isMonitoring = false;
				stopAccelerometerAndAdjustUI();
			}
		} else if (v == butShowLog) {
			AlertDialog.Builder logDialog = new AlertDialog.Builder(this);
			logDialog.setTitle("Log akcelerometru");
			ListView logListView = new ListView(this);
			ArrayAdapter<String> logListAdapter = new ArrayAdapter<String>(
					this, android.R.layout.simple_list_item_1,
					android.R.id.text1, LogList);
			logListView.setAdapter(logListAdapter);
			logDialog.setView(logListView);
			final Dialog dial = logDialog.create();
			dial.show();
		} else if (v == butClearLog) {
			LogList.clear();
			Toast.makeText(this, "Log cleared", Toast.LENGTH_SHORT).show();
		}

	}

	void startAccelerometerAndAdjustUI() {
		view.setImageDrawable(getResources().getDrawable(R.drawable.ok));
		mSensorManager.registerListener(this, accelerometer,
				SensorManager.SENSOR_DELAY_FASTEST);

	}

	void stopAccelerometerAndAdjustUI() {
		view.setImageDrawable(getResources().getDrawable(R.drawable.klepsydra));
		mSensorManager.unregisterListener(this);
		// mLocationManager.removeUpdates(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float rawX = event.values[0];
			float rawY = event.values[1];
			float rawZ = event.values[2];

			float srednia = sr.sredniaCalkowitaZWieluPomiarow(new WynikiACC(
					rawX, rawY, rawZ));
			log("Srednia = " + srednia);
			if (srednia < threshold && isMonitoring) {
				raiseAlarm();
			}
		}
	}

	private void raiseAlarm() {
		view.setImageDrawable(getResources().getDrawable(R.drawable.error));
		Toast.makeText(this, "ALARM, threshold = " + threshold,
				Toast.LENGTH_SHORT).show();
		maintainAlarm();

	}

	private void maintainAlarm() {
		// mLocationManager.removeUpdates(this);
		sendMessage();
		sr.wynikiPomiarow.clear();
		isMonitoring = false;
		mSensorManager.unregisterListener(this);

	}

	private void sendMessage() {
		if (isSendingMessage) {
			String telephonNumber = prefs.getString(telephonNumberPreference,
					"602780038");
			SmsManager sms = SmsManager.getDefault();
			sms.sendTextMessage(telephonNumber, null, messageText, null, null);
			Log.i(getClass().getSimpleName(), "Message sent to: "
					+ telephonNumber);
		}
	}

	void adjustUserInterface(boolean isTriggerd) {
		if (isTriggerd) {
			view.setImageDrawable(getResources().getDrawable(R.drawable.error));
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.i(getClass().getSimpleName(),
				"location = " + location.getLongitude() + " "
						+ location.getLatitude());

		isInSafeZone(location);
	}

	private boolean isInSafeZone(Location actual) {
		if (actual.distanceTo(middleOfSafeZone) > maximumSafeDistance) {
			if (isMonitoring)
				raiseAlarm();
			return false;
		}

		return true;
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
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

	private void log(String s) {
		LogList.add(s);
	}
}
