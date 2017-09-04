package com.example.idea.getdata;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class UartService extends Service implements Serializable {
	
	public enum SEND_STATE {
		SEND_START,
		SEND_MESSAGE,
		SEND_OVER
	}
	public enum RECEIVE_CONTENT_TYPE_T {
		CONTENT_NONE,
	    CONTENT_HEADER,
	    CONTENT_DATA,
	    CONTENT_ACK
	}
	
	public final int L1_HEADER_MAGIC_POS				= 0; 
	public final int L1_HEADER_PROTOCOL_VERSION_POS		= 1;
	public final int L1_PAYLOAD_LENGTH_HIGH_BYTE_POS	= 2;	/* L1 payload lengh high byte */
	public final int L1_PAYLOAD_LENGTH_LOW_BYTE_POS		= 3;
	public final int L1_HEADER_CRC16_HIGH_BYTE_POS		= 4;
	public final int L1_HEADER_CRC16_LOW_BYTE_POS		= 5;
	public final int L1_HEADER_SEQ_ID_HIGH_BYTE_POS		= 6;
	public final int L1_HEADER_SEQ_ID_LOW_BYTE_POS 		= 7;
	
	public final byte FIRMWARE_UPDATE_CMD_ID 	= (byte)0x01;
    public final byte SET_CONFIG_COMMAND_ID 		= (byte)0x02;
    public final byte BOND_COMMAND_ID 			= (byte)0x03;
    public final byte NOTIFY_COMMAND_ID 			= (byte)0x04;
    public final byte HEALTH_DATA_COMMAND_ID 	= (byte)0x05;
    public final byte FACTORY_TEST_COMMAND_ID 	= (byte)0x06;
    public final byte BLUETOOTH_LOG_COMMAND_ID 	= (byte)0x0A;
    public final byte GET_STACK_DUMP 			= (byte)0x10;
    public final byte TEST_FLASH_READ_WRITE 		= (byte)0xFE;
    public final byte TEST_COMMAND_ID 			= (byte)0xFF;
    
    public final byte L1_HEADER_MAGIC 		= (byte)0xAB;
    public final byte L1_HEADER_VERSION 		= (byte)0x00;
    public final byte L1_HEADER_SIZE 		= (byte)8;    
    public byte L1_Header_Res 				= (byte)0x00;
    public byte L1_Header_Err 				= (byte)0x00;
    public byte L1_Header_Ack 				= (byte)0x00;
    public byte L1_Header_Payload_Len_High 	= (byte)0x00;
    public byte L1_Header_Payload_Len_Low 	= (byte)0x00;
    public byte L1_Header_Crc_Len_High 		= (byte)0x00;
    public byte L1_Header_Crc_Len_Low 		= (byte)0x00;
    public byte L1_Header_Sequence_Id_High 	= (byte)0x00;
    public byte L1_Header_Sequence_Id_Low 	= (byte)0x00;
    
    public int L1_Header_Sequence_Id = 0;
	
	public byte command_id;
		
    private final static String TAG = UartService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    
    
    public final static String ACTION_GATT_CONNECTED =
            "com.example.idea.getdata.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.idea.getdata.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.idea.getdata.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.idea.getdata.ACTION_DATA_AVAILABLE";
    public final static String ACTION_DATA_WRITED =
            "com.example.idea.getdata.ACTION_DATA_WRITED";
    public final static String EXTRA_DATA =
            "com.example.idea.getdata.EXTRA_DATA";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART =
            "com.example.idea.getdata.DEVICE_DOES_NOT_SUPPORT_UART";
    
    public SEND_STATE send_state = SEND_STATE.SEND_START;
    public RECEIVE_CONTENT_TYPE_T receive_type = RECEIVE_CONTENT_TYPE_T.CONTENT_NONE;
       
        
    public static final UUID TX_POWER_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    
    private static int transmited_content_length = 0;
    private static int length_to_transmite;


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt );
            	
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, 
										BluetoothGattCharacteristic characteristic, 
										int status) {
			if(status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_WRITED, characteristic);
			}
		}
        
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
        
        
        
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
        	
           // Log.d(TAG, String.format("Received TX: %d",characteristic.getValue() ));
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        } else {
        	
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        UartService getService() {
            return UartService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
       // mBluetoothGatt.close();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.w(TAG, "mBluetoothGatt closed");
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    /*
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

                
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }*/
    
    /**
     * Enable TXNotification
     *
     * @return 
     */
    public void enableTXNotification()
    { 
    	/*
    	if (mBluetoothGatt == null) {
    		showMessage("mBluetoothGatt null" + mBluetoothGatt);
    		broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
    		return;
    	}
    		*/
    	BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
    	if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
    	BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Tx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar,true);
        
        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    	
    }
    
    public void writeRXCharacteristic(byte[] value,int s,int len)
    {
    	byte[] send_value = new byte[len];
    	int i;
    	for(i=0;i<len;i++){
    		send_value[i] = value[s+i];
    	}
    	
    	BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
    	showMessage("mBluetoothGatt null"+ mBluetoothGatt);
    	if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
    	BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        if (RxChar == null) {
            showMessage("Rx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        RxChar.setValue(send_value);
    	boolean status = mBluetoothGatt.writeCharacteristic(RxChar);
    	
        Log.d(TAG, "write TXchar - status=" + status);  
    }
    
    public int get_transmited_content_length() {
    	return transmited_content_length;
    }
    
    public int get_to_transmit_length() {
    	return length_to_transmite;
    }
    
    public void transmit_data(byte[] data,int s,int length) {
    	switch (send_state) {
		case SEND_START:
			if(data[s] != L1_HEADER_MAGIC)
			{
				//not a start package, so just igore
				break;
			}
			//get correct header
			transmited_content_length = length;			
			length_to_transmite = (data[s + L1_PAYLOAD_LENGTH_LOW_BYTE_POS] | (data[s + L1_PAYLOAD_LENGTH_HIGH_BYTE_POS] << 8)) + L1_HEADER_SIZE;
			length_to_transmite -= length;
			
			if(length_to_transmite <= 0) { // just one package                
				send_state = SEND_STATE.SEND_OVER;                
				writeRXCharacteristic(data, 0,transmited_content_length);
            } else {	// more than one package
            	send_state = SEND_STATE.SEND_MESSAGE;
            	writeRXCharacteristic(data, s,length);
            }
			break;
		case SEND_MESSAGE:
			transmited_content_length += length;
			length_to_transmite -= length;
			if(length_to_transmite <= 0) 
			{
				send_state = SEND_STATE.SEND_OVER;
				transmited_content_length = 0;
				length_to_transmite = 0;
				writeRXCharacteristic(data, s,length);
			} else {
				send_state = SEND_STATE.SEND_MESSAGE;
				writeRXCharacteristic(data, s,length);
			}
			break;

		default:
			break;
		}

    }
    
    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
    
}

