package com.admin.keyboard.settings;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import com.admin.keyboard.KeyboardActivity;
import com.admin.keyboard.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
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
}