/**
 * Copyright (C) 2013 HalZhang
 */

package com.halzhang.android.apps.startupnews.ui.fragment;

import com.halzhang.android.apps.startupnews.R;
import com.halzhang.android.apps.startupnews.analytics.Tracker;
import com.halzhang.android.apps.startupnews.entity.SNComment;
import com.halzhang.android.apps.startupnews.entity.SNComments;
import com.halzhang.android.apps.startupnews.parser.SNCommentsParser;
import com.halzhang.android.apps.startupnews.snkit.JsoupFactory;
import com.halzhang.android.apps.startupnews.ui.DiscussActivity;
import com.halzhang.android.apps.startupnews.utils.DateUtils;
import com.halzhang.android.common.CDLog;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * StartupNews
 * <p>
 * 评论
 * </p>
 * 
 * @author <a href="http://weibo.com/halzhang">Hal</a>
 * @version Mar 7, 2013
 */
public class CommentsListFragment extends AbsBaseListFragment {

    private static final String LOG_TAG = CommentsListFragment.class.getSimpleName();

    public interface OnCommentSelectedListener {
        /**
         * @param position
         * @param discussUrl url
         */
        public void onCommentSelected(int position, String discussUrl);
    }

    // private ArrayList<SNComment> mComments = new ArrayList<SNComment>(24);

    private CommentsAdapter mAdapter;

    private CommentsTask mTask;

    // private String mMoreUrl;

    private SNComments mSnComments = new SNComments();;

    private static final String NEWCOMMENTS_URL_PATH = "/newcomments";

    private JsoupFactory mJsoupFactory;
    
    private OnCommentSelectedListener mCommentSelectedListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new CommentsAdapter();
        mJsoupFactory = JsoupFactory.getInstance(getActivity().getApplicationContext());
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof OnCommentSelectedListener){
            mCommentSelectedListener = (OnCommentSelectedListener)activity;
        }else{
            CDLog.d(LOG_TAG, "Attach activity is not implements OnCommentSelectedListener!");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(mAdapter);
        if (mTask == null && mAdapter.isEmpty()) {
            mTask = new CommentsTask(CommentsTask.TYPE_REFRESH);
            mTask.execute(getString(R.string.host, NEWCOMMENTS_URL_PATH));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Tracker.getInstance().sendEvent("ui_action", "list_item_click",
                "comments_list_fragment_list_item_click", 0L);
        SNComment comment = mSnComments.getSnComments().get(position - 1);
        if(mCommentSelectedListener != null){
            mCommentSelectedListener.onCommentSelected(position - 1, comment.getDiscussURL());
        }else{
            Intent intent = new Intent(getActivity(), DiscussActivity.class);
            intent.putExtra(DiscussActivity.ARG_DISCUSS_URL, comment.getDiscussURL());
            startActivity(intent);
        }
    }

    @Override
    protected void onPullDownListViewRefresh(PullToRefreshListView refreshListView) {
        super.onPullDownListViewRefresh(refreshListView);
        Tracker.getInstance().sendEvent("ui_action", "pull_down_list_view_refresh",
                "comments_list_fragment_pull_down_list_view_refresh", 0L);
        if (mTask != null) {
            return;
        }
        mTask = new CommentsTask(CommentsTask.TYPE_REFRESH);
        mTask.execute(getString(R.string.host, NEWCOMMENTS_URL_PATH));
    }

    @Override
    protected void onPullUpListViewRefresh(PullToRefreshListView refreshListView) {
        super.onPullUpListViewRefresh(refreshListView);
        Tracker.getInstance().sendEvent("ui_action", "pull_up_list_view_refresh",
                "comments_list_fragment_pull_up_list_view_refresh", 0L);
        if (mTask != null) {
            return;
        }
        if (TextUtils.isEmpty(mSnComments.getMoreURL())) {
            Toast.makeText(getActivity(), R.string.tip_last_page, Toast.LENGTH_SHORT).show();
            getPullToRefreshListView().onRefreshComplete();
        } else {
            mTask = new CommentsTask(CommentsTask.TYPE_LOADMORE);
            mTask.execute(mSnComments.getMoreURL());
        }
    }

    private class CommentsTask extends AsyncTask<String, Void, Boolean> {

        public static final int TYPE_REFRESH = 1;

        public static final int TYPE_LOADMORE = 2;

        private int mType = 0;

        public CommentsTask(int type) {
            mType = type;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                Connection conn = mJsoupFactory.newJsoupConnection(params[0]);
                if (conn == null) {
                    return false;
                }
                Document doc = conn.get();
                // long start = System.currentTimeMillis();
                SNCommentsParser parser = new SNCommentsParser();
                SNComments comments = parser.parseDocument(doc);
                if (mType == TYPE_REFRESH && mSnComments.size() > 0) {
                    mSnComments.clear();
                }
                mSnComments.addComments(comments.getSnComments());
                mSnComments.setMoreURL(comments.getMoreURL());
                // Log.i(LOG_TAG, "Take Time: " + (System.currentTimeMillis() -
                // start));
                return true;
            } catch (Exception e) {
                Log.e(LOG_TAG, "", e);
                Tracker.getInstance().sendException("CommentsTask", e, false);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                onDataFirstLoadComplete();
                mAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_LONG).show();
            }
            getPullToRefreshListView().getLoadingLayoutProxy().setLastUpdatedLabel(
                    DateUtils.getLastUpdateLabel(getActivity()));
            getPullToRefreshListView().onRefreshComplete();
            mTask = null;
            super.onPostExecute(result);
        }

        @Override
        protected void onCancelled() {
            getPullToRefreshListView().onRefreshComplete();
            mTask = null;
            super.onCancelled();
        }

    }

    private class CommentsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mSnComments.size();
        }

        @Override
        public Object getItem(int position) {
            return mSnComments.getSnComments().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(getActivity()).inflate(
                        R.layout.comment_list_item, null);
                holder.mUserId = (TextView) convertView.findViewById(R.id.comment_item_user_id);
                holder.mCreated = (TextView) convertView.findViewById(R.id.comment_item_created);
                holder.mCommentText = (TextView) convertView.findViewById(R.id.comment_item_text);
                holder.mArtistTitle = (TextView) convertView
                        .findViewById(R.id.comment_item_artist_titile);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            SNComment comment = mSnComments.getSnComments().get(position);
            holder.mUserId.setText(comment.getUser().getId());
            holder.mCreated.setText(comment.getCreated());
            holder.mCommentText.setText(comment.getText());
            holder.mArtistTitle.setText(getString(R.string.comment_artist_title,
                    comment.getArtistTitle()));
            return convertView;
        }

        class ViewHolder {
            TextView mUserId;

            TextView mCreated;

            TextView mCommentText;

            TextView mArtistTitle;
        }

    }

    @Override
    public int getContentViewId() {
        return R.layout.ptr_list_layout;
    }

}
