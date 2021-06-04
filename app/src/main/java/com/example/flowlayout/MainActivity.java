package com.example.flowlayout;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    List<String> hostoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //        FlowLayout flHostory = findViewById(R.id.flHostory);
        //        addHostoryData(flHostory);

        FlowGravityLayout fglHostory = findViewById(R.id.fglHostory);
        addHostoryData(fglHostory);
    }

    private void addHostoryData(FlowGravityLayout flHostory) {
        hostoryList.add("图书勋章日");
        hostoryList.add("奶粉1段");
        hostoryList.add("羊奶粉");
        hostoryList.add("稳压器 电容");
        hostoryList.add("儿童滑板车");
        hostoryList.add("抽真空收纳袋");
        hostoryList.add("儿童汽车可坐人");
        hostoryList.add("小度");
        hostoryList.add("洗衣机全自动");
        hostoryList.add("儿童洗衣机");
        hostoryList.add("水果味孕妇奶粉");

        flHostory.setmVerticalSpacing(10)
                .setmHorizontalSpacing(10)
                .setHorizontalGravity(Gravity.CENTER)
                .setVerticalGravity(Gravity.CENTER_VERTICAL);
        flHostory.setBackgroundResource(R.drawable.parent_view_shape);

        // 本例中，FlowLayout直接继承自ViewGroup，内部没有提供设置Margins参数的LayoutParams，因此没法直接实现；
        // 这里采用一种变通的方式来实现：给TextView套一层LinearLayout的壳，每个LinearLayout内部只放一个TextView，
        // 然后给每个LinearLayout设置LayoutParams。最后把LinearLayout壳addView到FlowLayout里面
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayoutParams.setMargins(FlowLayout.dp2px(0), FlowLayout.dp2px(0), FlowLayout.dp2px(0), FlowLayout.dp2px(0));

        for (int i = 0; i < hostoryList.size(); i++) {
            final TextView textView = new TextView(this);
            textView.setText(hostoryList.get(i));
            textView.setTextSize(14f);
            textView.setTextColor(Color.parseColor("#5899EA"));
            textView.setBackgroundResource(R.drawable.text_shape);
            textView.setPadding(FlowLayout.dp2px(15), FlowLayout.dp2px(15), FlowLayout.dp2px(15), FlowLayout.dp2px(15));
            textView.setGravity(Gravity.CENTER_HORIZONTAL);

            textView.setLayoutParams(linearLayoutParams);
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.addView(textView);
            flHostory.addView(linearLayout);
            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("TAGTAG", "onClick: " + textView.getText().toString());
                }
            });
        }
    }
}
