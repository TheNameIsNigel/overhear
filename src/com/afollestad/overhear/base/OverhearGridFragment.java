package com.afollestad.overhear.base;

import com.afollestad.overhear.R;
import com.afollestad.silk.caching.SilkComparable;

/**
 * The base of all grid fragments, used for convenience (handles common functions that every
 * fragment uses, reducing the amount of code and complexity among activity Java files).
 *
 * @author Aidan Follestad
 */
public abstract class OverhearGridFragment<ItemType extends SilkComparable> extends OverhearListFragment<ItemType> {

    @Override
    protected int getLayout() {
        return R.layout.fragment_grid;
    }
}
