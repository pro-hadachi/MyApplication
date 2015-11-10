package com.example.hadachi.myapplication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

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
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.content.Context;
import android.util.Base64;

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

    ///コールバッククラス
    public Object callbackTarget;

    ///コールバックターゲット
    public Method callbackAction;

    ///エラーコールバックターゲット
    public Method callbackErrorAction;

    ///コールバックリスナー
    public onRequestResultListener<Boolean> callbackListenerBoolean;
    ///コールバックリスナー
    public onRequestResultListener<HashMap<String,String>> callbackListenerItem;
    ///コールバックリスナー
    public onRequestResultListener<responseListObject> callbackListenerList;
    ///コールバックリスナー
    public onRequestResultListener<String> callbackListenerFile;
    ///コールバックリスナー
    public onRequestResultListener<JSONObject> callbackListenerJson;
    ///コールバックリスナー
    public onRequestResultListener<responseMapAndEntrylistObject> callbackListenerMapAndEntrylist;

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
    public boolean downloadWithUrl(String url, HashMap<String,String> params, OnRequestResultListener<String> listener, Context context, boolean isPost) {

        if(!Utils.isConnectNetwork()){
            return false;
        }

        //引数をListに直す
        mPostParams = new ArrayList<NameValuePair>();
        if (params != null && params.size()>0){
            for(Map.Entry<String, String> entry: params.entrySet()){
                String key = entry.getKey();
                String value = entry.getValue();
                Utils.tauLog("RequestDownloaderImpl.downloadWithUrl", "API param " + key + " = " + value);

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

//		synchronized(this){
        {
            mUrl=sUrl;
            mContents=aContents;
            cancel=false;
            //スレッド開始
            Thread t = new Thread(this);
            t.start();
        }
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
        }
        finally{
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

            BsmoLogUtil.d(BsmoInternalConstant.BUSHIMO_SDK_DEBUG_TAG, String.format("download thread start %s -> %s,%s",mUrl,saveFile,basicAuthUser));

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

            DefaultHttpClient client = new DefaultHttpClient(my_httpParams);
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
            String basicAuth = EnvUtils.createBasicAuthString(mUrl);
            Utils.tauLog("RequestDownloder", "murl = " + mUrl);
            if(basicAuth != null){
                method.setHeader("Authorization", basicAuth);
            }

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
                BsmoLogUtil.d(BsmoInternalConstant.BUSHIMO_SDK_DEBUG_TAG,String.format("HTTP STATUS  %d", mStatuscode));
                //リクエスト成功 200 OK and 201 CREATED
                if ( mStatuscode == HttpStatus.SC_OK  || mStatuscode == HttpStatus.SC_CREATED ||
                        mStatuscode == HttpStatus.SC_PARTIAL_CONTENT){
                    BsmoLogUtil.d(BsmoInternalConstant.BUSHIMO_SDK_DEBUG_TAG, "download OK");

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

                    //ファイルに保存？
                    if (saveFile!=null){

                        bis = createInputStream(result,gzip);
                        byte []buf=new byte[1024];
                        int len=0,total=0;
                        FileOutputStream fos = new FileOutputStream(saveFile,continueDownload);
                        OutputStream os=new BufferedOutputStream(fos);

                        do{
                            len = bis.read(buf);
                            if (cancel)
                                return false;
                            if (len>0){
                                total+=len;
                                downloadedLength=total;
                                os.write(buf, 0, len);
                                os.flush();

//								BsmoLogUtil.d("download",String.format("download %d", total));

                                //進捗報告？
                                if (downloadingNotifyKey!=null){

                                }
                            }

                            if (cancel)
                                return false;

                            if (debugWait>0){
                                synchronized (this) {
                                    try {
                                        this.wait(debugWait);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (cancel)
                                    return false;
                            }

                        }while(len>0);

                        os.close();

                        if (cancel)
                            return false;
                        //一括ならコールバックする
                        responseCallbackFile(saveFile,mContents);
                    }
                    else if (encode!=null){	//メモリ上でテキストとして処理

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
                            BsmoLogUtil.d(BsmoInternalConstant.BUSHIMO_SDK_DEBUG_TAG, String.format("size..%d", sb.length()));
                            if (cancel)
                                return false;
                        }while(len>0);

                        retStr=sb.toString();
                        BsmoLogUtil.d(BsmoInternalConstant.BUSHIMO_SDK_DEBUG_TAG,retStr);

                        if (cancel)
                            return false;
                        //コールバックする
                        responseCallbackString(retStr,mContents);
                    }
                    else{
                        bis = createInputStream(result,gzip);

                        byte []buf=new byte[1024];
                        int len=0,total=0;
                        ByteArrayOutputStream os=new ByteArrayOutputStream();
                        do{
                            len = bis.read(buf);
                            if (cancel)
                                return false;
                            if (len>0){
                                total+=len;
                                downloadedLength=total;
                                os.write(buf, 0, len);

                                //進捗報告？
                                if (downloadingNotifyKey!=null){

                                }
                            }
//							BsmoLogUtil.d("download file ", String.format("size..%d", total));
                            if (cancel)
                                return false;
                        }while(len>0);

                        byte[]receive = os.toByteArray();
                        os.close();

                        if (cancel)
                            return false;
                        //コールバックする
                        responseCallbackBytes(receive,mContents);
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

            BsmoLogUtil.d(BsmoInternalConstant.BUSHIMO_SDK_DEBUG_TAG, " download thread end. "+String.format("sts=%d length=%d", mStatuscode,retStr.length()));
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
     * @param res
     * @param contentsId
     */
    private void responseCallbackBytes(byte[] res, Object contentsId) {

        //TODO:整理予定

//TODO:とりあえず使わないので、作らない
//		//リスナーがあったらそっち
//		if (callbackListener!=null){
//			//後ほど調整
//			callbackListener.onResponse("200", res);
//		}
//		else
        if (callbackAction!=null){
            try {
                callbackAction.invoke(callbackTarget, new Object[]{res,contentsId});
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * レスポンスをコールバックする
     * @param res
     * @param contentsId
     */
    private void responseCallbackString(String retStr, Object contentsId) {

        BsmoLogUtil.d(BsmoInternalConstant.BUSHIMO_SDK_DEBUG_TAG,"downloader response dump "+retStr);

        //リスナーがあったらそっち
        if (callbackAction==null){
            //形式に応じてコールバックする
//			callbackListener.onResponse(mStatuscode, retStr);
            @SuppressWarnings("rawtypes")
            onRequestResultListener listener=null;
            try {
                if (callbackListenerItem!=null){
                    listener=callbackListenerItem;
                    HashMap<String, String> res = BsmoResponseParser.parseBodyItem(retStr,responsekey);
                    callbackListenerItem.onResponse(mStatuscode, res);
                }
                else if (callbackListenerList!=null){
                    listener=callbackListenerList;
                    BsmoResponseListObject res = BsmoResponseParser.parseBodyList(retStr,responsekey);
                    callbackListenerList.onResponse(mStatuscode, res);
                }
                else if (callbackListenerBoolean!=null){
                    listener=callbackListenerBoolean;
                    Boolean res = BsmoResponseParser.parseBodyBoolean(retStr,responsekey);
                    callbackListenerBoolean.onResponse(mStatuscode, res);
                }
                else if (callbackListenerJson!=null){
                    listener=callbackListenerJson;
                    JSONObject res = BsmoResponseParser.parseBodyJson(retStr);
                    callbackListenerJson.onResponse(mStatuscode, res);
                }
                else if (callbackListenerMapAndEntrylist!=null){
                    listener=callbackListenerMapAndEntrylist;
                    BsmoResponseMapAndEntrylistObject res = BsmoResponseParser.parseBodyMapAndEntrylist(retStr,responsekey);
                    callbackListenerMapAndEntrylist.onResponse(mStatuscode, res);
                }
//				else if (callbackListenerFile!=null){
//					HashMap<String, String> res = BsmoResponseParser.parseBodyItem(retStr);
//					callbackListenerItem.onResponse(mStatuscode, res);
//				}
            } catch (BsmoException e) {
                e.printStackTrace();

                //例外発生時はエラーレスポンス扱い
                listener.onError(e.code1, mStatuscode);
            }
        }
        else if (callbackAction!=null){
            try {
                callbackAction.invoke(callbackTarget, new Object[]{retStr,contentsId});
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * レスポンスをコールバックする
     * @param res
     * @param contentsId
     */
    private void responseCallbackFile(String aSaveFile, Object contentsId) {

        //リスナーがあったらそっち
        if (callbackListenerFile!=null){
            callbackListenerFile.onResponse(mStatuscode, aSaveFile);
        }
        else
        if (callbackAction!=null){
            try {
                callbackAction.invoke(callbackTarget, new Object[]{aSaveFile,contentsId});
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 通信等のエラー発生
     * @param i
     * @param e
     */
    protected void errorEvent(int statusCode, Exception e) {

        //キャンセル済みの場合はエラーを無視する
        if (cancel)
            return;

        //エラー情報
        String error = (statusCode!=0)?String.format("レスポンスコードエラー %d",statusCode)
//										:"その他のエラー";//:e.getLocalizedMessage();
                //TODO 本番はその他のエラーにする。
                :e.getLocalizedMessage();

        //リスナーがあったらそっち
        if (callbackListenerBoolean!=null){
            //TODO 後ほど調整
            callbackListenerBoolean.onError(-1,mStatuscode);
        }
        else if (callbackListenerItem!=null){
            //TODO 後ほど調整
            callbackListenerItem.onError(-1,mStatuscode);
        }
        else if (callbackListenerList!=null){
            //TODO 後ほど調整
            callbackListenerList.onError(-1,mStatuscode);
        }
        else if (callbackListenerFile!=null){
            //TODO 後ほど調整
            callbackListenerFile.onError(-1,mStatuscode);
        }
        else if (callbackListenerJson!=null){
            //TODO 後ほど調整
            callbackListenerJson.onError(-1,mStatuscode);
        }
        else if (callbackListenerMapAndEntrylist!=null){
            //TODO 後ほど調整
            callbackListenerMapAndEntrylist.onError(-1,mStatuscode);
        }
        else if (callbackErrorAction!=null && callbackTarget!=null){
            try {
                if (mContents==null){
                    BsmoLogUtil.d(BsmoInternalConstant.BUSHIMO_SDK_DEBUG_TAG,"mContents==null");
                }
                callbackErrorAction.invoke(callbackTarget, new Object[]{error,mContents});
            } catch (IllegalArgumentException e1) {
                e1.printStackTrace();
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }
        else{
            //domain名が出たりするので、詳細は表示しない
            //alertを出す
            BsmoHelpers.alert(null,BsmoHelpers.getCurrentActivity().getString(Bushimo.getSharedInstance().getResourceId(R.string.bsmo_error_communication,"string","bsmo_error_communication"), error));
//			MyHelpers.alert(null,String.format("通信エラーが発生しました。"));
        }

        //通知もしておく
        BsmoNotificationCenter.getSharedInstance().postNotification("downloadError",error);
        if (isNotificationFlg) {
            if (statusCode == 401) {
                // 認証エラーのときは認証を始めるので、通知する
                BsmoNotificationCenter.getSharedInstance().postNotification(
                        BsmoInternalConstant.Events.EVENT_AUTH_ERROR, error);

            } else if(statusCode==412 && Bushimo.getSharedInstance().isSDK()==false) {
                // バージョンチェックエラーの通知
                BsmoNotificationCenter.getSharedInstance().postNotification(
                        BsmoInternalConstant.Events.EVENT_PF_VERSION_ERROR, error);

            }
        }
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
        callbackTarget=null;
        callbackAction=null;

    }

    /**
     * 使用中かどうか
     * @return
     */
    public boolean isUsing(){

        return inThread;
    }
}
