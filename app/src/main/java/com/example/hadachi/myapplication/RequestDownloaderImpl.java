package com.example.hadachi.myapplication;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;

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
    public BsmoOnRequestResultListener<Boolean> callbackListenerBoolean;
    ///コールバックリスナー
    public BsmoOnRequestResultListener<HashMap<String,String>> callbackListenerItem;
    ///コールバックリスナー
    public BsmoOnRequestResultListener<BsmoResponseListObject> callbackListenerList;
    ///コールバックリスナー
    public BsmoOnRequestResultListener<String> callbackListenerFile;
    ///コールバックリスナー
    public BsmoOnRequestResultListener<JSONObject> callbackListenerJson;
    ///コールバックリスナー
    public BsmoOnRequestResultListener<BsmoResponseMapAndEntrylistObject> callbackListenerMapAndEntrylist;

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
     * ダウンロードを開始する
     */
    public void startDownloadWithUrl(String sUrl,Object aContents){

        //分割ダウンロード指定じゃない？
        if (saveFile==null){
            //同時処理数をカウント
            countDownloading++;
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

    @Override
    public void run() {

    }
}
