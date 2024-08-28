package com.admin.keyboard;

import android.view.inputmethod.InputConnection;

// CandidateClickListener.java
public interface CandidateClickListener {
    void onCandidateClick(String suggestion, InputConnection ic);
}
