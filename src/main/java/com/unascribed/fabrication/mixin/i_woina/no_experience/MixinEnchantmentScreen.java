package com.unascribed.fabrication.mixin.i_woina.no_experience;

import java.util.List;
import java.util.stream.Collectors;

import com.unascribed.fabrication.FabConf;
import com.unascribed.fabrication.support.injection.FabModifyConst;
import com.unascribed.fabrication.support.injection.FabModifyVariable;
import com.unascribed.fabrication.support.injection.Hijack;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import com.unascribed.fabrication.support.injection.FabModifyArg;
import com.unascribed.fabrication.support.injection.ModifyGetField;

import com.unascribed.fabrication.support.EligibleIf;
import com.unascribed.fabrication.support.Env;

import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.text.Text;


@Mixin(EnchantmentScreen.class)
@EligibleIf(configAvailable="*.no_experience", envMatches=Env.CLIENT)
public abstract class MixinEnchantmentScreen extends HandledScreen<EnchantmentScreenHandler> {

	public MixinEnchantmentScreen(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@ModifyGetField(target="net/minecraft/client/network/ClientPlayerEntity.experienceLevel:I",
			method={
					"drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V",
					"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"
	})
	private static int fabrication$amendExperienceLevel(int old) {
		if (FabConf.isEnabled("*.no_experience")) return 65535;
		return old;
	}

	@FabModifyArg(method="render(Lnet/minecraft/client/gui/DrawContext;IIF)V", index=1,
			at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V"))
	public List<Text> removeLevelText(List<Text> original){
		if (FabConf.isEnabled("*.no_experience")){
			original = original.stream().filter(text ->{
				if (text instanceof MutableText && text.getContent() instanceof TranslatableTextContent) {
					return !((TranslatableTextContent) text.getContent()).getKey().startsWith("container.enchant.level");
				}
				return true;
			}).collect(Collectors.toList());
		}
		return original;
	}

	@Hijack(target="Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V",
			method="drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V")
	public boolean fabrication$noXpHijackDrawTexture(DrawContext drawContext, Identifier texture, int x, int y) {
		if (FabConf.isEnabled("*.no_experience")) {
			if (texture.getPath().startsWith("container/enchanting_table/level_")) {
				if (!texture.getPath().endsWith("_disabled")) {
					drawContext.drawText(textRenderer, "" + Integer.parseInt(texture.getPath().substring("container/enchanting_table/level_".length())), x + 98, y + 8, 0x5577FF, true);
				}
				return true;
			}
		}
		return false;
	}

	@FabModifyVariable(at=@At(value="INVOKE", target="net/minecraft/client/font/TextRenderer.getWidth(Ljava/lang/String;)I", ordinal=0),
			method="drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V", ordinal=0)
	public String modifyLevelText(String orig) {
		if (FabConf.isEnabled("*.no_experience")) return "";
		return orig;
	}

	@FabModifyConst(constant=@Constant(intValue=20, ordinal=0),
			method="drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V", require=0)
	public int modifyPhraseOffset(int orig) {
		if (FabConf.isEnabled("*.no_experience")) return 3;
		return orig;
	}

}
