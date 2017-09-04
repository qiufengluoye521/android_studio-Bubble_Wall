package com.example.idea.getdata;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BondDevice extends Activity {
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private static final String BONDTAG = "BondDevice";
	private List btNameList;
	private List btAddressList;
	private ListView islv_Scan;
			
	List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	Map<String, Object> map;
	SimpleAdapter adapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.scan_list);
		islv_Scan = (ListView)findViewById(R.id.Lv_Scan);
		btNameList = new ArrayList();
		btAddressList = new ArrayList();

		mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();

		mBluetoothAdapter.startLeScan(mLeScanCallback);
		
		adapter = new SimpleAdapter(this,list,R.layout.bond_devices,
				new String[]{"name","add","img"},
				new int[]{R.id.title,R.id.info,R.id.img});	
				
		
		islv_Scan.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				Map<String, Object> getMap = (HashMap<String, Object>)list.get(position);
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				String devicename = getMap.get("name").toString();
				String deviceadd = getMap.get("add").toString();	
				Bundle b = new Bundle();
				b.putString(BluetoothDevice.EXTRA_DEVICE, deviceadd);
				
				//Intent mIntent = new Intent(BondDevice.this,BleClientService.class);
				Intent mIntent = new Intent();
				mIntent.putExtras(b);
				setResult(Activity.RESULT_OK, mIntent);
	            finish();
				
			}
			
		});
		
	}
	
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						if(device.getName().equals("Bubble_Wall")) {
							if (!btAddressList.contains(device.getAddress())) {
								//btNameList.add(device.getName());
								btAddressList.add(device.getAddress());
								map = new HashMap<String, Object>();
								map.put("name", device.getName().toString());
								map.put("add", device.getAddress().toString());
								map.put("img", R.drawable.ble);
								list.add(map);
								islv_Scan.setAdapter(adapter);
								//setListAdapter(adapter);
							}
						}
					} catch (Exception e) {
						Log.e(BONDTAG, e.toString());
					}
				}
			});

		}
	};
			
	
}
