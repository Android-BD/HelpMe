package pwr.project.getrawdata;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import android.annotation.SuppressLint;
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
	static final float threshold = 3.5f;
	private ToggleButton butStart;
	ImageView view;
	final static String messageTextPreference = "textMessage";
	final static String sendPreference = "sendMessage";
	final static String telephonNumberPreference = "telephoneNumber";
	final static String maxDistPreference = "safeDistance";
	private static boolean isSendingMessage;
	private ArrayList<String> LogList;
	private String messageText;
	private Location currentLocation;

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
		mSensorManager.registerListener(this, accelerometer,
				SensorManager.SENSOR_DELAY_FASTEST);
		isSendingMessage = prefs.getBoolean(sendPreference, false);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				0, 0, this);

		middleOfSafeZone = mLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		if (middleOfSafeZone != null) {
			Log.i(getClass().getSimpleName(),
					"middleOfSafeZone = " + middleOfSafeZone.getLongitude()
							+ " " + middleOfSafeZone.getLatitude());
		}
		String maxDist = prefs.getString(maxDistPreference, "1000");
		maximumSafeDistance = Float.parseFloat(maxDist);
		messageText = prefs.getString(messageTextPreference, "ratunku!");

	}

	@Override
	public void onClick(View v) {
		if (v == butStart) {
			if (butStart.isChecked()) {
				Intent i = new Intent(this, MonitoringService.class);
				startService(i);
//				isMonitoring = true;
//				middleOfSafeZone = mLocationManager
//						.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//				startAccelerometerAndAdjustUI();
//				currentLocation = middleOfSafeZone;
//			} else {
//				isMonitoring = false;
//				stopAccelerometerAndAdjustUI();
			}
			
		} else if (v == butShowLog) {
			final Dialog dial = createAlertDialog();
			dial.show();
			
		} else if (v == butClearLog) {
			LogList.clear();
			Toast.makeText(this, "Log cleared", Toast.LENGTH_SHORT).show();
		}

	}
	
	private Dialog createAlertDialog(){
		AlertDialog.Builder logDialog = new AlertDialog.Builder(this);
		logDialog.setTitle("Log akcelerometru");
		ListView logListView = new ListView(this);
		ArrayAdapter<String> logListAdapter = new ArrayAdapter<String>(
				this, android.R.layout.simple_list_item_1,
				android.R.id.text1, LogList);
		logListView.setAdapter(logListAdapter);
		logDialog.setView(logListView);
		return logDialog.create();
	}

	void startAccelerometerAndAdjustUI() {
		view.setImageDrawable(getResources().getDrawable(R.drawable.ok));
		mSensorManager.registerListener(this, accelerometer,
				SensorManager.SENSOR_DELAY_FASTEST);

	}

	void stopAccelerometerAndAdjustUI() {
		view.setImageDrawable(getResources().getDrawable(R.drawable.klepsydra));
		mSensorManager.unregisterListener(this);
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
		maintainAlarm();

	}

	private void maintainAlarm() {
		sendMessage();
		sr.wynikiPomiarow.clear();
		isMonitoring = false;
		mSensorManager.unregisterListener(this);

	}

	private void sendMessage() {
		Log.i(getClass().getSimpleName(), parseTextMessage(messageText));

		if (isSendingMessage) {
			String telephonNumber = prefs.getString(telephonNumberPreference,
					"000000000");
			parseTextMessage(messageText);
			SmsManager sms = SmsManager.getDefault();
			sms.sendTextMessage(telephonNumber, null, messageText, null, null);
			Log.i(getClass().getSimpleName(), "Message sent to: "
					+ telephonNumber);
			Toast.makeText(this, "ALARM, message sento to = " + telephonNumber,
					Toast.LENGTH_SHORT).show();
		}
	}

	@SuppressLint("DefaultLocale")
	private String parseTextMessage(String message) {
		String resultMessage;
		if (currentLocation != null) {
			resultMessage = message.replace("%l", "" + "dlugosc = "
					+ currentLocation.getLongitude() + " szerokosc = "
					+ currentLocation.getLatitude());
		} else {
			resultMessage = message.replace("%l",
					"\"Brak informacji o lokalizacji!\"");
		}
		Calendar c = Calendar.getInstance();
		String date = String.format(Locale.ENGLISH, "%d:%d:%d %d/%d/%d",
				c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
				c.get(Calendar.SECOND), c.get(Calendar.DAY_OF_MONTH),
				c.get(Calendar.MONTH), c.get(Calendar.YEAR));

		resultMessage = resultMessage.replace("%d", date);
		return resultMessage;
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
		currentLocation = location;

		inSafeZone(location);
	}

	private void inSafeZone(Location actual) {
		if (actual.distanceTo(middleOfSafeZone) > maximumSafeDistance
				&& isMonitoring) {
			raiseAlarm();
		}
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
