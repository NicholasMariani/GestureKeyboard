package com.admin.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class EmojiHeader extends HorizontalScrollView {
    private LinearLayout headerLayout;
    private LinearLayout buttonsLayout;
    private LinearLayout searchLayout;
    private InputConnection ic;
    private int width, height;

    public EmojiHeader(Context context) {
        super(context);
        init();
    }

    public EmojiHeader(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EmojiHeader(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        headerLayout = new LinearLayout(getContext());
        headerLayout.setOrientation(LinearLayout.VERTICAL);

        buttonsLayout = new LinearLayout(getContext());
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        addButtonsToLayout();

        searchLayout = new LinearLayout(getContext());
        searchLayout.setOrientation(LinearLayout.HORIZONTAL);
        addSearchViewsToLayout();

        headerLayout.addView(buttonsLayout);
        headerLayout.addView(searchLayout);

        // Adjusted layout parameters to WRAP_CONTENT for height
        addView(headerLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    private void addButtonsToLayout() {
        Button emojiButton = createButton("Emoji");
        Button gifButton = createButton("GIFs");

        buttonsLayout.addView(emojiButton);
        buttonsLayout.addView(gifButton);
    }

    private Button createButton(String text) {
        Button button = new Button(getContext());
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        button.setText(text);
        button.setTextColor(Color.parseColor("#CCCCCC"));
        button.setTextSize(20);
        // Set other button properties as needed
        return button;
    }

    private void addSearchViewsToLayout() {
        ImageView imageView = new ImageView(getContext());
//        imageView.setImageDrawable(R.drawable.keyboard_icon);
        // Set ImageView properties as needed

        EditText editText = new EditText(getContext());
        editText.setHint("Search");

        searchLayout.addView(imageView);
        searchLayout.addView(editText);
    }

    public void setDisplaySize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
