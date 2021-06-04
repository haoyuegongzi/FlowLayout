package com.example.flowlayout;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class FlowGravityLayout extends ViewGroup {
    private static final String TAG = "FlowGravityLayout";
    private int mHorizontalSpacing = dp2px(16);//每个item水平方向的间距，默认16dp
    private int mVerticalSpacing = dp2px(8);//每个item垂直方向的间距，默认8dp
    private int selfWidth = 0;
    private int selfHeight = 0;
    private int gravityHorizontal = Gravity.LEFT;//默认左对齐
    private int gravityVertical = Gravity.TOP;//默认左对齐

    // measure时，记录所有的行，一行一行的存储，用于layout。需要注意的是：因为可能视图角度的父View会多次执行measure()方法，
    // 比如FrameLayout，他的onMeasure()方法里面多次调用了，会反复向里面添加对象；所以，为了保险起见，我们要记得在onMeasure()方法里面清空：clearMeasureParams()。
    private List<List<View>> allLines = new ArrayList<>();
    private List<Integer> lineHeights = new ArrayList<>();// 记录每一行的行高，用于layout

    //通过关键字new创建对象的时候，调用
    public FlowGravityLayout(Context context) {
        super(context);
    }

    // xml布局里面直接使用该View的时候，调用。Android对xml布局里面View对象的解析是发生在LayoutInflater.java里面，
    // 在这里面的inflate()方法通过反射的方式解析。其实我们看ViewGroup的构造方法源码可以知道：
    // 一个入参的构造方法内部调用的是两个入参的构造方法，两个入参的构造方法内部调用的是三个入参的构造方法，
    // 三个入参的构造方法内部调用的是四个入参的构造方法；
    // ViewGroup的四个入参的构造方法调用的是其父类View的四个入参的构造方法。因此，最终会调用到这个方法：
    // FlowGravityLayout(Context context, AttributeSet attrs, int defStyleAttr)
    public FlowGravityLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    //自定义style的时候。通过style="@style/*****"的形式在xml布局文件里面设置的时候调用。
    public FlowGravityLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //四个参数 自定义属性，是Android SDK 21新增的，通常在3个参数的构造方法中调用。
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FlowGravityLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public int getmHorizontalSpacing() {
        return mHorizontalSpacing;
    }

    public FlowGravityLayout setmHorizontalSpacing(int mHorizontalSpacing) {
        this.mHorizontalSpacing = dp2px(mHorizontalSpacing);
        Log.e(TAG, "setmHorizontalSpacing: mHorizontalSpacing===" + this.mHorizontalSpacing);
        return this;
    }

    public int getmVerticalSpacing() {
        return mVerticalSpacing;
    }

    public FlowGravityLayout setmVerticalSpacing(int mVerticalSpacing) {
        this.mVerticalSpacing = dp2px(mVerticalSpacing);
        return this;
    }

    public FlowGravityLayout setHorizontalGravity(int gravity) {
        this.gravityHorizontal = gravity;
        return this;
    }

    public FlowGravityLayout setVerticalGravity(int gravity) {
        this.gravityVertical = gravity;
        return this;
    }

    private void clearMeasureParams() {
        allLines.clear();
        lineHeights.clear();
    }

    // onMeasure()方法确定下来的宽高数据，不一定是最终的数据，有可能还会变化。因此在这里获取最终也是最可靠的结果
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        selfWidth = w;
        selfHeight = h;
    }

    // 测量，ViewGroup除了完成对自己的measure以外，还会对自己所有子View进行measure。
    // 另外，ViewGroup是一个抽象类，他没有重写View的OnMeasure方法，而是另外提供了measureChildren方法专门用来测量子View。
    // 几乎所有的ViewGroup在measure的时候，都是先measure内部的子View，最后measure自己本身。也就是确认了每一个子View的参数，
    // 才能确认自己的参数。因为递归调用，widthMeasureSpec、heightMeasureSpec这两个参数来自于他的父ViewGroup视图。
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        clearMeasureParams();//内存 抖动
        //可用宽度 = 父ViewGroup的宽度 - 父ViewGroup的paddingLeft - 父ViewGroup的paddingRight
        //          - 子View的marginLeft和 - 子View的marginRight和
        // 可用高度同理，要做计算。
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        List<View> lineViews = new ArrayList<>();//保存一行中的所有的view
        int lineWidthUsed = 0;//记录这行已经使用了多宽的size
        int lineHeight = 0;// 一行的行高

        selfWidth = MeasureSpec.getSize(widthMeasureSpec);// 父View的宽度，也是子View在水平方向上布局时可使用的最大宽度。
        selfHeight = MeasureSpec.getSize(heightMeasureSpec);// 父View的高度，也是子View在垂直方向上布局时可使用的最大高度。
        Log.e(TAG, "onMeasure: selfWidth===" + selfWidth);
        Log.e(TAG, "onMeasure: selfHeight===" + selfHeight);

        int parentNeededWidth = 0;// ⭐⭐⭐⭐⭐⭐measure过程中，子View要求的父ViewGroup的宽
        int parentNeededHeight = 0;// 🌙🌙🌙🌙🌙measure过程中，子View要求的父ViewGroup的高

        // 一、先度量孩子
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            // xml布局文件中的layout_width和layout_height中对应的值：“wrap_content”、“match_parent”、“xxxdp”/“xxxpx”
            // 在Java层面的表示形式就是LayoutParams；自定义View中有可能要自定义LayoutParams继承ViewGroup.LayoutParams
            // LayoutParams是ViewGroup中的一个static静态类型的class类。里面有几个关键的参数：
            // MATCH_PARENT = -1、WRAP_CONTENT = -2、width、height这些参数用于保存从xml布局文件中解析出来的View参数。
            // 解析过程是在setBaseAttributes(TypedArray a, int widthAttr, int heightAttr)方法里面：
            // protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            //     width = a.getLayoutDimension(widthAttr, "layout_width");
            //     height = a.getLayoutDimension(heightAttr, "layout_height");
            // }
            // 调用形式：在LayoutParams.java的构造方法里面调用：
            // public LayoutParams(Context c, AttributeSet attrs) {
            //     TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ViewGroup_Layout);
            //     setBaseAttributes(a,R.styleable.ViewGroup_Layout_layout_width,R.styleable.ViewGroup_Layout_layout_height);
            //     a.recycle();
            // }
            // public LayoutParams(int width, int height) {
            //     this.width = width;
            //     this.height = height;
            // }
            //public LayoutParams(LayoutParams source) {
            //     this.width = source.width;
            //     this.height = source.height;
            // }
            // 处于View.GONE状态的View因为不占用任何空间，因此可以不用考虑
            if (childView.getVisibility() == View.GONE) {
                continue;
            }
            // 调用childView.measure()，完成子View的测量
            LayoutParams childLP = childView.getLayoutParams();
            // 1、将layoutParams转变成为 measureSpec。
            // 可用宽度 = 父ViewGroup的宽度 - 父ViewGroup的paddingLeft - 父ViewGroup的paddingRight
            // “-”减操作发生在getChildMeasureSpec()内部。
            // 子View的尺寸 = 父容器的尺寸 - 父容器设置的padding。
            // 另外，子View的MeasueSpec是由父容器的MeasueSpec和自身的LayoutParams共同决定的，因此这里
            // 同时用到了自身的LayoutParams和父View容器的widthMeasureSpec/heightMeasureSpec
            // childLP.width和childLP.height对应xml布局文件中给FlowLayout设置的layout_width和layout_height
            int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                    paddingLeft + paddingRight, childLP.width);
            //可用高度 = 父ViewGroup的高度 - 父ViewGroup的paddingTop - 父ViewGroup的paddingBottom
            int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                    paddingTop + paddingBottom, childLP.height);
            // 计算下面的childWidthMeasureSpec、childHeightMeasureSpec两个参数以及后面的方法
            // setMeasuredDimension(realWidth, realHeight)中realWidth、realHeight两个参数是自定义View/ViewGroup的难点。
            // 2、对子View的的measure测量，如果子View是ViewGroup的话，那么其内部也会触发childView.measure()这个方法，
            // 以此形成递归调用。直到这些子View中不再包含ViewGroup，或者作为子View的ViewGroup内部不再有嵌套的子View，
            // 递归调用childView.measure()才结束。
            childView.measure(childWidthMeasureSpec, childHeightMeasureSpec);

            // 3、获取子view的度量宽高，将xml布局文件中的layout_width和layout_height中对应的值：
            // “wrap_content”、“match_parent”、“xxxdp”/“xxxpx”转换为系统可以识别的具体数值。
            int childMesauredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();

            //如果需要换行
            int hasUsedWidth = childMesauredWidth + lineWidthUsed + mHorizontalSpacing;
            Log.e(TAG, "onMeasure: \nhasUsedWidth===" + hasUsedWidth + "\nselfWidth===" + selfWidth);
            if (hasUsedWidth > selfWidth) {
                //一旦换行，我们就可以判断当前行需要的宽和高了，所以此时要记录下来
                allLines.add(lineViews);
                lineHeights.add(lineHeight);
                //🌙🌙🌙🌙🌙因为父ViewGroup的高度是可以扩展的（滑动），但是也不可能说无限制的提供
                // 毕竟在宽度方向因为可能放不下下一个子View，而换到下一行显示，导致这一行末尾有大量的空白空间。
                // 但是高度方向，是需要的时候才增加一行，不会在最后留置大量空白空间。
                // 因此需要用到的高度则需要根据子View实际用到的高度来计算父ViewGroup的高度
                parentNeededHeight = parentNeededHeight + lineHeight + mVerticalSpacing;
                // ⭐⭐⭐⭐⭐⭐其实这里，parentNeededWidth 子View要求的父ViewGroup的宽度可以不用管，直接使用父ViewGroup的宽度
                parentNeededWidth = Math.max(parentNeededWidth, lineWidthUsed + mHorizontalSpacing);

                lineViews = new ArrayList<>();
                lineWidthUsed = 0;
                lineHeight = 0;
            }
            // view 是分行layout的，所以要记录每一行有哪些view，这样可以方便layout布局
            lineViews.add(childView);
            //每行都会有自己的宽和高
            lineWidthUsed = lineWidthUsed + childMesauredWidth + mHorizontalSpacing;
            lineHeight = Math.max(lineHeight, childMeasuredHeight);

            //处理最后一行数据
            if (i == childCount - 1) {
                allLines.add(lineViews);
                lineHeights.add(lineHeight);
                //🌙🌙🌙🌙🌙因为父ViewGroup的高度是可以扩展的（滑动），但是也不可能说无限制的提供
                // 毕竟在宽度方向因为可能放不下下一个子View，而换到下一行显示，导致这一行末尾有大量的空白空间。
                // 但是高度方向，是需要的时候才增加一行，不会在最后留置大量空白空间。
                // 因此需要用到的高度则需要根据子View实际用到的高度来计算父ViewGroup的高度
                parentNeededHeight = parentNeededHeight + lineHeight + mVerticalSpacing;
                // ⭐⭐⭐⭐⭐⭐其实这里，parentNeededWidth 子View要求的父ViewGroup的宽度可以不用管，直接使用父ViewGroup的宽度
                parentNeededWidth = Math.max(parentNeededWidth, lineWidthUsed + mHorizontalSpacing);
            }
        }
        // 二、再度量自己,保存。根据子View的度量结果，来重新度量自己ViewGroup
        // 作为一个ViewGroup，它自己也是一个View,它的大小也需要根据它的父亲给它提供的宽高来度量
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        //自己本身最终的大小/宽高。
        int realWidth = (widthMode == MeasureSpec.EXACTLY) ? selfWidth : parentNeededWidth;
        int realHeight = (heightMode == MeasureSpec.EXACTLY) ? selfHeight : parentNeededHeight;
        // ⭐⭐⭐⭐⭐⭐其实这里，realWidth 最后要用到的宽度，可以直接使用父ViewGroup的宽度，因为一行的最大宽度
        // 不可能超过父ViewGroup的宽度；超过的部分是没法显示的。因此可以向下一行大妈那样直接使用 selfWidth 父ViewGroup的宽度。
        // ======================================================================================
        // 🌙🌙🌙🌙🌙因为父ViewGroup的高度是可以扩展的（滑动），但是也不可能说无限制的提供
        // 毕竟在宽度方向因为可能放不下下一个子View，而换到下一行显示，导致这一行末尾有大量的空白空间。
        // 但是高度方向，是需要的时候才增加一行，不会在最后留置大量空白空间。
        // 因此需要用到的高度则需要根据子View实际用到的高度来计算父ViewGroup的高度，也就是高度需要根据MeasureSpec--Mode来确定
        setMeasuredDimension(realWidth, realHeight);
        //setMeasuredDimension(selfWidth, realHeight);
    }

    // 布局，ViewGroup里面onLayout()方法是一个abstract的抽象方法，是自定义ViewGroup时必须实现并复写的方法，
    // 因为需要完成对他的内部的子View的布局。在自定义View时，没必要重写onLayout()方法，因为View本身就是一个独立的对象，没有子View。
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int lineCount = allLines.size();
        int curL = getPaddingLeft();
        int curT = getPaddingTop();
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        int totalViewHeight = 0;
        int gravityVerSpace = 0;

        Log.e(TAG, "onLayout: \nwidth===" + width + "\nheight===" + height);

        //获取到所有行的总高度
        for (int i = 0; i < lineHeights.size(); i++) {
            totalViewHeight = totalViewHeight + lineHeights.get(i);
            Log.e(TAG,  "第" + i + "行的高度 lineHeight===" + lineHeights.get(i));
        }
        if (lineHeights.size() > 1) {
            totalViewHeight = totalViewHeight + (lineHeights.size() - 1) * mVerticalSpacing;
        }
        Log.e(TAG, "所有行所占用的总高度 totalViewHeight===" + totalViewHeight);
        gravityVerSpace = (height - totalViewHeight) / 2;
        Log.e(TAG, "供View居中显示时上下均分的高度 gravityVerSpace====" + gravityVerSpace);

        //还需要考虑GRAVITY居中的问题。
        //循环布局每一行
        for (int i = 0; i < lineCount; i++) {
            List<View> lineViews = allLines.get(i);//获取每一行都有哪些View
            int lineHeight = lineHeights.get(i);//获取对应行的高度
            int size = lineViews.size();
            int gravityLayoutWidth = 0;
            ////////////////////////------------------居中布局的计算--------start////////////////////////
            if (gravityHorizontal == Gravity.CENTER || gravityHorizontal == Gravity.CENTER_HORIZONTAL) {
                //计算出每一行的所有的子View的所占用的宽度
                int totalViewWidth = 0;
                for (int k = 0; k < size; k++) {
                    View view = lineViews.get(k);
                    totalViewWidth = totalViewWidth + view.getMeasuredWidth();//计算本行各子View的总宽度
                }
                // 每行剩余宽度 = 父View的宽度 - 父View的左内距PaddingLeft -  父View的右内距PaddingRight
                //              - 每一行的所有的子View的宽度 - 每个子View之间的间距
                int gravityHoriSpace = width - getPaddingLeft() - getPaddingRight() - totalViewWidth;
                if (size > 1) {
                    gravityHoriSpace = gravityHoriSpace - (size - 1) * mHorizontalSpacing;
                }
                //然后在剩余的空间，左右各一半；
                gravityLayoutWidth = gravityHoriSpace / 2;
            }
            ////////////////////////------------------居中布局的计算--------end////////////////////////

            //每行的第一个View的布局起点
            int startLeft = curL + gravityLayoutWidth;
            int startTop = curT;
            if (gravityVertical == Gravity.CENTER || gravityVertical == Gravity.CENTER_VERTICAL) {
                startTop = startTop + gravityVerSpace;
            }

            Log.e(TAG, "每行的第一个子View的布局起点座标startLeft===" + startLeft);
            for (int j = 0; j < size; j++) {//循环布局第 j 行的每一个View
                View view = lineViews.get(j);

                int left = startLeft;//获取当前View的左上角顶点的水平座标
                int top = startTop;// curT startTop;//获取当前View的左上角顶点的垂直座标

                int right = left + view.getMeasuredWidth();
                int bottom = top + view.getMeasuredHeight();

                view.layout(left, top, right, bottom);
                //当前View的右边界 + 每个View之间的水平间距，得到的值就是下一个View的左边界起点。
                startLeft = right + mHorizontalSpacing;
            }
            curL = getPaddingLeft();//开启下一行的布局，因此水平方向的起点复位到初始值。
            //当前View的下边界 + 每个View之间的垂直间距，得到的值就是下一个View的上边界起点。
            curT = curT + lineHeight + mVerticalSpacing;
        }
    }

    // 绘制：在自定义View的时候，会直接用到；在自定义ViewGroup时，则可以不重写调用
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public static int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }
}
