package com.ltl.mpmp_lab3;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ltl.mpmp_lab3.ui.login.LoginActivity;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private final FileHandler fileHandler = FileHandler.getInstance(Constants.RECORD_FILE_NAME, this);

    private Button yesButton, noButton, startButton;
    private TextView leftText, rightText, pointsText, rulesText, timerText, recordText, usernameText;
    private ImageView logoutImage;
    private String[] colorNames;
    private int[] colors;
    private final HashMap<String, Integer> colorsMap = new HashMap<>();
    private Integer points = 0;

    private CountDownTimer timer;
    private Long timeLeftInMillis = 0L, mEndTime = 0L;
    boolean isStared = false;

    GoogleSignInAccount account;
    GoogleSignInOptions gso;
    GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    Random generator = new Random();
    private int penalty = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle arguments = getIntent().getExtras();
        penalty = arguments.getInt("penalty");

        init();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null){
            usernameText.setText(currentUser.getDisplayName());
        }

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleClick(AnswerOptions.YES);
            }
        });

        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleClick(AnswerOptions.NO);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (!isStared)
                        startGame(Constants.GAME_DURATION_MILLIS + Constants.ANIMATION_DURATION_MILLIS);
                    else {
                        finishGame();
                        goToResults();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        logoutImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        signOut();
                    }
                });
            }

        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {

        if (isStared){
            timeLeftInMillis = mEndTime - System.currentTimeMillis();
        }
        outState.putInt("points", points);
        outState.putLong("millisLeft", timeLeftInMillis);
        outState.putBoolean("isStarted", isStared);
        outState.putLong("endTime", mEndTime);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        points = savedInstanceState.getInt("points");
        timeLeftInMillis = savedInstanceState.getLong("millisLeft");
        isStared = savedInstanceState.getBoolean("isStarted");
        mEndTime = savedInstanceState.getLong("endTime");

        if (isStared){
            pointsText.setText(String.format(getString(R.string.current_points_text), points));
            startTimer(timeLeftInMillis);
            startButton.setText(getString(R.string.stop_button_text));
        }
    }

    @Override
    public void onBackPressed(){
        // Finish all activities in stack and app closes
        finishAffinity();
    }

    private void init(){
        yesButton = findViewById(R.id.yesButton);
        noButton = findViewById(R.id.noButton);
        startButton =findViewById(R.id.startButton);

        leftText = findViewById(R.id.leftTextView);
        rightText = findViewById(R.id.rightTextView);
        pointsText = findViewById(R.id.pointsTextView);
        rulesText = findViewById(R.id.rulesTextView);
        timerText = findViewById(R.id.timerTextView);
        recordText = findViewById(R.id.recordTextView);
        usernameText = findViewById(R.id.usernameTextView);

        logoutImage = findViewById(R.id.logoutImageView);

        mAuth = FirebaseAuth.getInstance();
        account = GoogleSignIn.getLastSignedInAccount(this);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        if (account != null){
            usernameText.setText(account.getDisplayName());
        }

        Integer previousRecord = fileHandler.loadRecord();
        String text = getText(R.string.record_text) + " " + previousRecord;

        recordText.setText(text);

        colorNames = getResources().getStringArray(R.array.color_names_array);
        colors = getResources().getIntArray(R.array.game_colors_array);
        if (colorNames.length != colors.length) {
            throw new IllegalArgumentException(
                    "The number of keys doesn't match the number of values.");
        }
        for (int i = 0; i < colorNames.length; i++){
            colorsMap.put(colorNames[i], colors[i]);
        }

        shuffle();
    }

    private void startGame(Long timeMillis) throws FileNotFoundException {
        Log.d("main_activity", "game started");
        isStared = true;
        points = 0;
        pointsText.setText(String.format(getString(R.string.current_points_text), points));
        startOpeningAnimations();

        mEndTime = System.currentTimeMillis() + timeMillis;
        startTimer(timeMillis);
    }

    private void startTimer(Long timeMillis){
        timer = new CountDownTimer(timeMillis, 1) {
            @Override
            public void onTick(long l) {
                timeLeftInMillis = l;

                if (l > 60000) {
                    timerText.setText("01:00");
                } else if (l > 10000) {
                    timerText.setText(String.format("00:%d", l / 1000));
                } else {
                    timerText.setText(String.format("00:0%d", l / 1000));
                }
            }
            @Override
            public void onFinish() {
                timerText.setText(R.string.finished_text);
                finishGame();
                goToResults();
            }
        }.start();
    }

    private void finishGame() {
        Log.d("main_activity", "game finished");
        timer.cancel();
        isStared = false;
        startButton.setText(getString(R.string.start_button_text));
        startEndingAnimations();

        Integer previousRecord = fileHandler.loadRecord();
        if (previousRecord < points){
            updateRecord();
        }

    }

    private void goToResults(){
        Intent intent = new Intent(this, GameResultActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(Constants.POINTS_EXTRA, points);
        intent.putExtra(Constants.RECORD_EXTRA, fileHandler.loadRecord());
        intent.putExtra(Constants.DISPLAY_NAME_EXTRA, account.getDisplayName());
        intent.putExtra(Constants.USER_EMAIL_EXTRA, account.getEmail());
        startActivity(intent);
    }

    private void handleClick(AnswerOptions answerOptions) {
        checkAnswer(answerOptions);
        shuffle();
    }

    private void checkAnswer(AnswerOptions answer){
        int expectedColor = colorsMap.get(leftText.getText());
        if (expectedColor == rightText.getCurrentTextColor() && answer == AnswerOptions.YES){
            points++;
            pointsText.setText(String.format(getString(R.string.current_points_text), points));
        } else if (expectedColor != rightText.getCurrentTextColor() && answer == AnswerOptions.NO){
            points++;
            pointsText.setText(String.format(getString(R.string.current_points_text), points));
        } else {
//            if (points > 0) points--;
            points -= penalty;
            pointsText.setText(String.format(getString(R.string.current_points_text), points));
        }
    }

    private void shuffle(){
        int randomTextIndex = generator.nextInt(colors.length);
        int randomColorIndex = generator.nextInt(colorNames.length);
        leftText.setText(colorNames[randomTextIndex]);
        leftText.setTextColor(colors[randomColorIndex]);

        randomTextIndex = generator.nextInt(colors.length);
        randomColorIndex = generator.nextInt(colorNames.length);
        rightText.setText(colorNames[randomTextIndex]);
        rightText.setTextColor(colors[randomColorIndex]);
    }

    private void startOpeningAnimations(){
        Animation moveUp = AnimationUtils.loadAnimation(MainActivity.this, R.anim.move_upwards_disappear);
//        Animation moveDown = AnimationUtils.loadAnimation(MainActivity.this, R.anim.move_downwards_disappear);
        rulesText.startAnimation(moveUp);
        startButton.setText(getString(R.string.stop_button_text));

        Animation reverseMoveDown = AnimationUtils.loadAnimation(MainActivity.this, R.anim.reverse_move_downwards_disappear);
        yesButton.startAnimation(reverseMoveDown);
        noButton.startAnimation(reverseMoveDown);
    }

    private void startEndingAnimations(){
        Animation reverseMoveUp = AnimationUtils.loadAnimation(MainActivity.this, R.anim.reverse_move_upwards_disappear);
//        Animation reverseMoveDown = AnimationUtils.loadAnimation(MainActivity.this, R.anim.reverse_move_downwards_disappear);
        rulesText.startAnimation(reverseMoveUp);
        startButton.setText(getString(R.string.start_button_text));


        Animation moveDown = AnimationUtils.loadAnimation(MainActivity.this, R.anim.move_downwards_disappear);
        yesButton.startAnimation(moveDown);
        noButton.startAnimation(moveDown);
    }

    private void signOut(){
        if (isStared){
            finishGame();
        }
        finish();
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
    }

    private void updateRecord(){
        fileHandler.saveRecord(points);
        recordText.setText(String.format(getString(R.string.current_record_text), points));
    }
}