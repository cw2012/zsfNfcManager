package com.zsf.ZsfNfcReader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BaseNfcWriteActivity extends BaseNfcActivity {
    protected Boolean working = false;
    protected AlertDialog alertDialog;
    protected View view=null;
    protected TextView adMessageView;
    protected Button confirmButton;

    // 弹窗部件的初始化
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = View.inflate(getApplicationContext(), R.layout.dialog_write_url, null);
        adMessageView = view.findViewById(R.id.tv_dialog);
    }

    // 清除输入法的键盘，显示弹窗，开户工作状态
    @Override
    protected void onStart(){
        super.onStart();
        confirmButton.setOnClickListener((v)->{
                    clearInput();
                    showAlertDialog();
                    working = true;
                }
        );
    }

    // 提示正在工作，提取标签信息
    @Override
    protected void onNewIntent(Intent intent) {
        if (!working) {
            Toast.makeText(this, R.string.info_state_tag_found_without_working, Toast.LENGTH_SHORT).show();
        } else {
            adMessageView.setText(R.string.info_state_writing_tag);
            // 父类会自动获取标签信息
            super.onNewIntent(intent);
        }
    }

    // 清除输入法的浮窗
    protected void clearInput(){
        InputMethodManager manager = ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE));
        if (manager != null)
            manager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(),
                    0);
    }
    // 显示弹窗
    protected void showAlertDialog(){
        if(alertDialog==null)
            alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_write_url_title)
                    .setIcon(R.mipmap.ic_launcher)
                    .setView(view)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_write_url_button_positive, (dialog, id) -> working = false)
                    .create();
        adMessageView.setText(R.string.info_state_writing_tag);
        alertDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
