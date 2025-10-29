package com.wishfox.demo;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.wishfox.foxsdk.utils.FoxSdkLongingPayUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_pay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FoxSdkLongingPayUtils().loginPay(MainActivity.this, "1", "10元档充值", "10", "10元", 1761730488, "BR202510211421531814414782437");
            }
        });
    }
}