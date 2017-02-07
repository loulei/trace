package com.example.trace;

import java.util.List;

import com.example.trace.ShellUtil.CommandResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class SearchResultActivity extends Activity {
	
	private ListView lv_result;
	private AddressAdapter addressAdapter;
	private List<Long> addresses;
	private int pid;
	private long base;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search_result);
		addresses = (List<Long>) getIntent().getSerializableExtra("addresses");
		pid = getIntent().getIntExtra("pid", 0);
		base = getIntent().getLongExtra("base", 0);
		
		if(addresses != null && pid != 0 && base != 0){
			lv_result = (ListView) findViewById(R.id.lv_result);
			addressAdapter = new AddressAdapter();
			lv_result.setAdapter(addressAdapter);
			lv_result.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, final int arg2,
						long arg3) {
					// TODO Auto-generated method stub
					Logger.d("select:"+addresses.get(arg2));
					AlertDialog.Builder builder = new AlertDialog.Builder(SearchResultActivity.this);
					builder.setTitle("Edit");
					final EditText editText = new EditText(SearchResultActivity.this);
					LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
					editText.setLayoutParams(layoutParams);
					builder.setView(editText);
					builder.setPositiveButton("Sure", new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							String num = editText.getText().toString();
							final Long value = Long.valueOf(num);
							new Thread(new Runnable() {
								
								@Override
								public void run() {
									// TODO Auto-generated method stub
									String traceCmd = String.format("/data/local/myapp/trace m %d %d %d", pid, base+addresses.get(arg2), value);
									CommandResult result = ShellUtil.execCommand(traceCmd, true, true);
									Logger.d(result.toString());
								}
							}).start();
						}
					});
					builder.setNegativeButton("Cancel", new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					});
					builder.create().show();
				}
			});
		}
		
		
	}
	
	class AddressAdapter extends BaseAdapter{
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return addresses == null ? 0 : addresses.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return addresses.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return getGenericView(String.valueOf(addresses.get(position)), SearchResultActivity.this);
		}
		
	}
	
	public TextView getGenericView(String text, Context context){
		AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 64);
		TextView textView = new TextView(context);
		textView.setLayoutParams(layoutParams);
		textView.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
		textView.setPadding(10, 5, 5, 5);
		textView.setText(text);
		textView.setTextSize(18);
		textView.setTextColor(Color.BLACK);
		return textView;
	}
}
