package pwr.project.getrawdata;

import java.util.Calendar;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Service;
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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class MonitoringService extends Service implements SensorEventListener,
		LocationListener {

	Srednia<WynikiACC> sredniaZPomiarow;
	private SensorManager mSensorManager;
	private LocationManager mLocationManager;
	private Sensor accelerometer;
	private Location currentLocation = null;
	SharedPreferences prefs;
	private Location middleOfSafeZone;
	static final String ALARM = "ALARM";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		sredniaZPomiarow = new Srednia<WynikiACC>();
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				0, 0, this);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mSensorManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, accelerometer,
				SensorManager.SENSOR_DELAY_FASTEST);
		middleOfSafeZone = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		return super.onStartCommand(intent, flags, startId);

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float rawX = event.values[0];
			float rawY = event.values[1];
			float rawZ = event.values[2];

			float srednia = sredniaZPomiarow
					.sredniaCalkowitaZWieluPomiarow(new WynikiACC(rawX, rawY,
							rawZ));

			GetRawData.log("Srednia = " + srednia);
			if (srednia < GetRawData.threshold) {
				raiseAlarm();
			}
		}
	}

	private void raiseAlarm() {
		if(GetRawData.isRunning)
			sendBroadcast(new Intent(ALARM));
		else{
			Intent i = new Intent(MonitoringService.this, GetRawData.class);
			i.putExtra(ALARM, true);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		}
		maintainAlarm();
		stopSelf();
	}

	private void maintainAlarm() {
		sendMessage();
		sredniaZPomiarow.wynikiPomiarow.clear();
		mSensorManager.unregisterListener(this);
		mLocationManager.removeUpdates(this);

	}

	@Override
	public void onDestroy() {
		mSensorManager.unregisterListener(this);
		mLocationManager.removeUpdates(this);
		super.onDestroy();

	}

	private void sendMessage() {
		String messageText = prefs.getString(GetRawData.messageTextPreference,
				"");
		Log.i(getClass().getSimpleName(), parseTextMessage(messageText));

		if (GetRawData.isSendingMessage) {
			String telephonNumber = prefs.getString(
					GetRawData.telephonNumberPreference, "000000000");
			messageText = parseTextMessage(messageText);
			// SmsManager sms = SmsManager.getDefault();
			// sms.sendTextMessage(telephonNumber, null, messageText, null,
			// null);
			Log.i(getClass().getSimpleName(), "Message sent to: "
					+ telephonNumber);
			Log.i(getClass().getSimpleName(), "Message sent: " + messageText);
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
		String date = String.format(Locale.getDefault(), "%d:%d:%d %d/%d/%d",
				c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
				c.get(Calendar.SECOND), c.get(Calendar.DAY_OF_MONTH),
				c.get(Calendar.MONTH), c.get(Calendar.YEAR));

		resultMessage = resultMessage.replace("%d", date);
		return resultMessage;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onLocationChanged(Location location) {
		currentLocation = location;

		inSafeZone(location);
	}

	private void inSafeZone(Location actual) {
		if (actual.distanceTo(middleOfSafeZone) > GetRawData.maximumSafeDistance) {
			raiseAlarm();
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

}
