package com.example.securenotes.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.securenotes.repository.ChangePinRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChangePinViewModel extends ViewModel {

    public static class Result {
        public final boolean success;
        public final String message;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private final MutableLiveData<Result> _result = new MutableLiveData<>();
    public LiveData<Result> result = _result;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void changePin(Context context, String currentPin, String newPin, String confirmPin) {
        Result validation = validateInput(currentPin, newPin, confirmPin);
        if (!validation.success) {
            _result.setValue(validation);
            return;
        }

        executor.execute(() -> {
            Result repoResult = ChangePinRepository.changePin(context, currentPin, newPin);
            _result.postValue(repoResult);
        });
    }

    private Result validateInput(String currentPin, String newPin, String confirmPin) {
        if (currentPin.isEmpty() || newPin.isEmpty() || confirmPin.isEmpty()) {
            return new Result(false, "Compila tutti i campi");
        }
        if (!newPin.equals(confirmPin)) {
            return new Result(false, "I nuovi PIN non coincidono");
        }
        if (newPin.length() < 4 || newPin.length() > 6) {
            return new Result(false, "Il nuovo PIN deve essere tra 4 e 6 cifre");
        }
        return new Result(true, "");
    }
}
