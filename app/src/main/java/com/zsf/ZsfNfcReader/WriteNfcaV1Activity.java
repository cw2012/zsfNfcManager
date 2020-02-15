package com.zsf.ZsfNfcReader;

import android.content.Intent;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.zsf.ZsfNfcReader.utils.MyURIEncoder;
import com.zsf.ZsfNfcReader.utils.UriRecord;

import java.io.IOException;
import java.util.Arrays;

/**
 * 理论上url控制在152个字符以内（1个汉字算3个字符;计算长度时不包括url的协议前缀）的情况下，数据是不会写到配置区的
 * 在这种情况下是安全的，如果超过了……没有做过多的校验，也没有测试过会出什么问题
 */
public class WriteNfcaV1Activity extends BaseNfcWriteActivity {

    private EditText urlText;
    // private EditText numText;
    private NfcA nfcA;
    private byte configPage = (byte)0x75; //一代和二代的配置页页码不同
    private int maxDataLength; // 可供写入的数据长度是不一样的,单位：字节
    private int mirror; // 是否启用加密
    private CheckBox checkboxEnableEnc; // 加密复选框
    private CheckBox checkBoxEnableAddEpc; // 是否追加epc
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_nfca_v1);

        confirmButton =findViewById(R.id.button_nfca_write);
        urlText = findViewById(R.id.editText_nfca_write__url);
        // numText = findViewById(R.id.editText_nfca_uid_length);
        checkboxEnableEnc = findViewById(R.id.checkBox_enable_enc);
        checkBoxEnableAddEpc = findViewById(R.id.checkBox_add_epc);
        view = View.inflate(getApplicationContext(), R.layout.dialog_write_url, null);
        adMessageView = findViewById(R.id.tv_dialog);
        super.onCreate(savedInstanceState);
    }
    // 清除输入法的键盘，显示弹窗，开户工作状态
    @Override
    protected void onStart(){
        super.onStart();
        confirmButton.setOnClickListener((v)->{
                    clearInput();
                    showAlertDialog();
                    working = true;
                    mirror = checkboxEnableEnc.isChecked()? 0x87:7;
                }
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        System.out.println(Arrays.toString(mTag.getTechList()));
        try{
            String url = urlText.getText().toString().trim().toLowerCase();
            if(url.length()>3){
                nfcA = NfcA.get(mTag);
                // 相当于js里的 encodeURI,允许使用中文
                url = MyURIEncoder.encode(url,"UTF-8");
                getVersion();
                if(url.length()>maxDataLength){
                    adMessageView.setText(R.string.warn_uri_length_expire);
                }else{
                    // 1. 寻卡
                    // wakeTag(nfcA);
                    nfcA.connect();
                    // 2.开始写码了
                    // 将url编码转换成 t + byte
                    byte [] urlInByte = urlToBytes(url);
                    writeUriIntoTag(urlInByte);
                }
            }else{
                adMessageView.setText(R.string.warn_uri_length_too_short);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(nfcA.isConnected()){
                try{
                    nfcA.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 126结尾是一代，26开头的是二代
     */
    private  void getVersion(){
        String uid = getEpc();
        if(uid.substring(0,2).equals("26")){
            configPage = (byte)0x75;
            maxDataLength = 110*4;
        }else if(uid.substring(4).equals("126")){
            configPage = (byte)0x2d;
            maxDataLength = 38*4;
        }
    }

    /**
     *
     * @param page 起始页码
     * @return 16字节。起始页后连续4页的数据，如page-0，则返回0-3页数据
     */
    private byte[] readDate(byte page){
        byte[] payLoad = {(byte)0x30, page };
        try{
            byte[] result = nfcA.transceive(payLoad);
            if(result.length>4){
                return result;
            }
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private void writeUriIntoTag(byte[] urlInByte){

        // 不满4字节的补0
        int length = urlInByte.length & 3;
        if(length!=0){
            length = 4 - length;
        }
        // 默认初始化为0,所以自动补0
        byte[] newArray = Arrays.copyOf(urlInByte, urlInByte.length + length);
        byte page = 0x03;
        for(byte i=0;i<newArray.length/4;i++){
            // 选择4字节，写完后再选后4字节
            byte[] dataPage = new byte[4];
            System.arraycopy(newArray,4*i,dataPage,0,4);
            if(!writeData(++page,dataPage)){
                System.out.println("停止在page" + page + Arrays.toString(dataPage));
                break;
            }
        }

        //传输完成后，写入配置信息
        length = urlInByte.length - 8;
        byte mirror_page = (byte)(length/4);
        mirror_page += 6;
        int mirror_byte = mirror + (length&0x03)*16;
        byte[] data = {(byte)mirror_byte, 0x00, mirror_page, (byte)0xff};
        if(writeData(configPage,data))
            adMessageView.setText(String.format(getResources().getString(R.string.info_success_colon), getEpc()));
    }

    // page从0x04开始，data有4个元素
    // USER Bytes 位于页地址 04h 到 73h
    // 不需要crc，transceive会自动计算
    private Boolean writeData(byte page,byte[] data){
        byte[] data2write = {(byte)0xA2, page, 0,0,0,0};
        System.arraycopy(data,0,data2write,2,4);
        //发送数据流
        try{
            // nfcA.connect();
            byte[] result = nfcA.transceive(data2write);
            return (result[0]==10);
        }catch (IOException e){
            e.printStackTrace();
            adMessageView.setText(R.string.warn_data_transfering_wrong);
            return false;
        }
    }
    // 将已编码的url转换成 t + byte
    private byte[] urlToBytes(String uri){
        // 字符串长度+1，再+秘文uid长度（默认为34） 不加密是0吗？
        int length = checkboxEnableEnc.isChecked()? 34:0;
        // 追加epc(uid)
        uri += checkBoxEnableAddEpc.isChecked()? getEpc():"";
        byte[] t = {0x03,0,(byte)0xD1,0x01,0,0x55,0};
        try{
            // t7 为协议编号
            for (Byte b : UriRecord.URI_PREFIX_MAP.keySet()) {
                String prefixStr = UriRecord.URI_PREFIX_MAP.get(b);
                if ("".equals(prefixStr))
                    continue;
                if (uri.startsWith(prefixStr)) {
                    t[6] = b;
                    // uri写入时不需要协议前缀(如http://)，前缀只是用户写入时自己方便看的
                    uri = uri.substring(prefixStr.length());
                    break;
                }
            }
            // 去除了uri前缀后再计算uri的长度
            // t2 = t5 + 4, 注意下标减了1
            t[4] = (byte)(uri.length() + 1 + length);
            t[1] = (byte)(t[4] + 4);
            // uri转换成16进制的byte 拼接 t1 + t2 + t3 + t4 + t5 + t6 + t7 + uri;
            byte[] byteUri = new byte[7+uri.length()];
            System.arraycopy(t,0,byteUri,0,7);
            System.arraycopy(uri.getBytes(),0,byteUri,7,uri.length());
            return byteUri;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

}
