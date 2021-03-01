/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
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

package org.tensorflow.lite.examples.textclassification.EGQA;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import org.tensorflow.lite.examples.textclassification.R;
import org.tensorflow.lite.examples.textclassification.client.Result;
import org.tensorflow.lite.examples.textclassification.client.TextClassificationClient;

import org.tensorflow.lite.examples.textclassification.databinding.ActivityBinding;
import org.tensorflow.lite.examples.textclassification.EGQA.DataBase.MyData;

/**
 * The main activity to provide interactions with users.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "TextClassificationDemo";

    private TextClassificationClient client;

    private TextView resultTextView;
    private EditText inputEditText;
    private Handler handler;
    private ScrollView scrollView;
    private ActivityBinding binding;
    private int SPEECH_REQUEST_CODE = 0;
    private String STT = "123";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        Log.v(TAG, "onCreate");


        binding = ActivityBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        client = new TextClassificationClient(getApplicationContext());
        handler = new Handler();
        Button classifyButton = binding.button;
        classifyButton.setOnClickListener(
                (View v) -> {
                     displaySpeechRecognizer();

                });

        resultTextView = binding.resultTextView;
        inputEditText = binding.inputText;
        scrollView = binding.scrollView;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");
        handler.post(
                () -> {
                    client.load();
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
        handler.post(
                () -> {
                    client.unload();
                });
    }

    //classify for sentence mood ;
    private void classify(final String text) {
        handler.post(
                () -> {
                    List<Result> results = client.classify(text);
                    showResult(text, results);
                });
    }

    //setting text to UI
    private void showResult(final String inputText, final List<Result> results) {
        runOnUiThread(
                () -> {
                    String textToShow = "Input: " + inputText + "\nOutput:\n";
                    Result Positive = results.get(0);
                    Result Negative = results.get(1);
                    textToShow += String.format("%s  \n %s\n", Positive.getConfidence(), Negative.getConfidence());
                    String SentenceClass = searchClass(Positive.getConfidence(), Negative.getConfidence());
                    textToShow += SentenceClass + "\n---------\n";
                    resultTextView.append(textToShow);

                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                    alertDialog(SentenceClass) ;
                });
    }

    //Find Ai database for question
    private String searchClass(final double Pos, final double Neg) {
        MyData data = new MyData();
        String Label[] = data.Labelgetter();
        Double Negative[] = data.Negativegetter();
        Double Positive[] = data.Positivegetter();
        int FindMood = 0;
        double FindMinGap = 10;
        for (int i = 0; i < 1482; i++) {
            double PosAbs = Math.abs(Pos - Positive[i]);
            double NegAbs = Math.abs(Neg - Negative[i]);
            if (FindMinGap > (PosAbs + NegAbs)) {
                FindMinGap = (PosAbs + NegAbs);
                FindMood = i;
                Log.d("FindMood", " " + FindMood);
            }
            if (FindMood > 196) {
                FindMood = 195;
                i = 1482;
            }
        }
        return Label[FindMood];
    }
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            Toast.makeText(this,spokenText,Toast.LENGTH_SHORT).show();
            spokenText = spokenText.toUpperCase().charAt(0) + spokenText.substring(1)+".";
            classify( spokenText);
        }
    }
    private void alertDialog(String Sentence)
    {
        AlertDialog.Builder alertDialog =
                new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("回答評價");
        if(Sentence.equals("Q1"))
            alertDialog.setMessage("太棒了!!但是答案可不只一種喔，可以再多試試");
        else if(Sentence.equals("T1"))
            alertDialog.setMessage("意思相反了，請再試一次");
        else
            alertDialog.setMessage("與題意完全不相符喔，請再試一次");

        alertDialog.setPositiveButton("確定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }
}
