package ru.wo0t.smarthouse.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.board.Sensor;
import ru.wo0t.smarthouse.common.constants;


public class SensorsFragment extends BasePageFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    void onItemSelected(Sensor sensor) {
        getBoard().onSensorAction(sensor, null);
    }

    @Override
    public View getLWItemView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.lvitem_sensors_list, parent, false);
        }

        Sensor sensor = (Sensor)(mAdapter.getItem(position));

        if (sensor != null) {

            try {
                ((TextView) view.findViewById(R.id.itemSenseName)).setText(sensor.getName());
                if (sensor.getVal() != null) {
                    String sVal = "";
                    switch (sensor.getType()) {
                        case TEMP:
                            sVal = String.valueOf((double) sensor.getVal()) +"°C";
                            break;
                        case DIGITAL:
                            if (String.valueOf((double) sensor.getVal()).equals(sensor.getErrVal()))
                                sVal = getString(R.string.not_ok);
                            else
                                sVal = getString(R.string.OK);
                            break;
                    }
                    ((TextView) view.findViewById(R.id.itemSensVal)).setText(sVal);
                }

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            Log.d(constants.APP_TAG, "could not find sensor at pos " + position);
        return view;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sensors, container, false);

        ListView lvMain = (ListView) rootView.findViewById(R.id.lwSensors);
        lvMain.setAdapter(mAdapter);

        lvMain.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Sensor sensor = (Sensor)parent.getItemAtPosition(position);
                onItemSelected(sensor);
            }
        });

        return rootView;

    }
}
