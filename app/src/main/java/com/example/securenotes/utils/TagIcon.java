package com.example.securenotes.utils;

import com.example.securenotes.R;

import java.util.HashMap;
import java.util.Map;

public enum TagIcon {
    TAG_RED("Casa", R.drawable.circle_red),
    TAG_BLUE("Lavoro", R.drawable.circle_blue),
    TAG_GREEN("Shopping", R.drawable.circle_green),
    TAG_PURPLE("Personale", R.drawable.circle_purple),
    TAG_ORANGE("Finanze", R.drawable.circle_orange),
    TAG_YELLOW("Viaggi", R.drawable.circle_yellow),
    TAG_PINK("Contatti", R.drawable.circle),
    TAG_CYAN("Documenti", R.drawable.circle_light_green);

    public final String name;
    public final int resId;

    TagIcon(String name, int resId) {
        this.name = name;
        this.resId = resId;
    }

    private static final Map<String, Integer> map = new HashMap<>();
    static {
        for (TagIcon icon : values()) {
            map.put(icon.name, icon.resId);
        }
    }
}
