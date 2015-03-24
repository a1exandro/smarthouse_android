package ru.wo0t.smarthouse.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.board.AbstractBoard;
import ru.wo0t.smarthouse.board.Sensor;
import ru.wo0t.smarthouse.board.boardsManager;

/**
 * Created by alex on 2/25/15.
 */

public class PagerAdapter extends FragmentStatePagerAdapter {
    public static enum PageType {INFO, SENS, SWITCH, CAME}

    private List<Page> mFragments;

    public PagerAdapter(FragmentManager fm, Context context, int boardId) {
        super(fm);
        mFragments = new ArrayList<>();
// INFO INDEX 0
        Fragment fragment = new InfoFragment();
        Bundle args = new Bundle();
        args.putInt(boardsManager.BOARD_ID, boardId);
        args.putString(boardsManager.MSG_SYSTEM_NAME, context.getString(R.string.systemNameINFO));
        fragment.setArguments(args);
        Page page = new Page(context, fragment, PageType.INFO);
        mFragments.add(page);
// SENSORS INDEX 1
        fragment = new SensorsFragment();
        args = new Bundle();
        args.putInt(boardsManager.BOARD_ID, boardId);
        args.putString(boardsManager.MSG_SYSTEM_NAME, Sensor.SENSOR_SYSTEM.SENSES.toString());
        fragment.setArguments(args);
        page = new Page(context, fragment, PageType.SENS);
        mFragments.add(page);
// SWITCHES INDEX 2
        fragment = new SwitchesFragment();
        args = new Bundle();
        args.putInt(boardsManager.BOARD_ID, boardId);
        args.putString(boardsManager.MSG_SYSTEM_NAME, Sensor.SENSOR_SYSTEM.SWITCHES.toString());
        fragment.setArguments(args);
        page = new Page(context, fragment, PageType.SWITCH);
        mFragments.add(page);
// CAMERAS INDEX 3
        fragment = new CamerasFragment();
        args = new Bundle();
        args.putInt(boardsManager.BOARD_ID, boardId);
        args.putString(boardsManager.MSG_SYSTEM_NAME, Sensor.SENSOR_SYSTEM.CAMES.toString());
        fragment.setArguments(args);
        page = new Page(context, fragment, PageType.CAME);
        mFragments.add(page);
    }

    public int getSystemIndex(Sensor.SENSOR_SYSTEM system) {
        if (system == Sensor.SENSOR_SYSTEM.SENSES) return 1;
        if (system == Sensor.SENSOR_SYSTEM.SWITCHES) return 2;
        if (system == Sensor.SENSOR_SYSTEM.CAMES) return 3;
        return 0;
    }
    @Override
    public Fragment getItem(int i) { return mFragments.get(i).mFragment; }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return (mFragments.get(position)).mCaption;
    }

    public class Page {
        public String mCaption;
        public Fragment mFragment;
        public PageType mType;

        Page (Context context,Fragment aFragment, PageType aType)
        {
            mFragment = aFragment; mType = aType;
            switch (mType) {

                case INFO:
                    mCaption = context.getResources().getString(R.string.frame_info);
                    break;
                case SENS:
                    mCaption = context.getResources().getString(R.string.frame_sensors);
                    break;
                case SWITCH:
                    mCaption = context.getResources().getString(R.string.frame_switches);
                    break;
                case CAME:
                    mCaption = context.getResources().getString(R.string.frame_cameras);
                    break;
            }
        }
    }

    public void changeBoard(int aBoardId) {
        for (int i = 0; i < getCount(); i++) {
            ((BasePageFragment)mFragments.get(i).mFragment).changeBoard(aBoardId);
        }
    }
}