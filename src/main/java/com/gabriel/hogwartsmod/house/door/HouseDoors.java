package com.gabriel.hogwartsmod.house.door;

import com.gabriel.hogwartsmod.house.House;
import com.gabriel.hogwartsmod.network.HouseDoorResultPacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.OpenHouseDoorPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.network.PacketDistributor;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HouseDoors extends SavedData {
    private static final String NAME = "hogwartsmod_portas";
    public static final int RINGS = 4;
    public static final int SYMBOLS = 6;
    public static final int BARRELS = 6;
    public static final int RESULT_WRONG = 0;
    public static final int RESULT_OPEN = 1;
    public static final int RESULT_STEP = 2;

    public record Riddle(String question, String answer) {
    }

    private String password = "Caput Draconis";
    private int[] combination = {1, 4, 2, 6};
    private final List<Riddle> riddles = new ArrayList<>(List.of(
            new Riddle("O que vem primeiro, a fênix ou a chama?", "um circulo nao tem comeco"),
            new Riddle("Quanto mais se tira, maior fica. O que é?", "buraco"),
            new Riddle("Tem dentes mas não morde. O que é?", "pente")));
    private int[] sequence = {2, 5, 1, 6};

    public static HouseDoors get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(HouseDoors::load, HouseDoors::new, NAME);
    }

    // -- Configuração da staff

    public String password() {
        return password;
    }

    public void setPassword(String value) {
        password = value.strip();
        setDirty();
    }

    public int[] combination() {
        return combination.clone();
    }

    public void setCombination(int[] value) {
        combination = value.clone();
        setDirty();
    }

    public List<Riddle> riddles() {
        return List.copyOf(riddles);
    }

    public void addRiddle(String question, String answer) {
        riddles.add(new Riddle(question.strip(), answer.strip()));
        setDirty();
    }

    public boolean removeRiddle(int index) {
        if (index < 0 || index >= riddles.size()) {
            return false;
        }
        riddles.remove(index);
        setDirty();
        return true;
    }

    public int[] sequence() {
        return sequence.clone();
    }

    public void setSequence(int[] value) {
        sequence = value.clone();
        setDirty();
    }

    // -- Abrir a tela

    public static void openScreen(ServerPlayer player, BlockPos pos, House house) {
        HouseDoors doors = get(player.server);
        int a = 0;
        int b = 0;
        String text = "";
        switch (house) {
            case GRIFINORIA -> player.level().playSound(null, pos.above(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.BLOCKS, 1f, 1.9f);
            case SONSERINA -> {
                a = RINGS;
                b = SYMBOLS;
            }
            case CORVINAL -> {
                if (doors.riddles.isEmpty()) {
                    a = -1;
                } else {
                    a = player.getRandom().nextInt(doors.riddles.size());
                    text = doors.riddles.get(a).question();
                }
                player.level().playSound(null, pos.above(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 0.25f, 1.8f);
            }
            case LUFA_LUFA -> a = doors.sequence.length;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenHouseDoorPacket(pos, house, a, b, text));
    }

    // -- Tentativas

    public static void attempt(ServerPlayer player, BlockPos pos, String text, int[] values, int extra) {
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof HouseDoorBlock door) || state.getValue(HouseDoorBlock.OPEN) || !door.isMember(player)
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5) > 100) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HouseDoorResultPacket(RESULT_WRONG));
            return;
        }
        HouseDoors doors = get(player.server);
        int result = switch (door.getHouse()) {
            case GRIFINORIA -> normalize(text).equals(normalize(doors.password)) ? RESULT_OPEN : RESULT_WRONG;
            case SONSERINA -> Arrays.equals(values, doors.combination) ? RESULT_OPEN : RESULT_WRONG;
            case CORVINAL -> extra >= 0 && extra < doors.riddles.size() && normalize(text).equals(normalize(doors.riddles.get(extra).answer()))
                    ? RESULT_OPEN : RESULT_WRONG;
            case LUFA_LUFA -> barrels(doors.sequence, values);
        };
        if (result == RESULT_OPEN) {
            HouseDoorBlock.setOpen(player.serverLevel(), pos, true);
            playOpen(player.serverLevel(), pos, door.getHouse());
        } else if (result == RESULT_WRONG && door.getHouse() == House.LUFA_LUFA) {
            player.level().playSound(null, pos, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 1f, 0.6f);
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HouseDoorResultPacket(result));
    }

    private static int barrels(int[] expected, int[] hits) {
        if (hits.length == 0 || hits.length > expected.length) {
            return RESULT_WRONG;
        }
        for (int i = 0; i < hits.length; i++) {
            if (hits[i] != expected[i]) {
                return RESULT_WRONG;
            }
        }
        return hits.length == expected.length ? RESULT_OPEN : RESULT_STEP;
    }

    public static String normalize(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").strip();
    }

    // -- Sons

    public static void playOpen(ServerLevel level, BlockPos pos, House house) {
        BlockPos at = pos.above();
        switch (house) {
            case GRIFINORIA -> {
                level.playSound(null, at, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.BLOCKS, 1f, 1.7f);
                level.playSound(null, at, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1f, 0.7f);
            }
            case CORVINAL -> level.playSound(null, at, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1f, 0.9f);
            case LUFA_LUFA -> level.playSound(null, at, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1f, 0.8f);
        }
    }

    public static void playClose(ServerLevel level, BlockPos pos, House house) {
        SoundEvent sound = switch (house) {
            case SONSERINA -> SoundEvents.PISTON_CONTRACT;
            case LUFA_LUFA -> SoundEvents.BARREL_CLOSE;
            default -> SoundEvents.WOODEN_DOOR_CLOSE;
        };
        level.playSound(null, pos.above(), sound, SoundSource.BLOCKS, 0.8f, house == House.SONSERINA ? 0.5f : 0.9f);
    }

    // -- Salvamento

    public static HouseDoors load(CompoundTag tag) {
        HouseDoors doors = new HouseDoors();
        if (tag.contains("Senha")) {
            doors.password = tag.getString("Senha");
        }
        if (tag.contains("Combinacao")) {
            doors.combination = tag.getIntArray("Combinacao");
        }
        if (tag.contains("Sequencia")) {
            doors.sequence = tag.getIntArray("Sequencia");
        }
        if (tag.contains("Charadas")) {
            doors.riddles.clear();
            ListTag list = tag.getList("Charadas", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                doors.riddles.add(new Riddle(entry.getString("Pergunta"), entry.getString("Resposta")));
            }
        }
        return doors;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("Senha", password);
        tag.put("Combinacao", new IntArrayTag(combination));
        tag.put("Sequencia", new IntArrayTag(sequence));
        ListTag list = new ListTag();
        for (Riddle riddle : riddles) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Pergunta", riddle.question());
            entry.putString("Resposta", riddle.answer());
            list.add(entry);
        }
        tag.put("Charadas", list);
        return tag;
    }
}
