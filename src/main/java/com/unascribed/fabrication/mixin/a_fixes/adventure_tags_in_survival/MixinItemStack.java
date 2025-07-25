package com.unascribed.fabrication.mixin.a_fixes.adventure_tags_in_survival;

import com.unascribed.fabrication.FabConf;
import com.unascribed.fabrication.support.injection.FabInject;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.unascribed.fabrication.support.EligibleIf;

import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

@Mixin(ItemStack.class)
@EligibleIf(configAvailable="*.adventure_tags_in_survival")
public class MixinItemStack {

	@FabInject(at=@At("HEAD"), method="useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
			cancellable=true)
	public void useOnBlock(ItemUsageContext iuc, CallbackInfoReturnable<ActionResult> ci) {
		if (!FabConf.isEnabled("*.adventure_tags_in_survival")) return;
		PlayerEntity player = iuc.getPlayer();
		if (player != null && (player.getAbilities().creativeMode || !player.getAbilities().allowModifyWorld)) return;
		ItemStack self = (ItemStack)(Object)this;
		if (!self.isEmpty()) {
			if (self.contains(DataComponentTypes.CUSTOM_DATA) && self.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).getNbt().contains("CanPlaceOn")) {
				boolean able = self.canPlaceOn(new CachedBlockPosition(iuc.getWorld(), iuc.getBlockPos(), false));
				if (!able) {
					ci.setReturnValue(ActionResult.PASS);
				}
			}
		}
	}

}
