package ru.wo0t.smarthouse.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.SMHZApp;
import ru.wo0t.smarthouse.board.AbstractBoard;
import ru.wo0t.smarthouse.board.boardsManager;
import ru.wo0t.smarthouse.common.constants;


public class BoardsLookupActivity extends Activity {

    public static final int REQUEST_CODE_GET_BOARD = 1;
    BoardsAdapter mAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lookup_boards);
        mAdapter = new BoardsAdapter();
        ListView lvMain = (ListView) findViewById(R.id.boardsList);
        lvMain.setAdapter(mAdapter);

        lvMain.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boardDescr board = (boardDescr)parent.getItemAtPosition(position);
                onBoardSelected(board);
            }
        });
    }

    private void onBoardSelected(boardDescr board) {
        Intent args = new Intent();
        args.putExtra(boardsManager.BOARD_ID, board.mBoardId);
        args.putExtra(boardsManager.BOARD_TYPE, board.mBoardType.toString());
        args.putExtra(boardsManager.BOARD_IP_ADDR, board.mIpAddr);
        args.putExtra(boardsManager.BOARD_LOGIN, board.mLogin);
        args.putExtra(boardsManager.BOARD_PW, board.mPassword);
        args.putExtra(boardsManager.BOARD_DESCR, board.mDescr);
        setResult(RESULT_OK,args);
        finish();
    }
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter iff= new IntentFilter(boardsManager.MSG_FOUND_NEW_BOARD);
        iff.addAction(boardsManager.MSG_BOARDS_DISCOVERY_FINISHED);

        mAdapter.clear();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onNotice, iff);
        ((SMHZApp)getApplication()).getBoardsManager().lookUpForBoards();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onNotice);
        ((SMHZApp)getApplication()).getBoardsManager().closeBoardsLookup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_lookup_boards, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(boardsManager.MSG_FOUND_NEW_BOARD))
            {
                Log.d(constants.APP_TAG, "Found new board: " + intent.getStringExtra(boardsManager.BOARD_DESCR));

                AbstractBoard.BOARD_TYPE boardType = AbstractBoard.BOARD_TYPE.valueOf(intent.getStringExtra(boardsManager.BOARD_TYPE));
                int boardId = intent.getIntExtra(boardsManager.BOARD_ID, -1);
                String descr = intent.getStringExtra(boardsManager.BOARD_DESCR);

                boardDescr board = new boardDescr();
                board.mBoardId = boardId;
                board.mDescr = descr;
                board.mBoardType = boardType;

                if (boardType == AbstractBoard.BOARD_TYPE.REMOTE) {

                    String login = intent.getStringExtra(boardsManager.BOARD_LOGIN);
                    String pw = intent.getStringExtra(boardsManager.BOARD_PW);

                    board.mLogin = login;
                    board.mPassword = pw;
                }
                else {
                    String ipAddr = intent.getStringExtra(boardsManager.BOARD_IP_ADDR);

                    board.mIpAddr = ipAddr;
                }
                mAdapter.addBoard(board);
            }
            else
            if (intent.getAction().equals(boardsManager.MSG_BOARDS_DISCOVERY_FINISHED))
            {
                Toast.makeText(getApplicationContext(), getString(R.string.msg_board_discovery_finished), Toast.LENGTH_SHORT).show();
                Log.d(constants.APP_TAG, "Finishing boards discovery");
            }
        }
    };

///// Adapter

    public class BoardsAdapter extends BaseAdapter {
        ArrayList<boardDescr> mBoards;
        LayoutInflater mInflater;

        BoardsAdapter () {
            mBoards = new ArrayList<>();
            mInflater = (LayoutInflater) getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mBoards.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.lvitem_boardslookupactivity, parent, false);
            }

            boardDescr board = mBoards.get(position);

            ((TextView) view.findViewById(R.id.itemTxtBoardDescr)).setText(board.mDescr);

            String boardText = "[" + board.mBoardId + "] ";
            if (board.mBoardType == AbstractBoard.BOARD_TYPE.REMOTE) {
                boardText += getString(R.string.remoteBoard);
            }
            else {
                boardText += getString(R.string.localBoard) + " ("+board.mIpAddr+")";
            }

            ((TextView) view.findViewById(R.id.itemTxtBoardType)).setText(boardText);
            return view;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return mBoards.get(position);
        }

        public void addBoard(boardDescr aBoard) {
            if (aBoard.mBoardType == AbstractBoard.BOARD_TYPE.REMOTE)
                mBoards.add(aBoard);    // just add remote board
            else
                mBoards.add(0, aBoard); // but local boards goes to the top of the list
            notifyDataSetChanged();
        }

        public void clear() {
            mBoards.clear();
        }
    }

// BoardDescription class

    public class boardDescr {
        public String mDescr;
        public AbstractBoard.BOARD_TYPE mBoardType;
        public int mBoardId;
        public String mIpAddr;
        public String mLogin;
        public String mPassword;
    }
}
