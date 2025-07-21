package com.example.securenotes.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.securenotes.repository.SetupRepository;
import com.example.securenotes.utils.Constants.AuthResult;

public class SetupViewModel extends ViewModel {

    private final MutableLiveData<AuthResult> _authResult = new MutableLiveData<>();
    public final LiveData<AuthResult> authResult = _authResult;

    public void init(String pin, String confirmPin, Context context) {
        SetupRepository.init(pin, confirmPin, context, _authResult::postValue);
    }
}
