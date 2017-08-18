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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jp.realglobe.android.function.Consumer;

/**
 * JSON で POST する Entry をつくる
 */
public class JsonEntryBuilder {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private static final Map<String, String> header;

    static {
        header = new HashMap<>();
        header.put(CONTENT_TYPE, CONTENT_TYPE_JSON);
    }

    private Poster.EntryBuilder builder;

    public JsonEntryBuilder() {
        this.builder = new Poster.EntryBuilder();
        this.builder.setHeader(header);
    }

    /**
     * @return POST する内容
     */
    @NonNull
    public Poster.Entry build() {
        return this.builder.build();
    }

    /**
     * @param url POST 先 URL
     * @return this
     */
    public JsonEntryBuilder setUrl(@NonNull URL url) {
        this.builder.setUrl(url);
        return this;
    }

    /**
     * @param data JSON にして POST するデータ
     * @return this
     */
    public JsonEntryBuilder setData(@NonNull Map<String, Object> data) {
        this.builder.setData((new JSONObject(data)).toString().getBytes());
        return this;
    }

    /**
     * @param onFinish POST した後で実行する関数
     * @return this
     */
    public JsonEntryBuilder setOnFinish(@Nullable Consumer<Integer> onFinish) {
        this.builder.setOnFinish(onFinish);
        return this;
    }

    /**
     * @param onError エラー発生時に実行する関数
     * @return this
     */
    public JsonEntryBuilder setOnError(@Nullable Consumer<Exception> onError) {
        this.builder.setOnError(onError);
        return this;
    }

    /**
     * @param timeout 接続タイムアウト
     * @return this
     */
    public JsonEntryBuilder setTimeout(int timeout) {
        this.builder.setTimeout(timeout);
        return this;
    }

}
