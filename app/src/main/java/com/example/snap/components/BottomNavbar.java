package com.example.snap.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.example.snap.R;

public class BottomNavbar extends LinearLayout {

    /**
     * Constructor
     * @param context
     */

    public BottomNavbar(Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructor
     * @param context
     * @param attrs
     */

    public BottomNavbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     *
     * Constructor
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public BottomNavbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Inicialización
     * @param context
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_bottom_navbar, this, true);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
    }
}