package com.imagpay.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.imagpay.MessageHandler;
import com.imagpay.Settings;
import com.imagpay.SwipeEvent;
import com.imagpay.SwipeListener;
import com.imagpay.emv.EMVApp;
import com.imagpay.emv.EMVCapk;
import com.imagpay.emv.EMVConstants;
import com.imagpay.emv.EMVHandler;
import com.imagpay.emv.EMVListener;
import com.imagpay.emv.EMVParam;
import com.imagpay.emv.EMVRevoc;
import com.imagpay.utils.StringUtils;

public class ICCardActivity extends MyActivity {
	private EMVHandler _handler;
	private Settings _settings;
	private MessageHandler _msg;
	private Handler _ui;
	private boolean _testFlag = false;
	private boolean _runFlag = false;
	
	private ProgressDialog _progress;
	private String _pin;
	private boolean _readingPin = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_iccard);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.title_iccard);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		_handler = new EMVHandler(this);
		// if call setShowAPDU(true), SDK will show EMV kernel log
		_handler.setShowAPDU(true);
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
				
				if (_readingPin) {
					// cancel
					if (event.getValue().startsWith("23 fe 23 00 00 00 00 00 00 00 00")) {
						_pin = "";
//						_settings.pinClose();
						_readingPin = false;
					}
					// ok
					else if (event.getValue().startsWith("23 ff ")) {
						String[] pins = event.getValue().split(" ");
						int len = 0;
						try {
							len = Integer.parseInt(pins[2], 16);
						}
						catch (Exception e) {
						}
						StringBuffer sbf = new StringBuffer();
						for (int i = 0; i < len; i++) {
							sbf.append((char) Integer.parseInt(pins[i + 3], 16));
						}
						_pin = sbf.toString();
						sendMessage("Pin: " + _pin);
//						_settings.pinClose();
						_readingPin = false;
					}
					else if (event.getValue().startsWith("23 ")) {
						String[] pins = event.getValue().split(" ");
						if (pins.length > 3 && pins[2].equals(23))
							sendMessage("Pin Count: " + _pin);
					}
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
		_handler.addEMVListener(new EMVListener() {
			@Override
			public String onSubmitData() {
				showProgressDialog("Submitting data to bank......");
				try { Thread.sleep(1000);} catch (Exception e) {}
				// submit data to payment server
				return "";
			}

			@Override
			public int onSelectApp(List apps) {
				int idx = 0;
				for (Object app : apps) {
					sendMessage("AID: " + idx + " " + app);
					idx ++;
				}
				// if have more aids, you can show a dialog for select a aid
				return 0;
			}

			@Override
			public String onReadPin(final int type, final int ucPinTryCnt) {
				closeProgressDialog();
				_pin = null;
				_readingPin = false;
				_ui.post(new Runnable() {
					@Override
					public void run() {
						String title;
						if (type == EMVConstants.EMV_CVM_ONLINE_PIN) {
//							return "4315";
							title = "Please input online pin";
						} else if (type == EMVConstants.EMV_CVM_OFF_PLAIN_PIN) {
//							return "4315";
							title = "Please input offline plain pin";
						} else {// (type == EMVConstants.EMV_CVM_OFF_ENCRYPT_PIN) {
//							return "4315";
							title = "Please input offline encrypt pin";
						}
						
						String data = _settings.pinOpen(Settings.PIN_TYPE_PLAIN_TEXT);
						// Non-PinPad
						if (data == null || !data.startsWith("6f 6b 3f 00 00 00 00 00 00 00 00")) {
							final EditText et = new EditText(ICCardActivity.this);
							new AlertDialog.Builder(ICCardActivity.this)
									.setTitle(title)
									.setView(et)
									.setPositiveButton("OK",
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														DialogInterface arg0,
														int arg1) {
													_pin = et.getText().toString();
												}
											})
									.setNegativeButton("Cancel",
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														DialogInterface arg0,
														int arg1) {
													_pin = "";
												}
											}).show();
							}
							// PinPad
							else {
								_readingPin = true;
								sendMessage(title + " with pinpad! try count=" + ucPinTryCnt);
							}
					}
				});
				// wait for pin
				while (_pin == null) { try { Thread.sleep(100); }catch (Exception e) {}}
				if ("".equalsIgnoreCase(_pin))
					showProgressDialog("Cancel......");
				else
					showProgressDialog("Verifying......");
				_readingPin = false;
				return _pin;
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
				if (_handler.isWritable()) {
					sendMessage("Device is ready");
					_handler.powerOn();
					checkChipCard();
				} else {
					_testFlag = false;
					sendMessage("Please wait! testing parameter now");
					if (_handler.test() && _handler.isWritable()) {
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
					if (!_handler.isRunning()) {
						// have a chip card or not
						String data = _settings.icReset();
						Log.d(TAG, "Check chip card: " + data);
						if (chargeCard
								&& data != null
								&& !data.startsWith("ff 3f 00 00 00 00 00 00 00 00")
								&& !data.startsWith("32 3f 00 00 00 00 00 00 00 00")
								&& !data.startsWith("33 3f 00 00 00 00 00 00 00 00")) {
							chargeCard = false;
							emv();
						} else if (!chargeCard
								&& data != null
								&& data.startsWith("ff 3f 00 00 00 00 00 00 00 00")) {
							chargeCard = true;
						}
					}
				}
				_runFlag = false;
			}
		}.start();
	}
	
	private void emv() {
		showProgressDialog("Reading data......");
		_readingPin = false;
		_ui.post(new Runnable() {
			@Override
			public void run() {
				EditText et = (EditText) findViewById(R.id.cardno);
				et.setText("");
				et = (EditText) findViewById(R.id.holder);
				et.setText("");
				et = (EditText) findViewById(R.id.expdate);
				et.setText("");
			}
		});
		
		EMVParam param = new EMVParam();
		param.setSlot((byte) 0x00);
		// if call setReadOnly(true), SDK will only read card data
		// if call setReadOnly(false), SDK will read card data and verify pin and submit data
		param.setReadOnly(false);

		param.setMerchName("4368696E61");// hex string of china
		param.setMerchCateCode("0001");
		param.setMerchId("313233343536373839303132333435");
		param.setTermId("3132333435363738");
		param.setTerminalType((byte) 0x22);
		param.setTransCurrExp((byte) 0x02);
		param.setCountryCode("0840");
		param.setTransCurrCode("0840");
		param.setTransType((byte) 0x00);

		param.setAuthAmnt(8000000);// transaction amount
		param.setOtherAmnt(0);
		Date date = new Date();
		DateFormat sdf = new SimpleDateFormat("yyMMdd");
		param.setTransDate(sdf.format(date));
		sdf = new SimpleDateFormat("HHmmss");
		param.setTransTime(sdf.format(date));

		// FIME parameters(MasterCard Test Card), if other card type, need to change.
		loadMasterCardAIDs(param);
		loadMasterCardCapks(param);
		loadMasterCardRevocs(param);
		// Visa
		loadVisaAIDs(param);
		loadVisaCapks(param);
		loadVisaRevocs(param);

		_handler.kernelInit(param);
		if (_handler.icReset() != null) {
			_handler.process();
		}
		_handler.icOff();

		_ui.post(new Runnable() {
			@Override
			public void run() {
				String data = _handler.getTLVData(0x5a);
				EditText et = (EditText) findViewById(R.id.cardno);
				if (data != null)
					et.setText(data);
				else
					et.setText("");
				
				data = _handler.getTLVData(0x5F20);
				et = (EditText) findViewById(R.id.holder);
				if (data != null)
					et.setText(new String(StringUtils.convertHexToBytes(data)));
				else
					et.setText("");
				
				data = _handler.getTLVData(0x5F24);
				et = (EditText) findViewById(R.id.expdate);
				if (data != null)
					et.setText(data);
				else
					et.setText("");
			}
		});
		closeProgressDialog();
	}
	
	private void loadVisaAIDs(EMVParam ep) {
		// Visa Credit/Debit
		EMVApp ea = new EMVApp();
		ea.setAppName("");
		ea.setAID("A0000000031010");
		ea.setSelFlag(EMVConstants.PART_MATCH);
		ea.setPriority((byte) 0x00);
		ea.setTargetPer((byte) 0x00);
		ea.setMaxTargetPer((byte) 0x00);
		ea.setFloorLimitCheck((byte) 0x01);
		ea.setFloorLimit(2000);
		ea.setThreshold((byte) 0x00);
		ea.setTACDenial("0000000000");
		ea.setTACOnline("0000001000");
		ea.setTACDefault("0000000000");
		ea.setAcquierId("000000123456");
		ea.setDDOL("039F3704");
		ea.setTDOL("0F9F02065F2A029A039C0195059F3704");
		ea.setVersion("008C");
		ep.addApp(ea);

		// Visa Electron
		ea = new EMVApp();
		ea.setAppName("");
		ea.setAID("A0000000032010");
		ea.setSelFlag(EMVConstants.PART_MATCH);
		ea.setPriority((byte) 0x00);
		ea.setTargetPer((byte) 0x00);
		ea.setMaxTargetPer((byte) 0x00);
		ea.setFloorLimitCheck((byte) 0x01);
		ea.setFloorLimit(2000);
		ea.setThreshold((byte) 0x00);
		ea.setTACDenial("0000000000");
		ea.setTACOnline("0000001000");
		ea.setTACDefault("0000000000");
		ea.setAcquierId("000000123456");
		ea.setDDOL("039F3704");
		ea.setTDOL("0F9F02065F2A029A039C0195059F3704");
		ea.setVersion("008C");
		ep.addApp(ea);

		// Visa Plus
		ea = new EMVApp();
		ea.setAppName("");
		ea.setAID("A0000000038010");
		ea.setSelFlag(EMVConstants.PART_MATCH);
		ea.setPriority((byte) 0x00);
		ea.setTargetPer((byte) 0x00);
		ea.setMaxTargetPer((byte) 0x00);
		ea.setFloorLimitCheck((byte) 0x01);
		ea.setFloorLimit(2000);
		ea.setThreshold((byte) 0x00);
		ea.setTACDenial("0000000000");
		ea.setTACOnline("0000001000");
		ea.setTACDefault("0000000000");
		ea.setAcquierId("000000123456");
		ea.setDDOL("039F3704");
		ea.setTDOL("0F9F02065F2A029A039C0195059F3704");
		ea.setVersion("008C");
		ep.addApp(ea);
	}

	private void loadMasterCardAIDs(EMVParam ep) {
		// MasterCard Credit/Debit
		EMVApp ea = new EMVApp();
		ea.setAppName("");
		ea.setAID("A0000000041010");
		ea.setSelFlag(EMVConstants.PART_MATCH);
		ea.setPriority((byte) 0x00);
		ea.setTargetPer((byte) 0x00);
		ea.setMaxTargetPer((byte) 0x00);
		ea.setFloorLimitCheck((byte) 0x01);
		ea.setFloorLimit(2000);
		ea.setThreshold(0x00);
		ea.setTACDenial("0000000000");
		ea.setTACOnline("0000001000");
		ea.setTACDefault("0000000000");
		ea.setAcquierId("000000123456");
		ea.setDDOL("039F3704");
		ea.setTDOL("0F9F02065F2A029A039C0195059F3704");
		ea.setVersion("008C");
		ep.addApp(ea);

		// Maestro
		ea = new EMVApp();
		ea.setAppName("");
		ea.setAID("A0000000043060");
		ea.setSelFlag(EMVConstants.PART_MATCH);
		ea.setPriority((byte) 0x00);
		ea.setTargetPer((byte) 0x00);
		ea.setMaxTargetPer((byte) 0x00);
		ea.setFloorLimitCheck((byte) 0x01);
		ea.setFloorLimit(2000);
		ea.setThreshold(0x00);
		ea.setTACDenial("0000000000");
		ea.setTACOnline("0000001000");
		ea.setTACDefault("0000000000");
		ea.setAcquierId("000000123456");
		ea.setDDOL("039F3704");
		ea.setTDOL("0F9F02065F2A029A039C0195059F3704");
		ea.setVersion("008C");
		ep.addApp(ea);

		// Cirrus
		ea = new EMVApp();
		ea.setAppName("");
		ea.setAID("A0000000046000");
		ea.setSelFlag(EMVConstants.PART_MATCH);
		ea.setPriority((byte) 0x00);
		ea.setTargetPer((byte) 0x00);
		ea.setMaxTargetPer((byte) 0x00);
		ea.setFloorLimitCheck((byte) 0x01);
		ea.setFloorLimit(2000);
		ea.setThreshold(0x00);
		ea.setTACDenial("0000000000");
		ea.setTACOnline("0000001000");
		ea.setTACDefault("0000000000");
		ea.setAcquierId("000000123456");
		ea.setDDOL("039F3704");
		ea.setTDOL("0F9F02065F2A029A039C0195059F3704");
		ea.setVersion("008C");
		ep.addApp(ea);
	}

	private void loadAmericanExpressAIDs(EMVParam ep) {
		// American
		EMVApp ea = new EMVApp();
		ea.setAppName("");
		ea.setAID("A00000002501");
		ea.setSelFlag(EMVConstants.PART_MATCH);
		ea.setPriority((byte) 0x00);
		ea.setTargetPer((byte) 0x00);
		ea.setMaxTargetPer((byte) 0x00);
		ea.setFloorLimitCheck((byte) 0x01);
		ea.setFloorLimit(2000);
		ea.setThreshold((byte) 0x00);
		ea.setTACDenial("0000000000");
		ea.setTACOnline("0000001000");
		ea.setTACDefault("0000000000");
		ea.setAcquierId("000000123456");
		ea.setDDOL("039F3704");
		ea.setTDOL("0F9F02065F2A029A039C0195059F3704");
		ea.setVersion("008C");
		ep.addApp(ea);
	}

	private void loadInteracAIDs(EMVParam ep) {
		// Interac
		EMVApp ea = new EMVApp();
		ea.setAppName("");
		ea.setAID("A0000002771010");
		ea.setSelFlag(EMVConstants.PART_MATCH);
		ea.setPriority((byte) 0x00);
		ea.setTargetPer((byte) 0x00);
		ea.setMaxTargetPer((byte) 0x00);
		ea.setFloorLimitCheck((byte) 0x01);
		ea.setFloorLimit(2000);
		ea.setThreshold((byte) 0x00);
		ea.setTACDenial("0000000000");
		ea.setTACOnline("0000001000");
		ea.setTACDefault("0000000000");
		ea.setAcquierId("000000123456");
		ea.setDDOL("039F3704");
		ea.setTDOL("0F9F02065F2A029A039C0195059F3704");
		ea.setVersion("008C");
		ep.addApp(ea);
	}

	private void loadDiscoverAIDs(EMVParam ep) {
		// Discover
		EMVApp ea = new EMVApp();
		ea.setAppName("");
		ea.setAID("A0000001523010");
		ea.setSelFlag(EMVConstants.PART_MATCH);
		ea.setPriority((byte) 0x00);
		ea.setTargetPer((byte) 0x00);
		ea.setMaxTargetPer((byte) 0x00);
		ea.setFloorLimitCheck((byte) 0x01);
		ea.setFloorLimit(2000);
		ea.setThreshold((byte) 0x00);
		ea.setTACDenial("0000000000");
		ea.setTACOnline("0000001000");
		ea.setTACDefault("0000000000");
		ea.setAcquierId("000000123456");
		ea.setDDOL("039F3704");
		ea.setTDOL("0F9F02065F2A029A039C0195059F3704");
		ea.setVersion("008C");
		ep.addApp(ea);
	}

	private void loadVisaCapks(EMVParam ep) {
		// 01
		EMVCapk ec = new EMVCapk();
		ec.setRID("A000000003");
		ec.setKeyID((byte) 0x01);
		ec.setModul("C696034213D7D8546984579D1D0F0EA5"
				+ "19CFF8DEFFC429354CF3A871A6F7183F"
				+ "1228DA5C7470C055387100CB935A712C"
				+ "4E2864DF5D64BA93FE7E63E71F25B1E5"
				+ "F5298575EBE1C63AA617706917911DC2"
				+ "A75AC28B251C7EF40F2365912490B939"
				+ "BCA2124A30A28F54402C34AECA331AB6"
				+ "7E1E79B285DD5771B5D9FF79EA630B75");
		ec.setExponent("03");
		ec.setExpDate("491231");// YYMMDD
		ec.setCheckSum("D34A6A776011C7E7CE3AEC5F03AD2F8CFC5503CC");
		ep.addCapk(ec);
		// 07
		ec = new EMVCapk();
		ec.setRID("A000000003");
		ec.setKeyID((byte) 0x07);
		ec.setModul("A89F25A56FA6DA258C8CA8B40427D927"
				+ "B4A1EB4D7EA326BBB12F97DED70AE5E4"
				+ "480FC9C5E8A972177110A1CC318D06D2"
				+ "F8F5C4844aC5FA79A4DC470BB11ED635"
				+ "699C17081B90F1B984F12E92C1C52927"
				+ "6D8AF8EC7F28492097D8CD5BECEA16FE"
				+ "4088F6CFAB4A1B42328A1B996F9278B0"
				+ "B7E3311CA5EF856C2F888474B83612A8"
				+ "2E4E00D0CD4069A6783140433D50725F");
		ec.setExponent("03");
		ec.setExpDate("491231");// YYMMDD
		ec.setCheckSum("B4BC56CC4E88324932CBC643D6898F6FE593B172");
		ep.addCapk(ec);
		// 08
		ec = new EMVCapk();
		ec.setRID("A000000003");
		ec.setKeyID((byte) 0x08);
		ec.setModul("D9FD6ED75D51D0E30664BD157023EAA1"
				+ "FFA871E4DA65672B863D255E81E137A5"
				+ "1DE4F72BCC9E44ACE12127F87E263D3a"
				+ "F9DD9CF35CA4A7B01E907000BA85D249"
				+ "54C2FCA3074825DDD4C0C8F186CB020F"
				+ "683E02F2DEAD3969133F06F7845166AC"
				+ "EB57CA0FC2603445469811D293BFEFBA"
				+ "FAB57631B3DD91E796BF850A25012F1A"
				+ "E38F05AA5C4D6D03B1DC2E5686127859"
				+ "38BBC9B3CD3A910C1DA55A5A9218ACE0"
				+ "F7A21287752682F15832A678D6E1ED0B");
		ec.setExponent("03");
		ec.setExpDate("491231");// YYMMDD
		ec.setCheckSum("20D213126955DE205ADC2FD2822BD22DE21CF9A8");
		ep.addCapk(ec);
		// 09
		ec = new EMVCapk();
		ec.setRID("A000000003");
		ec.setKeyID((byte) 0x09);
		ec.setModul("9D912248DE0A4E39C1A7DDE3F6D25889"
				+ "92C1A4095AFBD1824D1BA74847F2BC49"
				+ "26D2EFD904B4B54954CD189A54C5D117"
				+ "9654F8F9B0D2AB5F0357EB642FEDA95D"
				+ "3912C6576945FAB897E7062CAA44A4AA"
				+ "06B8FE6E3DBA18AF6aE3738E30429EE9"
				+ "BE03427C9D64F695FA8CAB4BFE376853"
				+ "EA34AD1D76BFCAD15908C077FFE6DC55"
				+ "21ECEF5D278A96E26F57359FFAEDA194"
				+ "34B937F1AD999DC5C41EB11935B44C18"
				+ "100E857F431A4A5A6BB65114F174C2D7"
				+ "B59FDF237D6BB1DD0916E644D709DED5"
				+ "6481477C75D95CDD68254615F7740EC0"
				+ "7F330AC5D67BCD75BF23D28a140826C0"
				+ "26DBDE971A37CD3EF9B8DF644AC38501" + "0501EFC6509D7A41");
		ec.setExponent("03");
		ec.setExpDate("491231");
		ec.setCheckSum("1FF80A40173F52D7D27E0F26A146A1C8CCB29046");
		ep.addCapk(ec);
		// 92
		ec = new EMVCapk();
		ec.setRID("A000000003");
		ec.setKeyID((byte) 0x92);
		ec.setModul("996AF56F569187D09293C14810450ED8"
				+ "EE3357397B18A2458EFAA92DA3B6DF65"
				+ "14EC060195318FD43BE9B8F0CC669E3F"
				+ "844057CBDDF8BDA191BB64473BC8DC9A"
				+ "730DB8F6B4EDE3924186FFD9B8C77357"
				+ "89C23A36BA0B8AF65372EB57EA5D89E7"
				+ "D14E9C7B6B557460F10885DA16AC923F"
				+ "15AF3758F0F03EBD3C5C2C949CBA306D"
				+ "B44E6A2C076C5F67E281D7EF56785DC4"
				+ "D75945E491F01918800A9E2DC66F6008"
				+ "0566CE0DAF8D17EAD46AD8E30A247C9F");
		ec.setExponent("03");
		ec.setExpDate("491231");// YYMMDD
		ec.setCheckSum("429C954A3859CEF91295F663C963E582ED6EB253");
		ep.addCapk(ec);
		// 94
		ec = new EMVCapk();
		ec.setRID("A000000003");
		ec.setKeyID((byte) 0x94);
		ec.setModul("ACD2B12302EE644F3F835ABD1FC7A6F6"
				+ "2CCE48FFEC622AA8EF062BEF6FB8BA8B"
				+ "C68BBF6AB5870EED579BC3973E121303"
				+ "D34841A796D6DCBC41DBF9E52C460979"
				+ "5C0CCF7EE86FA1D5CB041071ED2C51D2"
				+ "202F63F1156C58A92D38BC60BDF424E1"
				+ "776E2BC9648078A03B36FB554375FC53"
				+ "D57C73F5160EA59F3AFC5398EC7B6775"
				+ "8D65C9BFF7828B6B82D4BE124A416AB7"
				+ "301914311EA462C19F771F31B3B57336"
				+ "000DFF732D3B83DE07052D730354D297"
				+ "BEC72871DCCF0E193F171ABA27EE464C"
				+ "6A97690943D59BDABB2A27EB71CEEBDA"
				+ "FA1176046478FD62FEC452D5CA393296"
				+ "530AA3F41927ADFE434A2DF2AE3054F8"
				+ "840657A26E0FC617");
		ec.setExponent("03");
		ec.setExpDate("491231");// YYMMDD
		ec.setCheckSum("C4A3C43CCF87327D136B804160E47D43B60E6E0F");
		ep.addCapk(ec);
		// 95
		ec = new EMVCapk();
		ec.setRID("A000000003");
		ec.setKeyID((byte) 0x95);
		ec.setModul("BE9E1FA5E9A803852999C4AB432DB286"
				+ "00DCD9DAB76DFAAA47355A0FE37B1508"
				+ "AC6BF38860D3C6C2E5B12A3CAAF2A700"
				+ "5A7241EBAA7771112C74CF9A0634652F"
				+ "BCA0E5980C54A64761EA101A114E0F0B"
				+ "5572ADD57D010B7C9C887E104CA4EE12"
				+ "72DA66D997B9A90B5A6D624AB6C57E73"
				+ "C8F919000EB5F684898EF8C3DBEFB330"
				+ "C62660BED88EA78E909AFF05F6DA627B");
		ec.setExponent("03");
		ec.setExpDate("491231");// YYMMDD
		ec.setCheckSum("EE1511CEC71020A9B90443B37B1D5F6E703030F6");
		ep.addCapk(ec);
	}

	private void loadMasterCardCapks(EMVParam ep) {
		// FE
		EMVCapk ec = new EMVCapk();
		ec.setRID("A000000004");
		ec.setKeyID((byte) 0xFE);
		ec.setModul("A653EAC1C0F786C8724F737F172997D63D1C3251C4"
				+ "4402049B865BAE877D0F398CBFBE8A6035E24AFA08"
				+ "6BEFDE9351E54B95708EE672F0968BCD50DCE40F78"
				+ "3322B2ABA04EF137EF18ABF03C7DBC5813AEAEF3"
				+ "AA7797BA15DF7D5BA1CBAF7FD520B5A482D8D3FE"
				+ "E105077871113E23A49AF3926554A70FE10ED728CF793B62A1");
		ec.setExponent("03");
		ec.setExpDate("491231");// YYMMDD
		ec.setCheckSum("9A295B05FB390EF7923F57618A9FDA2941FC34E0");
		ep.addCapk(ec);
		// F3
		ec = new EMVCapk();
		ec.setRID("A000000004");
		ec.setKeyID((byte) 0xF3);
		ec.setModul("98F0C770F23864C2E766DF02D1E833DFF4FFE92D696E"
				+ "1642F0A88C5694C6479D16DB1537BFE29E4FDC6E6E8AFD1B0EB7EA012"
				+ "4723C333179BF19E93F10658B2F776E829E87DAEDA9C94A8B3382199A3"
				+ "50C077977C97AFF08FD11310AC950A72C3CA5002EF513FCCC286E646E3C"
				+ "5387535D509514B3B326E1234F9CB48C36DDD44B416D23654034A66F403BA511C5EFA3");
		ec.setExponent("03");
		ec.setExpDate("491231");// YYMMDD
		ec.setCheckSum("A69AC7603DAF566E972DEDC2CB433E07E8B01A9A");
		ep.addCapk(ec);
		// F8
		ec = new EMVCapk();
		ec.setRID("A000000004");
		ec.setKeyID((byte) 0xF8);
		ec.setModul("A1F5E1C9BD8650BD43AB6EE56B891EF7459C0A24FA8"
				+ "4F9127D1A6C79D4930F6DB1852E2510F18B61CD354DB83A356BD19"
				+ "0B88AB8DF04284D02A4204A7B6CB7C5551977A9B36379CA3DE1A08E"
				+ "69F301C95CC1C20506959275F41723DD5D2925290579E5A95B0DF632"
				+ "3FC8E9273D6F849198C4996209166D9BFC973C361CC826E1");
		ec.setExponent("03");
		ec.setExpDate("491231");// YYMMDD
		ec.setCheckSum("F06ECC6D2AAEBF259B7E755A38D9A9B24E2FF3DD");
		ep.addCapk(ec);
		// FA
		ec = new EMVCapk();
		ec.setRID("A000000004");
		ec.setKeyID((byte) 0xFA);
		ec.setModul("A90FCD55AA2D5D9963E35ED0F440177699832F49C6"
				+ "BAB15CDAE5794BE93F934D4462D5D12762E48C38BA83D8445DEAA"
				+ "74195A301A102B2F114EADA0D180EE5E7A5C73E0C4E11F67A43DDA"
				+ "B5D55683B1474CC0627F44B8D3088A492FFAADAD4F42422D0E70135"
				+ "36C3C49AD3D0FAE96459B0F6B1B6056538A3D6D44640F94467B10886"
				+ "7DEC40FAAECD740C00E2B7A8852D");
		ec.setExponent("03");
		ec.setExpDate("491231");
		ec.setCheckSum("5BED4068D96EA16D2D77E03D6036FC7A160EA99C");
		ep.addCapk(ec);
		// EF
		ec = new EMVCapk();
		ec.setRID("A000000004");
		ec.setKeyID((byte) 0xEF);
		ec.setModul("A191CB87473F29349B5D60A88B3EAEE0973AA6F1A08"
				+ "2F358D849FDDFF9C091F899EDA9792CAF09EF28F5D22404B88A2293"
				+ "EEBBC1949C43BEA4D60CFD879A1539544E09E0F09F60F065B2BF2A1"
				+ "3ECC705F3D468B9D33AE77AD9D3F19CA40F23DCF5EB7C04DC8F69EBA"
				+ "565B1EBCB4686CD274785530FF6F6E9EE43AA43FDB02CE00DAEC15C7B"
				+ "8FD6A9B394BABA419D3F6DC85E16569BE8E76989688EFEA2DF22FF7D35"
				+ "C043338DEAA982A02B866DE5328519EBBCD6F03CDD686673847F84DB65"
				+ "1AB86C28CF1462562C577B853564A290C8556D818531268D25CC98A4CC"
				+ "6A0BDFFFDA2DCCA3A94C998559E307FDDF915006D9A987B07DDAEB3B"
				+ "7DEC40FAAECD740C00E2B7A8852D");
		ec.setExponent("03");
		ec.setExpDate("491231");// YYMMDD
		ec.setCheckSum("21766EBB0EE122AFB65D7845B73DB46BAB65427A");
		ep.addCapk(ec);
		// F1
		ec = new EMVCapk();
		ec.setRID("A000000004");
		ec.setKeyID((byte) 0xF1);
		ec.setModul("A0DCF4BDE19C3546B4B6F0414D174DDE294AABBB828C"
				+ "5A834D73AAE27C99B0B053A90278007239B6459FF0BBCD7B4B9C6C5"
				+ "0AC02CE91368DA1BD21AAEADBC65347337D89B68F5C99A09D05BE02D"
				+ "D1F8C5BA20E2F13FB2A27C41D3F85CAD5CF6668E75851EC66EDBF9885"
				+ "1FD4E42C44C1D59F5984703B27D5B9F21B8FA0D93279FBBF69E0906429"
				+ "09C9EA27F898959541AA6757F5F624104F6E1D3A9532F2A6E51515AEAD1"
				+ "B43B3D7835088A2FAFA7BE7");
		ec.setExponent("03");
		ec.setExpDate("491231");// YYMMDD
		ec.setCheckSum("D8E68DA167AB5A85D8C3D55ECB9B0517A1A5B4BB");
		ep.addCapk(ec);
	}

	private void loadVisaRevocs(EMVParam ep) {
		EMVRevoc er = new EMVRevoc();
		er.setUCRID("A000000003");
		er.setUCIndex((byte) 0x50);
		er.setUCCertSn("024455");
		ep.addRecov(er);
	}

	private void loadMasterCardRevocs(EMVParam ep) {
		EMVRevoc er = new EMVRevoc();
		er.setUCRID("A000000004");
		er.setUCIndex((byte) 0xFE);
		er.setUCCertSn("082355");
		ep.addRecov(er);
	}
	
	private void showProgressDialog(final String message) {
		_ui.post(new Runnable() {
			@Override
			public void run() {
				if (_progress == null) {
					// 创建ProgressDialog对象
					_progress = new ProgressDialog(ICCardActivity.this, R.style.dialog_progress);
					// 设置进度条风格，风格为圆形，旋转的
					_progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					// 设置ProgressDialog 标题
					// _progress.setTitle("提示");
					// 设置ProgressDialog提示信息
					// _progress.setMessage("这是一个圆形进度条对话框");
					// 设置ProgressDialog标题图标
					// _progress.setIcon(R.drawable.img);
					// 设置ProgressDialog 的进度条是否不明确 false 就是不设置为不明确
					_progress.setIndeterminate(false);
					// 设置ProgressDialog 是否可以按退回键取消
					_progress.setCancelable(false);
					// 设置ProgressDialog 的一个Button
					// _progress.setButton("确定", new DialogListener());
				}
				_progress.setMessage(message);
				// 让ProgressDialog显示
				_progress.show();
			}
		});
	}
	
	private void closeProgressDialog() {
		if (_progress != null)
			_progress.cancel();
	}
}