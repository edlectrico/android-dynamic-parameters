package com.example.deviceinfo;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

	private TextView batteryPercent;
	private float mLastX, mLastY, mLastZ;
	private boolean mInitialized;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private final float NOISE = 2.0F;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mInitialized = false;
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);

		Log.i("Brand", android.os.Build.BRAND + " " + android.os.Build.MODEL);
		Log.i("Android Release Version", android.os.Build.VERSION.RELEASE);
		Log.i("Android SDK Version",
				String.valueOf(android.os.Build.VERSION.SDK_INT));
		Log.i("Available CPU", getDeviceCPU() + " GHz");
		Log.i("Available memory", getDeviceMemory() + " MB");
		Log.i("Device display's inches", getDeviceInches() + " inches");

		if (this.getResources().getConfiguration().orientation == 1) {
			Log.i("Orientation", "portrait");
		} else if (this.getResources().getConfiguration().orientation == 2) {
			Log.i("Orientation", "landscape");
		}

		try {
			int brightness = android.provider.Settings.System.getInt(
					getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
			Log.i("Brightness", String.valueOf(brightness));
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}

		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		Log.i("Volume", String.valueOf(audioManager
				.getStreamVolume(AudioManager.STREAM_RING)));

		batteryPercent = (TextView) this.findViewById(R.id.batteryLevel);
		getBatteryPercentage();

	}

	private void getBatteryPercentage() {
		BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				context.unregisterReceiver(this);
				int currentLevel = intent.getIntExtra(
						BatteryManager.EXTRA_LEVEL, -1);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				int level = -1;
				if (currentLevel >= 0 && scale > 0) {
					level = (currentLevel * 100) / scale;
				}
				batteryPercent.setText("Battery Level Remaining: " + level
						+ "%");
				Log.i("Remaining battery", level + "%");
			}
		};
		IntentFilter batteryLevelFilter = new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(batteryLevelReceiver, batteryLevelFilter);
	}

	private float getDeviceCPU() {
		try {
			RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
			String load = reader.readLine();

			String[] toks = load.split(" ");

			long idle1 = Long.parseLong(toks[5]);
			long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			try {
				Thread.sleep(360);
			} catch (Exception e) {
			}

			reader.seek(0);
			load = reader.readLine();
			reader.close();

			toks = load.split(" ");

			long idle2 = Long.parseLong(toks[5]);
			long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			return (float) (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return 0;
	}

	private long getDeviceMemory() {
		MemoryInfo mi = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);

		return mi.availMem / 1048576L;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		if (!mInitialized) {
			mLastX = x;
			mLastY = y;
			mLastZ = z;
			System.out.println("X: " + "0.0");
			System.out.println("Y: " + "0.0");
			System.out.println("Z: " + "0.0");
			mInitialized = true;
		} else {
			float deltaX = Math.abs(mLastX - x);
			float deltaY = Math.abs(mLastY - y);
			float deltaZ = Math.abs(mLastZ - z);
			if (deltaX < NOISE)
				deltaX = (float) 0.0;
			if (deltaY < NOISE)
				deltaY = (float) 0.0;
			if (deltaZ < NOISE)
				deltaZ = (float) 0.0;
			mLastX = x;
			mLastY = y;
			mLastZ = z;
			
			Log.i("X", Float.toString(deltaX));
			Log.i("Y", Float.toString(deltaY));
			Log.i("Z", Float.toString(deltaZ));
		}
	}

	public double getDeviceInches() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
		double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
		double screenInches = Math.sqrt(x + y);

		return screenInches;
	}

}
