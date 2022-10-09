package com.ltl.mpmp_lab3;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.ltl.mpmp_lab3.databinding.ActivityRegisterBinding;
import com.ltl.mpmp_lab3.registration.RegistrationRequest;
import com.ltl.mpmp_lab3.ui.login.LoginActivity;
import com.ltl.mpmp_lab3.ui.login.LoginViewModel;
import com.ltl.mpmp_lab3.ui.login.LoginViewModelFactory;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private LoginViewModel loginViewModel;


    private EditText usernameEditText, emailEditText, passwordEditText;
    private Button registerButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };

        usernameEditText.addTextChangedListener(afterTextChangedListener);

        passwordEditText.addTextChangedListener(afterTextChangedListener);

        registerButton.setOnClickListener(view -> {
            RegistrationRequest request = createRequest();
            createAccount(request);
        });
    }

    private void init(){
        usernameEditText = binding.usernameEdit;
        emailEditText = binding.emailEdit;
        passwordEditText = binding.passwordEdit;
        registerButton = binding.registerButton;

        mAuth = FirebaseAuth.getInstance();

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);
    }

    private void createAccount(RegistrationRequest request){
        mAuth.createUserWithEmailAndPassword(request.getEmail(), request.getPassword())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in request's information
                        Log.d("register_activity", "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        assert user != null;
                        UserProfileChangeRequest profileUpdates
                                = new UserProfileChangeRequest.Builder()
                                .setDisplayName(request.getDisplayName())
                                .build();
                        user.updateProfile(profileUpdates);

                        Log.d("register_activity", "createUserWithEmail:success");
                        Log.d("register_activity", "Email: " + user.getEmail());
                        Log.d("register_activity", "Display name:" + user.getDisplayName());

                        returnToLoginActivity();
                    } else {
                        // If sign in fails, display a message to the request.
                        Log.d("register_activity", "createUserWithEmail:failure", task.getException());
                        Toast.makeText(getApplicationContext(), Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    private RegistrationRequest createRequest(){
        String username = usernameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();


        return new RegistrationRequest(username, email, password);
    }

    private void returnToLoginActivity() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}