package com.gabriel.hogwartsmod.shop;

import com.gabriel.hogwartsmod.npc.HogwartsNpc;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ShopNpcEntity extends HogwartsNpc {
    public static final int MAX_OFFERS = 8;
    private final List<ShopOffer> offers = new ArrayList<>();
    private ShopKind kind = ShopKind.LOJA;
    private long leaveAt = -1;

    public ShopNpcEntity(EntityType<? extends ShopNpcEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected String defaultModel() {
        return switch (kind) {
            case COMERCIANTE -> "comerciante";
            case CONTRABANDISTA -> "contrabandista";
            case AUXILIAR_VARINHAS -> "auxiliar_varinhas";
            case AUXILIAR_VASSOURAS -> "auxiliar_vassouras";
            default -> "mercador";
        };
    }

    public ShopKind getKind() {
        return kind;
    }

    public void setKind(ShopKind value) {
        kind = value;
    }

    public void setLeaveAt(long gameTime) {
        leaveAt = gameTime;
    }

    public boolean isWandering() {
        return leaveAt >= 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && leaveAt >= 0 && level().getGameTime() >= leaveAt) {
            ((net.minecraft.server.level.ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.02);
            level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 0.6f);
            discard();
        }
    }

    public List<ShopOffer> getOffers() {
        return List.copyOf(offers);
    }

    public void setOffers(List<ShopOffer> list) {
        offers.clear();
        for (ShopOffer offer : list) {
            if (offers.size() < MAX_OFFERS) {
                offers.add(offer.sanitized());
            }
        }
    }

    @Nullable
    public ShopOffer offer(String id) {
        return offers.stream().filter(o -> o.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            getLookControl().setLookAt(player);
            if (serverPlayer.isShiftKeyDown() && serverPlayer.hasPermissions(2)) {
                ShopManager.openEditor(serverPlayer, this);
            } else {
                talk();
                ShopManager.open(serverPlayer, this);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        offers.forEach(o -> list.add(o.save()));
        tag.put("Ofertas", list);
        tag.putString("Tipo", kind.name());
        tag.putLong("VaiEmbora", leaveAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        kind = ShopKind.of(tag.getString("Tipo"));
        leaveAt = tag.contains("VaiEmbora") ? tag.getLong("VaiEmbora") : -1;
        offers.clear();
        ListTag list = tag.getList("Ofertas", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            offers.add(ShopOffer.load(list.getCompound(i)));
        }
    }
}
