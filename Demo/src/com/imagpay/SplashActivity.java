package com.imagpay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.imagpay.ui.R;
import com.imagpay.utils.VersionUtils;

public class SplashActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		TextView tv = (TextView) findViewById(R.id.version);
		// Display the current version number
		tv.setText("V " + VersionUtils.getLocalVersionName(this));
		tv = (TextView) findViewById(R.id.poweredby);
		tv.setText("Powered by www.szzcs.com");

		new Handler().postDelayed(new Runnable() {
			public void run() {
				/* Create an Intent that will start the Main Activity. */
				Intent intent = new Intent(SplashActivity.this,
						ContentActivity.class);
				SplashActivity.this.startActivity(intent);
				SplashActivity.this.finish();
			}
		}, 2500);
	}
}