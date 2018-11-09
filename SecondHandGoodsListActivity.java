package com.dianzhi.xianzhuan.activity.secondhand_sale;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cundong.recyclerview.HeaderAndFooterRecyclerViewAdapter;
import com.cundong.recyclerview.LRecyclerView;
import com.cundong.recyclerview.customview.shape.SuperTextView;
import com.cundong.recyclerview.util.RecyclerViewUtils;
import com.dianzhi.xianzhuan.R;
import com.dianzhi.xianzhuan.activity.CommonActivity;
import com.dianzhi.xianzhuan.activity.SearchActivityPro;
import com.dianzhi.xianzhuan.application.GlideLoaderHelper;
import com.dianzhi.xianzhuan.base.FloatitleActivity;
import com.dianzhi.xianzhuan.base.slider.IntentUtils;
import com.dianzhi.xianzhuan.model.GoodProduct;
import com.dianzhi.xianzhuan.model.GoodProducts;
import com.dianzhi.xianzhuan.network.Network;
import com.dianzhi.xianzhuan.utils.ToastUtil;
import com.dianzhi.xianzhuan.utils.Utils;
import com.dianzhi.xianzhuan.view.GoodsSelectTypeView;
import com.dianzhi.xianzhuan.view.NoScrollViewPager;
import com.dianzhi.xianzhuan.view.countdownview.CountdownView;
import com.dianzhi.xianzhuan.view.floatbutton.FloatingActionButton;
import com.dianzhi.xianzhuan.view.floatbutton.ScrollDirectionListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.SimpleDraweeView;
import com.tencent.stat.StatService;
import com.trello.rxlifecycle.android.ActivityEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Administrator on 2017/12/14.
 */

public class SecondHandGoodsListActivity extends FloatitleActivity implements LRecyclerView.LScrollListener {
    @BindView(R.id.back_iv)
    ImageView back_iv;
    @BindView(R.id.head_r_iv)
    ImageView head_r_iv;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.app)
    AppBarLayout app;
    @BindView(R.id.mlrv)
    LRecyclerView mlrv;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.imageView)
    ImageView imageView;
    @BindView(R.id.rl_404)
    RelativeLayout rl_404;
    @BindView(R.id.imageview)
    ImageView imageview;
    @BindView(R.id.tv_no_data)
    TextView tv_no_data;
    @BindView(R.id.rl_no_data)
    RelativeLayout rl_no_data;
    @BindView(R.id.iv_load)
    ImageView iv_load;
    @BindView(R.id.rl_load)
    RelativeLayout rl_load;
    @BindView(R.id.sv_type_top)
    GoodsSelectTypeView sv_type_top;
    @BindView(R.id.ll_search)
    LinearLayout ll_search;
    private boolean isRefresh = true;
    private boolean isLoadmore = false;
    private boolean ifNomore = false;
    private boolean isFirst = true;
    private AnimationDrawable animationDrawable;
    private List<GoodProduct> datas = new ArrayList<>();
    private long yhOverTime, endTIme;
    private int width = 0;
    private String cateId = "";

    @Override
    protected void onCreate(Bundle arg0) {
        needImmersive = false;
        super.onCreate(arg0);
        setContentView(R.layout.activity_second_hand_goodslist, true);
        ButterKnife.bind(this);
        init();
        StatService.trackCustomKVEvent(this,"优品列表界面", null);
    }

    private void init() {
        width = getResources().getDisplayMetrics().widthPixels / 2;
        animationDrawable = (AnimationDrawable) iv_load.getDrawable();
        animationDrawable.start();
        initView();
        initMoneyData();
        RecyclerViewUtils.setFooterView(mlrv, footView);
        initListener();
        initData();
    }

    private void initListener() {
        ll_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SecondHandGoodsListActivity.this, SearchActivityPro.class);
                intent.putExtra("type", "3");
                IntentUtils.getInstance().startActivity(SecondHandGoodsListActivity.this,intent);

            }
        });
    }

    private View footView, headView, no_goods_view;
    private RecyclerAdapter mAdapter;
    private ImageView iv_float;
    private HeaderAndFooterRecyclerViewAdapter mHeaderAndFooterRecyclerViewAdapter;
    private RecyclerView rl_preferential_goods;
    private PreferentialGoodsAdapter adapter;
    private GoodsSelectTypeView sv_type;
    private NoScrollViewPager vp_roll;
    private CountdownView cv_countdown;
    private TextView tv_countdown_des;
    private TextView tv_see_all_discounts;
    private SecondHandSelectTypePopupWindow typePopupWindow;
    private SecondHandSelectMoneyOrderPopupWindow moneyOrderPopupWindow;

    /**
     * 判断是否由中部的GoodsSelectTypeView打开
     */
//    private boolean is_sv_type_open = false;
    public String[] androidItems;

    public String[] iosItems;

    private void JSONArrayToStrings(JSONArray ja, String[] items) throws Exception {
        items = new String[ja.length()];
        for(int i=0; i < ja.length(); i++) {
            items[i] = ja.getString(i);
        }
    }

    private void initMoneyData() {
        String op = "phoneModels";
        Map<String, String> map = Utils.getMap(uid, imei, ver, op);
        String sign = Utils.getRequestParamString(map);
        String url = Utils.getUrl("api/zy_goods", map);
        Network.getMyCenterApiString()
                .getSecondHandTypeList("api/zy_goods", map, sign)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        isLoadmore = false;
                        isRefresh = false;
//                        Log.i("TAG", "TAG");
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        ToastUtil.showToast(SecondHandGoodsListActivity.this, R.string.wlyc);
                        isLoadmore = false;
                        isRefresh = false;
                        error();
                    }
                    // sv_type 上次点击idx
                    private int befIdx = -1;
                    @Override
                    public void onNext(String str) {
                        try {
                            JSONObject jo = new JSONObject(str);
                            if(jo.getString("status").equals("1")) {
                                JSONObject data = jo.getJSONObject("data");
                                JSONArray ja = data.getJSONArray("android");
                                androidItems = new String[ja.length()];
                                for(int i=0; i < ja.length(); i++) {
                                    androidItems[i] = ja.getString(i).replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", "");
                                }
                                JSONArray jai = data.getJSONArray("ios");
                                iosItems = new String[jai.length()];
                                for(int i=0; i < jai.length(); i++) {
                                    iosItems[i] = jai.getString(i).replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", "");
                                }
                                //JSONArrayToStrings(data.getJSONArray("android"), androidItems);
                                //JSONArrayToStrings(data.getJSONArray("ios"), iosItems);

                                sv_type.setOnSelectTypeListener(new GoodsSelectTypeView.SelectTypeListener() {
                                    @Override
                                    public void onSelectTypeListener(int position) {
                                        if (sv_type_top.getVisibility() != View.GONE) {

                                            return;
                                        }
                                        switch (position) {

                                            case 0:
                                                type = "2";
                                                p = 1;
                                                /*if(befIdx != 0) {
                                                    typePopupWindow.smoothPosition(0);
                                                }*/
                                                befIdx = 0;
                                                //typePopupWindow.setItems(iosItems);
                                                initData();
                                                break;
                                            case 1:
                                                type = "1";
                                                p = 1;
                                                befIdx = 1;
                                                //typePopupWindow.setItems(androidItems);
                                                initData();
                                                break;
                                            case 2:
//                        is_sv_type_open = true;
                                                if (moneyOrderPopupWindow.isShowing()) {
                                                    moneyOrderPopupWindow.dismiss();
                                                }
                                                // 初次或上次点击安卓时
                                                if(befIdx == -1 || befIdx == 0) {
                                                    typePopupWindow.setItems(iosItems);
                                                    typePopupWindow.smoothPosition(0);
                                                } else if(befIdx == 1) {
                                                    typePopupWindow.setItems(androidItems);
                                                    typePopupWindow.smoothPosition(1);
                                                }
                                                sv_type_top.setVisibility(View.VISIBLE);
                                                new Handler().post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        typePopupWindow.showAsDropDown(sv_type_top);
                                                    }
                                                });

//                        sv_type.setVisibility(View.GONE);
                                                break;
                                            case 3:
//                        is_sv_type_open = true;
                                                if (typePopupWindow.isShowing()) {
                                                    typePopupWindow.dismiss();
                                                }
                                                sv_type_top.setVisibility(View.VISIBLE);
                                                new Handler().post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        moneyOrderPopupWindow.showAsDropDown(sv_type_top);
                                                    }
                                                });

//                        sv_type.setVisibility(View.GONE);

                                                break;
                                            default:
                                        }
                                    }

                                    @Override
                                    public void onSelectTypeStartListener(int position) {

                                        sv_type_top.setIndex(position, 350);
                                    }
                                });
                                sv_type_top.setOnSelectTypeListener(new GoodsSelectTypeView.SelectTypeListener() {
                                    @Override
                                    public void onSelectTypeListener(int position) {
                                        if (sv_type_top.getVisibility() == View.GONE) {
                                            return;
                                        }
                                        switch (position) {
                                            case 0:
                                                type = "2";
                                                p = 1;
                                                befIdx = 0;
                                                typePopupWindow.setItems(iosItems);
                                                typePopupWindow.smoothPosition(0);
//                        if (!moneyOrderPopupWindow.isShowing() && !typePopupWindow.isShowing()) {

                                                initData();
//                        }
                                                break;
                                            case 1:
                                                type = "1";
                                                p = 1;
                                                befIdx = 1;
                                                typePopupWindow.setItems(androidItems);
                                                typePopupWindow.smoothPosition(1);
//                        if (!moneyOrderPopupWindow.isShowing() && !typePopupWindow.isShowing()) {
                                                initData();
//                        }
                                                break;
                                            case 2:
                                                if (moneyOrderPopupWindow.isShowing()) {
                                                    moneyOrderPopupWindow.dismiss();
                                                }
                                                sv_type_top.setVisibility(View.VISIBLE);
                                                typePopupWindow.showAsDropDown(sv_type_top);
                                                break;
                                            case 3:
                                                if (typePopupWindow.isShowing()) {
                                                    typePopupWindow.dismiss();
                                                }
                                                sv_type_top.setVisibility(View.VISIBLE);
                                                moneyOrderPopupWindow.showAsDropDown(sv_type_top);
                                                break;
                                            default:
                                        }
                                    }

                                    @Override
                                    public void onSelectTypeStartListener(int position) {
                                        sv_type.setIndex(position, 350);
                                    }
                                });
                            } else {
                                ToastUtil.showToast(SecondHandGoodsListActivity.this, jo.getString("msg"));
                            }

                        } catch (Exception e) {
                            //error();
                        }
                    }
                });
    }

    private LinearLayout ll_no_goods, boLayout, ll, ll_init_no_goods;

    private void initView() {
        typePopupWindow = new SecondHandSelectTypePopupWindow(this);
        moneyOrderPopupWindow = new SecondHandSelectMoneyOrderPopupWindow(this);
        typePopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                typePopupWindow.dismiss();
                //isnewType = typePopupWindow.getItems()[position];
                String[] spl = typePopupWindow.getItems()[position].split(",");
                cateId = spl[0];
                p = 1;
                initData();
            }
        });
        moneyOrderPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                moneyOrderPopupWindow.dismiss();
                moneyOrder = (position) + "";
                p = 1;
                initData();
            }
        });
        typePopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
//                if (is_sv_type_open) {
                if (mHeaderAndFooterRecyclerViewAdapter.getHeaderView().getBottom() >goodsSelectTypeViewHeight) {

                    sv_type_top.setVisibility(View.GONE);
                }

//                }
//                is_sv_type_open = false;
//                sv_type.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
////                        sv_type.setVisibility(View.VISIBLE);
//
//                    }
//                }, 300);

            }
        });
        moneyOrderPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
//                if (is_sv_type_open) {
                if (mHeaderAndFooterRecyclerViewAdapter.getHeaderView().getBottom() >goodsSelectTypeViewHeight) {

                    sv_type_top.setVisibility(View.GONE);
                }

//                }
//                is_sv_type_open = false;
//                sv_type.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
////                        sv_type.setVisibility(View.VISIBLE);
//
//                    }
//                }, 300);
            }
        });
        mAdapter = new RecyclerAdapter();
        GridLayoutManager manager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        mlrv.setLayoutManager(manager);
        mlrv.setPullRefreshEnabled(true);
        mHeaderAndFooterRecyclerViewAdapter = new HeaderAndFooterRecyclerViewAdapter(this, mAdapter);
        footView = LayoutInflater.from(this).inflate(R.layout.layout_no_goods, null);
        ll_no_goods = (LinearLayout) footView.findViewById(R.id.ll_no_goods);
        boLayout = (LinearLayout) footView.findViewById(R.id.boLayout);
        ll_init_no_goods = (LinearLayout) footView.findViewById(R.id.ll_init_no_goods);
        no_goods_view = LayoutInflater.from(this).inflate(R.layout.layout_no_goods, null);
        footView.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        no_goods_view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, Utils.dip2px(this, 90)));
        mlrv.setAdapter(mHeaderAndFooterRecyclerViewAdapter);
        mlrv.setLScrollListener(this);
        fab.attachToRecyclerView(mlrv, new ScrollDirectionListener() {
            @Override
            public void onScrollDown() {
                fab.hide();
            }

            @Override
            public void onScrollUp() {
                fab.show();
            }
        }, new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                RecyclerView.LayoutManager layoutManager = mlrv.getLayoutManager();
                LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
                int firstVisibleItemPosition = linearManager.findFirstVisibleItemPosition();
                if (firstVisibleItemPosition > 4) {
                    fab.show();
                } else {
                    fab.hide();

                }
                if (mHeaderAndFooterRecyclerViewAdapter.getHeaderView().getBottom() <= goodsSelectTypeViewHeight||firstVisibleItemPosition>1) {
                    if (sv_type_top.getVisibility() == View.GONE) {
                        sv_type_top.setVisibility(View.VISIBLE);

                    }


                } else {

                    if (sv_type_top.getVisibility() != View.GONE) {

                        sv_type_top.setVisibility(View.GONE);


                    }
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_DRAGGING) {

//                    GlideApp.with(SecondHandGoodsListActivity.this).resumeRequests();
                } else {

//                    GlideApp.with(SecondHandGoodsListActivity.this).pauseRequests();
                }
            }
        });
        mlrv.setPullRefreshEnabled(true);
        headView = View.inflate(this, R.layout.item_second_hand_head, null);
        RecyclerViewUtils.setHeaderView(mlrv, headView);
        rl_preferential_goods = (RecyclerView) headView.findViewById(R.id.rl_preferential_goods);
        ll = (LinearLayout) headView.findViewById(R.id.ll);
        sv_type = (GoodsSelectTypeView) headView.findViewById(R.id.sv_type);
        iv_float= (ImageView) headView.findViewById(R.id.iv_float);
        cv_countdown = (CountdownView) headView.findViewById(R.id.cv_countdown);
        tv_countdown_des = (TextView) headView.findViewById(R.id.tv_countdown_des);
        tv_see_all_discounts = (TextView) headView.findViewById(R.id.tv_see_all_discounts);
        vp_roll = (NoScrollViewPager) headView.findViewById(R.id.vp_roll);
        adapter = new PreferentialGoodsAdapter();
        rl_preferential_goods.setLayoutManager(new LinearLayoutManager(SecondHandGoodsListActivity.this, LinearLayoutManager.HORIZONTAL, false));
        rl_preferential_goods.setAdapter(adapter);
        sv_type_top.setVisibility(View.VISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mlrv.scrollToPosition(0);
                fab.hide();

            }
        });
        tv_see_all_discounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SecondHandGoodsListActivity.this, SecondHandDiscountsListActivity.class);
                intent.putExtra("yhOverTime",yhOverTime);
                IntentUtils.getInstance().startActivity(SecondHandGoodsListActivity.this, intent);
            }
        });
    }

    private int goodsSelectTypeViewHeight;


    /**
     * 1--安卓系列 | 2-苹果系列 | 3--机况 | 4--价格
     */
    private String type = "2";
    private int p = 1;
    private List<GoodProducts.Discount> discounts;
    private List<GoodProducts.BigEye> bigeye;
    private String isnewType, moneyOrder;
    private boolean isLoding = false;

    private void initData() {
        isLoding = true;

        ll_no_goods.setVisibility(View.GONE);
        boLayout.setVisibility(View.VISIBLE);
        ll_init_no_goods.setVisibility(View.GONE);
        String op = "publishList";
        Map<String, String> map = Utils.getMap(uid, imei, ver, op, "type=" + type, "p=" + p);
        if (!TextUtils.isEmpty(isnewType)) {
            map.put("isnewType", isnewType);
        }
        if (!TextUtils.isEmpty(cateId)) {
            map.put("cateId", cateId);
        }
        if (!TextUtils.isEmpty(moneyOrder)) {
            map.put("moneyOrder", moneyOrder);
        }

        String sign = Utils.getRequestParamString(map);
        String url = Utils.getUrl("api/zy_goods", map);
//        Log.i("TAG", url);
        subscription = Network.getMyCenterApi()
                .getSecondHandGoodsList("api/zy_goods", map, sign)
                .subscribeOn(Schedulers.io())
                .compose(this.<GoodProducts>bindUntilEvent(ActivityEvent.DESTROY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GoodProducts>() {
                    @Override
                    public void onCompleted() {
                        isLoadmore = false;
                        isRefresh = false;
//                        Log.i("TAG", "TAG");
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        ToastUtil.showToast(SecondHandGoodsListActivity.this, R.string.wlyc);
                        isLoadmore = false;
                        isRefresh = false;
                        error();
                    }

                    @Override
                    public void onNext(GoodProducts mGoodProducts) {
                        if (mGoodProducts.status.equals("1")) {
                            next();
                            if (p == 1) {

                                datas.clear();
                                if (null != mGoodProducts.data.list && mGoodProducts.data.list.size() > 0) {
                                    datas.addAll(mGoodProducts.data.list);
                                    if (mGoodProducts.data.list.size() < 4) {
                                        ll_no_goods.setVisibility(View.VISIBLE);
                                        boLayout.setVisibility(View.GONE);
                                    }
                                } else {

                                    ll_no_goods.setVisibility(View.GONE);
                                    boLayout.setVisibility(View.GONE);
                                    ll_init_no_goods.setVisibility(View.VISIBLE);
                                }
                                discounts = mGoodProducts.data.discounts;
                                bigeye = mGoodProducts.data.bigeye;
                                yhOverTime = mGoodProducts.data.yhOverTime;
                                setData2View();


                            } else {
                                if (mGoodProducts.data != null && mGoodProducts.data.list != null && mGoodProducts.data.list.size() > 0) {

                                    datas.addAll(mGoodProducts.data.list);


                                } else {
                                    ll_no_goods.setVisibility(View.VISIBLE);
                                    boLayout.setVisibility(View.GONE);
                                }
                            }
                            mAdapter.notifyDataSetChanged();
                            mHeaderAndFooterRecyclerViewAdapter.notifyDataSetChanged();


                        } else {
                            ToastUtil.showToast(SecondHandGoodsListActivity.this, mGoodProducts.msg);

                        }

                    }


                });
    }

    private void setData2View() {
        if (0 == yhOverTime) {
            cv_countdown.setVisibility(View.GONE);
            tv_countdown_des.setText("本场结束 明日上新");
        } else {
            cv_countdown.setVisibility(View.VISIBLE);
            tv_countdown_des.setText("距本次结束:");
        }
        cv_countdown.postDelayed(new Runnable() {
            @Override
            public void run() {
                endTIme = yhOverTime * 1000 + System.currentTimeMillis();
                cv_countdown.start(yhOverTime * 1000);
            }
        }, 200);
        cv_countdown.setOnCountdownEndListener(new CountdownView.OnCountdownEndListener() {
            @Override
            public void onEnd(CountdownView cv) {
                tv_countdown_des.setText("本场结束 明日上新");
                cv_countdown.setVisibility(View.GONE);
                p = 1;
                isRefresh = true;
                isLoadmore = false;
//                type = "2";
                fab.hide();
                initData();
            }
        });
        if (discounts == null) {
            ll.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
        goodsSelectTypeViewHeight = sv_type.getMeasuredHeight();
        BannerAdapter bannerAdapter = new BannerAdapter(vp_roll, this);
        vp_roll.setAdapter(bannerAdapter);

        cv_countdown.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                cv_countdown.start(endTIme - System.currentTimeMillis());
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                cv_countdown.stop();
            }
        });
    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder holder = null;
            holder = new RecyclerItemHolder(LayoutInflater.from(SecondHandGoodsListActivity.this).inflate(R.layout.item_second_hand_goods, parent, false));
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            RecyclerItemHolder mHolder = (RecyclerItemHolder) holder;
            mHolder.tv_cancel_order_see_identify_result.setText(datas.get(position).isnew);
            mHolder.tv_name.setText(datas.get(position).name);
            mHolder.tv_price.setText(datas.get(position).yprice);
            if (datas.get(position).num.equals("0")) {
                mHolder.iv_yishouqing.setVisibility(View.VISIBLE);
                mHolder.rl_yishouqing.setVisibility(View.VISIBLE);
                mHolder.tv_cancel_order_see_identify_result.setSolid(getResources().getColor(R.color.color_b3b3b3));
            } else {
                mHolder.iv_yishouqing.setVisibility(View.GONE);
                mHolder.rl_yishouqing.setVisibility(View.GONE);
                mHolder.tv_cancel_order_see_identify_result.setSolid(getResources().getColor(R.color.color_29b772));
            }

            GlideLoaderHelper.loadFrescoNetImg(mHolder.iv, datas.get(position).icon
                    , Utils.dip2px(SecondHandGoodsListActivity.this, 130), Utils.dip2px(SecondHandGoodsListActivity.this, 125));
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mHolder.ll.getLayoutParams();
            layoutParams.width = (getResources().getDisplayMetrics().widthPixels - Utils.dip2px(SecondHandGoodsListActivity.this, 18)) / 2;
            if (position % 2 == 1) {
                layoutParams.leftMargin = Utils.dip2px(SecondHandGoodsListActivity.this, 3);
            } else {
                layoutParams.leftMargin = Utils.dip2px(SecondHandGoodsListActivity.this, 6);
            }
            mHolder.ll.setLayoutParams(layoutParams);
            mHolder.ll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(SecondHandGoodsListActivity.this, SecondHandGoodsDetailsActivity.class);
                    intent.putExtra("publish_id", datas.get(position).id);
                    intent.putExtra("title", datas.get(position).name);
                    IntentUtils.getInstance().startActivity(SecondHandGoodsListActivity.this,intent);

                }
            });
        }

        @Override
        public int getItemCount() {
            return datas == null ? 0 : datas.size();
        }


    }

    class RecyclerItemHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.iv)
        SimpleDraweeView iv;
        @BindView(R.id.ll)
        LinearLayout ll;
        @BindView(R.id.tv_cancel_order_see_identify_result)
        SuperTextView tv_cancel_order_see_identify_result;
        @BindView(R.id.tv_name)
        TextView tv_name;
        @BindView(R.id.tv_price)
        TextView tv_price;
        @BindView(R.id.iv_yishouqing)
        SuperTextView iv_yishouqing;
        @BindView(R.id.iv_yijiesu)
        SuperTextView iv_yijiesu;
        @BindView(R.id.rl_yishouqing)
        RelativeLayout rl_yishouqing;

        public RecyclerItemHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
//            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) iv.getLayoutParams();
//            layoutParams.width = (getResources().getDisplayMetrics().widthPixels - Utils.dip2px(SecondHandGoodsListActivity.this, 18)) / 2;
//            layoutParams.height = layoutParams.width;
//            iv.setLayoutParams(layoutParams);
        }
    }

    private class PreferentialGoodsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder holder = null;
            holder = new PreferentialGoodHolder(LayoutInflater.from(SecondHandGoodsListActivity.this).inflate(R.layout.item_second_hand_preferentia_goods, parent, false));
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            PreferentialGoodHolder mHolder = (PreferentialGoodHolder) holder;
            final GoodProducts.Discount discount = discounts.get(position);
            GlideLoaderHelper.loadFrescoNetImg(mHolder.iv, discount.icon
                    , Utils.dip2px(SecondHandGoodsListActivity.this, 130), Utils.dip2px(SecondHandGoodsListActivity.this, 125));
            mHolder.tv_cancel_order_see_identify_result.setText(discount.isnew);
            mHolder.tv_price.setText(discount.zhprice);
            mHolder.tv_yprice.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG); // 设置中划线并加清晰
            mHolder.tv_yprice.setText("¥" + discount.yprice);
            mHolder.tv_discounts.setText(discount.discounts);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mHolder.tv_discounts.getLayoutParams();
            if (1 == discount.discounts.length()) {
                mHolder.tv_discounts.setPadding(0,0,0,0);
            } else {
                mHolder.tv_discounts.setPadding(0,0,Utils.dip2px(SecondHandGoodsListActivity.this,8),0);
            }
            mHolder.tv_discounts.setLayoutParams(layoutParams);
            mHolder.tv_discounts_name.setText(discount.name);
            if (0==yhOverTime) {
                mHolder.iv_yishouqing.setVisibility(View.INVISIBLE);
                mHolder.iv_yijiesu.setVisibility(View.VISIBLE);
                mHolder.rl_yishouqing.setVisibility(View.VISIBLE);
                mHolder.tv_cancel_order_see_identify_result.setSolid(getResources().getColor(R.color.color_b3b3b3));

            } else {
                mHolder.iv_yijiesu.setVisibility(View.INVISIBLE);
                if (discount.num.equals("0")) {
                    mHolder.iv_yishouqing.setVisibility(View.VISIBLE);
                    mHolder.rl_yishouqing.setVisibility(View.VISIBLE);
                    mHolder.tv_cancel_order_see_identify_result.setSolid(getResources().getColor(R.color.color_b3b3b3));
                } else {
                    mHolder.iv_yishouqing.setVisibility(View.GONE);
                    mHolder.rl_yishouqing.setVisibility(View.GONE);
                    mHolder.tv_cancel_order_see_identify_result.setSolid(getResources().getColor(R.color.color_29b772));
                }
            }

            mHolder.ll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(SecondHandGoodsListActivity.this, SecondHandGoodsDetailsActivity.class);
                    intent.putExtra("publish_id", discount.id);
                    intent.putExtra("title", discount.name);
                    intent.putExtra("isYh",1);
                    intent.putExtra("yhOverTime",yhOverTime);
                    IntentUtils.getInstance().startActivity(SecondHandGoodsListActivity.this, intent);

                }
            });


        }

        @Override
        public int getItemCount() {
            return discounts == null ? 0 : discounts.size();
        }
    }


    class PreferentialGoodHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_cancel_order_see_identify_result)
        SuperTextView tv_cancel_order_see_identify_result;
        @BindView(R.id.iv)
        SimpleDraweeView iv;
        @BindView(R.id.tv_price)
        TextView tv_price;
        @BindView(R.id.tv_yprice)
        TextView tv_yprice;
        @BindView(R.id.tv_discounts)
        TextView tv_discounts;
        @BindView(R.id.ll)
        ConstraintLayout ll;
        @BindView(R.id.iv_yishouqing)
        SuperTextView iv_yishouqing;
        @BindView(R.id.iv_yijiesu)
        SuperTextView iv_yijiesu;
        @BindView(R.id.tv_discounts_name)
        TextView tv_discounts_name;
        @BindView(R.id.rl_yishouqing)
        RelativeLayout rl_yishouqing;

        public PreferentialGoodHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public class BannerAdapter extends PagerAdapter {
        private Context context;

        public BannerAdapter(NoScrollViewPager viewPager, Context context) {

            this.context = context;
        }

        @Override
        public View instantiateItem(ViewGroup container, final int position) {
//            View view = View.inflate(context, R.layout.banner_item, null);
//            ImageView imageView = (ImageView) view.findViewById(R.id.iv);
            SimpleDraweeView simpleDraweeView=new SimpleDraweeView(context);
            simpleDraweeView.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.FIT_XY);
            simpleDraweeView.getHierarchy().setPlaceholderImage(R.drawable.zwt_normal2);
            GlideLoaderHelper.loadFrescoNetImg(simpleDraweeView,bigeye.get(position).picture,670,670);
//            GlideLoaderHelper.loadImageNoScaletype(context, bigeye.get(position).picture, R.drawable.zwt_normal2, imageView, false, getResources().getDisplayMetrics().widthPixels, Utils.dip2px(context, 180));
            container.addView(simpleDraweeView);
            simpleDraweeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CommonActivity.start(SecondHandGoodsListActivity.this,"闲转优品的常见问题",bigeye.get(position).link);
                }
            });
            return simpleDraweeView;
        }

        @Override
        public int getCount() {
            return bigeye == null ? 0 : bigeye.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view==object;
        }
    }

    @OnClick(R.id.back_iv)
    public void back_iv(View view) {
        finish();
    }


    private void error() {
        mlrv.refreshComplete();
        animationDrawable.stop();
        rl_load.setVisibility(View.INVISIBLE);
        if (p == 1) {
            rl_404.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRefresh() {
        p = 1;
        isRefresh = true;
        isLoadmore = false;
        isnewType = "";
        moneyOrder = "";
        initData();
        typePopupWindow.setIndex(-1);
        moneyOrderPopupWindow.setIndex(-1);
        fab.hide();
    }

    @Override
    public void onScrollUp() {
    }

    @Override
    public void onScrollDown() {

    }

    @Override
    public void onBottom() {

        if (boLayout.getVisibility() == View.VISIBLE && ll_no_goods.getVisibility() == View.GONE) {
            p++;

            initData();
        }
//        RecyclerViewUtils.setFooterView(mlrv, footView);
    }

    @Override
    public void onScrolled(int distanceX, int distanceY) {
//        if (is_sv_type_open) {
//            return;
//        }
//        if (mHeaderAndFooterRecyclerViewAdapter.getHeaderView().getBottom() <= goodsSelectTypeViewHeight) {
//            if (sv_type_top.getVisibility() == View.GONE) {
//                sv_type_top.setVisibility(View.VISIBLE);
//
//            }
//
//
//        } else {
//
//            if (sv_type_top.getVisibility() != View.GONE) {
//
//                sv_type_top.setVisibility(View.GONE);
//
//
//            }
//        }
    }

    private void next() {
        sv_type_top.postDelayed(new Runnable() {
            @Override
            public void run() {
                mlrv.refreshComplete();
                animationDrawable.stop();
                rl_load.setVisibility(View.INVISIBLE);
                rl_404.setVisibility(View.INVISIBLE);

            }
        }, 200);
    }

    @OnClick(R.id.rl_404)
    public void rl_404(View view) {
        rl_404.setVisibility(View.INVISIBLE);
        rl_load.setVisibility(View.VISIBLE);
        animationDrawable.start();
        p = 1;
        isRefresh = true;
        isLoadmore = false;
//        type = "2";
        fab.hide();
        initData();
    }
}
