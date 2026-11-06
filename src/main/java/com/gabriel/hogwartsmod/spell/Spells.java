package com.gabriel.hogwartsmod.spell;

import com.gabriel.hogwartsmod.spell.impl.AccioSpell;
import com.gabriel.hogwartsmod.spell.impl.AparatacaoSpell;
import com.gabriel.hogwartsmod.spell.impl.ArrestoMomentumSpell;
import com.gabriel.hogwartsmod.spell.impl.EsquivaSpell;
import com.gabriel.hogwartsmod.spell.impl.ExpelliarmusSpell;
import com.gabriel.hogwartsmod.spell.impl.IncendioSpell;
import com.gabriel.hogwartsmod.spell.impl.LumosSpell;
import com.gabriel.hogwartsmod.spell.impl.ProtegoSpell;
import com.gabriel.hogwartsmod.spell.impl.StupefySpell;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Spells {
    private static final Map<String, Spell> REGISTRY = new LinkedHashMap<>();

    public static final LumosSpell LUMOS = register(new LumosSpell());
    public static final ProtegoSpell PROTEGO = register(new ProtegoSpell());
    public static final ExpelliarmusSpell EXPELLIARMUS = register(new ExpelliarmusSpell());
    public static final StupefySpell STUPEFY = register(new StupefySpell());
    public static final AccioSpell ACCIO = register(new AccioSpell());
    public static final IncendioSpell INCENDIO = register(new IncendioSpell());
    public static final ArrestoMomentumSpell ARRESTO_MOMENTUM = register(new ArrestoMomentumSpell());
    public static final EsquivaSpell ESQUIVA = register(new EsquivaSpell());
    public static final AparatacaoSpell APARATACAO = register(new AparatacaoSpell());

    private Spells() {
    }

    private static <T extends Spell> T register(T spell) {
        REGISTRY.put(spell.getId(), spell);
        return spell;
    }

    @Nullable
    public static Spell get(@Nullable String id) {
        return id == null ? null : REGISTRY.get(id);
    }

    public static Collection<Spell> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }
}
