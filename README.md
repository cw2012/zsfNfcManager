# zsfNfcManager
- 对nfc标签（包括ndef, ndefformatter, nfca格式）进行读写，其中nfca仅支持[珠海晶通](http://www.crystonetech.com/ "晶通官网")的动态id标签（[JTN31DS28](http://www.crystonetech.com/index.php/Product/product_info/goods_id/33.html)）
- 安装时检测是否支持nfc，不支持将无法安装
- 一般的`intent-filter`会推荐使用3个，这里只使用了`android.nfc.action.TAG_DISCOVERED`这一个，说实话，用三个也没见有什么好处
