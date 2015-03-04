package ru.wo0t.smarthouse.ui;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;

import android.support.v4.view.ViewPager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.SMHZApp;
import ru.wo0t.smarthouse.board.AbstractBoard;
import ru.wo0t.smarthouse.board.boardsManager;
import ru.wo0t.smarthouse.common.constants;

public class MainActivity extends FragmentActivity {
    PagerAdapter mPagerAdapter;
    ViewPager mViewPager;

    private int mBoardId = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Log.d(constants.APP_TAG,"Starting the program");

        try {
            Thread.sleep(5000);     // wait for emulator

        } catch (InterruptedException e) {
            Log.e(constants.APP_TAG, e.toString());
        }

        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            int defaultBoard = pref.getInt("default_board",-1);
            if (defaultBoard == -1)
            {
                Intent intent = new Intent(this, BoardsLookupActivity.class);
                startActivityForResult(intent, BoardsLookupActivity.REQUEST_CODE_GET_BOARD);
            }
            configureActionBar();

        } catch (Exception e) {
            Log.e(constants.APP_TAG, e.toString());
        }

    }

    private void configureActionBar() {
        final ActionBar actionBar = getActionBar();

        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager(), getApplicationContext(),mBoardId);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        getActionBar().setSelectedNavigationItem(position);
                    }
                });

        for (int i = 0; i < mPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mPagerAdapter.getPageTitle(i))
                            .setTabListener(
                                    new ActionBar.TabListener() {
                                        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                                            mViewPager.setCurrentItem(tab.getPosition());
                                        }

                                        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                                            // hide the given tab
                                        }

                                        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                                            // probably ignore this event
                                        }
                                    }
                            ));
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            } break;
            case R.id.action_boards_lookup: {
                Intent intent = new Intent(this, BoardsLookupActivity.class);
                startActivityForResult(intent, BoardsLookupActivity.REQUEST_CODE_GET_BOARD);
            } break;
            default:  return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            switch (requestCode) {
                case BoardsLookupActivity.REQUEST_CODE_GET_BOARD:
                {
                    if (resultCode == RESULT_OK) {
                        String boardDscr = data.getStringExtra(boardsManager.BOARD_DESCR);
                        Log.d(constants.APP_TAG, "Connection to board requested: " + boardDscr);

                        if (mBoardId != -1) {
                            ((SMHZApp) getApplication()).getBoardsManager().closeBoard(mBoardId);
                            mBoardId = -1;
                        }

                        AbstractBoard.BOARD_TYPE boardType = AbstractBoard.BOARD_TYPE.valueOf(data.getStringExtra(boardsManager.BOARD_TYPE));
                        int boardId = data.getIntExtra(boardsManager.BOARD_ID, -1);

                        // connect to remote board
                        if (boardType == AbstractBoard.BOARD_TYPE.REMOTE) {

                            String login = data.getStringExtra(boardsManager.BOARD_LOGIN);
                            String pw = data.getStringExtra(boardsManager.BOARD_PW);

                            ((SMHZApp) getApplication()).getBoardsManager().connectToRemoteBoard(boardId, boardDscr, login, pw);
                        }
                        // connect to local board
                        else {
                            String ipAddr = data.getStringExtra(boardsManager.BOARD_IP_ADDR);

                            ((SMHZApp) getApplication()).getBoardsManager().connectToLocalBoard(boardId, boardDscr, ipAddr);
                        }
                        mBoardId = boardId;
                        mPagerAdapter.changeBoard(boardId);
                    }
                    else
                        Log.d(constants.APP_TAG, "Board selection has been canceled!");
                }
            }
        }
        catch (Exception e) {
            Log.e(constants.APP_TAG, e.toString());
        }

    }
}

