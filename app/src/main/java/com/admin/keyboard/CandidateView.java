package com.admin.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

public class CandidateView extends HorizontalScrollView {
    private LinearLayout suggestionsLayout;
    private InputConnection ic;
    private CandidateClickListener candidateClickListener;
    private int width, height;

    public CandidateView(Context context) {
        super(context);
        init();
    }

    public CandidateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CandidateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        suggestionsLayout = new LinearLayout(getContext());
        suggestionsLayout.setOrientation(LinearLayout.HORIZONTAL);
        addView(suggestionsLayout);
    }

    public void setCandidateClickListener(CandidateClickListener listener) {
        this.candidateClickListener = listener;
    }

    public void setSuggestions(boolean typo, String currentWord, List<String> suggestions, InputConnection ic) {
        this.ic = ic;
        suggestionsLayout.removeAllViews();

        int color = typo ? Color.RED : Color.GREEN;
        addButtonToLayout(color, currentWord, false);

        for(int i = 0; i < suggestions.size(); i++) {
            if(!currentWord.equals(suggestions.get(i))) {
                if (i != suggestions.size() - 1) {
                    addButtonToLayout(Color.parseColor("#CCCCCC"), suggestions.get(i), false);
                } else {
                    addButtonToLayout(Color.parseColor("#CCCCCC"), suggestions.get(i), true);
                }
            }
        }
    }

    public void setFailure(String suggestion, InputConnection ic) {
        this.ic = ic;
        suggestionsLayout.removeAllViews();
        addFailureToLayout(suggestion);
    }

    public void clearSuggestions() {
        suggestionsLayout.removeAllViews();
    }

    public void addButtonToLayout(int color, final String text, boolean last) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        LinearLayout buttonLayout = (LinearLayout) inflater.inflate(R.layout.candidate_button_layout, suggestionsLayout, false);
        LinearLayout.LayoutParams buttonParams= new LinearLayout.LayoutParams(width / 6, LinearLayout.LayoutParams.MATCH_PARENT);
        buttonLayout.setLayoutParams(buttonParams);
        Button button = buttonLayout.findViewById(R.id.suggestion);
        button.setTextColor(color);
        button.setText(text);
        button.setOnClickListener(v -> {
            if (candidateClickListener != null) {
                candidateClickListener.onCandidateClick(text, ic);
            }
        });

        if (!last) {
            View divider = new View(getContext());
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(4, LinearLayout.LayoutParams.MATCH_PARENT);
            divider.setBackgroundColor(Color.parseColor("#90909090"));
            divider.setLayoutParams(dividerParams);
            buttonLayout.addView(divider);
        }

        suggestionsLayout.addView(buttonLayout);
    }

    public void addFailureToLayout(final String text) {
        clearSuggestions();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        LinearLayout buttonLayout = (LinearLayout) inflater.inflate(R.layout.candidate_button_layout, suggestionsLayout, false);
        LinearLayout.LayoutParams buttonParams= new LinearLayout.LayoutParams(width /2, LinearLayout.LayoutParams.MATCH_PARENT);
        buttonLayout.setLayoutParams(buttonParams);
        buttonParams.setMargins(0, 0, 0, 0);
        Button button = buttonLayout.findViewById(R.id.suggestion);
        button.setTextSize(14);
        button.setText(text);
        suggestionsLayout.addView(buttonLayout);
    }

    public void setDisplaySize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
