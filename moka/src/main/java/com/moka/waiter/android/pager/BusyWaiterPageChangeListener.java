package com.moka.waiter.android.pager;

import androidx.viewpager.widget.ViewPager;

import com.moka.waiter.android.BusyWaiter;

public class BusyWaiterPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
    private BusyWaiter busyWaiter;

    public BusyWaiterPageChangeListener(BusyWaiter busyWaiter) {
        this.busyWaiter = busyWaiter;
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        super.onPageScrollStateChanged(state);
        if (state != ViewPager.SCROLL_STATE_IDLE) {
            busyWaiter.busyWith(this);
        } else {
            busyWaiter.completed(this);
        }
    }
}
