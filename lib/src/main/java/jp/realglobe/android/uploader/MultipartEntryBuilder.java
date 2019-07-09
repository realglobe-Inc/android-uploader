package jp.realglobe.android.uploader;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.realglobe.android.function.Consumer;

/**
 * multipart/form-data で POST する Entry をつくる。
 * Created by fukuchidaisuke on 17/08/25.
 */
public class MultipartEntryBuilder implements Poster.EntryBuilder {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";

    private static class Item {

        private final String name;
        private final String type;
        private final byte[] data;

        private Item(@NonNull String name, @Nullable String type, @NonNull byte[] data) {
            this.name = name;
            this.type = type;
            this.data = data;
        }

        @NonNull
        private byte[] toBytes() {
            final StringBuilder builder = (new StringBuilder())
                    .append("Content-Disposition: form-data; name=\"")
                    .append(this.name)
                    .append("\"; filename=\"")
                    .append(this.name)
                    .append("\"\r\n");

            if (this.type != null) {
                builder.append("Content-Type: ")
                        .append(this.type)
                        .append("\r\n");
            }

            builder.append("\r\n");

            final byte[] header = builder.toString().getBytes();

            final byte[] buff = new byte[header.length + this.data.length];
            System.arraycopy(header, 0, buff, 0, header.length);
            System.arraycopy(this.data, 0, buff, header.length, this.data.length);

            return buff;
        }

    }

    private Poster.BasicEntryBuilder builder;

    private Map<String, String> header;
    private final List<Item> items;

    public MultipartEntryBuilder() {
        this.builder = new Poster.BasicEntryBuilder();
        this.items = new ArrayList<>();
    }

    @NonNull
    private String makeBoundary() {
        final int hash = this.hashCode();
        final byte[] buff = new byte[]{
                (byte) (hash >> 24),
                (byte) (hash >> 16),
                (byte) (hash >> 8),
                (byte) hash,
        };
        return "------------------------------" + Base64.encodeToString(buff, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
    }

    /**
     * @return POST する内容
     */
    @Override
    @NonNull
    public Poster.Entry build() {
        final String boundary = makeBoundary();

        final Map<String, String> allHeader = new HashMap<>();
        if (this.header != null) {
            allHeader.putAll(this.header);
        }
        allHeader.put(CONTENT_TYPE, CONTENT_TYPE_MULTIPART + "; boundary=" + boundary);

        this.builder.setHeader(allHeader);


        try (final ByteArrayOutputStream buff = new ByteArrayOutputStream()) {
            final byte[] separator = ("--" + boundary + "\r\n").getBytes();
            for (final Item item : this.items) {
                buff.write(separator);
                buff.write(item.toBytes());
                buff.write("\r\n".getBytes());
            }
            buff.write(("--" + boundary + "--\r\n").getBytes());

            this.builder.setData(buff.toByteArray());
        } catch (IOException e) {
            // ByteArray なのでここには来ない
            throw new RuntimeException(e);
        }

        return this.builder.build();
    }

    /**
     * @param url POST 先 URL
     * @return this
     */
    @NonNull
    public MultipartEntryBuilder setUrl(@NonNull URL url) {
        this.builder.setUrl(url);
        return this;
    }

    /**
     * @param name 名前
     * @param type Content-Type
     * @param data データ
     * @return this
     */
    @NonNull
    public MultipartEntryBuilder addData(@NonNull String name, @Nullable String type, @NonNull byte[] data) {
        this.items.add(new Item(name, type, data));
        return this;
    }

    /**
     * データを全削除する
     *
     * @return this
     */
    @NonNull
    public MultipartEntryBuilder clearData() {
        this.items.clear();
        return this;
    }

    /**
     * @param header HTTP ヘッダ
     * @return this
     */
    @NonNull
    public MultipartEntryBuilder setHeader(@Nullable Map<String, String> header) {
        this.header = new HashMap<>(header);
        return this;
    }

    /**
     * @param onFinish POST した後で実行する関数
     * @return this
     */
    @NonNull
    public MultipartEntryBuilder setOnFinish(@Nullable Consumer<Integer> onFinish) {
        this.builder.setOnFinish(onFinish);
        return this;
    }

    /**
     * @param onError エラー発生時に実行する関数
     * @return this
     */
    @NonNull
    public MultipartEntryBuilder setOnError(@Nullable Consumer<Exception> onError) {
        this.builder.setOnError(onError);
        return this;
    }

    /**
     * @param timeout 接続タイムアウト
     * @return this
     */
    @NonNull
    public MultipartEntryBuilder setTimeout(int timeout) {
        this.builder.setTimeout(timeout);
        return this;
    }

}
