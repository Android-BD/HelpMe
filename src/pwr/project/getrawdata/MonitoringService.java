package pwr.project.getrawdata;

import java.util.Calendar;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.widget.Toast;


@SuppressLint("InlinedApi")
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
	private boolean isMonitoring;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		sredniaZPomiarow = new Srednia<WynikiACC>();
		isMonitoring = true;
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
		middleOfSafeZone = mLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
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

			if (srednia < GetRawData.threshold && isMonitoring) {
				isMonitoring = false;
				raiseAlarm();
			}
		}
	}

	private void raiseAlarm() {
		if (GetRawData.isRunning) {
			sendBroadcast(new Intent(ALARM));
		} else {
			createNotification();
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

	private void createNotification() {

		Intent resultIntent = new Intent(this, GetRawData.class);
		resultIntent.putExtra(ALARM, true);
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
				resultIntent, PendingIntent.FLAG_ONE_SHOT
						| PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.error)
				.setContentTitle(getResources().getString(R.string.app_name))
				.setContentIntent(resultPendingIntent)
				.setContentText("Alarm zostal wykryty!!").setAutoCancel(true);

		int mNotificationId = 001;
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotifyMgr.notify(mNotificationId, mBuilder.build());
	}

	@Override
	public void onDestroy() {
		mSensorManager.unregisterListener(this);
		mLocationManager.removeUpdates(this);
		super.onDestroy();
	}

	private void sendMessage() {
		String messageText = prefs.getString(
				GetRawData.messageTextPreference,"");

		if (GetRawData.isSendingMessage) {
			
			String telephonNumber = prefs.getString(
					GetRawData.telephonNumberPreference, "000000000");
			
			messageText = parseTextMessage(messageText);
			
			SmsManager sms = SmsManager.getDefault();
			sms.sendTextMessage(telephonNumber, null, messageText, null, null);
			
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
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

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
	public void onStatusChanged(String provider, int status, Bundle extras) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onProviderDisabled(String provider) {}

}
