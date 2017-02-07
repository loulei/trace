package com.example.trace;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Display;
import android.view.WindowManager;

public class ScreenUtils {
	private static int screenWidth = 0;
	private static int screenHeight = 0;
	private static int statusbarHeight = 0;
	
	public static int getScreenHeight(Context context){
		if(screenHeight == 0){
			WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			Display display = manager.getDefaultDisplay();
			Point point = new Point();
			screenHeight = display.getHeight();
//			screenHeight = point.y;
		}
		return screenHeight;
	}
	
	public static int getScreenWidth(Context context){
		if(screenWidth == 0){
			WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			Display display = manager.getDefaultDisplay();
			Point point = new Point();
//			display.getSize(point);
			screenWidth = display.getWidth();
//			screenWidth = point.x;
		}
		return screenWidth;
	}
	
	public static int getStatusbarHeight(Activity activity){
		if(statusbarHeight == 0){
			Rect rect = new Rect();
			activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
			statusbarHeight = rect.top;
		}
		return statusbarHeight;
	}
}





















