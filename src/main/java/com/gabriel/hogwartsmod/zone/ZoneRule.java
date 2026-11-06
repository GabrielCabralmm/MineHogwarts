package com.gabriel.hogwartsmod.zone;

import org.jetbrains.annotations.Nullable;

public enum ZoneRule {
    VOO("voo"),
    APARATACAO("aparatacao"),
    COMBATE("combate"),
    CRIATURAS("criaturas");

    private final String id;

    ZoneRule(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public String key() {
        return "zone.hogwartsmod.rule." + id;
    }

    public String blockedKey() {
        return "message.hogwartsmod.zone_blocked." + id;
    }

    @Nullable
    public static ZoneRule byId(String id) {
        for (ZoneRule rule : values()) {
            if (rule.id.equalsIgnoreCase(id)) {
                return rule;
            }
        }
        return null;
    }
}
