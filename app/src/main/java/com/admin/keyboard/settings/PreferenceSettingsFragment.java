package com.admin.keyboard.settings;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.admin.keyboard.KeyboardActivity;
import com.admin.keyboard.R;

public class PreferenceSettingsFragment extends PreferenceFragmentCompat {
    KeyboardActivity keyboardActivity = KeyboardActivity.getInstance();
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_pref);

        // Respond to changes in the number row preference
        findPreference("number_row_preference").setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (boolean) newValue;
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .putBoolean("number_row_preference", enabled).apply();
            keyboardActivity.updateNumRowPref();
            return true;
        });
    }
}
