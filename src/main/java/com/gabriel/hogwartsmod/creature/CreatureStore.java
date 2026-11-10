package com.gabriel.hogwartsmod.creature;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CreatureStore {
    private final Map<Species, Entry> entries = new LinkedHashMap<>();

    public record Entry(Species species, CompoundTag data, float maxHealth, @Nullable UUID summoned) {
        public float health() {
            return data.getFloat("Health");
        }

        public String name() {
            if (data.contains("CustomName")) {
                Component custom = Component.Serializer.fromJson(data.getString("CustomName"));
                if (custom != null) {
                    return custom.getString();
                }
            }
            return species.getDisplayName().getString();
        }

        public Entry withSummoned(@Nullable UUID id) {
            return new Entry(species, data, maxHealth, id);
        }
    }

    public List<Entry> all() {
        return new ArrayList<>(entries.values());
    }

    @Nullable
    public Entry get(Species species) {
        return entries.get(species);
    }

    public void put(Entry entry) {
        entries.put(entry.species(), entry);
    }

    public void remove(Species species) {
        entries.remove(species);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("Especie", e.species().getId());
            t.put("Dados", e.data());
            t.putFloat("VidaMax", e.maxHealth());
            if (e.summoned() != null) {
                t.putUUID("Invocada", e.summoned());
            }
            list.add(t);
        }
        tag.put("Lista", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        entries.clear();
        ListTag list = tag.getList("Lista", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Species species = Species.byId(t.getString("Especie"));
            if (species != null) {
                entries.put(species, new Entry(species, t.getCompound("Dados"), t.getFloat("VidaMax"), t.hasUUID("Invocada") ? t.getUUID("Invocada") : null));
            }
        }
    }
}
