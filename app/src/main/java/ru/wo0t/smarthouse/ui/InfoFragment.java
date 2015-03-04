package ru.wo0t.smarthouse.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.board.Sensor;


public class InfoFragment extends BasePageFragment {

    @Override
    void onItemSelected(Sensor sensor) {

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.lvitem_boardslookupactivity, parent, false);
        }

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_info, container, false);

        ListView lvMain = (ListView) rootView.findViewById(R.id.lwInfo);
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
