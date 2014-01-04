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
import com.imagpay.helper.M1Helper;

public class M1CardActivity extends MyActivity {
	private SwipeHandler _handler;
	private Settings _settings;
	private MessageHandler _msg;
	private Handler _ui;
	private boolean _testFlag = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_m1card);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.title_m1card);
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

		Button btn = (Button) findViewById(R.id.btnsetpwd);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new Thread() {
					public void run() {
						String send;
						String resp =_settings.reset(Settings.SLOT_M1);
						Log.d(TAG, "M1 Reset: " + resp);
						
						//int sectorIdx, String oldPwdType, String oldPwd, String newPwdType, String newPwd
						send = M1Helper.getWritePWDCommand(0, M1Helper.PWD_TYPE_0A, "ff ff ff ff ff ff", M1Helper.PWD_TYPE_0A, "ff ff ff ff ff ff");
						Log.d(TAG, "Sector 0 Set Password: " + send);
						sendMessage("Sector 0 Set Password: " + send);
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Response: " + resp);
						if (resp != null && resp.startsWith("6f 6b 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Written password successfully!");
						} else {
							sendMessage("Written password unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						send = M1Helper.getWritePWDCommand(1, M1Helper.PWD_TYPE_0B, "ff ff ff ff ff ff", M1Helper.PWD_TYPE_0B, "ff ff ff ff ff ff");
						Log.d(TAG, "Sector 1 Set Password: " + send);
						sendMessage("Sector 1 Set Password: " + send);
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Response: " + resp);
						if (resp != null && resp.startsWith("6f 6b 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Written password successfully!");
						} else {
							sendMessage("Written password unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						send = M1Helper.getWritePWDCommand(2, M1Helper.PWD_TYPE_0A, "ff ff ff ff ff ff", M1Helper.PWD_TYPE_0A, "ff ff ff ff ff ff");
						Log.d(TAG, "Sector 2 Set Password: " + send);
						sendMessage("Sector 2 Set Password: " + send);
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Response: " + resp);
						if (resp != null && resp.startsWith("6f 6b 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Written password successfully!");
						} else {
							sendMessage("Written password unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						_settings.off(Settings.SLOT_M1);
					}
				}.start();
			}
		});

		btn = (Button) findViewById(R.id.btnsetautoread);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String send = M1Helper.getSetAutoReadCommand();
				Log.d(TAG, "Set auto read");
				sendMessage("Set auto read");
				String resp = _settings.getDataWithCipherCode(send);
				Log.d(TAG, "Response: " + resp);
				if (resp != null && resp.startsWith("6f 6b 3f 00 00 00 00 00 00 00 00")) {
					sendMessage("Set auto read successfully!");
				} else {
					sendMessage("Set auto read unsuccessfully!");
				}
			}
		});

		btn = (Button) findViewById(R.id.btnsetmanualread);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String send = M1Helper.getSetManualReadCommand();
				Log.d(TAG, "Set manual read");
				sendMessage("Set manual read");
				String resp = _settings.getDataWithCipherCode(send);
				Log.d(TAG, "Response: " + resp);
				if (resp != null && resp.startsWith("6f 6b 3f 00 00 00 00 00 00 00 00")) {
					sendMessage("Set manual read successfully!");
				} else {
					sendMessage("Set manual read unsuccessfully!");
				}
			}
		});
		
		btn = (Button) findViewById(R.id.btnreadcard);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new Thread() {
					public void run() {
						String send;
						String resp =_settings.reset(Settings.SLOT_M1);
						Log.d(TAG, "M1 Reset: " + resp);
		
						// Read sector(No password)
//						send = M1Helper.getReadSectorCommand(0);
//						Log.d(TAG, "Read sector 0");
//						sendMessage("Read sector 0");
//						resp = _settings.getDataWithCipherCode(send);
//						Log.d(TAG, "Response: " + resp);
//						sendMessage("Response: " + resp);
		
						// Read sector(password)
						// int sectorIdx, String pwdType, String pwd
						send = M1Helper.getReadSectorCommand(0, M1Helper.PWD_TYPE_0A, "ff ff ff ff ff ff");
						Log.d(TAG, "Read sector 0");
						sendMessage("Read sector 0");
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Response: " + resp);
						if (resp != null && !resp.startsWith("ff 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Read successfully!");
						} else {
							sendMessage("Read unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						send = M1Helper.getReadSectorCommand(1, M1Helper.PWD_TYPE_0B, "ff ff ff ff ff ff");
						Log.d(TAG, "Read sector 1");
						sendMessage("Read sector 1");
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Response: " + resp);
						if (resp != null && !resp.startsWith("ff 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Read successfully!");
						} else {
							sendMessage("Read unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						send = M1Helper.getReadSectorCommand(2, M1Helper.PWD_TYPE_0A, "ff ff ff ff ff ff");
						Log.d(TAG, "Read sector 2");
						sendMessage("Read sector 2");
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Response: " + resp);
						if (resp != null && !resp.startsWith("ff 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Read successfully!");
						} else {
							sendMessage("Read unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						// Read block(First check pwd, second read block data)
						// int blockIdx, String pwdType, String pwd
						send = M1Helper.getCheckPWDCommand(0, M1Helper.PWD_TYPE_0A, "ff ff ff ff ff ff");
						Log.d(TAG, "Check PWD: " + send);
						sendMessage("Check PWD: " + send);
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Response: " + resp);
						if (resp != null && !resp.startsWith("ff 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Read successfully!");
						} else {
							sendMessage("Read unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						send = M1Helper.getReadBlockCommand(0);
						Log.d(TAG, "Read block 0");
						sendMessage("Read block 0");
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Block 0 data: " + resp);
						if (resp != null && !resp.startsWith("ff 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Read successfully!");
						} else {
							sendMessage("Read unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						_settings.off(Settings.SLOT_M1);
					}
				}.start();
			}
		});
		
		btn = (Button) findViewById(R.id.btnwritecard);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new Thread() {
					public void run() {
						// M1 card have 4 sector and 16 block. the block 0 of sector 0 is the data of factory, the block 3 of sector 3 is the data of password. so you can not write the two block
						String send;
						String resp =_settings.reset(Settings.SLOT_M1);
						Log.d(TAG, "M1 Reset: " + resp);
				
						// Write sector(No password)
						// int sectorIdx, String data
//						send = M1Helper.getWriteSectorCommand(1, "aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd");
//						Log.d(TAG, "Write sector 0");
//						sendMessage("Write sector 0");
//						resp = _settings.getDataWithCipherCode(send);
//						Log.d(TAG, "Response: " + resp);
//						sendMessage("Response: " + resp);

						// Write sector(password)
						// int sectorIdx, String pwdType, String pwd, String data
//						send = M1Helper.getWriteSectorCommand(0, M1Helper.PWD_TYPE_0A, "ff ff ff ff ff ff",
//								"aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd");
//						Log.d(TAG, "Write sector 0");
//						sendMessage("Write sector 0");
//						resp = _settings.getDataWithCipherCode(send);
//						Log.d(TAG, "Response: " + resp);
//						if (resp != null && resp.startsWith("6f 6b 3f 00 00 00 00 00 00 00 00")) {
//							sendMessage("Written successfully!");
//						} else {
//							sendMessage("Written unsuccessfully!");
//						}
//						try {Thread.sleep(2000);} catch (Exception e) {}
						
						send = M1Helper.getWriteSectorCommand(1, M1Helper.PWD_TYPE_0B, "ff ff ff ff ff ff",
								"aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd");
						Log.d(TAG, "Write sector 1");
						sendMessage("Write sector 1");
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Response: " + resp);
						if (resp != null && resp.startsWith("6f 6b 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Written successfully!");
						} else {
							sendMessage("Written unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						send = M1Helper.getWriteSectorCommand(2, M1Helper.PWD_TYPE_0A, "ff ff ff ff ff ff",
								"aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd");
						Log.d(TAG, "Write sector 2");
						sendMessage("Write sector 2");
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Response: " + resp);
						if (resp != null && resp.startsWith("6f 6b 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Written successfully!");
						} else {
							sendMessage("Written unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						// Write block(First check pwd, second read block data)
						// int blockIdx, String pwdType, String pwd
						send = M1Helper.getCheckPWDCommand(1, M1Helper.PWD_TYPE_0A, "ff ff ff ff ff ff");
						Log.d(TAG, "Check PWD: " + send);
						sendMessage("Check PWD: " + send);
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Response: " + resp);
						if (resp != null && resp.startsWith("6f 6b 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Written successfully!");
						} else {
							sendMessage("Written unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						send = M1Helper.getWriteBlockCommand(1, "aa aa aa aa bb bb bb bb cc cc cc cc dd dd dd dd");
						Log.d(TAG, "Write block 1");
						sendMessage("Write block 1");
						resp = _settings.getDataWithCipherCode(send);
						Log.d(TAG, "Block 1 data: " + resp);
						if (resp != null && resp.startsWith("6f 6b 3f 00 00 00 00 00 00 00 00")) {
							sendMessage("Written successfully!");
						} else {
							sendMessage("Written unsuccessfully!");
						}
						try {Thread.sleep(2000);} catch (Exception e) {}
						
						_settings.off(Settings.SLOT_M1);
					}
				}.start();
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