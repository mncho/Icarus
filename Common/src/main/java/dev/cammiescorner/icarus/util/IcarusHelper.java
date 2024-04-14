package dev.cammiescorner.icarus.util;

import dev.cammiescorner.icarus.IcarusConfig;
import dev.cammiescorner.icarus.api.SlowFallingEntity;
import dev.cammiescorner.icarus.init.IcarusItemTags;
import dev.cammiescorner.icarus.item.WingItem;
import dev.cammiescorner.icarus.network.s2c.SyncConfigValuesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class IcarusHelper {

    public static Predicate<LivingEntity> hasWings = entity -> false;
    public static Function<LivingEntity, ItemStack> getEquippedWings = entity -> ItemStack.EMPTY;
    public static BiPredicate<LivingEntity, ItemStack> equipFunc = (entity, stack) -> false;

    public static boolean onFallFlyingTick(LivingEntity entity, @Nullable ItemStack wings, boolean tick) {
        if (wings != null) {
            if (!(wings.getItem() instanceof WingItem wingItem) || !wingItem.isUsable(entity, wings)) {
                if (entity instanceof Player player) {
                    stopFlying(player);
                }
                return false;
            }

            if (tick) {
                if ((IcarusConfig.canSlowFall && entity.isShiftKeyDown()) || entity.isUnderWater()) {
                    if (entity instanceof Player player) {
                        stopFlying(player);
                    }
                    return false;
                }

                if (!wings.is(IcarusItemTags.FREE_FLIGHT) && entity instanceof Player player && !player.isCreative()) {
                    player.getFoodData().addExhaustion(IcarusConfig.exhaustionAmount);
                    if (player.getFoodData().getFoodLevel() <= 6) {
                        stopFlying(player);
                        return false;
                    }
                }

                var ticks = entity.getFallFlyingTicks() + 1;
                if (!wingItem.onFlightTick(entity, wings, ticks)) {
                    if (entity instanceof Player player) {
                        stopFlying(player);
                    }
                    return false;
                }
            }
        }

        return true;
    }

    public static void onPlayerTick(Player player) {
        if (((SlowFallingEntity) player).icarus$isSlowFalling()) {
            player.fallDistance = 0F;

            if (player.onGround() || player.isInWater()) {
                ((SlowFallingEntity) player).icarus$setSlowFalling(false);
            } else {
                var move = player.getDeltaMovement();
                player.setDeltaMovement(move.x(), -0.4, move.z());
            }
        }
    }

    public static void stopFlying(Player player) {
        ((SlowFallingEntity) player).icarus$setSlowFalling(true);

        if (player.getXRot() < -90 || player.getXRot() > 90) {
            float offset = (player.getXRot() < -90 ? player.getXRot() + 180 : player.getXRot() - 180) * 2;
            player.setXRot((player.getXRot() < -90 ? 180 + offset : -180 - offset) + player.getXRot());
            player.setYRot(180 + player.getYRot());
        }
    }

    public static void onServerPlayerJoin(ServerPlayer player) {
        SyncConfigValuesPacket.send(player);
    }
}