package com.gabriel.hogwartsmod.teach;

public enum StudentStatus {
    CAN_LEARN,
    KNOWS,
    LOW_LEVEL,
    INVITED,
    IN_LESSON;

    public String key() {
        return "teach.hogwartsmod.status." + name().toLowerCase();
    }
}
