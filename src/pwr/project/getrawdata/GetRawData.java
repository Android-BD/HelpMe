package pwr.project.getrawdata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class GetRawData extends Activity implements OnClickListener,
		SensorEventListener, LocationListener {

	Button butShowLog;
	Button butClearLog;
	TextView tvWysokosc;
	TextView tvSzerokosc;
	TextView tvDlugosc;
	TextView tvSzerGEO;
	TextView tvDlGEO;
	private Sensor accelerometer;
	private Srednia<WynikiACC> sr;
	private SharedPreferences prefs;
	private SensorManager mSensorManager;
	private LocationManager mLocationManager;

	private long mLastUpdate, mLastUpdateGPS;
	private boolean isMonitoring = false;
	// private final float mAlpha = 0.8f;
	private static float threshold;
	private ToggleButton butStart;
	ImageView view;
	final static String thresholdPreference = "threshold";
	final static String sendPreference = "sendMessage";
	final static String telephonNumberPreference = "telephonNumber";
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
		mLastUpdate = System.currentTimeMillis();
		mLastUpdateGPS = System.currentTimeMillis();

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
		threshold = Float.parseFloat(prefs
				.getString(thresholdPreference, "2.0"));
	}

	@Override
	public void onClick(View v) {
		if (v == butStart) {
			if (butStart.isChecked()) {
				isMonitoring = true;
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
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				SensorManager.SENSOR_DELAY_UI, 0, this);
	}

	void stopAccelerometerAndAdjustUI() {
		view.setImageDrawable(getResources().getDrawable(R.drawable.klepsydra));
		mSensorManager.unregisterListener(this);
		mLocationManager.removeUpdates(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			long actualTime = System.currentTimeMillis();
			if (actualTime - mLastUpdate > 10) {
				mLastUpdate = actualTime;
				float rawX = event.values[0];
				float rawY = event.values[1];
				float rawZ = event.values[2];

				// mGravity[0] = lowPass(rawX, mGravity[0]);
				// mGravity[1] = lowPass(rawY, mGravity[1]);
				// mGravity[2] = lowPass(rawZ, mGravity[2]);
				//
				// mAccel[0] = highPass(rawX, mGravity[0]);
				// mAccel[1] = highPass(rawY, mGravity[1]);
				// mAccel[2] = highPass(rawZ, mGravity[2]);
				//

				float srednia = sr.sredniaCalkowitaZWieluPomiarow(new WynikiACC(rawX, rawY,
						rawZ));
				Log.d(getClass().getSimpleName(), "Srednia = " + srednia);
				log("Srednia = " + srednia);
				if (srednia < threshold && isMonitoring) {
					Toast.makeText(this, "ALARM, threshold = " + threshold,
							Toast.LENGTH_SHORT).show();
					maintainAlarm();
					sr.wynikiPomiarow.clear();
					isMonitoring = false;
					sendMessage();
				}
			}
		}
	}

	private void maintainAlarm() {
		view.setImageDrawable(getResources().getDrawable(R.drawable.error));
		mSensorManager.unregisterListener(this);
		mLocationManager.removeUpdates(this);
	}

	private void sendMessage() {

		if (prefs.getBoolean(sendPreference, false)) {
			String telephonNumber = prefs.getString(telephonNumberPreference,
					"602780038");
			SmsManager sms = SmsManager.getDefault();
			sms.sendTextMessage(telephonNumber, null, messageText, null, null);
			Log.i(getClass().getSimpleName(), "Message sent to: "
					+ telephonNumber);

		}
		Log.i(getClass().getSimpleName(), "triggered");
	}

	void adjustUserInterface(boolean isTriggerd) {
		if (isTriggerd) {
			view.setImageDrawable(getResources().getDrawable(R.drawable.error));
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		long actualTime = System.currentTimeMillis();
		if (actualTime - mLastUpdateGPS > 500) {
			mLastUpdateGPS = actualTime;

			tvDlGEO.setText("" + location.getLongitude());
			tvSzerGEO.setText("" + location.getLatitude());
			try {
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
						openFileOutput("dataGPS.txt", Context.MODE_APPEND));
				outputStreamWriter.write(location.getLongitude() + " "
						+ location.getLatitude() + "\n");
				outputStreamWriter.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
