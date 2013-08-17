
package com.halzhang.android.apps.startupnews.ui;

import com.halzhang.android.apps.startupnews.R;
import com.halzhang.android.apps.startupnews.utils.UIUtils;
import com.halzhang.android.common.CDLog;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnLastItemVisibleListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * StartupNews
 * <p>
 * </p>
 *
 * @author <a href="http://weibo.com/halzhang">Hal</a>
 * @version Feb 26, 2013
 */
public abstract class AbsBaseListFragment extends Fragment implements OnItemClickListener,
        OnScrollListener, OnLastItemVisibleListener, OnRefreshListener2<ListView> {

    private static final String LOG_TAG = "BaseListFragment";
    
    private ListView mListView;

    private PullToRefreshListView mPullToRefreshListView;

    private ListAdapter mAdapter;

    private View mEmptyView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CDLog.d(LOG_TAG, this.toString()+" create view!");
        View view = inflater.inflate(getContentViewId(), null);
        mPullToRefreshListView = (PullToRefreshListView) view.findViewById(R.id.pull_refresh_list);
        if (mPullToRefreshListView == null) {
            throw new RuntimeException("Your content must have a ListView whose id attribute is "
                    + "'R.id.pull_refresh_list'");
        }
        mPullToRefreshListView.setOnRefreshListener(this);
        mPullToRefreshListView.setOnLastItemVisibleListener(this);
        mPullToRefreshListView.setMode(Mode.BOTH);
        mPullToRefreshListView.getLoadingLayoutProxy(false, true).setPullLabel(getString(R.string.pull_to_refresh_from_bottom_pull_label));
        mListView = mPullToRefreshListView.getRefreshableView();
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);
        mEmptyView = view.findViewById(android.R.id.empty);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && UIUtils.hasHoneycomb()) {
            mListView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
        }
        // if(mEmptyView != null && mAdapter == null/*第一次初始化*/){
        // mPullToRefreshListView.setVisibility(View.INVISIBLE);
        // mEmptyView.setVisibility(View.VISIBLE);
        // }else{
        // mPullToRefreshListView.setVisibility(View.VISIBLE);
        // mEmptyView.setVisibility(View.INVISIBLE);
        // }
        mListView.setEmptyView(mEmptyView);
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Set Adapter here！
    }

    public void setListAdapter(ListAdapter adapter) {
        mAdapter = adapter;
        mListView.setAdapter(adapter);
    }

    /**
     * hide loading view,show listview
     */
    protected void onDataFirstLoadComplete() {
        // if(mEmptyView != null){
        // mEmptyView.setVisibility(View.GONE);
        // }
        // if(mPullToRefreshListView != null){
        // mPullToRefreshListView.setVisibility(View.VISIBLE);
        // }
    }

    @Override
    public void onLastItemVisible() {
        onPullListLastItemVisible();
    }

    @Override
    public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
        onPullDownListViewRefresh((PullToRefreshListView) refreshView);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
        onPullUpListViewRefresh((PullToRefreshListView) refreshView);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onListItemClick((ListView) parent, view, position, id);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mListView == view && mAdapter != null) {
            // mAdapter.searchAsyncImageViews(view, scrollState ==
            // SCROLL_STATE_IDLE);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                         int totalItemCount) {
        // do nothing
    }

    /**
     * Get the fragment's list view widget.
     */
    public ListView getListView() {
        return mListView;
    }

    /**
     * Get the fragment's pull refresh list view widget
     *
     * @return
     */
    public PullToRefreshListView getPullToRefreshListView() {
        return mPullToRefreshListView;
    }

    /**
     * Get the ListAdapter associated with this fragment's ListView.
     */
    public ListAdapter getListAdapter() {
        return mAdapter;
    }

    protected void onPullListLastItemVisible() {

    }

    /**
     * 向上拖动刷新
     *
     * @param refreshListView
     */
    protected void onPullUpListViewRefresh(PullToRefreshListView refreshListView) {

    }

    /**
     * 向下拖动刷新
     *
     * @param refreshListView
     */
    protected void onPullDownListViewRefresh(PullToRefreshListView refreshListView) {

    }

    @SuppressLint("NewApi")
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (UIUtils.hasHoneycomb()){
            if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
                mListView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
            }else{
                mListView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
            }
        }
    }

    /**
     * This method will be called when an item in the list is selected.
     * Subclasses should override. Subclasses can call
     * getListView().getItemAtPosition(position) if they need to access the data
     * associated with the selected item.
     *
     * @param l        The ListView where the click happened
     * @param v        The view that was clicked within the ListView
     * @param position The position of the view in the list
     * @param id       The row id of the item that was clicked
     */
    protected void onListItemClick(ListView l, View v, int position, long id) {

    }

    /**
     * Get layout res for Fragment
     *
     * @return Fragment Content View Id
     * @see #onCreateView(LayoutInflater, ViewGroup, Bundle)
     */
    public abstract int getContentViewId();


}
