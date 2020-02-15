package com.zsf.ZsfNfcReader;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.support.v13.app.FragmentPagerAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        PagerAdapter x;
        FragmentPagerItemAdapter adapter = new FragmentPagerItemAdapter(
                getSupportFragmentManager(), FragmentPagerItems.with(this)
                .add(R.string.titleA, PageFragment.class)
                .add(R.string.titleB, PageFragment.class)
                .create());
        Button button_write_URL;
        Button button_read;
        Button button_nfca;

        // 按钮的跳转
        button_write_URL =  findViewById(R.id.button_write_URL);
        button_write_URL.setOnClickListener((v)->{
                Intent intent = new Intent();
                intent.setClass(com.zsf.ZsfNfcReader.MainActivity.this, WriteNdefActivity.class);
                startActivity(intent);
        });
        button_read = findViewById(R.id.button_read);
        button_read.setOnClickListener((v)->{
                Intent intent = new Intent();
                intent.setClass(com.zsf.ZsfNfcReader.MainActivity.this, ReadActivity.class);
                startActivity(intent);
        });
        button_nfca = findViewById(R.id.button_nfcav1);
        button_nfca.setOnClickListener((v)->{
            Intent intent = new Intent();
            intent.setClass(com.zsf.ZsfNfcReader.MainActivity.this, WriteNfcaV1Activity.class);
            startActivity(intent);
        });
    }

}
