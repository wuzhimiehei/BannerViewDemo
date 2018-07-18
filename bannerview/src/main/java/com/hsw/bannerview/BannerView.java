package com.hsw.bannerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hsw on 2017/8/2.
 */
public class BannerView extends RelativeLayout {

    private int mLayoutResId = R.layout.banner;
    private boolean isAutoPlay;
    private int mIndicatorMargin;
    private int mIndicatorWidth;
    private int mIndicatorHeight;
    private int mIndicatorSelectedResId;
    private int mIndicatorUnselectedResId;
    private Context context;
    private List imageUrls;
    private List<ImageView> imageViews = new ArrayList<>();
    private List<View> indicatorViews = new ArrayList<>();
    private int count;
    private int currentItem;
    private int lastPosition = 1;
    private int delayTime;

    private ImageLoaderInterface imageLoader;

    private BannerAdapter adapter;
    private BannerScroller mScroller;

    private BannerViewPager bannerViewPager;
    private LinearLayout indicatorsLayout;

    private WeakHandler handler = new WeakHandler();

    public BannerView(Context context) {
        this(context, null);
    }

    public BannerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Banner);
        mIndicatorWidth = typedArray.getDimensionPixelSize(R.styleable.Banner_indicator_width, 0);
        mIndicatorHeight = typedArray.getDimensionPixelSize(R.styleable.Banner_indicator_height, 0);
        mIndicatorMargin = typedArray.getDimensionPixelSize(R.styleable.Banner_indicator_margin, 0);
        mIndicatorSelectedResId = typedArray.getResourceId(R.styleable.Banner_indicator_drawable_selected, 0);
        mIndicatorUnselectedResId = typedArray.getResourceId(R.styleable.Banner_indicator_drawable_unselected, 0);
        delayTime = typedArray.getInt(R.styleable.Banner_delay_time, 3000);
        isAutoPlay = typedArray.getBoolean(R.styleable.Banner_is_auto_play, true);
        typedArray.recycle();

        View view = LayoutInflater.from(context).inflate(mLayoutResId, this, true);
        bannerViewPager = view.findViewById(R.id.bannerViewPager);
        indicatorsLayout = view.findViewById(R.id.indicatorsLayout);

        initViewPagerScroll();
    }

    private void initViewPagerScroll() {
        try {
            Field mField = ViewPager.class.getDeclaredField("mScroller");
            mField.setAccessible(true);
            mScroller = new BannerScroller(bannerViewPager.getContext());
            mScroller.setDuration(1000);
            mField.set(bannerViewPager, mScroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setImageUrls(List imageUrls) {
        this.imageUrls = imageUrls;
        this.count = imageUrls.size();
    }

    public void setImageLoader(ImageLoaderInterface imageLoader) {
        this.imageLoader = imageLoader;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isAutoPlay) {
            int action = ev.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                    || action == MotionEvent.ACTION_OUTSIDE) {
                startAutoPlay();
            } else if (action == MotionEvent.ACTION_DOWN) {
                stopAutoPlay();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public void create() {
        currentItem = 1;
        initImageViews();
        initIndicatorViews();
        adapter = new BannerAdapter(imageViews);
        bannerViewPager.setAdapter(adapter);
        bannerViewPager.addOnPageChangeListener(bannerOnPageChangeListener);
//        bannerViewPager.setCurrentItem(1);
        if (isAutoPlay)
            startAutoPlay();
    }

    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            if (count > 1 && isAutoPlay) {
                currentItem = currentItem % (count + 1) + 1;
//                Log.i(tag, "curr:" + currentItem + " count:" + count);
                if (currentItem == 1) {
                    bannerViewPager.setCurrentItem(currentItem, false);
                    handler.post(task);
                } else {
                    bannerViewPager.setCurrentItem(currentItem);
                    handler.postDelayed(task, delayTime);
                }
            }
        }
    };


    private ViewPager.OnPageChangeListener bannerOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            currentItem = position;
            indicatorViews.get((lastPosition - 1 + count) % count).setBackgroundResource(mIndicatorUnselectedResId);
            indicatorViews.get((position - 1 + count) % count).setBackgroundResource(mIndicatorSelectedResId);
            lastPosition = position;
            if (position == 0) position = count;
            if (position > count) position = 1;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (state) {
                case 0://No operation
                    if (currentItem == 0) {
                        bannerViewPager.setCurrentItem(count, false);
                    } else if (currentItem == count + 1) {
                        bannerViewPager.setCurrentItem(1, false);
                    }
                    break;
                case 1://start Sliding
                    if (currentItem == count + 1) {
                        bannerViewPager.setCurrentItem(1, false);
                    } else if (currentItem == 0) {
                        bannerViewPager.setCurrentItem(count, false);
                    }
                    break;
                case 2://end Sliding
                    break;
            }
        }
    };

    public void update(List<?> imageUrls) {
        this.imageUrls.clear();
        this.imageViews.clear();
        this.indicatorViews.clear();
        this.imageUrls.addAll(imageUrls);
        this.count = this.imageUrls.size();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void initImageViews() {
        if (imageUrls == null || imageUrls.size() == 0) {
            return;
//            throw new NullPointerException("imageUrls cannot be empty");
        }
        for (int i = 0; i <= count + 1; i++) {
            ImageView imageView = null;
//            if (imageLoader != null) {
//                imageView = imageLoader.createImageView(context);
//            }
            if (imageView == null) {
                imageView = new ImageView(context);
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            }
            Object url = null;
//            url = imageUrls.get(i);
            if (i == 0) {
                url = imageUrls.get(count - 1);
            } else if (i == count + 1) {
                url = imageUrls.get(0);
            } else {
                url = imageUrls.get(i - 1);
            }
            imageViews.add(imageView);
            if (imageLoader != null) {
                imageLoader.displayImage(context, url, imageView);
            } else {
                throw new NullPointerException("Please set images loader.");
            }
        }

    }

    private void initIndicatorViews() {
        indicatorViews.clear();
        indicatorsLayout.removeAllViews();

        for (int i = 0; i < count; i++) {
            View view = new View(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mIndicatorWidth, mIndicatorHeight);
            params.leftMargin = mIndicatorMargin;
            params.rightMargin = mIndicatorMargin;
            if (i == 0) {
                view.setBackgroundResource(mIndicatorSelectedResId);
            } else {
                view.setBackgroundResource(mIndicatorUnselectedResId);
            }
            view.setLayoutParams(params);
            indicatorViews.add(view);
            indicatorsLayout.addView(view);
        }
    }

    /**
     * 返回真实的位置
     *
     * @param position
     * @return 下标从0开始
     */
    private int toRealPosition(int position) {
        int realPosition = (position - 1) % count;
        if (realPosition < 0)
            realPosition += count;
        return realPosition;
    }

    public void startAutoPlay() {
        handler.removeCallbacks(task);
        handler.postDelayed(task, delayTime);
    }

    public void stopAutoPlay() {
        handler.removeCallbacks(task);
    }

    public void releaseBanner() {
        handler.removeCallbacksAndMessages(null);
    }
}
