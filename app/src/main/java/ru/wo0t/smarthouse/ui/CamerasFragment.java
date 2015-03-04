package ru.wo0t.smarthouse.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.board.Sensor;


public class CamerasFragment extends BasePageFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    void onItemSelected(Sensor sensor) {
        getBoard().onSensorAction(sensor, null);
    }

    @Override
    public View getLWItemView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.lvitem_cameras_list, parent, false);
        }

        Sensor sensor = (Sensor)mAdapter.getItem(position);

        return view;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_cameras, container, false);

        ListView lvMain = (ListView) rootView.findViewById(R.id.lwCameras);
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
