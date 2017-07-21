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

    private static final int DEFAULT_TIMEOUT = 30_000; // ミリ秒

    /**
     * POST する内容
     */
    public static class Entry {

        private final URL url;
        private final Map<String, Object> data;
        private final Consumer<Integer> onFinish;
        private final Consumer<Exception> onError;
        private final int timeout;

        private Entry(@NonNull URL url, @NonNull Map<String, Object> data, @NonNull Consumer<Integer> onFinish, @NonNull Consumer<Exception> onError, int timeout) {
            this.url = url;
            this.data = data;
            this.onFinish = onFinish;
            this.onError = onError;
            this.timeout = timeout;
        }

    }

    /**
     * Entry をつくる
     */
    public static class EntryBuilder {

        private URL url;
        private Map<String, Object> data;
        private Consumer<Integer> onFinish;
        private Consumer<Exception> onError;
        private int timeout;

        public EntryBuilder() {
            this.timeout = -1;
        }

        /**
         * @return POST する内容
         */
        @NonNull
        public Entry build() {
            if (this.url == null) {
                throw new IllegalStateException("null URL");
            } else if (this.data == null) {
                throw new IllegalStateException("null data");
            }

            return new Entry(
                    this.url,
                    this.data,
                    (this.onFinish != null ? this.onFinish : (Integer status) -> Log.v(TAG, "Post resulted in " + status)),
                    (this.onError != null ? onError : (Exception e) -> Log.e(TAG, "Post failed", e)),
                    (this.timeout >= 0 ? this.timeout : DEFAULT_TIMEOUT)
            );
        }

        /**
         * @param url POST 先 URL
         * @return this
         */
        public EntryBuilder setUrl(@NonNull URL url) {
            this.url = url;
            return this;
        }

        /**
         * @param data JSON にして POST するデータ
         * @return this
         */
        public EntryBuilder setData(@NonNull Map<String, Object> data) {
            this.data = data;
            return this;
        }

        /**
         * @param onFinish POST した後で実行する関数
         * @return this
         */
        public EntryBuilder setOnFinish(@Nullable Consumer<Integer> onFinish) {
            this.onFinish = onFinish;
            return this;
        }

        /**
         * @param onError エラー発生時に実行する関数
         * @return this
         */
        public EntryBuilder setOnError(@Nullable Consumer<Exception> onError) {
            this.onError = onError;
            return this;
        }

        /**
         * @param timeout 接続タイムアウト
         * @return this
         */
        public EntryBuilder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

    }

    private static class JsonPostHandler extends Handler {

        private static final int MSG_POST = 0;

        private JsonPostHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_POST: {
                    final Entry entry = (Entry) msg.obj;
                    try {
                        post(entry.url, entry.data, entry.onFinish, entry.timeout);
                    } catch (Exception e) {
                        entry.onError.accept(e);
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
         * @param entry POST する内容
         * @param clear true なら溜まってる分は捨てる
         */
        void post(@NonNull Entry entry, boolean clear) {
            if (clear && hasMessages(MSG_POST)) {
                removeMessages(MSG_POST);
            }
            sendMessage(obtainMessage(MSG_POST, entry));
        }

    }

    private final JsonPostHandler handler;

    /**
     * @param looper スレッド
     */
    public JsonPoster(@NonNull Looper looper) {
        this.handler = new JsonPostHandler(looper);
    }

    /**
     * @param entry POST する内容
     * @param clear true なら溜まってる分は捨てる
     */
    public void post(@NonNull Entry entry, boolean clear) {
        this.handler.post(entry, clear);
    }

    /**
     * post(entry, false) と同じ
     */
    public void post(@NonNull Entry entry) {
        this.handler.post(entry, false);
    }

}
