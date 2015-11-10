package com.example.hadachi.myapplication;

/**
 * Created by h.adachi on 2015/11/10.
 */
public interface OnRequestResultListener<T> {

    /**
     * リクエストの結果としてエラーが返ってきた。もしくはリクエストに失敗した。
     * @param statusCode
     */
    void onError(int errorCode, int statusCode);

    /**
     * リクエストのレスポンスを受信した。
     * @param statusCode
     * @param body
     */
    void onResponse(int statusCode, T response);
}
