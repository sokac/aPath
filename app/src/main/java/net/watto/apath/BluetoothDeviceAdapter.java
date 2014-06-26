package net.watto.apath;

import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class BluetoothDeviceAdapter extends ArrayAdapter<BluetoothDevice> {
	List<BluetoothDevice> objects;
	private final Context context;
	
	public BluetoothDeviceAdapter(MainActivity context, int simpleListItem1,
			List<BluetoothDevice> devices) {
		super(context, simpleListItem1);

		this.context = context;
		this.objects = devices;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.device_item, parent, false);
        TextView deviceName = (TextView) rowView.findViewById(R.id.bluetooth_device_name);
        TextView deviceMac = (TextView) rowView.findViewById(R.id.bluetooth_device_mac);
        deviceName.setText(objects.get(position).getName());
        deviceMac.setText(objects.get(position).getAddress());
        return rowView;
	}
}
