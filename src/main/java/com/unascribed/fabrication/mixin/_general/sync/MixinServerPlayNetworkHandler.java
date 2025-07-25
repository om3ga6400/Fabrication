package com.unascribed.fabrication.mixin._general.sync;

import com.unascribed.fabrication.EarlyAgnos;
import com.unascribed.fabrication.FabConf;
import com.unascribed.fabrication.FabricationMod;
import com.unascribed.fabrication.FeaturesFile;
import com.unascribed.fabrication.features.FeatureHideArmor;
import com.unascribed.fabrication.interfaces.SetFabricationConfigAware;
import com.unascribed.fabrication.loaders.LoaderFScript;
import com.unascribed.fabrication.support.OptionalFScript;
import com.unascribed.fabrication.support.injection.FabInject;
import com.unascribed.fabrication.util.ByteBufCustomPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {

	@FabInject(at=@At("HEAD"), method="onCustomPayload(Lnet/minecraft/network/packet/c2s/common/CustomPayloadC2SPacket;)V", cancellable=true)
	public void fabrication$onCustomPayload(CustomPayloadC2SPacket packet, CallbackInfo ci) {
		Object self = this;
		if (!(self instanceof ServerPlayNetworkHandler)) return;
		ServerPlayerEntity player = ((ServerPlayNetworkHandler) self).getPlayer();
		CustomPayload payload = packet.payload();
		if (!(payload instanceof ByteBufCustomPayload)) return;
		Identifier channel = ((ByteBufCustomPayload) payload).id();
		if (channel.getNamespace().equals("fabrication")) {
			if (channel.getPath().equals("config")) {
				ci.cancel();
				PacketByteBuf recvdData = ((ByteBufCustomPayload) payload).buf();
				int id = recvdData.readVarInt();
				if (id == 0) {
					// hello
					int reqVer = 0;
					if (recvdData.isReadable(4)) reqVer = recvdData.readVarInt();
					if (player instanceof SetFabricationConfigAware) {
						((SetFabricationConfigAware) player).fabrication$setReqVer(reqVer);
						FabricationMod.sendConfigUpdate(player.server, null, player, reqVer);
						if (FabConf.isEnabled("*.hide_armor")) {
							FeatureHideArmor.sendSuppressedSlotsForSelf(player);
						}
					}
				} else if (id == 1 || id == 2) {
					// set
					if (player.hasPermissionLevel(2)) {
						String key = recvdData.readString(32767);
						if (FabConf.isValid(key)) {
							String value = recvdData.readString(32767);
							if (id == 1) FabConf.set(key, value);
							else FabConf.worldSet(key, value);
							if (FabricationMod.isAvailableFeature(key)) {
								FabricationMod.updateFeature(key);
							}
							FabricationMod.sendConfigUpdate(player.server, key);
							fabrication$sendCommandFeedback(
									Text.translatable("chat.type.admin", player.getDisplayName(), Text.literal(key + " is now set to " + value))
									.formatted(Formatting.GRAY, Formatting.ITALIC));
						}
					}
				}
			} else if (channel.getPath().equals("fscript")) {
				ci.cancel();
				PacketByteBuf recvdData = ((ByteBufCustomPayload) payload).buf();
				int id = recvdData.readVarInt();
				if(id == 0){
					// get
					String key = recvdData.readString(32767);
					if (FabConf.isValid(key)) {
						PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
						data.writeVarInt(0);
						data.writeString(LoaderFScript.get(key));
						player.networkHandler.sendPacket(new CustomPayloadS2CPacket(new ByteBufCustomPayload(Identifier.of("fabrication", "fscript"), data)));
					}
				}else if (id == 1) {
					// set
					if (player.hasPermissionLevel(2)) {
						String key = FabConf.remap(recvdData.readString(32767));
						if (FabConf.isValid(key) && FeaturesFile.get(key).fscript != null) {
							String value = recvdData.readString(32767);
							if (EarlyAgnos.isModLoaded("fscript") && OptionalFScript.set(key, value, player)) {
								fabrication$sendCommandFeedback(
										Text.translatable("chat.type.admin", player.getDisplayName(), Text.literal(key + " script is now set to " + value))
										.formatted(Formatting.GRAY, Formatting.ITALIC));
							}
						}
					}
				}else if (id == 2) {
					// unset
					if (player.hasPermissionLevel(2)) {
						String key = FabConf.remap(recvdData.readString(32767));
						if (FabConf.isValid(key) && FeaturesFile.get(key).fscript != null && EarlyAgnos.isModLoaded("fscript")) {
							OptionalFScript.restoreDefault(key);
							fabrication$sendCommandFeedback(
									Text.translatable("chat.type.admin", player.getDisplayName(), Text.literal(key + " script has been unset"))
									.formatted(Formatting.GRAY, Formatting.ITALIC));
						}
					}
				}else if (id == 3) {
					// TODO currently unused
					// reload
					if (player.hasPermissionLevel(2)) {
						LoaderFScript.reload();
						if (EarlyAgnos.isModLoaded("fscript")) OptionalFScript.reload();
						fabrication$sendCommandFeedback(
								Text.translatable("chat.type.admin", player.getDisplayName(), Text.literal(" scripts have been reloaded"))
								.formatted(Formatting.GRAY, Formatting.ITALIC));
					}
				}
				// TODO id 4 world local SET
			}
		}
	}
	public void fabrication$sendCommandFeedback(Text text){
		Object self = this;
		if (!(self instanceof ServerPlayNetworkHandler)) return;
		ServerPlayerEntity player = ((ServerPlayNetworkHandler) self).getPlayer();
		if (player.server.getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK)) {
			for (ServerPlayerEntity spe : player.server.getPlayerManager().getPlayerList()) {
				if (player.server.getPlayerManager().isOperator(spe.getGameProfile())) {
					spe.sendMessage(text);
				}
			}
		}
		if (player.server.getGameRules().getBoolean(GameRules.LOG_ADMIN_COMMANDS)) {
			player.server.sendMessage(text);
		}
	}

}
