package com.zsf.ZsfNfcReader;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.zsf.ZsfNfcReader.utils.UriRecord;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Locale;

public class WriteNdefActivity extends BaseNfcWriteActivity {

    private CheckBox checkBox;
    private EditText editText_url; // 准备写入的uri
    private TextView editText_text; // 准备写入的文本


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_url);
        checkBox = findViewById(R.id.checkBox);
        editText_url = findViewById(R.id.editText_url);
        confirmButton = findViewById(R.id.button_continue_write);
        editText_text = findViewById(R.id.editText_text);
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        if(working){
            // 写码操作
            String url = editText_url.getText().toString().trim();
            String text = editText_text.getText().toString().trim();
            if(url.length()>1 || text.length()>0){
                try{
                    if(simpleTechList.contains("Ndef")){
                        Ndef ndef = Ndef.get(mTag);
                        ndef.connect();
                        ndef.writeNdefMessage(buildNdefMessage(url,text));
                        ndef.close();
                        adMessageView.setText(String.format(getResources().getString(R.string.info_success_colon), getEpc()));
                    }else if(simpleTechList.contains("NdefFormatable")){
                        NdefFormatable ndefFormatable = NdefFormatable.get(mTag);
                        ndefFormatable.connect();
                        ndefFormatable.format(buildNdefMessage(url,text));
                        ndefFormatable.close();
                        adMessageView.setText(String.format(getResources().getString(R.string.info_success_colon), getEpc()));
                    }else{
                        adMessageView.setText(R.string.warn_tag_unspported + simpleTechList.toString());
                    }
                }catch (Exception e){
                    Toast.makeText(WriteNdefActivity.this, e.getMessage(),Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(WriteNdefActivity.this, R.string.warn_uri_length_too_short, Toast.LENGTH_SHORT).show();
            }
        }
    }


    // 建立一个NdefMessage
    private NdefMessage buildNdefMessage(String url,String text){
        ArrayList<NdefRecord> ndefRecords = new ArrayList<>();
        String[] strings;
        if(url.length()>1){
            strings = url.split("\n");
            for(String str: strings){
                if(checkBox.isChecked()){
                    str += getEpc();
                }
                ndefRecords.add(createUriRecord(str));
            }

        }
        if(text.length()>0){
            strings = text.split("\n");
            for(String str:strings){
                ndefRecords.add(createTextRecord(str));
            }
        }
        return new NdefMessage(ndefRecords.toArray(new NdefRecord[0]));
    }
    /**
     * 创建一个文本数据记录
     * @param text 需要写入的文本
     * @return NdefRecord
     */
    protected NdefRecord createTextRecord(String text){
        byte[] langBytes = Locale.CHINA.getLanguage().getBytes(Charset.forName("US-ASCII"));
        Charset utfEncoding = Charset.forName("UTF-8");
        //将文本转换为UTF-8格式
        byte[] textBytes = text.getBytes(utfEncoding);
        //设置状态字节编码最高位数为0
        int utfBit = 0;
        //定义状态字节
        char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        //设置第一个状态字节，先将状态码转换成字节
        data[0] = (byte) status;
        //设置语言编码，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1到langBytes.length的位置
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        //设置文本字节，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1 + langBytes.length
        //到textBytes.length的位置
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        //通过字节传入NdefRecord对象
        //NdefRecord.RTD_TEXT：传入类型 读写
        NdefRecord ndefRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, new byte[0], data);
        return ndefRecord;
    }

    /*
     * 封装uri格式数据
     * 现部分组成: 第一个字节是uri协议映射值
     * uri协议映射值,如:0x01 表示uri以 http://www.开头.
     * uri的内容 如:www.g.cn
     */
    public NdefRecord createUriRecord(String uriStr) {
        /*
         * 封装uri格式数据 第1步, 根据uriStr中的值找到构造第一个字节的内容:uri协议映射值
         */
        byte prefix = 0;
        for (Byte b : UriRecord.URI_PREFIX_MAP.keySet()) {
            String prefixStr = UriRecord.URI_PREFIX_MAP.get(b).toLowerCase();
            if ("".equals(prefixStr))
                continue;
            if (uriStr.toLowerCase().startsWith(prefixStr)) {
                prefix = b;
                uriStr = uriStr.substring(prefixStr.length());
                break;
            }

        }
        /*
         * 封装uri格式数据 第2步, uri的内容就是本函数的参数 uriStr
         */
        /*
         * 封装uri格式数据 第3步, 申请分配空间
         */
        byte[] data = new byte[1 + uriStr.length()];

        /*
         * 封装uri格式数据 第4步, 把uri头的映射值和uri内容写到刚申请的空间中
         */
        data[0] = prefix;
        System.arraycopy(uriStr.getBytes(), 0, data, 1, uriStr.length());

        /*
         * 封装uri格式数据 第5步, 根据uri构造一个NdefRecord
         */
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_URI, new byte[0], data);
        return record;
    }


}
