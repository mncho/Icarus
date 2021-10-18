package dev.cammiescorner.icarus.common.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import dev.cammiescorner.icarus.Icarus;
import dev.cammiescorner.icarus.core.network.client.DeleteHungerMessage;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.Tag;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.caelus.api.CaelusApi;

import java.util.UUID;

public class WingItem extends TrinketItem {
	private final Multimap<EntityAttribute, EntityAttributeModifier> attributeModifiers;
	private final ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
	private final DyeColor primaryColour;
	private final DyeColor secondaryColour;
	private final WingType wingType;
	private boolean shouldSlowfall;
	private static final Tag<Item> FREE_FLIGHT = TagRegistry.item(new Identifier(Icarus.MOD_ID, "free_flight"));
	private static final Tag<Item> MELTS = TagRegistry.item(new Identifier(Icarus.MOD_ID, "melts"));

	/**
	 * The default constructor.
	 */
	public WingItem(DyeColor primaryColour, DyeColor secondaryColour, WingType wingType) {
		super(new Item.Settings().group(Icarus.ITEM_GROUP).maxCount(1).maxDamage(Icarus.getConfig().wingsDurability).rarity(wingType == WingType.UNIQUE ? Rarity.EPIC : Rarity.RARE));
		this.builder.put(CaelusApi.getInstance().getFlightAttribute(), new EntityAttributeModifier(UUID.fromString("7d9704a0-383f-11eb-adc1-0242ac120002"), "Flight", 1, EntityAttributeModifier.Operation.ADDITION));
		this.attributeModifiers = this.builder.build();
		this.primaryColour = primaryColour;
		this.secondaryColour = secondaryColour;
		this.wingType = wingType;
	}

	public boolean isUsable(ItemStack stack) {
		return stack.getDamage() < stack.getMaxDamage() - 1;
	}

	@Override
	public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
		if(entity instanceof PlayerEntity player) {
			if(player.getHungerManager().getFoodLevel() <= 6 || !isUsable(stack)) {
				stopFlying(player);
				return;
			}

			if(player.isFallFlying()) {
				if(player.forwardSpeed > 0)
					applySpeed(player);

				if(player.isSneaking())
					stopFlying(player);

				if(player.getPos().y > player.world.getHeight() + 64 && player.age % 2 == 0 && MELTS.contains(this))
					stack.damage(1, player, p -> p.sendEquipmentBreakStatus(EquipmentSlot.CHEST));
			}
			else {
				if(player.isOnGround() || player.isTouchingWater())
					shouldSlowfall = false;

				if(shouldSlowfall) {
					player.fallDistance = 0F;
					player.setVelocity(player.getVelocity().x, -0.4, player.getVelocity().z);
				}
			}
		}
	}

	@Override
	public Multimap<EntityAttribute, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, UUID uuid) {
		return this.attributeModifiers;
	}

	@Override
	public boolean canRepair(ItemStack stack, ItemStack ingredient) {
		return ingredient.isOf(Items.PHANTOM_MEMBRANE);
	}

	@Nullable
	@Override
	public SoundEvent getEquipSound() {
		return SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA;
	}

	public DyeColor getPrimaryColour() {
		return this.primaryColour;
	}

	public DyeColor getSecondaryColour() {
		return this.secondaryColour;
	}

	public WingType getWingType() {
		return this.wingType;
	}

	public void stopFlying(PlayerEntity player) {
		shouldSlowfall = true;
		player.stopFallFlying();
	}

	public void applySpeed(PlayerEntity player) {
		shouldSlowfall = false;
		Vec3d rotation = player.getRotationVector();
		Vec3d velocity = player.getVelocity();
		float modifier = Icarus.getConfig().armourSlows ? Math.max(1F, (player.getArmor() / 30F) * Icarus.getConfig().maxSlowedMultiplier) : 1F;
		System.out.println(modifier);

		player.setVelocity(velocity.add(rotation.x * (Icarus.getConfig().wingsSpeed / modifier) + (rotation.x * 1.5D - velocity.x) * (Icarus.getConfig().wingsAcceleration / modifier),
				rotation.y * (Icarus.getConfig().wingsSpeed / modifier) + (rotation.y * 1.5D - velocity.y) * (Icarus.getConfig().wingsAcceleration / modifier),
				rotation.z * (Icarus.getConfig().wingsSpeed / modifier) + (rotation.z * 1.5D - velocity.z) * (Icarus.getConfig().wingsAcceleration / modifier)));

		if(!FREE_FLIGHT.contains(this) && !player.isCreative())
			DeleteHungerMessage.send();
	}

	public enum WingType {
		FEATHERED, DRAGON, MECHANICAL_FEATHERED, MECHANICAL_LEATHER, LIGHT, UNIQUE
	}
}