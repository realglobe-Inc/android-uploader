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

package jp.realglobe.android.uploader.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jp.realglobe.android.uploader.JsonPoster;

public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.INTERNET,
    };
    private static final int PERMISSION_REQUEST_CODE = 10895;

    private HandlerThread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.thread = new HandlerThread(getClass().getName());
        this.thread.start();

        final EditText editUrl = (EditText) findViewById(R.id.edit_url);
        final EditText editJson = (EditText) findViewById(R.id.edit_json);
        final View buttonPost = findViewById(R.id.button_post);

        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        try {
            editJson.setText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map));
        } catch (JsonProcessingException e) {
            showToast(getString(R.string.notification_error, e));
            return;
        }
        buttonPost.setOnClickListener((View v) -> {
            try {
                final URL url = new URL(editUrl.getText().toString());
                final JsonPoster poster = new JsonPoster(this.thread.getLooper(), url, (Exception e) -> runOnUiThread(() -> showToast(getString(R.string.notification_error, e))), 30_000);
                final Map<String, Object> data = mapper.readValue(editJson.getText().toString(), new TypeReference<Map<String, Object>>() {
                });
                poster.post(data);
            } catch (IOException e) {
                showToast(getString(R.string.notification_error, e));
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }

        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                showToast(getString(R.string.notification_permissions));
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        this.thread.quit();
        super.onDestroy();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

}