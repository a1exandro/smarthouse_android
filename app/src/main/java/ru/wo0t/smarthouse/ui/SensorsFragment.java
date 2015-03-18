package ru.wo0t.smarthouse.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.board.Sensor;
import ru.wo0t.smarthouse.board.boardsManager;
import ru.wo0t.smarthouse.common.constants;


public class SensorsFragment extends BasePageFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    void onItemSelected(Sensor sensor) {
        getBoard().onSensorAction(sensor, null);
        updateSensDialog(sensor);
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
                    ((TextView) view.findViewById(R.id.itemSensVal)).setText(sensor.getStringValue(getActivity().getApplicationContext()));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            Log.d(constants.APP_TAG, "could not find sensor at pos " + position);

        view.setTag(R.id.TAG_LW_ITEM_ID, position);

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

        lvMain.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), EditSensActivity.class);
                Sensor sensor = (Sensor)parent.getItemAtPosition(position);
                intent.putExtra(boardsManager.BOARD_ID, mBoardId);
                intent.putExtra(boardsManager.SENSOR_NAME, sensor.getName());
                startActivityForResult(intent, constants.REQUEST_CODE_EDIT_SENS);
                return true;
            }
        });

        return rootView;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            switch (requestCode) {
                case constants.REQUEST_CODE_EDIT_SENS: {
                    if (resultCode == Activity.RESULT_OK) {

                    }
                    Log.i(constants.APP_TAG, "sensor editing finished");
                } break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
