package com.gabriel.hogwartsmod.spell;

public enum SpellDelivery {
    EM_SI_MESMO,
    PROJETIL,
    FEIXE,
    ALVO_DIRETO,
    AREA,
    PROTECAO;

    public boolean isSustained() {
        return this == FEIXE || this == PROTECAO;
    }
}
