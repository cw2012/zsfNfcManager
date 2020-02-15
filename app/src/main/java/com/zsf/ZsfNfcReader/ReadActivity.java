package com.zsf.ZsfNfcReader;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.NfcA;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.zsf.ZsfNfcReader.utils.TextRecord;
import com.zsf.ZsfNfcReader.utils.UriRecord;

import java.io.IOException;

public class ReadActivity extends BaseNfcActivity {

    // 开启nfc的功能由父类BaseNfcActivity完成了
    private TextView mText;

    @Override
    protected void onStart() {
        super.onStart();
        setContentView(R.layout.activity_read);

        mText = findViewById(R.id.editText3);
        Button buttonClear = findViewById(R.id.button_clear);

        // 清空按钮
        buttonClear.setOnClickListener((v)-> mText.setText(""));

    }



    //初次判断是什么类型的NFC卡
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String str= getResources().getString(R.string.info_spported_technology_in_tag) + "\n";
        str += simpleTechList.toString()+"\n";
        str += "EPC:\n"+ getEpc();
        if(simpleTechList.contains("Ndef")){
            NdefMessage[] ndefMessages= getNdefMessages(intent);
            if(ndefMessages !=null){
                for(NdefMessage msg: ndefMessages){
                    NdefRecord[] records = msg.getRecords();
                    for(NdefRecord rec:records){
                        if(UriRecord.isUri(rec)){
                            str += "\nURI:\n" + UriRecord.parse(rec).getUri().toString();
                        }
                        if(TextRecord.isText(rec)){
                            str += "\n"+getResources().getString(R.string.info_text_data_in_tag)+"\n" + TextRecord.parse(rec).getText();
                        }
                    }
                }
            }
        }else if(simpleTechList.contains("NfcA")){
            NfcA nfcA = NfcA.get(mTag);
            try{
                nfcA.connect();
                String atqa="";
                for(byte tmpByte:nfcA.getAtqa())
                {
                    atqa+=tmpByte;
                }
                str += "\nATQA:" + atqa+";"+toReversedHex(nfcA.getAtqa());
                str += "\nSAK:" + nfcA.getSak();
                str += "\n"+getResources().getString(R.string.info_max_transceive_length_of_nfca)+ "\n"+ nfcA.getMaxTransceiveLength();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        mText.setText(str);
    }

    // 返回按钮
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
