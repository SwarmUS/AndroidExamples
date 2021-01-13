package com.swarmus.usbcomm;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.nio.charset.StandardCharsets.*;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDeviceConnection;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import com.swarmus.usbcomm.R;

public class MainActivity extends AppCompatActivity {
    PendingIntent mPermissionIntent;
    Button btnCheck;
    Button btnReset;
    Button btnUp;
    Button btnLeft;
    Button btnRight;
    Button btnDown;
    TextView textInfo;
    TextView dataReceivedText;
    CheckBox checkBox;
    UsbDevice device;
    UsbManager manager;
    UsbDeviceConnection connection;
    UsbSerialDevice serial;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ACTION_USE_PERMISSION = "com.swarmus.usbcomm.USB_PERMISSION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnCheck = (Button) findViewById(R.id.showDevices);
        btnReset = (Button) findViewById(R.id.resetDataReceived);
        btnUp = (Button) findViewById(R.id.buttonUp);
        btnLeft = (Button) findViewById(R.id.buttonLeft);
        btnRight = (Button) findViewById(R.id.buttonRight);
        btnDown = (Button) findViewById(R.id.buttonDown);
        textInfo = (TextView) findViewById(R.id.deviceListingText);
        dataReceivedText = (TextView) findViewById(R.id.dataReceivedText);
        checkBox = (CheckBox) findViewById(R.id.checkBox);
        // Activate textview scrolling
        textInfo.setMovementMethod(new ScrollingMovementMethod());
        dataReceivedText.setMovementMethod(new ScrollingMovementMethod());
        btnCheck.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                textInfo.setText("");
                checkInfo();
            }
        });
        btnReset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dataReceivedText.setText("");
            }
        });
        btnUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serial != null) {
                    byte[] data = ("Up"+'\n').getBytes();
                    serial.write(data);
                }
            }
        });
        btnLeft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serial != null) {
                    byte[] data = ("Left"+'\n').getBytes();
                    serial.write(data);
                }
            }
        });
        btnRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serial != null) {
                    byte[] data = ("Right"+'\n').getBytes();
                    serial.write(data);
                }
            }
        });
        btnDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serial != null) {
                    byte[] data = ("Down"+'\n').getBytes();
                    serial.write(data);
                }
            }
        });
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    listenToSerial();
                } else {
                    stopListenToSerial();
                }
            }
        });
    }

    private void listenToSerial() {
        if (device != null && manager != null) {
            startSerialConnection(manager, device);
        }
    }

    private void stopListenToSerial() {
        if (serial != null) {
            stopSerialConnection(serial);
        }
    }

    void startSerialConnection(UsbManager usbManager, UsbDevice device) {
        connection = usbManager.openDevice(device);
        serial = UsbSerialDevice.createUsbSerialDevice(device, connection);

        if (serial != null && serial.open()) {
            serial.setBaudRate(9600);
            serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
            serial.setParity(UsbSerialInterface.PARITY_NONE);
            serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
            serial.read(mCallback);
        }
    }

    void stopSerialConnection(UsbSerialDevice serial) {
        if (serial != null && serial.open()) {
            serial.close();
        }
    }

    public void write(byte[] data) {
        if (serial != null)
            serial.write(data);
    }

    UsbSerialInterface.UsbReadCallback mCallback = (data) -> {
        String dataStr = new String(data, StandardCharsets.UTF_8);
        if (dataReceivedText != null) {
            dataReceivedText.append(dataStr);
        }
        Log.i(TAG, "Data received: " + dataStr);
    };

    private void checkInfo() {
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USE_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USE_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        String i = "";
        while(deviceIterator.hasNext()) {
            device = deviceIterator.next();
            manager.requestPermission(device, mPermissionIntent);
            i += "\n" + "DeviceID: " + device.getDeviceId() + "\n"
                    + "DeviceName: " + device.getDeviceName() + "\n"
                    + "DeviceClass: " + device.getDeviceClass() + " - "
                    + "DeviceSubClass: " + device.getDeviceSubclass() + "\n"
                    + "VendorID: " + device.getVendorId() + "\n"
                    + "ProductID: " + device.getProductId() + "\n"
                    + "Protocol: " + device.getDeviceProtocol() + "\n";
        }
        textInfo.setText(i);
        Log.d("INFO", "I: " + i);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USE_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // call method to set up device communication
                        }
                    } else {
                        Log.d("ERROR", "permission denied for device " + device);
                    }
                }
            }
        }
    };
}
