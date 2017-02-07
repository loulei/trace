package com.example.trace;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trace.ShellUtil.CommandResult;

public class FloatService extends Service {
	private LinearLayout floatlayout;
	private WindowManager.LayoutParams layoutParams;
	private WindowManager windowManager;
	private Button btn_dump;
	private ImageButton iv_indicator;
	private EditText et_vaule;
	private Button btn_find;
	private Button btn_clear;
	private int statusbar;
	private String dumpfilename;
	private Map<Integer, List<Long>> array;
	private int tracePid;
	private long baseAddr;
	private static String DUMP_FILE_FORMAT = "trace_%d.dump";
	
	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				Toast.makeText(getApplicationContext(), "dump complete", Toast.LENGTH_SHORT).show();
				break;
			case 1:
				int target = msg.arg1;
				if(array != null && array.size() > 0){
					final List<Long> addresses = array.get(target);
					if(addresses != null){
						Intent intent = new Intent(getApplicationContext(), SearchResultActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
						intent.putExtra("addresses", (Serializable)addresses);
						intent.putExtra("pid", tracePid);
						intent.putExtra("base", baseAddr);
						startActivity(intent);
					}
				}
				break;
			default:
				break;
			}
		};
	};
	
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		createFloatView();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		statusbar = intent.getIntExtra("statusbar", 25);
		array = new HashMap<Integer, List<Long>>();
		return START_REDELIVER_INTENT;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void createFloatView(){
		layoutParams = new WindowManager.LayoutParams();
		windowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
		layoutParams.type = LayoutParams.TYPE_PHONE;
		layoutParams.format = PixelFormat.RGBA_8888;
		layoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
		layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
		layoutParams.x = 0;
		layoutParams.y = 0;
		layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		LayoutInflater inflater = LayoutInflater.from(getApplication());
		floatlayout = (LinearLayout) inflater.inflate(R.layout.float_view, null);
		windowManager.addView(floatlayout, layoutParams);
		btn_dump = (Button) floatlayout.findViewById(R.id.btn_float);
		iv_indicator = (ImageButton) floatlayout.findViewById(R.id.iv_indicator);
		et_vaule = (EditText) floatlayout.findViewById(R.id.et_vaule);
		btn_find = (Button) floatlayout.findViewById(R.id.btn_find);
		btn_clear = (Button) floatlayout.findViewById(R.id.btn_clear);
		floatlayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		iv_indicator.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				layoutParams.x = (int)event.getRawX() - btn_dump.getMeasuredWidth()/2;
				layoutParams.y = (int)event.getRawY() - btn_dump.getMeasuredHeight()/2-statusbar;
				windowManager.updateViewLayout(floatlayout, layoutParams);
				return false;
			}
		});
		
		btn_dump.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String pkgName = getTopActivity();
				final int pid = getPid(pkgName);
				tracePid = pid;
				Logger.d("pid="+pid);
				if(pid != -1){
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							String cat = String.format("cat /proc/%d/maps | grep rw-p | grep /dev/ashmem/dalvik-heap", pid);
							Logger.d(cat);
							CommandResult result = ShellUtil.execCommand(cat, true, true);
							Logger.d(result.responseMsg);
							String[] array = result.responseMsg.split("\n");
							if(array.length > 0){
								Logger.d("find maps");
								int size = array.length;
								String startStr = array[0].split("-")[0];
								String endStr = array[size-1].split(" ")[0].split("-")[1];
								long start = Long.parseLong(startStr, 16);
								baseAddr = start;
								long end = Long.parseLong(endStr, 16);
								long offset = end - start;
								Logger.d("start:"+start+" end:"+end+" offset:"+offset);
								dumpfilename = Environment.getExternalStorageDirectory().getPath() + File.separator + String.format(DUMP_FILE_FORMAT, pid);
								Logger.d("save file:"+dumpfilename);
								String dumpCmd = String.format("/data/local/myapp/trace s %d %d %d %s", pid, start, offset, dumpfilename);
//								int dumpRet = new NTrace().dumpMem(pid, start, offset, dumpfilename);
								CommandResult dumpRet = ShellUtil.execCommand(dumpCmd, true, true);
								Logger.d("dumpMen:"+dumpRet.toString());
								handler.sendEmptyMessage(0);
							}
						}
					}).start();
				}
			}
		});
		//12-05 17:36:17.566: D/debugTAG(2979): ║ cat /proc/1957/maps | grep rw-p | grep /dev/ashmem/dalvik-heap

		
		btn_find.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						try {
							String valueStr = et_vaule.getText().toString();
							Logger.d("value:"+valueStr);
							int target = Integer.valueOf(valueStr);
							List<Long> addrList = new ArrayList<Long>();
							File file = new File(dumpfilename);
							BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
							byte[] buffer = new byte[4];
							int len = 0;
							long index = 0;
							while((len=inputStream.read(buffer, 0, buffer.length)) != -1){
								if(len == 4){
									int value = ((buffer[3] << 24) & (0xff000000))
											+ ((buffer[2] << 16) & 0x00ff0000)
											+ ((buffer[1] << 8) & 0x0000ff00)
											+ (buffer[0] & 0x000000ff);
									if(value == target){
										addrList.add(index);
										System.out.println(index);
									}
								}
								index+=len;
							}
							inputStream.close();
							array.put(target, addrList);
							Logger.d("scan complete, find " + addrList.size() + " match");
							handler.sendMessage(handler.obtainMessage(1, target, 0));
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start();
			}
		});
		
		btn_clear.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				et_vaule.setText("");
			}
		});
	}
	
	//12-01 14:56:49.264: D/debugTAG(14027): ║ 41e55000-6157b000 rw-p 00000000 00:04 6618       /dev/ashmem/dalvik-heap (deleted)
	//12-01 10:26:19.281: I/System.out(4090): 3420820 1102389248
	//12-01 10:26:19.296: I/System.out(4090): 3511744

	//11-30 18:58:29.894: D/debugTAG(6870): ║ save file:/storage/sdcard0/trace_4010.dump
	//11-30 18:58:29.695: D/debugTAG(6870): ║ pid=4010


	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(floatlayout != null){
			windowManager.removeView(floatlayout);
		}
	}
	
	private String getTopActivity(){
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> infos = activityManager.getRunningTasks(1);
		RunningTaskInfo taskInfo = infos.get(0);
		ComponentName componentName = taskInfo.topActivity;
		System.out.println("package name:"+ componentName.getPackageName());
		return componentName.getPackageName();
	}
	
	private int getPid(String pkgName){
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> appProcessInfos = activityManager.getRunningAppProcesses();
//		StringBuilder builder = new StringBuilder();
		for(RunningAppProcessInfo info : appProcessInfos){
			String[] list = info.pkgList;
//			builder.append(info.processName).append(":").append("\n");
			for(String pkg : list){
//				builder.append("    ").append(pkg).append("\n");
				if(pkg.equals(pkgName)){
					return info.pid;
				}
			}
		}
//		System.out.println(builder.toString());
		return -1;
	}
}



/*showmap is dumpping the smap data from one process. The smap is describing the process's memory area's detail. In virtual memory manage system, the memory can be gained by the system API such as mmap, brk. After gaining virtual memory address by these APIs, the address and length will be recorded in the smap.

And let's list each section of the dalvik relative memory usage:

Dalvik Heap section(Heap Management, GC)
dalvik-bitmap-1, dalvik-bitmap-2 is the Dalvik Heap management data stucture. In Dalvik, the GC is marksweep, and 8 bytes memory will be marked(Used or free) as one bit in the bitmap. These two bitmaps will be used as active map(used for marking @ runtime) and the other will be used as marked map(used @ GC time).
dalvik-mark-stack: For GC mark step use. The mark step will iterate the bitmap, so this is a Breadth-first search which will need a stack.
dalvik-card-table: is used for Dalvik Concurrent GC, in bitmap marking steps, the process will do other tasks which will lead using memory. These card tables is recording the memory dirty after first marking step. You can see the detail by searching mark sweep GC.
dalvik-heap is used for process memory usage
dalvik-zygote is one part of the hole heap, which will not be used @ GC. All processes will share these memories such as framework resources.
dalvik-jit is The jit memory used in Dalvik. JIT: just in time, which will convert dex bytecode to machine code which can be executed by CPU.
dalvik-LinearAlloc: is the dalvik's perm memory such as: Method, Class definition datas, thread stack datas. These memory can be setted READONLY after parsing the class definition.
dalvik-aux-structure: auxillary data structures, which will compress the method/class/string const reference. These references will be used @ each dex file, but sum of these memory will cost a large memory. So Dalvik create a tmp memory to shared these references.
If you want to analysis your program's memory, I suggest you to use MAT in eclipse. And the native heap usage, you can use mmap to manage.
*/

















































