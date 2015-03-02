package ru.wo0t.smarthouse.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.SMHZApp;
import ru.wo0t.smarthouse.board.AbstractBoard;
import ru.wo0t.smarthouse.board.boardsManager;
import ru.wo0t.smarthouse.common.constants;


public class BoardsLookupActivity extends Activity {
    public static final String FOUND_NEW_BOARD = "FOUND_NEW_BOARD";
    public static final String BOARDS_DISCOVERY_FINISHED = "BOARDS_DISCOVERY_FINISHED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lookup_boards);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter iff= new IntentFilter(FOUND_NEW_BOARD);
        iff.addAction(BOARDS_DISCOVERY_FINISHED);

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onNotice, iff);
        ((SMHZApp)getApplication()).getBoardsManager().lookUpForBoards();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onNotice);
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

    private BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(FOUND_NEW_BOARD))
            {
                Log.d(constants.APP_TAG, "Found new board: " + intent.getStringExtra(boardsManager.BOARD_DESCR));

                AbstractBoard.BOARD_TYPE boardType = AbstractBoard.BOARD_TYPE.valueOf(intent.getStringExtra(boardsManager.BOARD_TYPE));
                if (boardType == AbstractBoard.BOARD_TYPE.REMOTE) {
                    int boardId = intent.getIntExtra(boardsManager.BOARD_ID, -1);
                    String descr = intent.getStringExtra(boardsManager.BOARD_DESCR);
                    String login = intent.getStringExtra(boardsManager.BOARD_LOGIN);
                    String pw = intent.getStringExtra(boardsManager.BOARD_PW);
                    ((SMHZApp) getApplication()).getBoardsManager().connectToRemoteBoard(boardId, descr, login, pw);
                }
                else {
                    int boardId = intent.getIntExtra(boardsManager.BOARD_ID, -1);
                    String descr = intent.getStringExtra(boardsManager.BOARD_DESCR);
                    String ipAddr = intent.getStringExtra(boardsManager.BOARD_IP_ADDR);
                    ((SMHZApp) getApplication()).getBoardsManager().connectToLocalBoard(boardId, descr, ipAddr);
                }
            }
            else
            if (intent.getAction().equals(BOARDS_DISCOVERY_FINISHED))
            {
                Log.d(constants.APP_TAG, "Finishing boards discovery");
            }
        }
    };
}
