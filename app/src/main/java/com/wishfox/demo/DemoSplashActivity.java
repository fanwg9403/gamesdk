package com.wishfox.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Demo 启动页，用于观察 SDK 初始化后的悬浮球是否能跨 Activity 保持显示。 */
public class DemoSplashActivity extends Activity {

    public static final String EXTRA_ENTERED_FROM_SPLASH =
            "com.wishfox.demo.ENTERED_FROM_SPLASH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int padding = Math.round(32 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("WishFox SDK Demo 启动页");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("请先观察启动页悬浮球，再进入 Demo 首页");
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = Math.round(16 * getResources().getDisplayMetrics().density);
        root.addView(hint, hintParams);

        Button enter = new Button(this);
        enter.setText("进入 Demo");
        enter.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(EXTRA_ENTERED_FROM_SPLASH, true);
            startActivity(intent);
            finish();
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = Math.round(24 * getResources().getDisplayMetrics().density);
        root.addView(enter, buttonParams);

        setContentView(root);
    }
}
