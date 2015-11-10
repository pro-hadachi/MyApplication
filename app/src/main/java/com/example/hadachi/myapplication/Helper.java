package com.example.hadachi.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;



/**
 * Created by h.adachi on 2015/11/06.
 */
public class Helper {
    /** UIスレッドハンドラー */
    public static Handler uiHandler = new Handler();
    /** コンテキスト */
    public static Context context;
    /** 現在のアクティビティ */
    private static Activity mActivity;
    /** プログレスダイアログ */
    public static ProgressDialog progressDialog;

    /**
     * JSONArray形式のものをパース
     * @param body JSONArray形式の文字列
     * @return パースされたHashMap<String,String>のArray
     */
    public static ArrayList<HashMap<String, String>> parseBodyJsonArray(String body) {
        ArrayList<HashMap<String, String>> res = new ArrayList<HashMap<String, String>>();

        try {
            JSONArray jsonArray = new JSONArray(body);

            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject jobj = jsonArray.getJSONObject(i);
                    HashMap<String, String> s = Helper.json2Item(jobj);
                    res.add(s);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return res;
    }

    /**
     * JSON BODYをパースして、HashMap<String,String>を返す。
     * @param body 対象文字列
     * @param key キー
     * @return HashMap<String,String> パース後のHashMap
     */
    public static HashMap<String, String> parseBodyItem(String body, String key) {

        if(body == null){
            return null;
        }

        try {
            JSONObject json = new JSONObject(body);

            //キー指定のときはキーに限定する
            if (key != null) {
                json = json.getJSONObject(key);
            }

            //HashMapに変換
            HashMap<String, String> item = json2Item(json);

            return item;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * アイテムをJSONに変換する
     * @param jitem アイテム
     * @return JSON 返還後のJSONアイテム
     * @throws JSONException
     */
    public static HashMap<String, String> json2Item(JSONObject jitem) throws JSONException {
        HashMap<String, String> item = new HashMap<String, String>();
        @SuppressWarnings("rawtypes")
        Iterator it = jitem.keys();
        while (it.hasNext()) {
            String n = (String) it.next();

            if(jitem.isNull(n)){
                item.put(n, null);
            }else{
                item.put(n, jitem.getString(n));
            }
        }
        return item;
    }

    /**
     * バージョンを満たしているかチェック
     * x.x.xで渡ってくる想定
     * @param mustVersion 必要バージョン
     * @return バージョンを満たしている→true 満たしていない→false
     */
    public static boolean isFulfillVersion(String mustVersion) {
        //現行バージョン
        PackageManager packageManager = Helper.context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = packageManager.getPackageInfo(
                    Helper.context.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        String currentVersion = packageInfo.versionName;

        mustVersion = mustVersion.replace(".", "");
        currentVersion = currentVersion.replace(".", "");

        vmappLog("Utils.isFulfillVersion", "mustVersion = " + mustVersion + ", currentVersion = " + currentVersion);
        return Integer.valueOf(mustVersion) <= Integer.valueOf(currentVersion);
    }

    /**
     * ネットワークの状態をチェックする
     * @return true オンライン:false オフライン
     */
    public static boolean isConnectNetwork(){
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if( ni == null ){
//            vmappLog("Utils.isConnectNetwork", "not connect network!");
            return false;
        }
//        vmappLog("Utils.isConnectNetwork", "connected network!");
        return true;
    }

    /**
     * タブレットかどうかを返す。
     * @return タブレットならtrue
     */
    public static boolean isTablet(){

        //このコードは流用しているが、画面サイズを取得する処理が他でも必要になったので別メソッドに分割。t.itou
        int displayH = getDisplayHeight();
        int displayW = getDisplayWidth();

//        vmappLog("Utils.isTablet", String.format("W = %d, H = %d, sca = %f", displayW, displayH, getScaledDensity()));

        //表示上の概ねの大きさ
        int siz = (int) (Math.max(displayW, displayH)/getScaledDensity());
//        vmappLog("Utils.isTablet", ("size = " + siz));

        //現在、７インチで解像度が低めのタブレットを基準にしているため、800にする。
        //参照チケット：https://pro-nbu.backlog.jp/view/TAU_BID-424#comment-1099394919
        //FIXME 基準になるタブレットが新しく出た場合、ここを考慮しなおさないといけない。
        return siz>=800;
    }

    /**
     * ディスプレイの高さを取得する
     * @return ディスプレイの高さ
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static int getDisplayHeight(){
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display d=wm.getDefaultDisplay();

        int displayH;
        //バージョンによってディスプレイサイズの取得方法が異なる
        if(Integer.valueOf(android.os.Build.VERSION.SDK_INT) < 13){
            displayH = d.getHeight();
        }else{
            Point size = new Point();
            d.getSize(size);
            displayH = size.y;
        }

        return displayH;
    }

    /**
     * ディスプレイの幅を取得する
     * @return ディスプレイの幅
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static int getDisplayWidth(){
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display d=wm.getDefaultDisplay();

        int displayW;
        //バージョンによってディスプレイサイズの取得方法が異なる
        if(Integer.valueOf(android.os.Build.VERSION.SDK_INT) < 13){
            displayW = d.getWidth();
        }else{
            Point size = new Point();
            d.getSize(size);
            displayW = size.x;
        }

        return displayW;
    }

    /**
     * DIPスケールを返す。
     * @return DIPスケール
     */
    public static float getScaledDensity(){

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.scaledDensity;
    }

    /**
     * ログ出力
     * @param tag ログのタグ
     * @param message ログ出力内容
     */
    public static void vmappLog(String tag, String message){
        if ((message != null) && (!InternalConstant.IS_DEVELOPMENT)){
            Log.d(tag, message);
        }
    }

    /**
     * 横フリックしたかどうか
     * @param x x方向の動き
     * @param y y方向の動き
     * @return true:開く false:開かない
     */
    public static boolean isFlickSide(float x,float y){

        //画面のサイズを取得
        int height = getDisplayHeight();
        int width = getDisplayWidth();

        //x > y : 横フリックなので、y方向の動きより、x方向の動きが大きくなるはず
        //x > width/4 : 画面幅の1/4以上横フリックしたか
        //y < height/10 : 画面高さの1/10以上y方向に動いていたら、斜めにフリックしているので、横ではない
        return (x > y && x > width/4 && y < height/10);
    }

    /**
     * URLがSSLページか返す
     * @param url 調べたいURL
     * @return SSLページならtrue
     */
    public static boolean isSsl(String url){
        return url.startsWith("https");
    }

    /**
     * アラートを表示する。コールバック付き
     * @param title
     * @param message
     */
    public static void alert(final String title, final String message,final OnClickListener listener){

        uiHandler.post(new Runnable(){

            public void run(){

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder( getCurrentActivity() );
                alertDialogBuilder.setTitle(title);
                alertDialogBuilder.setMessage(message);
                alertDialogBuilder.setPositiveButton( "OK" ,listener);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });
    }

    /**
     * アラート(YES/NO)を表示する。
     * @param title
     * @param message
     */
    public static void confirm(final String title, final String message,final Runnable exec){

        uiHandler.post(new Runnable(){

            public void run(){

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder( getCurrentActivity() );
                alertDialogBuilder.setTitle(title);
                alertDialogBuilder.setMessage(message);
                alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(exec).start();
                    }
                });
                alertDialogBuilder.setNegativeButton("No", null);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });
    }

    /**
     * confirmのリスナー
     * @author mharada
     *
     */
    private static class ConfirmListener implements OnClickListener{

        public Object mParam;
        public Object mTarget;
        public Method mAction;
        public Method mCancelAction;

        @Override
        public void onClick(DialogInterface dialoginterface, int id) {

            //YES?
            if (id==DialogInterface.BUTTON_POSITIVE){

                Helper.invoke(mTarget, mAction, mParam);
            }
            else if (id==DialogInterface.BUTTON_NEGATIVE){

                if (mCancelAction!=null)
                    Helper.invoke(mTarget, mCancelAction, mParam);
            }
        }
    }

    /**
     * invokeDelay用クラス
     * @author mharada
     *
     */
    private static class InvokeDelayRun implements Runnable{
        Object target;
        Method method;
        public InvokeDelayRun(Object target,Method method){
            this.target=target;
            this.method=method;
        }
        @Override
        public void run() {

            //呼び出す
            try {
                method.invoke(target,new Object[]{});
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
     * 指定メソッドを呼び出す。例外キャッチが面倒なので、ある。それだけ。
     */
    public static boolean invoke(Object target,Method method, Object param){

        try {
            method.invoke(target, new Object[]{param});
            return true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * プログレスダイアログを表示する。
     * @param message
     * @param cancelable
     */
    public static void showProgressDialog(final String message,final boolean cancelable){

        Helper.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                ProgressDialog dialog = new ProgressDialog(getCurrentActivity());
                dialog.setMessage(message);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setCancelable(cancelable);
                dialog.show();
                progressDialog = dialog;
            }
        });
    }

    /**
     * プログレスダイアログを表示する。
     */
    public static void dismissProgressDialog(){

        Helper.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (progressDialog!=null){
                    progressDialog.dismiss();
                    progressDialog=null;
                }
            }
        });
    }

    /**
     * Wifiの有効無効を切り替える
     * @param status
     */
    public static void setWifiEnabled(boolean status){

        WifiManager wifiManager = (WifiManager) getSharedContext().getSystemService(Context.WIFI_SERVICE);

        // android.permission.CHANGE_WIFI_STATE
        wifiManager.setWifiEnabled(status); // true or false
    }

    /**
     * URLエンコードする。例外は無視。発生しないし。
     * @param value
     * @return
     */
    public static String urlEncode(String value){

        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //空文字列を返す
        return "";
    }

    /**
     * アプリプリファレンスを返す
     * @return
     */
    public static SharedPreferences getSharedPreferencesPrivate(){

        return Helper.getSharedContext().getSharedPreferences("bsmo_pref",Context.MODE_PRIVATE);
    }

    /**
     * SharedPreferencesにintを保存する
     * @param key 保存キー
     * @param intValue 値
     */
    public static void savePreference(String key, int intValue) {

        SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(Helper.getSharedContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(key, intValue);
        editor.commit();
    }

    /**
     * SharedPreferencesにStringを保存する
     * @param key 保存キー
     * @param strValue 値
     */
    public static void savePreference(String key, String strValue) {

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(Helper.getSharedContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, strValue);
        editor.commit();
    }

    /**
     * 日付をフォーマットする。
     * @param date
     * @param format
     * @return
     */
    public static String stringWithDate(Date date, String format) {

        SimpleDateFormat fmt = new SimpleDateFormat(format, Locale.JAPAN);
        String sdate = fmt.format(date);
//        vmappLog(BsmoInternalConstant.BUSHIMO_SDK_DEBUG_TAG, String.format("stringWithDate %s->%s", date.toString(), sdate));
        return sdate;
    }

    /**
     * 日時に日を足す
     * @param date
     * @param days
     * @return
     */
    public static Date dateAddDays(Date date, int days) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }

    /**
     * 指定年月日時分秒にする。
     * @param year
     * @param month
     * @param day
     * @param h
     * @param m
     * @param s
     * @return
     */
    public static Date dateWithYear(int year, int month, int day, int h, int m,int s) {

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, h, m, s);
        return cal.getTime();
    }

    /**
     * 暗号化キーを生成する。
     * @return
     */
    @SuppressLint("TrulyRandom")
    public static SecretKey createCryptKeyNoSeed(String algorithm) {

        try {
            //これで一応ランダムなはず
            KeyGenerator kg = KeyGenerator.getInstance(algorithm);
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            kg.init(128, random);
            return kg.generateKey();
//			Cipher ch = Cipher.getInstance("AES/CBC/PKCS5Padding");
//			ch.init(Cipher.ENCRYPT_MODE, sk);
//			return ch;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 暗号化オブジェクトを生成する。
     * @return
     */
    public static Cipher createCipher(String algorithmProviderPadding,SecretKey sk,int mode) {

        try {
            Cipher ch = Cipher.getInstance(algorithmProviderPadding);
            ch.init(mode, sk);
            return ch;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * カレントのActivityを取得する
     * @return
     */
    public static Activity getCurrentActivity() {
        return mActivity;
    }

    /**
     * カレントActicityをセットする
     * @param aActivity
     */
    public static void setCurrentActivity(Activity aActivity) {
        mActivity =  aActivity;
    }

    /**
     * コンテキストを返す。
     * @return
     */
    public static Context getSharedContext(){
        if (getCurrentActivity()==null){
            return context;
        }
        return getCurrentActivity().getApplicationContext();
    }

    /**
     * アプリバージョンを返す。
     * @return
     */
    public static String getAppVersion(){
        //バージョンを取得
        try {
            return getSharedContext().getPackageManager().getPackageInfo( getSharedContext().getPackageName(), 1 ).versionName;
        } catch (NameNotFoundException e) {
        }
        //エラーのときは1.0.0にしておく
        return "1.0.0";
    }

    /**
     * 端末のOSバージョンを取得
     * @return String OSバージョン
     */
    public static String getOSVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * 端末名取得
     * @return String 端末名
     */
    public static String getDeviceName() {
        return Build.MODEL;
    }

    /**
     * 配列を指定文字区切りの一文に変換する
     * @param arrayList 配列
     * @param delimiter 区切り文字
     *
     * @return String result 文
     */
    public static String changeArrayToText(ArrayList<String> arrayList, String delimiter) {
        String result = "";

        for(int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i) != null && arrayList.get(i).length() != 0) {

                // 先頭に区切り文字を入れない
                if (result != null && result.length() != 0) {
                    result += delimiter;
                }

                result = result + arrayList.get(i);
            }
        }

        return result;
    }
}
