package ru.wo0t.smarthouse;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alex on 2/25/15.
 */

public class PagerAdapter extends FragmentStatePagerAdapter {
    public static enum PageType {INFO, SENS, SWITCH, CAME}

    private List<Page> mFragments;

    public PagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mFragments = new ArrayList<>();
// INFO
        Fragment fragment = new InfoFragment();
        Bundle args = new Bundle();
        args.putInt(SensorsFragment.ARG_OBJECT, 1);
        fragment.setArguments(args);
        Page page = new Page(context, fragment, PageType.INFO);
        mFragments.add(page);
// SENSORS
        fragment = new SensorsFragment();
        args = new Bundle();
        args.putInt(SensorsFragment.ARG_OBJECT, 1);
        fragment.setArguments(args);
        page = new Page(context, fragment, PageType.SENS);
        mFragments.add(page);
// SWITCHES
        fragment = new SwitchesFragment();
        args = new Bundle();
        args.putInt(SensorsFragment.ARG_OBJECT, 1);
        fragment.setArguments(args);
        page = new Page(context, fragment, PageType.SWITCH);
        mFragments.add(page);
// CAMERAS
        fragment = new CamerasFragment();
        args = new Bundle();
        args.putInt(SensorsFragment.ARG_OBJECT, 1);
        fragment.setArguments(args);
        page = new Page(context, fragment, PageType.CAME);
        mFragments.add(page);
    }

    @Override
    public Fragment getItem(int i) {
        return mFragments.get(i).mFragment;
    }

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
}