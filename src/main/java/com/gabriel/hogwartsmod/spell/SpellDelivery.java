package com.gabriel.hogwartsmod.spell;

public enum SpellDelivery {
    EM_SI_MESMO("cast_si_mesmo", 5),
    PROJETIL("cast_projetil", 5),
    FEIXE("cast_feixe", 0),
    ALVO_DIRETO("cast_alvo_direto", 6),
    AREA("cast_area", 14),
    PROTECAO("cast_protecao", 0);

    private final String animation;
    private final int releaseTick;

    SpellDelivery(String animation, int releaseTick) {
        this.animation = animation;
        this.releaseTick = releaseTick;
    }

    public String getAnimation() {
        return animation;
    }

    public int getReleaseTick() {
        return releaseTick;
    }

    public boolean isSustained() {
        return this == FEIXE || this == PROTECAO;
    }
}
