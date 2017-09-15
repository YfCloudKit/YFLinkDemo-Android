package com.demo.yflinkdemo.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

/**
 * Created by yunfan on 2017/7/8.
 */

public class MenuPagerAdapter extends FragmentPagerAdapter {


    private final ArrayList<Fragment> mList;

    public MenuPagerAdapter(FragmentManager fm, ArrayList<Fragment> fragments) {
        super(fm);
        mList = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return mList.get(position);
    }

    @Override
    public int getCount() {
        return mList.size();
    }
}
