package com.gabriel.hogwartsmod.mission;

public enum ObjectiveType {
    ENTREGAR("entregar"),
    DERROTAR("derrotar"),
    IR("ir");

    private final String id;

    ObjectiveType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String key() {
        return "mission.hogwartsmod.type." + id;
    }

    public static ObjectiveType byId(String id) {
        for (ObjectiveType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return ENTREGAR;
    }

    public ObjectiveType next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
