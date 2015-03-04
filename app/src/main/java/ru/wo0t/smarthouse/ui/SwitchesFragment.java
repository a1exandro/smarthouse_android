package ru.wo0t.smarthouse.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.board.Sensor;
import ru.wo0t.smarthouse.common.constants;


public class SwitchesFragment extends BasePageFragment {
    private static final int ITEM_TAG = 1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    void onItemSelected(Sensor sensor, boolean val) {
        getBoard().onSensorAction(sensor, val? 1:0);
    }

    @Override
    public View getLWItemView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.lvitem_switches_list, parent, false);
        }

        Sensor sensor = (Sensor)mAdapter.getItem(position);

        if (sensor != null) {
            ((Switch) view.findViewById(R.id.itemSwitch)).setText(sensor.getName());
            ((Switch) view.findViewById(R.id.itemSwitch)).setTag(R.id.TAG_LWSWITCHES_ITEM_ID, position);
            ((Switch) view.findViewById(R.id.itemSwitch)).setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Sensor sens = (Sensor)mAdapter.getItem( (int)buttonView.getTag(R.id.TAG_LWSWITCHES_ITEM_ID));
                    onItemSelected(sens, isChecked);
                }
            });
        }
        else
            Log.d(constants.APP_TAG, "could not find switch at pos " + position);
        return view;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_switches, container, false);

        ListView lvMain = (ListView) rootView.findViewById(R.id.lwSwitches);
        lvMain.setAdapter(mAdapter);

        return rootView;

    }
}
