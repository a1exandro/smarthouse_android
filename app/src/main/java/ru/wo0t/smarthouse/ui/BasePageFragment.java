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
import android.widget.BaseAdapter;

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

    protected int mBoardId =-1;
    protected String mSystem;
    protected LayoutInflater mInflater;
    protected SensorsAdapter mAdapter;

    abstract View getLWItemView(int position, View convertView, ViewGroup parent);

    protected AbstractBoard getBoard() {
        return ((SMHZApp) getActivity().getApplication()).getBoardsManager().getBoard(getBoardId());
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Bundle args = getArguments();
            if (mBoardId == -1)
                mBoardId = args.getInt(boardsManager.BOARD_ID);
            mSystem = args.getString(boardsManager.MSG_SYSTEM_NAME);
            mAdapter = new SensorsAdapter();
        }
        mInflater = (LayoutInflater) getActivity().getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void changeBoard(int aBoardId) { mBoardId = aBoardId; }

    public int getBoardId() { return mBoardId; }

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
            int boardId = intent.getIntExtra(boardsManager.BOARD_ID, -1);
            String system = intent.getStringExtra(boardsManager.MSG_SYSTEM_NAME);

            if (boardId == getBoardId()) {
                switch (intent.getAction()) {
                    case boardsManager.MSG_BOARD_CFG_CHANGED: {
                        if (mSystem.equals(context.getString(R.string.systemNameINFO))) {
                            mAdapter.update();
                        }
                    } break;
                    case boardsManager.MSG_SENSOR_DATA: {
                        if (mSystem.equals(system)) {
                            mAdapter.update();

                            Log.d(constants.APP_TAG, mSystem + ": update " + intent.getStringExtra(boardsManager.SENSOR_NAME));
                        }
                    } break;
                }
            }
        }

    };

///// Adapter

    public class SensorsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            AbstractBoard board = getBoard();
            int size = 0;
            if (board != null) {
                try {
                    size = board.getSensors(Sensor.SENSOR_SYSTEM.valueOf(mSystem)).size();
                }
                catch (Exception e) {
                    size = 0;
                }

            }
            return size;
        }

        public void update() {
            notifyDataSetChanged();
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getLWItemView(position, convertView, parent);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            AbstractBoard board = getBoard();
            Sensor sens = null;
            if (board != null) {
                try {
                    sens = board.getSensors(Sensor.SENSOR_SYSTEM.valueOf(mSystem)).get(position);
                }
                catch (Exception e){
                    Log.e(constants.APP_TAG, "Could not find sensor at pos "+position);
                }
            }
            return sens;
        }
    }
}
