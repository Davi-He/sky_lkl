package com.imagpay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.imagpay.ui.ICCardActivity;
import com.imagpay.ui.M1CardActivity;
import com.imagpay.ui.MagneticCardActivity;
import com.imagpay.ui.R;
import com.imagpay.ui.RFCardActivity;
import com.imagpay.ui.SettingsActivity;
import com.imagpay.utils.VersionUtils;

public class ContentActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_content);

		Button btn = (Button) findViewById(R.id.btnmagneticcard);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ContentActivity.this,
						MagneticCardActivity.class);
				startActivity(intent);
			}
		});

		btn = (Button) findViewById(R.id.btniccard);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ContentActivity.this,
						ICCardActivity.class);
				startActivity(intent);
			}
		});

		btn = (Button) findViewById(R.id.btnrfcard);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ContentActivity.this,
						RFCardActivity.class);
				startActivity(intent);
			}
		});

		btn = (Button) findViewById(R.id.btnm1card);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ContentActivity.this,
						M1CardActivity.class);
				startActivity(intent);
			}
		});

		btn = (Button) findViewById(R.id.btnsettings);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ContentActivity.this,
						SettingsActivity.class);
				startActivity(intent);
			}
		});
		
		VersionUtils.checkForUpdates(this,
				"http://imagpay.googlecode.com/files/imagpaydemo.json");
	}

	public void onResume() {
		super.onResume();
		Button btn = (Button) findViewById(R.id.btnmagneticcard);
		btn.setText(R.string.magneticcard);
		btn = (Button) findViewById(R.id.btniccard);
		btn.setText(R.string.iccard);
		btn = (Button) findViewById(R.id.btnrfcard);
		btn.setText(R.string.rfcard);
		btn = (Button) findViewById(R.id.btnm1card);
		btn.setText(R.string.m1card);
		btn = (Button) findViewById(R.id.btnsettings);
		btn.setText(R.string.settings);
	}

	public void onDestroy() {
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
}