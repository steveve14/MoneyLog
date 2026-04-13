package com.moneylog.util;

import com.moneylog.R;

import java.util.LinkedHashMap;
import java.util.Map;

public final class IconHelper {

    private IconHelper() {}

    private static final Map<String, Integer> ICON_MAP = new LinkedHashMap<>();

    static {
        // 지출 카테고리
        ICON_MAP.put("restaurant",              R.drawable.ic_cat_restaurant);
        ICON_MAP.put("directions_bus",          R.drawable.ic_cat_directions_bus);
        ICON_MAP.put("home",                    R.drawable.ic_cat_home);
        ICON_MAP.put("sports_esports",          R.drawable.ic_cat_sports_esports);
        ICON_MAP.put("checkroom",               R.drawable.ic_cat_checkroom);
        ICON_MAP.put("local_hospital",          R.drawable.ic_cat_local_hospital);
        ICON_MAP.put("menu_book",               R.drawable.ic_cat_menu_book);
        ICON_MAP.put("redeem",                  R.drawable.ic_cat_redeem);
        ICON_MAP.put("local_cafe",              R.drawable.ic_cat_local_cafe);
        ICON_MAP.put("inventory_2",             R.drawable.ic_cat_inventory_2);
        // 수입 카테고리
        ICON_MAP.put("payments",                R.drawable.ic_cat_payments);
        ICON_MAP.put("account_balance_wallet",  R.drawable.ic_cat_account_balance_wallet);
        ICON_MAP.put("savings",                 R.drawable.ic_cat_savings);
        ICON_MAP.put("trending_up",             R.drawable.ic_cat_trending_up);
        // 추가 아이콘
        ICON_MAP.put("shopping_cart",           R.drawable.ic_cat_shopping_cart);
        ICON_MAP.put("flight",                  R.drawable.ic_cat_flight);
        ICON_MAP.put("pets",                    R.drawable.ic_cat_pets);
        ICON_MAP.put("fitness_center",          R.drawable.ic_cat_fitness_center);
        ICON_MAP.put("local_bar",               R.drawable.ic_cat_local_bar);
        ICON_MAP.put("music_note",              R.drawable.ic_cat_music_note);
        ICON_MAP.put("phone_android",           R.drawable.ic_cat_phone_android);
        ICON_MAP.put("attach_money",            R.drawable.ic_cat_attach_money);
        ICON_MAP.put("credit_card",             R.drawable.ic_cat_credit_card);
        ICON_MAP.put("work",                    R.drawable.ic_cat_work);
        ICON_MAP.put("school",                  R.drawable.ic_cat_school);
        ICON_MAP.put("person",                  R.drawable.ic_cat_person);
        ICON_MAP.put("warning",                 R.drawable.ic_cat_warning);
        ICON_MAP.put("category",                R.drawable.ic_cat_category);
    }

    public static int getDrawableResId(String iconName) {
        Integer resId = ICON_MAP.get(iconName);
        return resId != null ? resId : R.drawable.ic_cat_category;
    }

    public static Map<String, Integer> getAllIcons() {
        return ICON_MAP;
    }
}
