package com.example.hadachi.myapplication;

/**
 * Created by h.adachi on 2015/11/11.
 */
public class ApiConst {
    /*--------- リクエストパラメータ ---------*/
    // 新システムAPI
    /** 新システムAPIリクエストパラメータ : inCoreUserId */
    public static final String API_REQPARAM_CORE_USER_ID = "inCoreUserId";
    /** 新システムAPIリクエストパラメータ : inDeviceToken */
    public static final String API_REQPARAM_DEVICE_TOKEN = "inDeviceToken";
    /** 新システムAPIリクエストパラメータ : inOSType */
    public static final String API_REQPARAM_OS_TYPE = "inOSType";
    /** 新システムAPIリクエストパラメータ : inLang */
    public static final String API_REQPARAM_LANG = "inLang";
    /** 新システムAPIリクエストパラメータ : inUpdateCount */
    public static final String API_REQPARAM_UPDATE_COUNT = "inUpdateCount";
    // 既存システムAPI
    /** 既存システムAPIリクエストパラメータ : access_token */
    public static final String EXAPI_REQPARAM_ACCESS_TOKEN = "access_token";
    /** 既存システムAPIリクエストパラメータ : device_id */
    public static final String EXAPI_REQPARAM_DEVICE_ID = "device_id";

	/*--------- レスポンスキー ---------*/
    // 新システムAPI
    /** 新システムAPIレスポンスキー : outReturnCd */
    public static final String API_RESKEY_RETURN_CD = "outReturnCd";
    /** 新システムAPIレスポンスキー : outErrMsg */
    public static final String API_RESKEY_ERR_MSG = "outErrMsg";
    /** 新システムAPIレスポンスキー : outDeviceId */
    public static final String API_RESKEY_DEVICE_ID = "outDeviceId";
    /** 新システムAPIレスポンスキー : outVersion */
    public static final String API_RESKEY_VERSION = "outVersion";
    /** 新システムAPIレスポンスキー : outId */
    public static final String API_RESKEY_ID = "outId";
    /** 新システムAPIレスポンスキー : outOpened */
    public static final String API_RESKEY_OPENED = "outOpened";
    /** 新システムAPIレスポンスキー : outTitle */
    public static final String API_RESKEY_TITLE = "outTitle";
    /** 新システムAPIレスポンスキー : outNewFlg */
    public static final String API_RESKEY_NEW_FLG = "outNewFlg";
    /** 新システムAPIレスポンスキー : outCount */
    public static final String API_RESKEY_COUNT = "outCount";
    // 既存システムAPI
    /** 既存システムAPIレスポンスキー : result */
    public static final String EXAPI_RESKEY_RESULT = "result";
    /** 既存システムAPIレスポンスキー : id */
    public static final String EXAPI_RESKEY_ID = "id";
    /** 既存システムAPIレスポンスキー : name */
    public static final String EXAPI_RESKEY_NAME = "name";

	/*--------- レスポンス値 ---------*/
    /** お知らせのNewフラグがONの場合のレスポンス値 */
    public static final String API_RES_NEW_FLG_ON = "1";
    /** お知らせのNewフラグがOFFの場合のレスポンス値 */
    public static final String API_RES_NEW_FLG_OFF = "0";

	/*--------- リターンコード ---------*/
    // 新システムAPI
    /** 新システムAPIリターンコード : 成功 */
    public static final String RETURN_CD_SUCCESS = "0";
    /** 新システムAPIリターンコード : 不正なパラメータ */
    public static final String RETURN_CD_INVALID_PARAM = "400";
    /** 新システムAPIリターンコード : 認証失敗 */
    public static final String RETURN_CD_AUTH_FAILURE = "401";
    /** 新システムAPIリターンコード : 未検出 */
    public static final String RETURN_CD_NOT_FOUND = "404";
    /** 新システムAPIリターンコード : 不正なデバイス */
    public static final String RETURN_CD_INVALID_DEVICE = "409";
    /** 新システムAPIリターンコード : Push通知で、使用されなくなったデバイス */
    public static final String RETURN_CD_INACTIVE_DEVICE = "412";
    // 既存システムAPI
    /** 既存システムAPIリターンコード : 成功 */
    public static final String RESULT_SUCCESS = "0";
    /** 既存システムAPIリターンコード : アクセス権限がない */
    public static final String RESULT_NOT_ACCESSIBLE = "100";
    /** 既存システムAPIリターンコード : その他エラー */
    public static final String RESULT_OTHER_ERROR = "999";

	/*--------- HTTPステータスコード ---------*/
    /** 接続エラーのステータスコード */
    public static final int STATUS_CD_CONNECTION_ERROR = 0;
    /** 通信成功時のサーバレスポンスコード 成功 */
    public static final int STATUS_CD_OK = 200;
    /** 通信成功時のサーバレスポンスコード 生成 */
    public static final int STATUS_CD_CREATED = 201;
    /** 通信成功時のサーバレスポンスコード 部分的コンテンツ */
    public static final int STATUS_CD_PARTIAL_CONTENT = 206;
    /** コンテンツが見つからない */
    public static final int STATUS_CD_NOT_FOUND = 404;
    /** サーバエラー時 : サーバ内部エラー */
    public static final int STATUS_CD_SERVER_ERROR = 500;
    /** サーバエラー時 : 未実装 */
    public static final int STATUS_CD_NOT_IMPLEMENTED = 501;
    /** サーバエラー時 : 不正ゲートウェイ */
    public static final int STATUS_CD_BAD_GATEWAY = 502;
    /** サーバメンテナンス時 : サービス利用不可 */
    public static final int STATUS_CD_SERVER_MAINTENANCE = 503;
    /** サーバエラー時 : ゲートウェイ・タイムアウト */
    public static final int STATUS_CD_GATEWAY_TIME_OUT = 504;
    /** サーバエラー時 : 非サポートHTTPバージョン */
    public static final int STATUS_CD_HTTP_VERSION_NOT_SUPPORTED = 505;
}
