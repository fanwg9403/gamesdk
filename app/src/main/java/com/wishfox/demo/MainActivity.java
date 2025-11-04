package com.wishfox.demo;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.wishfox.foxsdk.utils.FoxSdkLongingPayUtils;
import com.wishfox.foxsdk.utils.FoxSdkUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_pay).setOnClickListener(v -> new FoxSdkLongingPayUtils().loginPay(
                MainActivity.this,
                "1",
                "元宝",
                "0.01",
                "元宝",
                System.currentTimeMillis(),
                "284020251030173253530079483",
                (userId, token) -> {
                    ((TextView) findViewById(R.id.tv_info)).setText("userId: " + userId + "\ntoken: " + token);
                }));

        findViewById(R.id.tv_pay).postDelayed(FoxSdkUtils::hideFloatX, 1000);
    }
}