package com.gabriel.hogwartsmod.magic;

import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class MagicData {
    public static final Capability<MagicData> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final int SLOT_COUNT = 6;

    private float reserve = -1f;
    private int level = 1;
    private int xp;
    private final Set<String> learned = new LinkedHashSet<>();
    private final Map<String, Integer> masteryUses = new HashMap<>();
    private final String[] slots = new String[SLOT_COUNT];
    private int activeSlot;
    private final Map<String, long[]> cooldowns = new HashMap<>();
    private final Map<String, Long> teachReadyAt = new HashMap<>();
    private Profession profession = Profession.NONE;
    private final com.gabriel.hogwartsmod.mission.MissionProgress missions = new com.gabriel.hogwartsmod.mission.MissionProgress();
    private final com.gabriel.hogwartsmod.creature.CreatureStore creatures = new com.gabriel.hogwartsmod.creature.CreatureStore();
    @org.jetbrains.annotations.Nullable
    private com.gabriel.hogwartsmod.house.House house;
    private long lumosUntil;
    @Nullable
    private BlockPos lumosLight;
    private boolean lumosAnnounced;

    private boolean shielding;
    private float shieldHp;
    private long shieldStart;
    private long lastIncendioMastery;
    @Nullable
    private String pendingSpell;
    private long pendingAt;
    private boolean pendingLevelUp;
    private int lives = -1;
    private boolean seenDeath;
    private boolean firstWand;
    private boolean dirty = true;
    private boolean forceSync = true;

    @Nullable
    public static MagicData get(Player player) {
        return player.getCapability(CAPABILITY).resolve().orElse(null);
    }

    // -- Reserva e nível

    public float getMaxReserve() {
        return (float) (MagicConfig.RESERVE_BASE.get() + MagicConfig.RESERVE_PER_LEVEL.get() * (level - 1));
    }

    public float getReserve() {
        if (reserve < 0) {
            reserve = getMaxReserve();
        }
        return reserve;
    }

    public void setReserve(float value) {
        reserve = Math.max(0, Math.min(getMaxReserve(), value));
        dirty = true;
    }

    public boolean spend(float amount) {
        if (getReserve() < amount) {
            return false;
        }
        setReserve(getReserve() - amount);
        return true;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int value) {
        level = Math.max(1, Math.min(MagicConfig.MAX_LEVEL.get(), value));
        xp = 0;
        setReserve(getReserve());
        markForSync();
    }

    public int getXp() {
        return xp;
    }

    public static int xpForNextLevel(int level) {
        return (int) Math.round(50 * Math.pow(level, 1.5));
    }

    public boolean addXp(int amount) {
        if (amount <= 0 || level >= MagicConfig.MAX_LEVEL.get()) {
            return false;
        }
        xp += amount;
        boolean leveledUp = false;
        while (level < MagicConfig.MAX_LEVEL.get() && xp >= xpForNextLevel(level)) {
            xp -= xpForNextLevel(level);
            level++;
            leveledUp = true;
            pendingLevelUp = true;
        }
        markForSync();
        return leveledUp;
    }

    public boolean consumeLevelUp() {
        boolean value = pendingLevelUp;
        pendingLevelUp = false;
        return value;
    }

    // -- Feitiços aprendidos e maestria

    public boolean knows(Spell spell) {
        return learned.contains(spell.getId());
    }

    public Set<String> getLearned() {
        return learned;
    }

    public void learn(Spell spell) {
        if (learned.add(spell.getId())) {
            for (int i = 0; i < SLOT_COUNT && !spell.isAbility(); i++) {
                if (slots[i] == null) {
                    slots[i] = spell.getId();
                    break;
                }
            }
            markForSync();
        }
    }

    public void forget(Spell spell) {
        learned.remove(spell.getId());
        masteryUses.remove(spell.getId());
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (spell.getId().equals(slots[i])) {
                slots[i] = null;
            }
        }
        markForSync();
    }

    public int getMasteryUses(Spell spell) {
        return masteryUses.getOrDefault(spell.getId(), 0);
    }

    public int getMastery(Spell spell) {
        int uses = getMasteryUses(spell);
        int mastery = 0;
        for (int l = 1; l <= spell.getMaxMastery(); l++) {
            if (uses >= spell.getMasteryBase() * l * (l + 1) / 2) {
                mastery = l;
            }
        }
        return mastery;
    }

    public boolean hasMaxMastery(Spell spell) {
        return getMastery(spell) >= spell.getMaxMastery();
    }

    public void setMastery(Spell spell, int mastery) {
        int l = Math.max(0, Math.min(spell.getMaxMastery(), mastery));
        masteryUses.put(spell.getId(), spell.getMasteryBase() * l * (l + 1) / 2);
        markForSync();
    }

    public boolean addMastery(Spell spell, int uses, int xpGain) {
        masteryUses.merge(spell.getId(), uses, Integer::sum);
        markForSync();
        return addXp(xpGain);
    }

    // -- Atalhos

    @Nullable
    public Spell getSlot(int index) {
        return Spells.get(slots[index]);
    }

    public void setSlot(int index, @Nullable Spell spell) {
        if (spell != null && spell.isAbility()) {
            spell = null;
        }
        slots[index] = spell == null ? null : spell.getId();
        markForSync();
    }

    public int getActiveSlot() {
        return activeSlot;
    }

    public void setActiveSlot(int index) {
        activeSlot = Math.floorMod(index, SLOT_COUNT);
        markForSync();
    }

    @Nullable
    public Spell getActiveSpell() {
        return getSlot(activeSlot);
    }

    // -- Cooldowns

    public void setCooldown(Spell spell, long gameTime, int ticks) {
        if (ticks > 0) {
            cooldowns.put(spell.getId(), new long[]{gameTime + ticks, ticks});
            markForSync();
        }
    }

    public int getCooldownRemaining(Spell spell, long gameTime) {
        long[] cd = cooldowns.get(spell.getId());
        return cd == null ? 0 : (int) Math.max(0, cd[0] - gameTime);
    }

    public int getCooldownTotal(Spell spell) {
        long[] cd = cooldowns.get(spell.getId());
        return cd == null ? 0 : (int) cd[1];
    }

    // -- Esquiva

    private long dodgeUntil;
    private long invulnerableUntil;

    private boolean dodgeActive;

    public void startDodge(long now, int ticks, int invulnerableTicks) {
        dodgeUntil = now + ticks;
        invulnerableUntil = now + invulnerableTicks;
        dodgeActive = true;
    }

    public boolean consumeDodgeEnd(long now) {
        if (dodgeActive && now >= dodgeUntil) {
            dodgeActive = false;
            return true;
        }
        return false;
    }

    public boolean isDodging(long now) {
        return now < dodgeUntil;
    }

    public boolean isInvulnerable(long now) {
        return now < invulnerableUntil;
    }

    // -- Aparatação

    private final java.util.List<com.gabriel.hogwartsmod.apparition.ApparitionPoint> places = new java.util.ArrayList<>();
    private boolean apparitionLicense;
    @Nullable
    private net.minecraft.world.phys.Vec3 castTarget;
    private float castYaw;
    private long castEnd;
    @Nullable
    private net.minecraft.world.phys.Vec3 castFrom;

    public java.util.List<com.gabriel.hogwartsmod.apparition.ApparitionPoint> getPlaces() {
        return java.util.List.copyOf(places);
    }

    public void addPlace(com.gabriel.hogwartsmod.apparition.ApparitionPoint point) {
        places.add(point);
        markForSync();
    }

    public void renamePlace(int index, String name) {
        if (index >= 0 && index < places.size()) {
            places.set(index, places.get(index).withName(name));
            markForSync();
        }
    }

    public void removePlace(int index) {
        if (index >= 0 && index < places.size()) {
            places.remove(index);
            markForSync();
        }
    }

    public boolean hasApparitionLicense() {
        return apparitionLicense;
    }

    public void setApparitionLicense(boolean value) {
        apparitionLicense = value;
        markForSync();
    }

    public void startCast(net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 target, float yaw, long end) {
        castFrom = from;
        castTarget = target;
        castYaw = yaw;
        castEnd = end;
    }

    public boolean isCasting() {
        return castTarget != null;
    }

    @Nullable
    public net.minecraft.world.phys.Vec3 getCastTarget() {
        return castTarget;
    }

    @Nullable
    public net.minecraft.world.phys.Vec3 getCastFrom() {
        return castFrom;
    }

    public float getCastYaw() {
        return castYaw;
    }

    public long getCastEnd() {
        return castEnd;
    }

    public void clearCast() {
        castTarget = null;
        castFrom = null;
    }

    // -- Ensino e profissão

    public Profession getProfession() {
        return profession;
    }

    public void setProfession(Profession value) {
        profession = value;
        markForSync();
    }

    public com.gabriel.hogwartsmod.creature.CreatureStore getCreatures() {
        return creatures;
    }

    public com.gabriel.hogwartsmod.mission.MissionProgress getMissions() {
        return missions;
    }

    public boolean isProfessor() {
        return profession == Profession.PROFESSOR;
    }

    @org.jetbrains.annotations.Nullable
    public com.gabriel.hogwartsmod.house.House getHouse() {
        return house;
    }

    public void setHouse(@org.jetbrains.annotations.Nullable com.gabriel.hogwartsmod.house.House value) {
        house = value;
        markForSync();
    }

    public void setTeachCooldown(Spell spell, long readyAt) {
        teachReadyAt.put(spell.getId(), readyAt);
        markForSync();
    }

    public int getTeachCooldownRemaining(Spell spell, long gameTime) {
        Long readyAt = teachReadyAt.get(spell.getId());
        return readyAt == null ? 0 : (int) Math.max(0, readyAt - gameTime);
    }

    // -- Conjuração com animação

    public boolean hasPendingCast() {
        return pendingSpell != null;
    }

    public void schedule(Spell spell, long gameTime) {
        pendingSpell = spell.getId();
        pendingAt = gameTime;
    }

    @Nullable
    public Spell pollPendingCast(long gameTime) {
        if (pendingSpell == null || gameTime < pendingAt) {
            return null;
        }
        Spell spell = Spells.get(pendingSpell);
        pendingSpell = null;
        return spell;
    }

    public void clearPendingCast() {
        pendingSpell = null;
    }

    // -- Lumos

    public long getLumosUntil() {
        return lumosUntil;
    }

    public void setLumosUntil(long value) {
        lumosUntil = value;
        lumosAnnounced = value > 0;
        markForSync();
    }

    public boolean isLumosAnnounced() {
        return lumosAnnounced;
    }

    public void setLumosAnnounced(boolean value) {
        lumosAnnounced = value;
    }

    @Nullable
    public BlockPos getLumosLight() {
        return lumosLight;
    }

    public void setLumosLight(@Nullable BlockPos pos) {
        lumosLight = pos;
    }

    // -- Escudo

    public boolean isShielding() {
        return shielding;
    }

    public void startShield(float hp, long gameTime) {
        shielding = true;
        shieldHp = hp;
        shieldStart = gameTime;
    }

    public void stopShield() {
        shielding = false;
        shieldHp = 0;
    }

    public float getShieldHp() {
        return shieldHp;
    }

    public void setShieldHp(float hp) {
        shieldHp = hp;
    }

    public long getShieldStart() {
        return shieldStart;
    }

    public long getLastIncendioMastery() {
        return lastIncendioMastery;
    }

    public void setLastIncendioMastery(long value) {
        lastIncendioMastery = value;
    }

    // -- Vidas e testrálios

    public int getLives() {
        if (lives < 0) {
            lives = MagicConfig.LIVES_START.get();
        }
        return lives;
    }

    public void setLives(int value) {
        lives = Math.max(0, value);
        markForSync();
    }

    public boolean hasFirstWand() {
        return firstWand;
    }

    public void setFirstWand(boolean value) {
        firstWand = value;
        markForSync();
    }

    public boolean hasSeenDeath() {
        return seenDeath;
    }

    public void setSeenDeath(boolean value) {
        seenDeath = value;
        markForSync();
    }

    // -- Sincronização

    public void markForSync() {
        dirty = true;
        forceSync = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isForceSync() {
        return forceSync;
    }

    public void clearSync() {
        dirty = false;
        forceSync = false;
    }

    // -- NBT

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Reserve", getReserve());
        tag.putInt("Level", level);
        tag.putInt("Xp", xp);
        ListTag learnedTag = new ListTag();
        learned.forEach(id -> learnedTag.add(StringTag.valueOf(id)));
        tag.put("Learned", learnedTag);
        CompoundTag masteryTag = new CompoundTag();
        masteryUses.forEach(masteryTag::putInt);
        tag.put("Mastery", masteryTag);
        CompoundTag slotsTag = new CompoundTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slots[i] != null) {
                slotsTag.putString(String.valueOf(i), slots[i]);
            }
        }
        tag.put("Slots", slotsTag);
        tag.putInt("ActiveSlot", activeSlot);
        CompoundTag cooldownTag = new CompoundTag();
        cooldowns.forEach((id, cd) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("ReadyAt", cd[0]);
            entry.putLong("Total", cd[1]);
            cooldownTag.put(id, entry);
        });
        tag.put("Cooldowns", cooldownTag);
        CompoundTag teachTag = new CompoundTag();
        teachReadyAt.forEach(teachTag::putLong);
        tag.put("TeachCooldowns", teachTag);
        tag.putString("Profession", profession.getId());
        tag.put("Missoes", missions.save());
        tag.put("Criaturas", creatures.save());
        if (house != null) {
            tag.putString("House", house.getId());
        }
        net.minecraft.nbt.ListTag placeTag = new net.minecraft.nbt.ListTag();
        places.forEach(p -> placeTag.add(p.save()));
        tag.put("Places", placeTag);
        tag.putBoolean("ApparitionLicense", apparitionLicense);
        tag.putInt("Vidas", lives);
        tag.putBoolean("PresenciouMorte", seenDeath);
        tag.putBoolean("PrimeiraVarinha", firstWand);
        tag.putLong("LumosUntil", lumosUntil);
        if (lumosLight != null) {
            tag.put("LumosLight", NbtUtils.writeBlockPos(lumosLight));
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        level = Math.max(1, tag.getInt("Level"));
        xp = tag.getInt("Xp");
        learned.clear();
        tag.getList("Learned", Tag.TAG_STRING).forEach(t -> learned.add(t.getAsString()));
        masteryUses.clear();
        CompoundTag masteryTag = tag.getCompound("Mastery");
        masteryTag.getAllKeys().forEach(id -> masteryUses.put(id, masteryTag.getInt(id)));
        CompoundTag slotsTag = tag.getCompound("Slots");
        for (int i = 0; i < SLOT_COUNT; i++) {
            String key = String.valueOf(i);
            slots[i] = slotsTag.contains(key) ? slotsTag.getString(key) : null;
        }
        activeSlot = Math.floorMod(tag.getInt("ActiveSlot"), SLOT_COUNT);
        cooldowns.clear();
        CompoundTag cooldownTag = tag.getCompound("Cooldowns");
        cooldownTag.getAllKeys().forEach(id -> {
            CompoundTag entry = cooldownTag.getCompound(id);
            cooldowns.put(id, new long[]{entry.getLong("ReadyAt"), entry.getLong("Total")});
        });
        teachReadyAt.clear();
        CompoundTag teachTag = tag.getCompound("TeachCooldowns");
        teachTag.getAllKeys().forEach(id -> teachReadyAt.put(id, teachTag.getLong(id)));
        places.clear();
        net.minecraft.nbt.ListTag placeTag = tag.getList("Places", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < placeTag.size(); i++) {
            places.add(com.gabriel.hogwartsmod.apparition.ApparitionPoint.load(placeTag.getCompound(i)));
        }
        apparitionLicense = tag.getBoolean("ApparitionLicense");
        lives = tag.contains("Vidas") ? tag.getInt("Vidas") : -1;
        seenDeath = tag.getBoolean("PresenciouMorte");
        firstWand = tag.getBoolean("PrimeiraVarinha");
        Profession loaded = Profession.byId(tag.getString("Profession"));
        profession = loaded == null ? Profession.NONE : loaded;
        missions.load(tag.getCompound("Missoes"));
        creatures.load(tag.getCompound("Criaturas"));
        house = com.gabriel.hogwartsmod.house.House.byId(tag.getString("House"));
        lumosUntil = tag.getLong("LumosUntil");
        lumosLight = tag.contains("LumosLight") ? NbtUtils.readBlockPos(tag.getCompound("LumosLight")) : null;
        reserve = tag.contains("Reserve") ? tag.getFloat("Reserve") : -1f;
    }

    public void copyFrom(MagicData other) {
        load(other.save());
        lumosUntil = 0;
        lumosLight = null;
        markForSync();
    }
}
