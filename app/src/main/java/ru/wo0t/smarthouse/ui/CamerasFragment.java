package ru.wo0t.smarthouse.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.board.Sensor;
import ru.wo0t.smarthouse.board.boardsManager;
import ru.wo0t.smarthouse.common.constants;


public class CamerasFragment extends BasePageFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    void onItemSelected(Sensor sensor) {
        getBoard().onSensorAction(sensor, null);
        updateSensDialog(sensor);
    }

    @Override
    public View getLWItemView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.lvitem_cameras_list, parent, false);
        }

        Sensor sensor = (Sensor)mAdapter.getItem(position);

        if (sensor != null) {
            try {
                Bitmap bMap;
                if (sensor.getVal() != null) {
                    byte[] picData = (byte[])sensor.getVal();
                    bMap = BitmapFactory.decodeByteArray( picData, 0, picData.length );
                }
                else
                    bMap = BitmapFactory.decodeResource(getActivity().getResources(),R.drawable.img_photo_not_available);

                if (bMap != null) {
                    ((ImageView) view.findViewById(R.id.itemCameraPicture)).setImageBitmap(bMap);
                    ((ImageView) view.findViewById(R.id.itemCameraPicture)).setContentDescription(sensor.getName());
                    ((TextView) view.findViewById(R.id.itemCameraDescr)).setText(sensor.getName());
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
        View rootView = inflater.inflate(R.layout.fragment_cameras, container, false);

        ListView lvMain = (ListView) rootView.findViewById(R.id.lwCameras);
        lvMain.setAdapter(mAdapter);

        lvMain.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), ShowPictureFullScreen.class);
                Sensor sensor = (Sensor)parent.getItemAtPosition(position);
                intent.putExtra(boardsManager.BOARD_ID, mBoardId);
                intent.putExtra(boardsManager.SENSOR_NAME, sensor.getName());
                startActivity(intent);
            }
        });

        lvMain.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Sensor sensor = (Sensor)parent.getItemAtPosition(position);
                onItemSelected(sensor);
                return false;
            }
        });


        return rootView;

    }
}
