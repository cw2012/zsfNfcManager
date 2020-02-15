package com.zsf.ZsfNfcReader;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 使用nfc的页面的基类
 */
public class BaseNfcActivity extends AppCompatActivity {
    // 只在本界面判断是否开启nfc，开启则可进入其他界面，否则按钮无法跳转
    protected NfcAdapter mNfcAdapter;
    protected PendingIntent mPendingIntent;
    protected Tag mTag;
    protected int tagSize;// 容量 bytes
    protected int blocksCount;
    protected int sectorCount;
    protected ArrayList<String> simpleTechList=new ArrayList<>();
    protected List<String> techArrayList;
    /*
    nfca MIFARE Classic数据格式就是NfcA，MIFARE DESFire数据格式是IsoDep就是各种交通卡像武汉通，羊城通，深圳通，北京市政交通卡，长安通
    我们使用的二代身份证用的就是NfcB,
    Felica用的就是NfcF,
    德州仪器的VicinityCard卡用的是NfcV,
    而Android分享文件就是实用的Ndef格式传输数据。
     */

    @Override
    protected void onStart() {
        super.onStart();

        //初始化nfc
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(BaseNfcActivity.this, R.string.warn_nfc_unspported, Toast.LENGTH_LONG).show();
            finish();
            return;
        }else{
            if(!mNfcAdapter.isEnabled())
                Toast.makeText(this, R.string.warn_nfc_closed, Toast.LENGTH_LONG).show();
        }
        // launchMode设为singleTop，这样无论NFC标签靠近手机多少次，保障只有一个Activity实例。
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // 添加返回按钮
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mTag=intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);//获取到Tag标签对象
        if (mTag==null){
            Toast.makeText(this,R.string.warn_tag_unspported,Toast.LENGTH_SHORT).show();
            finish();
        }
        getTagInfo(intent);
    }

    // 当此方法回调时，则说明Activity已在前台可见，可与用户交互了（处于前面所说的Active/Running形态）
    //页面获取焦点。打开前台发布系统，使页面优于其它nfc处理.当检测到一个Tag标签就会执行mPendingItent
    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter!=null){
            mNfcAdapter.enableForegroundDispatch(this,mPendingIntent,null,null);
        } else {
            Toast.makeText(BaseNfcActivity.this, R.string.warn_nfc_unspported, Toast.LENGTH_LONG).show();
        }
    }

    //页面失去焦点,停止读取标签
    @Override
    protected void onPause() {
        super.onPause();
        if(mNfcAdapter!=null){
            mNfcAdapter.disableForegroundDispatch(this);//关闭前台发布系统
        }
    }

    // 转换成epc
    protected String toReversedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }
    // 返回epc
    protected String getEpc(){
        return toReversedHex(mTag.getId());
    }

    // 获得标签的信息
    protected void getTagInfo(Intent intent){
        String[] techList=mTag.getTechList();
        simpleTechList.clear();

        //读取标签的类型
        techArrayList = Arrays.asList(techList);
        for(String str:techArrayList){
            simpleTechList.add(str.substring(17));
        }
        // 非ndef类型的
        if (techArrayList.contains(MifareClassic.class.getName())) {
            String type = "未知";
            try {
                MifareClassic mifareTag;
                try {
                    mifareTag = MifareClassic.get(mTag);
                } catch (Exception e) {
                    // Fix for Sony Xperia Z3/Z5 phones
                    mTag = cleanupTag(mTag);
                    mifareTag = MifareClassic.get(mTag);
                }
                switch (mifareTag.getType()) {
                    case MifareClassic.TYPE_CLASSIC:
                        type = "Classic";
                        break;
                    case MifareClassic.TYPE_PLUS:
                        type = "Plus";
                        break;
                    case MifareClassic.TYPE_PRO:
                        type = "Pro";
                        break;
                }
                simpleTechList.add("Mifare Classic " + type);

                tagSize=mifareTag.getSize();
                blocksCount = mifareTag.getBlockCount();
                sectorCount = mifareTag.getSectorCount();
            } catch (Exception e) {
                Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        }

        if (techArrayList.contains(MifareUltralight.class.getName())) {
            MifareUltralight mifareUlTag = MifareUltralight.get(mTag);
            String type = "未知";
            switch (mifareUlTag.getType()) {
                case MifareUltralight.TYPE_ULTRALIGHT:
                    type = "Ultralight";
                    break;
                case MifareUltralight.TYPE_ULTRALIGHT_C:
                    type = "Ultralight C";
                    break;
            }
            simpleTechList.add(type);
        }
    }

    protected NdefMessage[] getNdefMessages(Intent intent){
        Parcelable[] rawMessage = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        // 判断是哪种类型的数据 默认为NDEF格式
        if (rawMessage != null) {
            NdefMessage[] ndefMessages = new NdefMessage[rawMessage.length];
            for (int i = 0; i < rawMessage.length; i++) {
                ndefMessages[i] = (NdefMessage) rawMessage[i];
            }
            return ndefMessages;
        }
        return null;
    }
    /**
     * 特殊标签的处理
     * @param oTag
     * @return
     */
    protected Tag cleanupTag(Tag oTag) {
        if (oTag == null)
            return null;

        String[] sTechList = oTag.getTechList();

        Parcel oParcel = Parcel.obtain();
        oTag.writeToParcel(oParcel, 0);
        oParcel.setDataPosition(0);

        int len = oParcel.readInt();
        byte[] id = null;
        if (len >= 0) {
            id = new byte[len];
            oParcel.readByteArray(id);
        }
        int[] oTechList = new int[oParcel.readInt()];
        oParcel.readIntArray(oTechList);
        Bundle[] oTechExtras = oParcel.createTypedArray(Bundle.CREATOR);
        int serviceHandle = oParcel.readInt();
        int isMock = oParcel.readInt();
        IBinder tagService;
        if (isMock == 0) {
            tagService = oParcel.readStrongBinder();
        } else {
            tagService = null;
        }
        oParcel.recycle();

        int nfca_idx = -1;
        int mc_idx = -1;
        short oSak = 0;
        short nSak = 0;

        for (int idx = 0; idx < sTechList.length; idx++) {
            if (sTechList[idx].equals(NfcA.class.getName())) {
                if (nfca_idx == -1) {
                    nfca_idx = idx;
                    if (oTechExtras[idx] != null && oTechExtras[idx].containsKey("sak")) {
                        oSak = oTechExtras[idx].getShort("sak");
                        nSak = oSak;
                    }
                } else {
                    if (oTechExtras[idx] != null && oTechExtras[idx].containsKey("sak")) {
                        nSak = (short) (nSak | oTechExtras[idx].getShort("sak"));
                    }
                }
            } else if (sTechList[idx].equals(MifareClassic.class.getName())) {
                mc_idx = idx;
            }
        }

        boolean modified = false;

        if (oSak != nSak) {
            oTechExtras[nfca_idx].putShort("sak", nSak);
            modified = true;
        }

        if (nfca_idx != -1 && mc_idx != -1 && oTechExtras[mc_idx] == null) {
            oTechExtras[mc_idx] = oTechExtras[nfca_idx];
            modified = true;
        }

        if (!modified) {
            return oTag;
        }

        Parcel nParcel = Parcel.obtain();
        nParcel.writeInt(id.length);
        nParcel.writeByteArray(id);
        nParcel.writeInt(oTechList.length);
        nParcel.writeIntArray(oTechList);
        nParcel.writeTypedArray(oTechExtras, 0);
        nParcel.writeInt(serviceHandle);
        nParcel.writeInt(isMock);
        if (isMock == 0) {
            nParcel.writeStrongBinder(tagService);
        }
        nParcel.setDataPosition(0);

        Tag nTag = Tag.CREATOR.createFromParcel(nParcel);

        nParcel.recycle();

        return nTag;
    }
}
