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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import jp.realglobe.android.function.Consumer;
import jp.realglobe.android.logger.simple.Log;

/**
 * HTTP POST する。
 * Created by fukuchidaisuke on 17/07/05.
 */
public class Poster {

    private static final String TAG = Poster.class.getName();

    private static final int DEFAULT_TIMEOUT = 30_000; // ミリ秒

    /**
     * POST する内容
     */
    public static class Entry {

        private final URL url;
        private final byte[] data;
        private final Map<String, String> header;
        private final Consumer<Integer> onFinish;
        private final Consumer<Exception> onError;
        private final int timeout;

        private Entry(@NonNull URL url, @Nullable byte[] data, @NonNull Map<String, String> header, @NonNull Consumer<Integer> onFinish, @NonNull Consumer<Exception> onError, int timeout) {
            this.url = url;
            this.data = data;
            this.header = header;
            this.onFinish = onFinish;
            this.onError = onError;
            this.timeout = timeout;
        }

    }

    /**
     * Entry をつくる
     */
    public interface EntryBuilder {

        /**
         * @return POST する内容
         */
        @NonNull
        Entry build();

    }

    public static class BasicEntryBuilder implements EntryBuilder {

        private URL url;
        private byte[] data;
        private Map<String, String> header;
        private Consumer<Integer> onFinish;
        private Consumer<Exception> onError;
        private int timeout;

        public BasicEntryBuilder() {
            this.timeout = -1;
        }

        @Override
        @NonNull
        public Entry build() {
            if (this.url == null) {
                throw new IllegalStateException("null URL");
            }

            return new Entry(
                    this.url,
                    this.data,
                    (this.header != null ? this.header : Collections.emptyMap()),
                    (this.onFinish != null ? this.onFinish : (Integer status) -> Log.v(TAG, "Post resulted in " + status)),
                    (this.onError != null ? onError : (Exception e) -> Log.e(TAG, "Post failed", e)),
                    (this.timeout >= 0 ? this.timeout : DEFAULT_TIMEOUT)
            );
        }

        /**
         * @param url POST 先 URL
         * @return this
         */
        public BasicEntryBuilder setUrl(@NonNull URL url) {
            this.url = url;
            return this;
        }

        /**
         * @param data POST するデータ
         * @return this
         */
        public BasicEntryBuilder setData(@Nullable byte[] data) {
            this.data = data;
            return this;
        }

        /**
         * @param header HTTP ヘッダ
         * @return this
         */
        public BasicEntryBuilder setHeader(@Nullable Map<String, String> header) {
            this.header = header;
            return this;
        }

        /**
         * @param onFinish POST した後で実行する関数
         * @return this
         */
        public BasicEntryBuilder setOnFinish(@Nullable Consumer<Integer> onFinish) {
            this.onFinish = onFinish;
            return this;
        }

        /**
         * @param onError エラー発生時に実行する関数
         * @return this
         */
        public BasicEntryBuilder setOnError(@Nullable Consumer<Exception> onError) {
            this.onError = onError;
            return this;
        }

        /**
         * @param timeout 接続タイムアウト
         * @return this
         */
        public BasicEntryBuilder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

    }

    private static class PostHandler extends Handler {

        private static final int MSG_POST = 0;

        private PostHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_POST: {
                    final Entry entry = (Entry) msg.obj;
                    try {
                        post(entry);
                    } catch (Exception e) {
                        entry.onError.accept(e);
                    }
                }
            }
        }

        private static void post(@NonNull Entry entry) throws IOException {
            final HttpURLConnection connection = (HttpURLConnection) entry.url.openConnection();
            try {
                connection.setConnectTimeout(entry.timeout);
                connection.setDoOutput(entry.data != null);
                connection.setDoInput(false);
                for (final Map.Entry<String, String> header : entry.header.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
                connection.connect();

                if (entry.data != null) {
                    try (final OutputStream reqBody = new BufferedOutputStream(connection.getOutputStream())) {
                        reqBody.write(entry.data);
                    }
                }
                entry.onFinish.accept(connection.getResponseCode());
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

    private final PostHandler handler;

    /**
     * @param looper スレッド
     */
    public Poster(@NonNull Looper looper) {
        this.handler = new PostHandler(looper);
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
