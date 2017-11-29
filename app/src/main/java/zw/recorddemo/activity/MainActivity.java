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

package zw.recorddemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import li.filedirchoose.ChooseFileActivity;
import zw.recorddemo.R;


public class MainActivity extends Activity {
    public static final int PATHREQUESTCODE = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        initData();
    }

    private void initData() {
        ChooseFileActivity.enterActivityForResult(this, PATHREQUESTCODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("zww","onActivityResult ");
        if (requestCode == PATHREQUESTCODE && resultCode == ChooseFileActivity.RESULTCODE) {
            ArrayList<String> resPath = data.getStringArrayListExtra(ChooseFileActivity.SELECTPATH);
            Log.d("ZWW", resPath.toString());
        }
    }
}
