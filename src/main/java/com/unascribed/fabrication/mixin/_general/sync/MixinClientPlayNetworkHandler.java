package com.unascribed.fabrication.mixin._general.sync;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.brigadier.ParseResults;
import com.unascribed.fabrication.client.FScriptScreen;
import com.unascribed.fabrication.interfaces.GetServerConfig;
import com.unascribed.fabrication.support.ConfigValues;
import com.unascribed.fabrication.support.EligibleIf;
import com.unascribed.fabrication.support.Env;
import com.unascribed.fabrication.support.injection.FabInject;
import com.unascribed.fabrication.util.ByteBufCustomPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Random;
import java.util.Set;

@Mixin(ClientPlayNetworkHandler.class)
@EligibleIf(envMatches=Env.CLIENT)
public abstract class MixinClientPlayNetworkHandler extends ClientCommonNetworkHandler implements GetServerConfig {

	@Shadow
	protected abstract ParseResults<CommandSource> parse(String command);

	private boolean fabrication$hasHandshook = false;
	private final Map<String, ConfigValues.ResolvedFeature> fabrication$serverTrileanConfig = Maps.newHashMap();
	private final Map<String, String> fabrication$serverStringConfig = Maps.newHashMap();
	private long fabrication$launchId;
	private final Map<String, String> fabrication$serverFailedConfig = Maps.newHashMap();
	private final Set<String> fabrication$serverBanned = Sets.newHashSet();
	private String fabrication$serverVersion;
	private final Random fabrication$random = new Random();

	protected MixinClientPlayNetworkHandler(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
		super(client, connection, connectionState);
	}

	@FabInject(at=@At("TAIL"), method="onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V")
	public void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
		PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
		data.writeVarInt(0);
		data.writeVarInt(1);
		connection.send(new CustomPayloadC2SPacket(new ByteBufCustomPayload(Identifier.of("fabrication", "config"), data)));
	}

	@FabInject(at=@At("HEAD"), method="onCustomPayload(Lnet/minecraft/network/packet/CustomPayload;)V", cancellable=true)
	public void onCustomPayload(CustomPayload payload, CallbackInfo ci) {
		if (!(payload instanceof ByteBufCustomPayload)) return;

		if (((ByteBufCustomPayload) payload).id().getNamespace().equals("fabrication")) {
			if (((ByteBufCustomPayload) payload).id().getPath().equals("config") || ((ByteBufCustomPayload) payload).id().getPath().equals("config2")) {
				try {
					fabrication$hasHandshook = true;
					PacketByteBuf buf = ((ByteBufCustomPayload) payload).buf();
					int reqVer = 0;
					if (((ByteBufCustomPayload) payload).id().getPath().equals("config2")) {
						reqVer = buf.readVarInt();
					}
					int trileanKeys = buf.readVarInt();
					for (int i = 0; i < trileanKeys; i++) {
						String k = buf.readString(32767);
						int v = buf.readUnsignedByte();
						fabrication$serverTrileanConfig.put(k, ConfigValues.ResolvedFeature.values()[v]);
					}
					int stringKeys = buf.readVarInt();
					for (int i = 0; i < stringKeys; i++) {
						String k = buf.readString(32767);
						String v = buf.readString(32767);
						fabrication$serverStringConfig.put(k, v);
					}
					if (buf.isReadable(8)) {
						fabrication$launchId = buf.readLong();
						if (buf.isReadable()) {
							fabrication$serverVersion = buf.readString(32767);
							fabrication$serverFailedConfig.clear();
							int failedKeys = buf.readVarInt();
							if (reqVer == 1) {
								for (int i = 0; i < failedKeys; i++) {
									fabrication$serverFailedConfig.put(buf.readString(32767), buf.readString(32767));
								}
							} else if (reqVer == 0) {
								for (int i = 0; i < failedKeys; i++) {
									fabrication$serverFailedConfig.put(buf.readString(32767), "Unknown");
								}
							}
						} else {
							fabrication$serverVersion = "1.2.11 or earlier";
						}
					} else if (fabrication$launchId == 0) {
						fabrication$launchId = hashCode() * 31L;
						fabrication$serverVersion = "1.2 or earlier";
					}
					if (buf.isReadable()) {
						fabrication$serverBanned.clear();
						int bannedKeys = buf.readVarInt();
						for (int i = 0; i < bannedKeys; i++) {
							String k = buf.readString(32767);
							fabrication$serverBanned.add(k);
						}
					}
					ci.cancel();
				} catch (RuntimeException e) {
					e.printStackTrace();
					throw e;
				}
			}else if (((ByteBufCustomPayload) payload).id().getPath().equals("fscript")){
				try{
					PacketByteBuf buf = ((ByteBufCustomPayload) payload).buf();
					int code = buf.readVarInt();
					if (code == 0){
						if (client.currentScreen instanceof FScriptScreen){
							((FScriptScreen) client.currentScreen).fabrication$setScript(buf.readString());
						}
					}
					ci.cancel();
				}catch (RuntimeException e) {
					e.printStackTrace();
					throw e;
				}
			}
		}
	}

	@Override
	public boolean fabrication$hasHandshook() {
		return fabrication$hasHandshook;
	}

	@Override
	public Map<String, ConfigValues.ResolvedFeature> fabrication$getServerTrileanConfig() {
		return fabrication$serverTrileanConfig;
	}

	@Override
	public Map<String, String> fabrication$getServerStringConfig() {
		return fabrication$serverStringConfig;
	}

	@Override
	public long fabrication$getLaunchId() {
		return fabrication$launchId;
	}

	@Override
	public Map<String, String> fabrication$getServerFailedConfig() {
		return fabrication$serverFailedConfig;
	}

	@Override
	public String fabrication$getServerVersion() {
		return fabrication$serverVersion;
	}

	@Override
	public Set<String> fabrication$getServerBanned() {
		return fabrication$serverBanned;
	}

}
