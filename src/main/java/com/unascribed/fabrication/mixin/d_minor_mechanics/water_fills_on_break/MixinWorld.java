package com.unascribed.fabrication.mixin.d_minor_mechanics.water_fills_on_break;

import com.unascribed.fabrication.FabConf;
import com.unascribed.fabrication.support.injection.FabInject;
import net.minecraft.fluid.FlowableFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.unascribed.fabrication.logic.WaterFillsOnBreak;
import com.unascribed.fabrication.support.EligibleIf;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(World.class)
@EligibleIf(anyConfigAvailable={"*.water_fills_on_break", "*.water_fills_on_break_strict"})
public class MixinWorld {

	@FabInject(at=@At("HEAD"), method="removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z", cancellable=true)
	public void removeBlock(BlockPos pos, boolean move, CallbackInfoReturnable<Boolean> ci) {
		if (FabConf.isAnyEnabled("*.water_fills_on_break")) {
			World self = (World)(Object)this;
			FlowableFluid fluid = WaterFillsOnBreak.shouldFill(self, pos);
			if (fluid != null) {
				ci.setReturnValue(self.setBlockState(pos, fluid.getStill(false).getBlockState(), 3 | (move ? 64 : 0)));
			}
		}
	}

	@FabInject(at=@At("RETURN"), method="breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;I)Z", cancellable=true)
	public void breakBlock(BlockPos pos, boolean drop, Entity breakingEntity, int maxUpdateDepth, CallbackInfoReturnable<Boolean> ci) {
		if (FabConf.isAnyEnabled("*.water_fills_on_break") && ci.getReturnValueZ()) {
			World self = (World)(Object)this;
			FlowableFluid fluid = WaterFillsOnBreak.shouldFill(self, pos);
			if (fluid != null) {
				ci.setReturnValue(self.setBlockState(pos, fluid.getStill(false).getBlockState(), 3));
			}
		}
	}


}
