package ru.wo0t.smarthouse.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.SMHZApp;
import ru.wo0t.smarthouse.board.AbstractBoard;
import ru.wo0t.smarthouse.board.Sensor;
import ru.wo0t.smarthouse.board.boardsManager;
import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 3/4/15.
 */
abstract public class BasePageFragment extends Fragment{
    protected final String ITEM_RESOURCE_ID = "ITEM_RESOURCE_ID";

    protected int mBoardId;
    protected String mSystem;
    protected LayoutInflater mInflater;
    protected SensorsAdapter mAdapter;

    abstract View getView(int position, View convertView, ViewGroup parent);

    void onItemSelected(Sensor sensor) {

    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Bundle args = getArguments();
            mBoardId = args.getInt(boardsManager.BOARD_ID);
            mSystem = args.getString(boardsManager.MSG_SYSTEM_NAME);
            mAdapter = new SensorsAdapter();
        }
        mInflater = (LayoutInflater) getActivity().getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void changeBoard(int aBoardId) {
        mBoardId = aBoardId;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter iff= new IntentFilter(boardsManager.MSG_BOARD_CFG_CHANGED);
        iff.addAction(boardsManager.MSG_SENSOR_DATA);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(onNotice, iff);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(onNotice);
    }

    private final BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            AbstractBoard board = ((SMHZApp) getActivity().getApplication()).getBoardsManager().getBoard(mBoardId);
            if (intent.getAction().equals(boardsManager.MSG_BOARD_CFG_CHANGED))
            {
                String system = intent.getStringExtra(boardsManager.MSG_SYSTEM_NAME);
                if (mSystem.equals(system)) {   // out system cfg changed
                    mAdapter.clear();
                    List<Sensor> sensLst = board.getSensors(Sensor.SENSOR_SYSTEM.valueOf(mSystem));
                    mAdapter.addSensors(sensLst);
                    Log.d(constants.APP_TAG, system + " system configuration changed: " + intent.getStringExtra(boardsManager.BOARD_DESCR));
                }
            }
            else
            if (intent.getAction().equals(boardsManager.MSG_SENSOR_DATA))
            {
                int boardId = intent.getIntExtra(boardsManager.BOARD_ID,-1);
                String sensSystem = intent.getStringExtra(boardsManager.MSG_SYSTEM_NAME);
                if (boardId != -1 && boardId == mBoardId && mSystem.equals(sensSystem)) {   // our board's sensor changed
                    Log.d(constants.APP_TAG, "Sensor val changed " + intent.getStringExtra(boardsManager.SENSOR_NAME));
                }
            }
        }
    };

///// Adapter

    public class SensorsAdapter extends BaseAdapter {
        ArrayList<Sensor> mSensors;

        SensorsAdapter () {
            mSensors = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return mSensors.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return mSensors.get(position);
        }

        public void addSensor(Sensor aSensor) {
            mSensors.add(aSensor);
            notifyDataSetChanged();
        }

        public void addSensors(List<Sensor> aSensors) {
            mSensors.addAll(aSensors);
            notifyDataSetChanged();
        }

        public void clear() {
            mSensors.clear();
        }
    }
}
