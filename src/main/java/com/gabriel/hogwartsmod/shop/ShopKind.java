package com.gabriel.hogwartsmod.shop;

public enum ShopKind {
    LOJA, COMERCIANTE, CONTRABANDISTA;

    public static ShopKind of(int ordinal) {
        ShopKind[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : LOJA;
    }

    public static ShopKind of(String name) {
        for (ShopKind kind : values()) {
            if (kind.name().equalsIgnoreCase(name)) {
                return kind;
            }
        }
        return LOJA;
    }
}
