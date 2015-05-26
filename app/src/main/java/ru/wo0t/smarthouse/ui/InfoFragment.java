package ru.wo0t.smarthouse.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.board.AbstractBoard;
import ru.wo0t.smarthouse.board.RemoteBoard;
import ru.wo0t.smarthouse.board.Sensor;


public class InfoFragment extends BasePageFragment {

    void onItemSelected(Sensor sensor) {

    }
    
    @Override
    public View getLWItemView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.lvitem_info_list, parent, false);
        }
        ArrayList<String> list = (ArrayList<String>)(mAdapter.getItem(position));

        ((TextView) view.findViewById(R.id.infoLwItemCapt)).setText(list.get(0));
        ((TextView) view.findViewById(R.id.infoLwItemVal)).setText(list.get(1));

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new InfoAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_info, container, false);

        ListView lvMain = (ListView) rootView.findViewById(R.id.lwInfo);
        lvMain.setAdapter(mAdapter);

        lvMain.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Sensor sensor = (Sensor)parent.getItemAtPosition(position);
                //onItemSelected(sensor);
            }
        });

        return rootView;

    }

///// Adapter

    public class InfoAdapter extends SensorsAdapter {
        ArrayList<String> mKeys = new ArrayList<>();
        HashMap<String,String> mItems = new HashMap<>();

        InfoAdapter() {
            update();
        }

        @Override
        public int getCount() { return mKeys.size(); }

        public void update() {
            AbstractBoard board = getBoard();
            if (board == null) return;

            mKeys.clear();
            mItems.clear();

            mKeys.add(getString(R.string.name));
            mKeys.add(getString(R.string.type));
            if (board.getBoardType() == AbstractBoard.BOARD_TYPE.REMOTE) {
                mKeys.add(getString(R.string.user_name));
            }
            mKeys.add(getString(R.string.boardId));
            mKeys.add(getString(R.string.lastActivity));
            mKeys.add(getString(R.string.boardState));

            for (int i = 0; i < mKeys.size(); i++) {
                String key = mKeys.get(i);
                String val = key;

                if (key.equals(getString(R.string.boardId))) val = String.valueOf(board.getBoardId());
                if (key.equals(getString(R.string.name))) val = board.getBoardName();
                if (key.equals(getString(R.string.type))) {
                    if (board.getBoardType() == AbstractBoard.BOARD_TYPE.REMOTE)
                        val = getString(R.string.remoteBoard);
                    else
                        val = getString(R.string.localBoard);
                }
                if (key.equals(getString(R.string.user_name))) val = ((RemoteBoard)board).getLogin();
                if (key.equals(getString(R.string.lastActivity))) val = board.getLastActivityString();
                if (key.equals(getString(R.string.boardState))) {
                    AbstractBoard.BOARD_STATE state = board.getBoardState();
                    if (state == AbstractBoard.BOARD_STATE.CONNECTED)
                        val = getString(R.string.boardConnected);
                    else
                        val = getString(R.string.boardDisconnected);
                }
                mItems.put(key,val);
            }

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
            ArrayList<String> list = new ArrayList<>();

            list.add(mKeys.get(position));
            list.add(mItems.get(mKeys.get(position)));

            return list;
        }
    }
}
