package ru.wo0t.smarthouse;

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

import ru.wo0t.smarthouse.board.boardsManager;
import ru.wo0t.smarthouse.common.constants;

public class MainActivity extends FragmentActivity {
    PagerAdapter mPagerAdapter;
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Log.d("smhz","Starting the program");

        try {
            Thread.sleep(5000);     // wait for emulator

        } catch (InterruptedException e) {
            Log.e(constants.APP_TAG, e.toString());
        }

        try {

            final ActionBar actionBar = getActionBar();

            // Specify that tabs should be displayed in the action bar.
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            mPagerAdapter = new PagerAdapter(getSupportFragmentManager(), getApplicationContext());

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



        } catch (Exception e) {
            Log.e(constants.APP_TAG, e.toString());
        }


        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String login = pref.getString("remote_login","");
            String password = pref.getString("remote_password","");
            new boardsManager().lookUpForBoards(constants.LOCAL_BOARD_PORT, login, password);
        } catch (Exception e) {
            Log.e(constants.APP_TAG, e.toString());
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
                Intent intent = new Intent(this, BoardsLookup.class);
                startActivity(intent);
            } break;
            default:  return super.onOptionsItemSelected(item);
        }

        return true;
    }
}

