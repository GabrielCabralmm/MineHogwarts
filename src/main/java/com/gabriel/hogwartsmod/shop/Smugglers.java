package com.gabriel.hogwartsmod.shop;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicConfig;
import com.gabriel.hogwartsmod.registry.ModEntities;
import com.gabriel.hogwartsmod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public class Smugglers extends SavedData {
    private static final String NAME = HogwartsMod.MOD_ID + "_contrabandista";
    private static final int CHECK_TICKS = 2400;
    private final List<ShopOffer> template = new ArrayList<>();
    private boolean custom;

    public static Smugglers get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Smugglers::load, Smugglers::new, NAME);
    }

    public List<ShopOffer> template() {
        return custom ? List.copyOf(template) : smugglerDefaults();
    }

    public void setTemplate(List<ShopOffer> offers) {
        template.clear();
        template.addAll(offers);
        custom = true;
        setDirty();
    }

    // -- Ofertas padrão

    private static ShopOffer offer(String id, Item item, int g, int s) {
        return new ShopOffer(id, ShopOffer.Goods.of(item, 1), g, s, 0, List.of());
    }

    public static List<ShopOffer> merchantDefaults() {
        return List.of(
                offer("licenca", ModItems.LICENCA_CRIATURAS.get(), 5, 0),
                offer("bolsa", ModItems.BOLSA_CRIATURAS.get(), 2, 0),
                offer("coruja", ModItems.OVO_CORUJA.get(), 3, 0),
                offer("hipogrifo", ModItems.OVO_HIPOGRIFO.get(), 12, 0));
    }

    public static List<ShopOffer> wandHelperDefaults() {
        return List.of(offer("varinha", ModItems.CAIXA_VARINHA.get(), 7, 0));
    }

    public static List<ShopOffer> smugglerDefaults() {
        return List.of(
                offer("bolsa", ModItems.BOLSA_CRIATURAS.get(), 4, 0),
                offer("coruja", ModItems.OVO_CORUJA.get(), 8, 0),
                offer("hipogrifo", ModItems.OVO_HIPOGRIFO.get(), 25, 0),
                offer("testralio", ModItems.OVO_TESTRALIO.get(), 40, 0));
    }

    // -- Aparição natural

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (event.phase != TickEvent.Phase.END || server.getTickCount() % CHECK_TICKS != 0 || !MagicConfig.SMUGGLER_SPAWNS.get()) {
            return;
        }
        ServerLevel level = server.overworld();
        long time = level.getDayTime() % 24000;
        if (time < 13000 || time > 22500 || level.random.nextInt(100) >= MagicConfig.SMUGGLER_CHANCE.get()) {
            return;
        }
        if (!level.getEntities(ModEntities.SHOP_NPC.get(), e -> e.getKind() == ShopKind.CONTRABANDISTA && e.isWandering()).isEmpty()) {
            return;
        }
        List<ServerPlayer> players = level.players().stream().filter(p -> !p.isSpectator()).toList();
        if (!players.isEmpty()) {
            spawnNear(players.get(level.random.nextInt(players.size())));
        }
    }

    @Nullable
    public static ShopNpcEntity spawnNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int dist = 12 + random.nextInt(14);
            int x = player.getBlockX() + (int) (Math.cos(angle) * dist);
            int z = player.getBlockZ() + (int) (Math.sin(angle) * dist);
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getBlockState(pos.below()).isSolidRender(level, pos.below()) || !level.getFluidState(pos).isEmpty()
                    || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) {
                continue;
            }
            ShopNpcEntity npc = ModEntities.SHOP_NPC.get().create(level);
            if (npc == null) {
                return null;
            }
            npc.setKind(ShopKind.CONTRABANDISTA);
            npc.setOffers(get(level.getServer()).template());
            npc.setCustomName(Component.translatable("entity.hogwartsmod.contrabandista"));
            npc.setCustomNameVisible(true);
            float yaw = random.nextFloat() * 360f;
            npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
            npc.setYHeadRot(yaw);
            npc.setYBodyRot(yaw);
            npc.setLeaveAt(level.getGameTime() + MagicConfig.SMUGGLER_MINUTES.get() * 1200L);
            level.addFreshEntity(npc);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, npc.getX(), npc.getY() + 1, npc.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            player.sendSystemMessage(Component.translatable("message.hogwartsmod.smuggler_near").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return npc;
        }
        return null;
    }

    // -- NBT

    public static Smugglers load(CompoundTag tag) {
        Smugglers data = new Smugglers();
        data.custom = tag.getBoolean("Personalizado");
        ListTag list = tag.getList("Ofertas", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.template.add(ShopOffer.load(list.getCompound(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        template.forEach(o -> list.add(o.save()));
        tag.put("Ofertas", list);
        tag.putBoolean("Personalizado", custom);
        return tag;
    }
}
