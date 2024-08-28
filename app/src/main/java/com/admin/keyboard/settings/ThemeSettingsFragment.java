package com.admin.keyboard.settings;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.admin.keyboard.KeyboardActivity;
import com.admin.keyboard.R;

public class ThemeSettingsFragment extends PreferenceFragmentCompat {
    KeyboardActivity keyboardActivity = KeyboardActivity.getInstance();
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_theme);
    }
}
