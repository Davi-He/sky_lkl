package com.imagpay.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.imagpay.iMagPayApp;

public class SettingsActivity extends MyActivity {
	private SwipeHandler _handler;
	private Settings _settings;
	private MessageHandler _msg;
	private Handler _ui;
	private boolean _testFlag = false;
	
	private String _sn, _des, _bdk, _ksn;
	private boolean _plainFlag = false;
	private boolean _desFlag = false;
	private boolean _dukptFlag = false;
	private boolean _snFlag = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_settings);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.title_settings);
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
				if (_snFlag && result.startsWith("31 " + _sn)) {
					sendMessage("Written SN successfully!");
					_snFlag = false;
				} else if (_plainFlag && result.startsWith("6f 6b 3f")) {
					sendMessage("Set plain text mode successfully!");
					_plainFlag = false;
				} else if (_desFlag && result.startsWith("6f 6b 3f")) {
					sendMessage("Set 3DES mode successfully!");
					_desFlag = false;
				} else if (_dukptFlag && result.startsWith("6f 6b 3f")) {
					sendMessage("Set DUKPT mode successfully!");
					_dukptFlag = false;
				} else if (_desFlag && result.startsWith("31 " + _des)) {
					_settings.writeMode(Settings.TYPE_3DES);
				} else if (_dukptFlag && result.startsWith("31 " + _bdk + " " + _ksn)) {
					_settings.writeMode(Settings.TYPE_DUKPT);
//					_settings.writeMode(Settings.TYPE_DUKPT_HSM);
				}
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

		Button btn = (Button) findViewById(R.id.btnen);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getApp().setLanguage(iMagPayApp.LOCAL_EN);
				Intent intent = new Intent();
				intent.setClass(SettingsActivity.this, SettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});
		
		btn = (Button) findViewById(R.id.btncn);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getApp().setLanguage(iMagPayApp.LOCAL_CN);
				Intent intent = new Intent();
				intent.setClass(SettingsActivity.this, SettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});
		
		btn = (Button) findViewById(R.id.btnplaintext);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_snFlag = false;
				_plainFlag = true;
				_desFlag = false;
				_dukptFlag = false;
				_settings.writeMode(Settings.TYPE_PLAINTEXT);
			}
		});

		btn = (Button) findViewById(R.id.btndes);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_snFlag = false;
				_plainFlag = false;
				_desFlag = true;
				_dukptFlag = false;
				_des = "aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd";
				_settings.writeDESKey(_des);
			}
		});

		btn = (Button) findViewById(R.id.btndukpt);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_snFlag = false;
				_plainFlag = false;
				_desFlag = false;
				_dukptFlag = true;
				_bdk = "aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd";
				_ksn = "11 22 33 44 55 66 77 00 00 00";
				_settings.writeDUKPTKey(_bdk, _ksn);
			}
		});

		btn = (Button) findViewById(R.id.btnwsn);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_snFlag = true;
				_plainFlag = false;
				_desFlag = false;
				_dukptFlag = false;
				_sn = "201309300001";
				_sn = _settings.formatSN(_sn);
				sendMessage("Writing SN: " + _sn);
				_settings.writeSN(_sn);
			}
		});

		btn = (Button) findViewById(R.id.btnrsn);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendMessage("SN: " + _settings.getSN());
			}
		});
		
		btn = (Button) findViewById(R.id.btnclear);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_handler.powerOff();
				_handler.clearEnvironment();
				checkDevice();
			}
		});

		btn = (Button) findViewById(R.id.btnback);
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
				if (_handler.isWritable()) {
					sendMessage("Device is ready");
					_handler.powerOn();
				} else {
					_testFlag = false;
					sendMessage("Please wait! testing parameter now");
					if (_handler.test() && _handler.isWritable()) {
						_testFlag = false;
						sendMessage("Device is ready");
						_handler.powerOn();
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
}