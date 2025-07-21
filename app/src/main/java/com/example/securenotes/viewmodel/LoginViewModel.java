package com.example.securenotes.viewmodel;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.securenotes.repository.LoginRepository;
import com.example.securenotes.utils.Constants.AuthResult;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<AuthResult> _authResult = new MutableLiveData<>();
    public final LiveData<AuthResult> authResult = _authResult;

    public void authenticatePin(Context context, String pin) {
        LoginRepository.authenticatePin(context, pin, _authResult::postValue);
    }

    public void authenticateBiometric(FragmentActivity activity) {
        LoginRepository.authenticateBiometric(activity, _authResult::postValue);
    }
}

