package com.example.hadachi.myapplication;

import android.content.Context;
import android.util.Base64;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

/**
 * Created by h.adachi on 2015/11/09.
 */
public class RequestDownloaderImpl implements Runnable {

    /** リクエスト先URL */
    String mUrl;
    ///BASIC認証ユーザーパスワード
    public String basicAuthUser;
    ///BASIC認証ユーザーパスワード
    public String basicAuthPasswd;

    /** コールバックリスナー */
    private OnRequestResultListener<String, String> callbackListener;

    ///レスポンスのキー（テーブル名）
    public String responsekey;

    ///メモリ保持のときのエンコード。nullにするとBYTE列
    public String encode="UTF-8";

    ///識別子
    Object mContents;

    ///POSTフラグ
    boolean post;

    /** POSTするBODY*/
    protected ArrayList<NameValuePair> mPostParams;

    ///キャンセル済みフラグ
    boolean cancel;

    ///保存するファイル名
    public String saveFile;

    public boolean isNotify;
    public String downloadingNotifyKey;
    public boolean shortTimeout;

    ///ダウンロード済みバイト数　バイナリの場合のみ
    public long downloadedLength;

    ///受信したContent-Length
    public String contentLength;

    ///Content-Lengthを集めるかどうか
    public boolean collectContentLegnth;

//	///集めているontent-Length
//	private static MyListItem contentLengthMap=new MyListItem();

    ///ファイルが既に存在するときは、継続でダウンロードする。
    public boolean continueDownload;

    ///スレッド動作中フラグ
    boolean inThread;

    ///分割以外のダウンロードスレッド数
    public static int countDownloading;

    ///DEBUG用のWAIT(ms)
    int debugWait=0;

    ///長いタイムアウト
    public static int CONNECTION_TIMEOUT_LONG = 30000; // = 30 sec

    ///短いタイムアウト
    public static int CONNECTION_TIMEOUT_SHORT = 15000; // = 15 sec

    /** SSL Handshake エラー時の判定処理（無限ループ対策） */
    protected boolean isSSLHandshakeError = false;

    /** ステータスコード */
    int mStatuscode;
    /** 通知フラグ */
    public boolean isNotificationFlg = true;

    /**
     * 指定URLにPOSTする
     * @param url POST先URL
     * @param params パラメータ
     * @param listener リスナー
     * @param context コンテキスト
     * @param isPost リクエストの種別
     * @return 正しくネットワークに接続出来ていなかったらfalse
     */
    public boolean downloadWithUrl(String url, HashMap<String,String> params, OnRequestResultListener<String, String> listener, Context context, boolean isPost) {

        if(!Helper.isConnectNetwork()){
            return false;
        }

        //引数をListに直す
        mPostParams = new ArrayList<NameValuePair>();
        if (params != null && params.size()>0){
            for(Map.Entry<String, String> entry: params.entrySet()){
                String key = entry.getKey();
                String value = entry.getValue();
                Helper.vmappLog("RequestDownloaderImpl.downloadWithUrl", "API param " + key + " = " + value);

                mPostParams.add(new BasicNameValuePair(key,value));
            }
        }

        mUrl = url;
        callbackListener = listener;
        cancel=false;
        post=isPost;
        //スレッド開始
        Thread t = new Thread(this);
        t.start();

        return true;
    }

    /**
     * スレッド実行
     */
    @Override
    public void run() {

        inThread=true;

        try{
            //リクエストとダウンロード
            requestAndDownload(null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            countDownloading--;
            inThread=false;
        }
    }
    /**
     * １リクエスト・ダウンロード処理
     */
    protected boolean requestAndDownload(String rangeValue) throws IOException {

        HttpUriRequest method=null;
        try{

//            Helper.vmappLog(InternalConstant.BUSHIMO_SDK_DEBUG_TAG, String.format("download thread start %s -> %s,%s", mUrl, saveFile, basicAuthUser));

            int connection_Timeout = CONNECTION_TIMEOUT_LONG; // = 10 sec

            //短いタイムアウト指定なら１５秒程度にする
            if (shortTimeout)
                connection_Timeout=CONNECTION_TIMEOUT_SHORT;

            HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
            SchemeRegistry registry = new SchemeRegistry();
            SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
            socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
            registry.register(new Scheme("https", socketFactory, 443));
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

            HttpParams my_httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(my_httpParams, connection_Timeout);
            HttpConnectionParams.setSoTimeout(my_httpParams, connection_Timeout);

            if (!post){
                HttpGet get = new HttpGet( mUrl );
                method = get;
            }
            else{	//post

                HttpPost post = new HttpPost(mUrl);
                try {
                    post.setEntity(new UrlEncodedFormEntity(mPostParams, HTTP.UTF_8));
                } catch (UnsupportedEncodingException e){
                    //無視
                    e.printStackTrace();
                }
                method = post;
            }

            // 必要ならBasic認証を持たせる
            Helper.vmappLog("RequestDownloder", "murl = " + mUrl);
            if(basicAuthUser != null){
                method.setHeader("Authorization", getBasicAuthString());
            }

            DefaultHttpClient client = new DefaultHttpClient(my_httpParams);
            SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);

            DefaultHttpClient httpClient = new DefaultHttpClient(mgr, client.getParams());
            HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

            String retStr="";
            mStatuscode=-1;
            HttpResponse response;
            HttpEntity entity = null;
            InputStream bis = null;
            InputStream result = null;

            try {
                if (cancel)
                    return false;
                response = client.execute(method);
                mStatuscode = response.getStatusLine().getStatusCode();
                if (cancel)
                    return false;
//                Helper.vmappLog(InternalConstant.BUSHIMO_SDK_DEBUG_TAG,String.format("HTTP STATUS  %d", mStatuscode));
                //リクエスト成功 200 OK and 201 CREATED
                if ( mStatuscode == HttpStatus.SC_OK  || mStatuscode == HttpStatus.SC_CREATED ||
                        mStatuscode == HttpStatus.SC_PARTIAL_CONTENT){
//                    Helper.vmappLog(InternalConstant.BUSHIMO_SDK_DEBUG_TAG, "download OK");

                    //コンテンツサイズも保存
                    Header header = response.getFirstHeader("Content-Length");
                    if(header != null){
                        contentLength = header.getValue();
                    }

                    //パース処理
                    entity = response.getEntity();
                    result = entity.getContent();

                    //レスポンスの圧縮状態を取り出す
                    Header headerE = response.getFirstHeader("Content-Encoding");
                    boolean gzip = headerE!=null && "gzip".equals(headerE.getValue());

                    if (encode!=null){	//メモリ上でテキストとして処理

                        bis = createInputStream(result,gzip);
                        Reader reader = new InputStreamReader(bis, encode);
                        char []buf=new char[4096];
                        int len=0;
                        StringBuffer sb = new StringBuffer();
                        do{
                            len = reader.read(buf);
                            if (cancel)
                                return false;
                            if (len>0){

                                //バッファへ追加
                                sb.append(buf, 0, len);
                            }
//                            Helper.vmappLog(InternalConstant.BUSHIMO_SDK_DEBUG_TAG, String.format("size..%d", sb.length()));
                            if (cancel)
                                return false;
                        }while(len>0);

                        retStr=sb.toString();
//                        Helper.vmappLog(InternalConstant.BUSHIMO_SDK_DEBUG_TAG, retStr);

                        if (cancel)
                            return false;
                        //コールバックする
                        responseCallbackString(retStr);
                    }
                }
                else{	//error
                    errorEvent(mStatuscode,null);
                    return false;
                }
            } catch (ConnectTimeoutException e) {
                e.printStackTrace();
                errorEvent(ApiConst.STATUS_CD_CONNECTION_ERROR, null);
                return false;
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                errorEvent(ApiConst.STATUS_CD_CONNECTION_ERROR, null);
                return false;
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                errorEvent(ApiConst.STATUS_CD_CONNECTION_ERROR, null);
                return false;
            } catch (IOException e) {
                // SSL通信でのダウンロード件数が多い場合、ダウンロード処理の所々でSSL handshakeエラーが発生する。
                // SSL handshakeエラーだった場合、もう一度ダウンロード処理を実施してみる。
                boolean ret = false;
                if(e.getMessage().indexOf("SSL handshake") > -1 && !isSSLHandshakeError){
                    isSSLHandshakeError = true;
                    ret = requestAndDownload(rangeValue);
                }
                // エラーだった場合コールバック
                if(!ret){
                    e.printStackTrace();
                    errorEvent(0,e);
                }
                return ret;
            }finally{
                if(bis != null){
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(result != null){
                    try {
                        result.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (entity != null) {
                    try {
                        entity.consumeContent();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (client != null) {
                    client.getConnectionManager().shutdown();
                }
            }

//            Helper.vmappLog(InternalConstant.BUSHIMO_SDK_DEBUG_TAG, " download thread end. " + String.format("sts=%d length=%d", mStatuscode, retStr.length()));
            return true;
        }
        finally{
            if (method!=null){
                method.abort();
            }
        }
    }

    /**
     * 圧縮を考慮して、入力ストリームを作る
     * @param result
     * @param gzip
     * @return
     * @throws IOException
     */
    private InputStream createInputStream(InputStream result, boolean gzip) throws IOException {

        BufferedInputStream bis = new BufferedInputStream( result );
        //圧縮指定無し？
        if (!gzip){
            return bis;
        }
        //圧縮指定の場合はgzipにする
        return new GZIPInputStream(bis);
    }

    /**
     * レスポンスをコールバックする
     * @param retStr レスポンス
     */
    private void responseCallbackString(String retStr) {

//        Helper.vmappLog(InternalConstant.BUSHIMO_SDK_DEBUG_TAG, "downloader response dump " + retStr);

        //キャンセル済みの場合は無視する
        if (cancel){
            return;
        }

        callbackListener.onResponse(mStatuscode, retStr);
    }

    /**
     * 通信等のエラー発生
     * @param statusCode ステータスコード
     * @param e Exception
     */
    protected void errorEvent(int statusCode, Exception e) {

        Helper.vmappLog("RequestDownloaderImpl.errorEvent", "statusCode = " + statusCode + " e = " + e);
        //キャンセル済みの場合はエラーを無視する
        if (cancel){
            return;
        }

        callbackListener.onResponse(statusCode, null);
    }

    /**
     * BASIC認証用valueを返す
     * @return
     */
    protected String getBasicAuthString() {

        //:で区切る
        String str = String.format("%s:%s", basicAuthUser,basicAuthPasswd);

        //BASE64
        try {
//			String ret = Base64.encodeBytes(str.getBytes("UTF-8"), Base64.DONT_BREAK_LINES);
            String ret = Base64.encodeToString(str.getBytes("UTF-8"), Base64.NO_WRAP);
            return "BASIC "+ret;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ダウンロードをキャンセルする。
     */
    public void cancelDownload() {
        cancel=true;
    }

    /**
     * 使用中かどうか
     * @return
     */
    public boolean isUsing(){

        return inThread;
    }

    /**
     * 指定URLにPOSTする
     * @param url
     * @param params
     */
    public void postAndDownloadWithUrl(String url,String contents, HashMap<String,String> params) {

        boolean isSetToken = false;

        //引数をListに直す
        mPostParams = new ArrayList<NameValuePair>();
        if (params != null && params.size()>0){
            for(Map.Entry<String, String> entry: params.entrySet()){
                String key = entry.getKey();
                String value = entry.getValue();

                if(!isSetToken && key.equals("accessToken")){
                    isSetToken = true;
                }

                mPostParams.add(new BasicNameValuePair(key,value));
            }
        }

//        //アクセストークン追加
//        if(url.startsWith("https://") && !isSetToken){
//            String token = (Bushimo.getSharedInstance().getAccessToken()==null)?"":Bushimo.getSharedInstance().getAccessToken();
//
//            if (url.endsWith(BsmoInternalConstant.WAPI_URL_INSPECTION) && "".equals(token)) {
//                //※BSM_DEV-2539 inspection APIで、非会員の時はkeyを付与しないよう変更
//            } else {
//                mPostParams.add(new BasicNameValuePair("accessToken",token));
//            }
//        }

//        Helper.vmappLog(BsmoInternalConstant.BUSHIMO_SDK_DEBUG_TAG,"WAPI request. "+String.format("url=%s,params=%s",url,mPostParams.toString()));

        mUrl = url;
        mContents=contents;
        cancel=false;
        post=true;
        //スレッド開始
        Thread t = new Thread(this);
        t.start();
    }
}
