package com.admin.keyboard.settings;

import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import androidx.preference.PreferenceFragmentCompat;
import com.admin.keyboard.R;

public class KeyboardPreviewPreference extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);

        findPreference("keyboard_preview").setOnPreferenceClickListener(preference -> {
            if (imm != null)
                imm.showInputMethodPicker();
            return true;
        });
    }
}