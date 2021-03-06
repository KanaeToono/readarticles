package com.example.conga.tvo.adapters.recycleradapters;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.conga.tvo.R;
import com.example.conga.tvo.adapters.viewholders.ListLinksViewHolder;
import com.example.conga.tvo.constants.ConstantRssItem;
import com.example.conga.tvo.controllers.OnItemClickListener;
import com.example.conga.tvo.databases.RssItemHelper;
import com.example.conga.tvo.models.ContentRss;
import com.example.conga.tvo.models.RssItem;
import com.example.conga.tvo.utils.NetworkUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by ConGa on 12/04/2016.
 */
public class ListRssItemAdapter extends RecyclerView.Adapter<ListLinksViewHolder> {
    private static String TAG = ListRssItemAdapter.class.getSimpleName();
    private List<RssItem> mArrayListRssItems;
    private LayoutInflater mLayoutInflater;
    //  private Context mContext;
    private OnItemClickListener.OnItemClickCallback onItemClickCallback;
    public SharedPreferences prefs;
    private RssItemHelper mRssItemDatabase;
    private Activity mActivity;
    private static final int HTTP_OK = 200;
    private ProgressDialog mProgressDialog;
    private NetworkUtils mNetworkUtils;
    // constructor

    public ListRssItemAdapter(Activity mContext, List<RssItem> mArrayListRssItems,
                              OnItemClickListener.OnItemClickCallback onItemClickCallback
    ) {
        this.mArrayListRssItems = mArrayListRssItems;
        this.mActivity = mContext;
        mLayoutInflater = LayoutInflater.from(mContext);
        this.onItemClickCallback = onItemClickCallback;
        prefs = mContext.getSharedPreferences("MyPREFERENCES", Context.MODE_PRIVATE);
        mNetworkUtils = new NetworkUtils(mContext);


    }


    @Override
    public ListLinksViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.customlistlinksrsslayout, parent, false);
        ListLinksViewHolder myViewHolder = new ListLinksViewHolder(view);
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(final ListLinksViewHolder holder, final int position) {

        holder.textViewTitleRss.setText(mArrayListRssItems.get(position).getTitle());
        holder.textViewPubDate.setText(mArrayListRssItems.get(position).getPubDate());
        // xu li phan nut like , bo vao muc favorites
        holder.imageViewLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRssItemDatabase = new RssItemHelper(mActivity);
                try {
                    mRssItemDatabase.open();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                String title = mArrayListRssItems.get(position).getTitle();
                String pubDate = mArrayListRssItems.get(position).getPubDate();
                String image = mArrayListRssItems.get(position).getImage();
                String link = mArrayListRssItems.get(position).getLink();
                RssItem rssItem = new RssItem(title, link, pubDate, image);
                mRssItemDatabase.addNewItemRss(rssItem);
                mRssItemDatabase.closeDatabase();
                Toast.makeText(mActivity, R.string.notification_when_click_like, Toast.LENGTH_SHORT).show();

            }
        });
        // xu li phan download de doc offline

        holder.imageViewDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
if(mNetworkUtils.isConnectingToInternet()) {
    mActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
            //       String linkTag = mArrayListRssItems.get(position).getLinkTag();
            new SaveContentRssAsyncTask().execute();
        }
    });
}
                else {
    Toast.makeText(mActivity, R.string.network_unvalable, Toast.LENGTH_SHORT).show();
                }
//                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//                    @Override
//                    public void onCancel(DialogInterface dialog) {
//                        SaveContentRssAsyncTask.cancel(true);
//                    }
//                });

            }

            class SaveContentRssAsyncTask extends AsyncTask<Void, Void, Void> {
                String respondString = "";
                static final String USER_AGENT_BROWER = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36";


                @Override
                protected Void doInBackground(Void... params) {

                    Connection.Response response = null;
                    try {
                        response = Jsoup.connect(mArrayListRssItems.get(position).getLink()).timeout(100 * 10000)
                                .method(Connection.Method.POST)
                                .userAgent(ConstantRssItem.USER_AGENT_WEB)
                                .ignoreHttpErrors(true).execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Map<String, String> cookies = response.cookies();
                    Document document = null;
                    try {
                        document = Jsoup.connect(mArrayListRssItems.get(position).getLink()).timeout(100 * 100000).
                                userAgent(ConstantRssItem.USER_AGENT_WEB2).
                                ignoreHttpErrors(true).method(Connection.Method.POST).cookies(cookies).
                                get();
                        if (mArrayListRssItems.get(position).getLink().contains("vnexpress.net")) {

                            Elements content = document.select("div [class= fck_detail width_common]");
                            // Elements elements = document.select("html");
                            for (Element element : content) {
                                respondString += element.text();
                            }
                        }
                        if (mArrayListRssItems.get(position).getLink().contains("dantri.com.vn")) {
                            Elements content = document.select("div#divNewsContent");
                            // Elements elements = document.select("html");
                            for (Element element : content) {
                                respondString += element.text();
                            }
                        }
                        if (mArrayListRssItems.get(position).getLink().contains("www.24h.com")) {
                            Elements content = document.select("div.text-conent");
                            // Elements elements = document.select("html");
                            for (Element element : content) {
                                respondString += element.text();
                            }

                        }
                        if (mArrayListRssItems.get(position).getLink().contains("vietnamnet.vn")) {
                            Elements content = document.select("div [class = ArticleDetail]");
                            // Elements elements = document.select("html");
                            for (Element element : content) {
                                respondString += element.text();
                            }
                        }
                        Log.d(TAG, respondString);
                        // lay ve cai title cua bai bao
                        String title = document.title();
                        String pubDate = mArrayListRssItems.get(position).getPubDate();
                        mRssItemDatabase = new RssItemHelper(mActivity);
                        try {
                            mRssItemDatabase.open();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // THEM VAO CSDL
                        ContentRss contentRss = new ContentRss(title, respondString, pubDate);
                        mRssItemDatabase.addNewItemContent(contentRss);
                        mRssItemDatabase.closeDatabase();


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Elements elements = document.select("div [class= fck_detail width_common]");

                    // ContentRss contentRss= new ContentRss(title, result,"" );
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    super.onPostExecute(result);
                    Toast.makeText(mActivity, R.string.respond_when_download_complete, Toast.LENGTH_SHORT).show();


                }
            }

        });
        //
//        int key = prefs.getInt("STATUS_KEY", 0);
//        int pos = prefs.getInt("POSITION", 0);
//        if (key == 1 && pos == position) {
//            holder.mLinearLayout.setBackgroundResource(R.color.colorWhite);
//        }
        //
        if (mArrayListRssItems.get(position).getImage() == null) {
            holder.imageViewImageTitle.setBackgroundResource(R.drawable.blue);
        } else {
            ImageLoader.getInstance().displayImage(mArrayListRssItems.
                            get(position).getImage(), holder.imageViewImageTitle,
                    new ImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            holder.progressBar.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            holder.progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            holder.progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadingCancelled(String imageUri, View view) {
                            holder.progressBar.setVisibility(View.GONE);
                        }
                    });
        }
        holder.mView.setOnClickListener(new OnItemClickListener(position, onItemClickCallback));
    }


    @Override
    public int getItemCount() {

        if (mArrayListRssItems == null) {

           // Toast.makeText(mActivity, R.string.respond_server_empty, Toast.LENGTH_SHORT).show();
            return 0;
        } else {
            return mArrayListRssItems.size();
        }
//        return mArrayListRssItems == null ? 0 : mArrayListRssItems.size();
    }


}
