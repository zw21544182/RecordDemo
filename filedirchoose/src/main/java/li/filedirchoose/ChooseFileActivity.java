/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package li.filedirchoose;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChooseFileActivity extends Activity {
    private static final int REQUESCODE = 5;
    private static final String NEED_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private String mPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private RecyclerView rvFileView;
    private EditText editText;
    private Button btChose;
    private List<String> data;
    private List<String> searchData;
    private FileAdapter fileAdapter;
    private static final String TAG = "ZWS";
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int c, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String res = editable.toString();
            if (res.trim().equals("")) {
                fileAdapter.setData(data);
                return;
            }
            if (searchData == null) {
                searchData = new ArrayList<>();
            }
            searchData.clear();
            for (int i = 0; i < data.size(); i++) {
                String content = data.get(i).substring(data.get(i).lastIndexOf("/")).toLowerCase();
                if (content.contains(res)) {
                    searchData.add(data.get(i));
                }
            }
            Log.d("ZWW", "searchData size " + searchData.size());
            fileAdapter.setData(searchData);

        }
    };
    public static final String SELECTPATH = "SELECTPATH";
    public static final int RESULTCODE = 3;

    public static void enterActivityForResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, ChooseFileActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filechoose_activity_main);
        initView();
        initData();
    }

    private void initData() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(NEED_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
                getDataFromPath(mPath);
            } else {
                requestPermissions(new String[]{NEED_PERMISSION}, REQUESCODE);
            }
            return;
        }
        getDataFromPath(mPath);
    }

    private void getDataFromPath(final String mPath) {
        data.clear();
        new Thread() {
            public void run() {
                super.run();
                File file = new File(mPath);
                File[] listFiles = file.listFiles();
                for (File f : listFiles
                        ) {
                    if (!f.isDirectory() || f.getName().startsWith(".")) {
                        continue;
                    }
                    Log.d("ZWS", f.getAbsolutePath());
                    data.add(f.getAbsolutePath());
                    Log.d("ZWS", "new " + data.size());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("ZWS", "last " + data.size());
                        fileAdapter.setData(data);
                    }
                });
            }
        }.start();


    }

    private void initView() {
        editText = findViewById(R.id.editText);
        rvFileView = findViewById(R.id.rvFileView);
        btChose = findViewById(R.id.btChose);
        data = new ArrayList<>();
        rvFileView.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new FileAdapter(data, this, R.layout.filechoose_layout_fileitme);
        fileAdapter.setEvent(new FileAdapter.Event() {
            @Override
            public void enterNextDir(String path) {
                editText.setText("");
                mPath = path;
                getDataFromPath(mPath);
            }
        });
        rvFileView.setAdapter(fileAdapter);
        editText.addTextChangedListener(textWatcher);
        btChose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putStringArrayListExtra(SELECTPATH, fileAdapter.getSelectData());
                setResult(RESULTCODE, intent);
                finish();
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUESCODE && permissions[0].equals(NEED_PERMISSION)) {
            getDataFromPath(mPath);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {//点击的是返回键
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {//按键的按下事件
                Log.d("ZWW", mPath);
                if (mPath.trim().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    return super.onKeyDown(keyCode, event);
                } else {
                    File file = new File(mPath);
                    if (!file.exists()) {
                        return super.onKeyDown(keyCode, event);
                    }
                    mPath = file.getParent();
                    getDataFromPath(mPath);
                    return false;
                }
            }


        }
        return super.onKeyDown(keyCode, event);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }
}
