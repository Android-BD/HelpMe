package pwr.project.getrawdata;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

public class MonitoringService extends Service implements SensorEventListener {
	
	Srednia<WynikiACC> sredniaZPomiarow;
	private SensorManager mSensorManager;
	private Sensor accelerometer;

	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		sredniaZPomiarow = new Srednia<WynikiACC>();
		mSensorManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, accelerometer,
				SensorManager.SENSOR_DELAY_FASTEST);	
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

			float srednia = sredniaZPomiarow.sredniaCalkowitaZWieluPomiarow(new WynikiACC(
					rawX, rawY, rawZ));

			//			log("Srednia = " + srednia);
			if (srednia < GetRawData.threshold ) {
				raiseAlarm();
			}
		}		
	}

	private void raiseAlarm(){
		Log.i(getClass().getSimpleName(), "YUPII");
		mSensorManager.unregisterListener(this);
		stopSelf();
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

}
