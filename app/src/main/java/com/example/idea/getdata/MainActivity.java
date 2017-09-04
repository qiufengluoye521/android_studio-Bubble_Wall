package com.example.idea.getdata;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    Button button;
	Button draw_Button;
	Button connect_Button;
	GradientProgressBar gra;
	EditText savedata_EditText;
	TextView show_message;
	private int[] main_data = new int[2000];
	String fileName = getSDPath() + "/" + "save_data.txt";

	private static final int REQUEST_SELECT_DEVICE = 1;
	public static final String TAG = "nRFUART";

	private BluetoothDevice mDevice = null;
	private BluetoothAdapter mBtAdapter = null;
	private UartService mService = null;

	private byte[] send_to_data = new byte[100];
	private  int received_data_len=0;

	private static int[] crc16_table = new int[]{
			0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241,
			0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440,
			0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40,
			0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841,
			0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40,
			0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41,
			0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641,
			0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
			0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
			0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441,
			0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41,
			0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840,
			0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41,
			0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40,
			0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640,
			0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
			0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240,
			0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441,
			0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41,
			0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840,
			0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
			0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
			0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640,
			0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041,
			0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241,
			0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440,
			0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40,
			0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841,
			0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40,
			0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41,
			0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641,
			0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040
	};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		//		WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        setContentView(R.layout.activity_main);

        button = (Button) this.findViewById(R.id.savedata_bt);
		//gra = (GradientProgressBar) findViewById(R.id.curvel);
		draw_Button = (Button)findViewById(R.id.draw_curve_bt);
		savedata_EditText = (EditText)findViewById(R.id.savedata_et);
		connect_Button =(Button)findViewById(R.id.connect_bt);
		show_message = (TextView)findViewById(R.id.info_tv);


		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String save_data_string = savedata_EditText.getText().toString();
				savedata(fileName, save_data_string + "\n");
			}
		});

		connect_Button.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				Intent serchintent;
				if (connect_Button.getText().equals("扫描设备")) {
					serchintent = new Intent(MainActivity.this, BondDevice.class);
					MainActivity.this.startActivityForResult(serchintent, REQUEST_SELECT_DEVICE);
				}
			}
		});
		service_init();
    }


	private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			final Intent mIntent = intent;
			// 联接成功 //
			if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
				Toast.makeText(MainActivity.this, "Devices connected!!!",Toast.LENGTH_SHORT).show();
				connect_Button.setText("断开联接");
			}

			// 断开联接 //
			if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
				runOnUiThread(new Runnable() {
					public void run() {
						connect_Button.setText("扫描设备");
						mService.close();
					}
				});
			}

			// 发现服务成功  //
			if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
				mService.enableTXNotification();
			}

			// 数据允许——读数据 //
			if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
				String show_message_String = "";
				String save_data_string = "";
				final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
				final int package_length = txValue.length;
				received_data_len ++;
				show_message_String = "收到" + received_data_len+"条数据";
				//save_data_string = txValue.toString();
				//savedata(fileName, save_data_string + "\n");
				show_message.setText(show_message_String);
				runOnUiThread(new Runnable() {
					public void run() {
						int x_value = (int) (((txValue[0] << 8) & (0xFF00)) | (txValue[1] & (0x00FF)));
						for (int i = 0; i < 99; i++) {
							main_data[i] = main_data[i + 1];
						}
						main_data[99] = x_value;
						gra.draw_curve(main_data,100);
					}
				});

			}

			// 写数据，手机给设备写数据，写成功后调用
			if (action.equals(UartService.ACTION_DATA_WRITED)) {
				;
			}
		}
	};
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
		intentFilter.addAction(UartService.ACTION_DATA_WRITED);
		return intentFilter;
	}

	private void service_init() {
		Intent bindIntent = new Intent(this, UartService.class);
		this.getApplicationContext().bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

		LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
	}

	//UART service connected/disconnected
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			mService = ((UartService.LocalBinder) rawBinder).getService();
			Log.d(TAG, "onServiceConnected mService= " + mService);
			if (!mService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
		}
		public void onServiceDisconnected(ComponentName classname) {
			////     mService.disconnect(mDevice);
			mService = null;
		}
	};
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_SELECT_DEVICE:
				//When the DeviceListActivity return, with the selected device address
				if (resultCode == Activity.RESULT_OK && data != null) {
					String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
					//String deviceAddress = "F5:2C:4E:71:9E:04";
					mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
					mService.connect(deviceAddress);
				}
				break;
			default:
				Log.e(TAG, "wrong request code");
				break;
		}
	}

	// 获取文件路径
	public String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
		}
		return sdDir.toString();
	}

	/**
	 * 追加文件：使用RandomAccessFile
	 *
	 * @param fileName
	 *            文件名
	 * @param content
	 *            追加的内容
	 */
	public void savedata(String fileName, String content) {
		try {
			// 打开一个随机访问文件流，按读写方式
			RandomAccessFile randomFile = new RandomAccessFile(fileName, "rw");
			// 文件长度，字节数
			long fileLength = randomFile.length();
			// 将写文件指针移到文件尾。
			randomFile.seek(fileLength);
			randomFile.writeBytes(content);
			randomFile.close();
			Toast.makeText(this, "保存成功",Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, "保存失败",Toast.LENGTH_SHORT).show();
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

	@Override
	protected void onResume() {
		/**
		 * 设置为横屏
		 */
		/*
		if(getRequestedOrientation()!= ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
		*/
		super.onResume();
	}
}
