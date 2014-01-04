package com.imagpay.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.imagpay.MessageHandler;
import com.imagpay.Settings;
import com.imagpay.SwipeEvent;
import com.imagpay.SwipeHandler;
import com.imagpay.SwipeListener;

public class RFCardActivity extends MyActivity {
	private SwipeHandler _handler;
	private Settings _settings;
	private MessageHandler _msg;
	private Handler _ui;
	private boolean _testFlag = false;
	private boolean _runFlag = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_rfcard);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.title_rfcard);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		_handler = new SwipeHandler(this);
		_settings = new Settings(_handler);
		_msg = new MessageHandler((TextView) findViewById(R.id.status));
		_ui = new Handler(Looper.myLooper());
		_handler.addSwipeListener(new SwipeListener() {
			@Override
			public void onReadData(SwipeEvent event) {
			}

			@Override
			public void onParseData(SwipeEvent event) {
				if (_testFlag)
					return;
				String result = event.getValue();
				// hex string message
				sendMessage("Final(16)=>% " + result);
			}

			@Override
			public void onDisconnected(SwipeEvent event) {
				sendMessage("Device is disconnected!");
				toggleConnectStatus();
			}

			@Override
			public void onConnected(SwipeEvent event) {
				sendMessage("Device is connected!");
				checkDevice();
			}

			@Override
			public void onStarted(SwipeEvent event) {
				if (_testFlag)
					return;
				sendMessage("Device is started");
				toggleConnectStatus();
			}

			@Override
			public void onStopped(SwipeEvent event) {
				if (_testFlag)
					return;
				sendMessage("Device is stopped");
				toggleConnectStatus();
			}
		});

		Button btn = (Button) findViewById(R.id.btnback);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finishAll();
			}
		});
	}

	private void checkDevice() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (!_handler.isConnected()) {
					toggleConnectStatus();
					return;
				}
				if (_handler.isPowerOn()) {
					toggleConnectStatus();
					return;
				}
				if (_handler.isReadable()) {
					sendMessage("Device is ready");
					_handler.powerOn();
					checkChipCard();
				} else {
					_testFlag = false;
					sendMessage("Please wait! testing parameter now");
					if (_handler.test() && _handler.isReadable()) {
						_testFlag = false;
						sendMessage("Device is ready");
						_handler.powerOn();
						checkChipCard();
					} else {
						_testFlag = false;
						sendMessage("Device is not supported or Please close some audio effects(SRS/DOLBY/BEATS/JAZZ/Classic...) and insert device!");
					}
				}
				toggleConnectStatus();
			}
		}).start();
	}

	private void toggleConnectStatus() {
		_ui.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (_handler.isConnected() && _handler.isPowerOn()
						&& _handler.isReadable()) {
					ImageView iv = (ImageView) findViewById(R.id.connect);
					iv.setVisibility(View.VISIBLE);
					iv = (ImageView) findViewById(R.id.disconnect);
					iv.setVisibility(View.INVISIBLE);
				} else {
					ImageView iv = (ImageView) findViewById(R.id.connect);
					iv.setVisibility(View.INVISIBLE);
					iv = (ImageView) findViewById(R.id.disconnect);
					iv.setVisibility(View.VISIBLE);
				}
			}
		}, 500);
	}

	private void sendMessage(String msg) {
		_msg.sendMessage(msg);
	}

	public void onStart() {
		super.onStart();
		checkDevice();
	}

	public void onStop() {
		_handler.powerOff();
		super.onStop();
	}

	public void onDestroy() {
		_handler.onDestroy();
		super.onDestroy();
	}
	
	private void checkChipCard() {
		if (_runFlag) return;
		_runFlag = true;
		new Thread() {
			public void run() {
				boolean chargeCard = true;
				while (_handler.isPowerOn()) {
					try {Thread.sleep(1000);} catch (Exception e) {}
					// have a chip card or not
					String data = _settings.reset(Settings.SLOT_RF);
//					String data = _settings.getDataWithCipherCode("03 07 00 05");
					Log.d(TAG, "Check chip card: " + data);
					if (chargeCard && data != null && !data.startsWith("ff 3f 00 00 00 00 00 00 00 00")) {
						chargeCard = false;
						exchangeAPDU();
					} else if (!chargeCard && data != null && data.startsWith("ff 3f 00 00 00 00 00 00 00 00")){
						chargeCard = true;
					}
				}
				_runFlag = false;
			}
		}.start();
	}
	
	private void exchangeAPDU() {
		String data = _settings.reset(Settings.SLOT_RF);
		if (data == null || data.startsWith("ff 3f 00 00 00 00 00 00 00 00")) {
			Log.d(TAG, "Call reset unsuccessfully! " + data);
			return;
		}
		data = _settings.getDataWithAPDU(Settings.SLOT_RF, "00 a4 04 00 0e 31 50 41 59 2e 53 59 53 2e 44 44 46 30 31");
		if (data == null || data.startsWith("ff 3f 00 00 00 00 00 00 00 00")) {
			Log.d(TAG, "Call pse unsuccessfully! " + data);
			return;
		}
		data = _settings.getDataWithAPDU(Settings.SLOT_RF, "00 c0 00 00 20");
		if (data == null || data.startsWith("ff 3f 00 00 00 00 00 00 00 00")) {
			Log.d(TAG, "Call pse unsuccessfully! " + data);
			return;
		}
		// more apdu .......
		_settings.off(Settings.SLOT_RF);
	}
}