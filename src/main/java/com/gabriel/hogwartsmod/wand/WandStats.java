package com.gabriel.hogwartsmod.wand;

public record WandStats(float damage, float range, float cooldown, float cost, float shield, float mastery) {
    public static final WandStats NEUTRAL = new WandStats(1f, 1f, 1f, 1f, 1f, 1f);

    public static WandStats of(WandWood wood, WandCore core, int quarters) {
        float damage = 1f;
        float range = 1f;
        float cooldown = 1f;
        float cost = 1f;
        float shield = 1f;
        float mastery = 1f;
        switch (core) {
            case FENIX -> {
                damage *= 1.12f;
                range *= 1.10f;
                cooldown *= 0.95f;
            }
            case DRAGAO -> {
                damage *= 1.20f;
                cost *= 1.10f;
            }
            case UNICORNIO -> {
                shield *= 1.20f;
                cost *= 0.85f;
                damage *= 0.90f;
            }
        }
        switch (wood) {
            case CARVALHO -> shield *= 1.05f;
            case AZEVINHO -> cooldown *= 0.95f;
            case TEIXO -> damage *= 1.06f;
            case SABUGUEIRO -> {
                damage *= 1.08f;
                cost *= 1.05f;
            }
            case EBANO -> range *= 1.06f;
            case VIDEIRA -> mastery *= 1.10f;
            case SALGUEIRO -> cost *= 0.94f;
            case CEREJEIRA -> {
                damage *= 1.04f;
                range *= 1.04f;
            }
            case NOGUEIRA -> {
                cooldown *= 0.97f;
                cost *= 0.97f;
            }
            case ACACIA -> {
                shield *= 1.05f;
                range *= 1.03f;
            }
            case ABETO -> {
                shield *= 1.08f;
                range *= 0.97f;
            }
            case FREIXO -> {
                damage *= 1.05f;
                shield *= 1.03f;
            }
        }
        float t = (Wands.clampLength(quarters) - Wands.MIN_LENGTH) / (float) (Wands.MAX_LENGTH - Wands.MIN_LENGTH);
        cooldown *= 0.94f + 0.12f * t;
        range *= 0.94f + 0.14f * t;
        return new WandStats(damage, range, cooldown, cost, shield, mastery);
    }
}
