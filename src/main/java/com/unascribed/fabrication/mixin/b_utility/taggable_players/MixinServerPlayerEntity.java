package com.unascribed.fabrication.mixin.b_utility.taggable_players;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.GameProfile;
import com.unascribed.fabrication.FabConf;
import com.unascribed.fabrication.FabLog;
import com.unascribed.fabrication.features.FeatureTaggablePlayers;
import com.unascribed.fabrication.interfaces.TaggablePlayer;
import com.unascribed.fabrication.support.EligibleIf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.unascribed.fabrication.support.injection.FabInject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Mixin(ServerPlayerEntity.class)
@EligibleIf(configAvailable="*.taggable_players")
public abstract class MixinServerPlayerEntity extends PlayerEntity implements TaggablePlayer {

	private final Set<String> fabrication$tags = new HashSet<>();
	private Map<String, Boolean> fabrication$tagsOverride = null;

	public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}

	@Override
	public Set<String> fabrication$getTags() {
		return ImmutableSet.copyOf(fabrication$tags);
	}

	@Override
	public void fabrication$clearTags() {
		fabrication$tags.clear();
	}

	@Override
	public void fabrication$setTag(String tag, boolean enabled) {
		if (enabled) {
			fabrication$tags.add(tag);
		} else {
			fabrication$tags.remove(tag);
		}
	}

	@Override
	public boolean fabrication$hasTag(String tag) {
		if (fabrication$tagsOverride != null) {
			Boolean b = fabrication$tagsOverride.get(tag);
			if (b != null) return b;
		}
		return fabrication$tags.contains(tag);
	}
	@Override
	public Boolean fabrication$getTagOverride(String tag) {
		if (fabrication$tagsOverride == null) return null;
		return fabrication$tagsOverride.get(tag);
	}

	@FabInject(at=@At("TAIL"), method="<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/world/ServerWorld;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/network/packet/c2s/common/SyncedClientOptions;)V")
	public void fabrication$genOverride(MinecraftServer server, ServerWorld world, GameProfile profile, SyncedClientOptions clientOptions, CallbackInfo ci) {
		Map<String, Boolean> mapName = FeatureTaggablePlayers.playerNameOverrideMap.get(profile.getName());
		Map<String, Boolean> mapUuid = FeatureTaggablePlayers.playerUUIDOverrideMap.get(profile.getId());
		if (mapUuid != null || mapName != null) {
			Map<String, Boolean> map = new HashMap<>();
			if (mapName != null) map.putAll(mapName);
			if (mapUuid != null) map.putAll(mapUuid);
			fabrication$tagsOverride = map;
		}
	}

	@FabInject(at=@At("HEAD"), method="copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V")
	public void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
		fabrication$tags.clear();
		fabrication$tags.addAll(((TaggablePlayer)oldPlayer).fabrication$getTags());
	}

	@FabInject(at=@At("TAIL"), method="writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V")
	public void writeCustomDataToTag(NbtCompound tag, CallbackInfo ci) {
		NbtList li = new NbtList();
		for (String pt : fabrication$tags) {
			li.add(NbtString.of(pt));
		}
		if (!li.isEmpty()) {
			tag.put("fabrication:Tags", li);
		}
	}

	@FabInject(at=@At("TAIL"), method="readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V")
	public void readCustomDataFromTag(NbtCompound tag, CallbackInfo ci) {
		fabrication$tags.clear();
		NbtList li = tag.getList("fabrication:Tags", NbtElement.STRING_TYPE);
		for (int i = 0; i < li.size(); i++) {
			String key = li.getString(i);
			String fullKey = FabConf.remap("*."+key.toLowerCase(Locale.ROOT));
			if (!FeatureTaggablePlayers.activeTags.containsKey(fullKey)) {
				FabLog.warn("TaggablePlayers added "+fullKey+" as a valid option because a player was tagged with it");
				FeatureTaggablePlayers.add(fullKey, 0);
			}
			fabrication$tags.add(key);
		}
	}

}
