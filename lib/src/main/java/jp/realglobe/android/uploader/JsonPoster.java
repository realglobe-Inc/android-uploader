/*----------------------------------------------------------------------
 * Copyright 2017 realglobe Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *----------------------------------------------------------------------*/

package jp.realglobe.android.uploader;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import jp.realglobe.android.function.Consumer;
import jp.realglobe.android.logger.simple.Log;

/**
 * JSON を HTTP POST する。
 * Created by fukuchidaisuke on 17/07/05.
 */
public class JsonPoster {

    private static final String TAG = JsonPoster.class.getName();

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private static class JsonPostHandler extends Handler {

        private static final int MSG_POST = 0;

        private final URL url;
        private final int timeout;
        @NonNull
        private final Consumer<Integer> onUpload;
        @NonNull
        private final Consumer<Exception> onError;

        JsonPostHandler(@NonNull Looper looper, @NonNull URL url, @Nullable Consumer<Integer> onUpload, @Nullable Consumer<Exception> onError, int timeout) {
            super(looper);
            this.url = url;
            this.timeout = timeout;
            this.onUpload = (onUpload != null ? onUpload : (Integer status) -> Log.v(TAG, "Upload resulted in " + status));
            this.onError = (onError != null ? onError : (Exception e) -> Log.e(TAG, "Post failed", e));
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_POST: {
                    try {
                        post(this.url, (Map) msg.obj, this.onUpload, this.timeout);
                    } catch (Exception e) {
                        this.onError.accept(e);
                    }
                }
            }
        }

        private static void post(@NonNull URL url, @NonNull Map data, @NonNull Consumer<Integer> onFinish, int timeout) throws IOException {
            final String json = (new JSONObject(data)).toString();
            final byte[] bytes = json.getBytes();
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setConnectTimeout(timeout);
                connection.setDoOutput(true);
                connection.setDoInput(false);
                connection.setRequestProperty(CONTENT_TYPE, CONTENT_TYPE_JSON);
                connection.connect();

                try (final OutputStream reqBody = new BufferedOutputStream(connection.getOutputStream())) {
                    reqBody.write(bytes);
                }
                onFinish.accept(connection.getResponseCode());
            } finally {
                connection.disconnect();
            }
        }

        /**
         * @param data  POST させるデータ
         * @param clear true なら溜まってる分は捨てる
         */
        void post(@NonNull Map data, boolean clear) {
            if (clear && hasMessages(MSG_POST)) {
                removeMessages(MSG_POST);
            }
            sendMessage(obtainMessage(MSG_POST, data));
        }

    }

    private final JsonPostHandler handler;

    /**
     * @param looper   スレッド
     * @param url      POST 先 URL
     * @param onUpload アップロード時実行関数。引数はレスポンスステータス
     * @param onError  エラー時実行関数
     * @param timeout  接続タイムアウト（ミリ秒）
     */
    public JsonPoster(@NonNull Looper looper, @NonNull URL url, @NonNull Consumer<Integer> onUpload, @Nullable Consumer<Exception> onError, int timeout) {
        this.handler = new JsonPostHandler(looper, url, onUpload, onError, timeout);
    }

    /**
     * @param data  POST させるデータ
     * @param clear true なら溜まってる分は捨てる
     */
    public void post(@NonNull Map data, boolean clear) {
        this.handler.post(data, clear);
    }

    /**
     * post(data, false) と同じ
     */
    public void post(@NonNull Map data) {
        this.handler.post(data, false);
    }

}
