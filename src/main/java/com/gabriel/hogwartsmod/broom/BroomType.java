package com.gabriel.hogwartsmod.broom;

public enum BroomType {
    SPORT("vassoura_esportiva", 1.05f, 0.045f, 7.0f, 1.5f, 100f, 1),
    TRANSPORT("vassoura_transporte", 0.75f, 0.03f, 4.5f, 1.3f, 140f, 2);

    private final String id;
    private final float maxSpeed;
    private final float acceleration;
    private final float handling;
    private final float boost;
    private final float stamina;
    private final int seats;

    BroomType(String id, float maxSpeed, float acceleration, float handling, float boost, float stamina, int seats) {
        this.id = id;
        this.maxSpeed = maxSpeed;
        this.acceleration = acceleration;
        this.handling = handling;
        this.boost = boost;
        this.stamina = stamina;
        this.seats = seats;
    }

    public String getId() {
        return id;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public float getAcceleration() {
        return acceleration;
    }

    public float getHandling() {
        return handling;
    }

    public float getBoost() {
        return boost;
    }

    public float getStamina() {
        return stamina;
    }

    public int getSeats() {
        return seats;
    }

    public static BroomType byOrdinal(int ordinal) {
        BroomType[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}
