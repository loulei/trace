package com.example.trace;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	public void onStart(View view){
		int statusbar = ScreenUtils.getStatusbarHeight(MainActivity.this);
		Intent intent = new Intent(MainActivity.this, FloatService.class);
		intent.putExtra("statusbar", statusbar);
		startService(intent);
		finish();
	}
	
	public void onStop(View view){
		Intent intent = new Intent(MainActivity.this, FloatService.class);
		stopService(intent);
	}
}


























