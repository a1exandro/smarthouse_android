package ru.wo0t.smarthouse.ui;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

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

    private ProgressDialog mSensorUpdProcessDialog;
    private Sensor mCurrentUpdatingSensor;
    
    abstract View getLWItemView(int position, View convertView, ViewGroup parent);

    protected AbstractBoard getBoard() {
        boardsManager bm = ((SMHZApp) getActivity().getApplication()).getBoardsManager();
        if (bm != null) return bm.getBoard(getBoardId());
            else
        return null;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mBoardId = ((SMHZApp) getActivity().getApplication()).getBoardId();
        }
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
    
    protected boolean updateSensDialog(Sensor sensor) {
        if (mCurrentUpdatingSensor != null) return false;
        
        mSensorUpdProcessDialog = new ProgressDialog(getActivity());
        mSensorUpdProcessDialog.setMessage(getString(R.string.sensUpdating) + "'" + sensor.getName()+"'");
        mSensorUpdProcessDialog.show();
        mSensorUpdProcessDialog.setOnCancelListener( new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mCurrentUpdatingSensor = null;
            }
        });
        mCurrentUpdatingSensor = sensor;

        return true;
    }
    
    public void changeBoard(int aBoardId) {
        mBoardId = aBoardId;
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    public int getBoardId() { return mBoardId; }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter iff= new IntentFilter(boardsManager.MSG_BOARD_CFG_CHANGED);
        iff.addAction(boardsManager.MSG_SENSOR_DATA);
        iff.addAction(boardsManager.MSG_BOARD_DISCONNECTED);
        iff.addAction(boardsManager.MSG_BOARD_CONNECTED);
        iff.addAction(boardsManager.MSG_BOARD_PING);
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
                    case boardsManager.MSG_BOARD_CONNECTED: {
                        mAdapter.update();

                    } break;
                    case boardsManager.MSG_BOARD_DISCONNECTED: {    // TODO: why i don't receive this fucking message?
                        mAdapter.update();

                    } break;
                    case boardsManager.MSG_BOARD_PING: {
                        if (mSystem.equals(context.getString(R.string.systemNameINFO))) {
                            mAdapter.update();
                        }
                    } break;
                    case boardsManager.MSG_BOARD_CFG_CHANGED: {
                        if ((mSystem.equals(context.getString(R.string.systemNameINFO))) || (mSystem.equals(system))) {
                            mAdapter.update();
                        }
                    } break;
                    case boardsManager.MSG_SENSOR_DATA: {
                        if (mSystem.equals(system)) {
                            mAdapter.update();

                            String sName = intent.getStringExtra(boardsManager.SENSOR_NAME);

                            if (mCurrentUpdatingSensor != null) {
                                if (mCurrentUpdatingSensor.getName().equals(sName)) {
                                    mSensorUpdProcessDialog.dismiss();
                                    mCurrentUpdatingSensor = null;
                                    mSensorUpdProcessDialog = null;
                                    Toast.makeText(getActivity().getApplicationContext(), context.getString(R.string.sensSuccessfullyUpdated)+ "'" + sName + "'", Toast.LENGTH_SHORT).show();
                                }
                            }

                            Log.d(constants.APP_TAG, mSystem + ": update " + sName);
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
