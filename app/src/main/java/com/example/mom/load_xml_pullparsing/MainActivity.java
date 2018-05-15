package com.example.mom.load_xml_pullparsing;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    ListView listView;
    private final String FILE_URL = "http://opendata.epa.gov.tw/ws/Data/AQX/?format=xml";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);
        //XML直接網路下載,網路操作一定要在新的執行序
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //XML讀取完 parse() 會得到一個ArrayList
                    final ArrayList<HashMap<String, Object>> arrayList = parse(FILE_URL);
                    //ListView操作需要在原來的UIThread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SimpleAdapter adapter = new SimpleAdapter(
                                    getApplicationContext() , arrayList , android.R.layout.simple_list_item_2 ,
                                    new String[] {"SiteName" , "CO"} , new int[] { android.R.id.text1 , android.R.id.text2});
                            listView.setAdapter(adapter);
                        }
                    });
                }
                catch (URISyntaxException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /*
    //取得url內的資料
    public InputStream getUrlData(String url) throws URISyntaxException , IOException{
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet method = new HttpGet(new URI(url));
        HttpResponse response = client.execute(method);
        return response.getEntity().getContent();
    }*/

    //解析空氣品質的OPEN DATA返回一個ArrayList集合
    public ArrayList< HashMap<String ,Object>> parse(String FILE_URL) throws URISyntaxException{
        String tagName = null; //標籤名稱
        ArrayList< HashMap<String , Object>> arrayList = new ArrayList<>();
        HashMap< String ,Object> hashMap = new HashMap<>();
        //記錄出現次數
        int findCount = 0;
        try {
            //設定URL
            URL url =  new URL(FILE_URL);
            //定義Factory XmlPullParserFactory
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            //定義解析器 XmlPullParser
            XmlPullParser parser = factory.newPullParser();
            //獲取xml輸入數據
            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( url.openStream()));
            parser.setInput( bufferedReader );
            //開始解析事件
            int eventType = parser.getEventType();
            //處理事件 , 不碰到文檔結束就一直處理
            while (eventType != XmlPullParser.END_DOCUMENT){
                //XmlPullParser預先定義了一堆靜態常量 , 所以這裡可用switch
                switch (eventType){
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        //給當前標籤起個名字
                        tagName = parser.getName();
                        //看到感興趣的標籤個計數
                        if( findCount ==0 && tagName.equals("Data")){
                            findCount++;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        if( tagName.equals("SiteName") && hashMap.containsKey("SiteName") ==false){
                            hashMap.put("SiteName" , parser.getText());
                            Log.d("HashMap" , "SiteName"+"/"+parser.getText());
                        }
                        else if (tagName.equals("CO") && hashMap.containsKey("CO") ==false){
                            hashMap.put("CO" , parser.getText());
                            Log.d("HashMap" , "CO"+"/"+parser.getText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        //嘗試取得當前標籤名稱 , 若是Data才可以增加到arrayList , 並且重置
                        String trytagName = parser.getName();
                        if( trytagName.equals("Data")){
                            tagName = parser.getName();
                            findCount =0 ;
                            arrayList.add(hashMap);
                            hashMap = new HashMap<String , Object>();
                        }
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        break;
                }
                //一定要用next方法處理下一個事件 , 忘了就成無窮迴圈

                eventType = parser.next();
            }
            return arrayList;
        }
        catch (XmlPullParserException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return arrayList;
    }
}
