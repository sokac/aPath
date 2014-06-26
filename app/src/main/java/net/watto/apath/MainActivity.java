package net.watto.apath;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener  {
    final List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
    private BroadcastReceiver mReceiver;
    private BluetoothAdapter mBluetoothAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (this.mBluetoothAdapter == null) {
		    Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_LONG).show();
		    this.finish();
            return;
		}
		if (!this.mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 0);
		}

		setContentView(R.layout.activity_main);
		final BluetoothDeviceAdapter adapter = new BluetoothDeviceAdapter(
			this,
			android.R.layout.simple_list_item_1,
			this.devices
		);
		ListView deviceList = (ListView) findViewById(R.id.deviceList);
		deviceList.setAdapter(adapter);
		
		// Create a BroadcastReceiver for ACTION_FOUND
		this.mReceiver = new BroadcastReceiver() {
		    public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                if (!devices.contains(device)) {
                    devices.add(device);
                    adapter.add(device);
                    adapter.notifyDataSetChanged();
                }
            }
		    }
		};
		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
		this.mBluetoothAdapter.startDiscovery();

        deviceList.setOnItemClickListener(this);
	}

    protected void onDestroy() {
        this.mBluetoothAdapter.cancelDiscovery();
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
		}
		super.onDestroy();
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        this.mBluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = this.devices.get(i);
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        RadioGroup rg = (RadioGroup) this.findViewById(R.id.controlRadio);
        RadioButton r = (RadioButton)  this.findViewById(rg.getCheckedRadioButtonId());
        Intent intent;
        switch (r.getId()) {
            case R.id.controlManual:
                intent = new Intent(this, ManualControlActivity.class);
                break;
            default:
                intent = new Intent(this, GyroscopeControlActivity.class);
                break;
        }
        intent.putExtra("device", device.getAddress());
        startActivity(intent);
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
    }
}
