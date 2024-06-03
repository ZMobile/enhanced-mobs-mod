package net.fabricmc.zmobile.bloodmoon.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.zmobile.bloodmoon.config.BloodmoonConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class ClientBloodmoonHandler implements ClientModInitializer {
	public static final ClientBloodmoonHandler INSTANCE = new ClientBloodmoonHandler();

	private boolean bloodmoonActive;

	private final float sinMax = (float) (Math.PI / 12000d);

	private float lightSub;
	public float fogRemove;
	private float skyColorAdd;
	private float moonColorRed;

	private float d = 1f / 15000f;
	private int difTime = 0;

	private double sin;

	public ClientBloodmoonHandler() {
		bloodmoonActive = false;
	}

	public boolean isBloodmoonActive() {
		return bloodmoonActive;
	}

	public void setBloodmoon(boolean active) {
		this.bloodmoonActive = active;
	}

	public void moonColorHook() {
		if (isBloodmoonActive() && BloodmoonConfig.APPEARANCE.RED_MOON) {
			GL11.glColor3f(0.8f, 0, 0);
		}
	}

	public Vec3d skyColorHook(Vec3d color) {
		if (isBloodmoonActive() && BloodmoonConfig.APPEARANCE.RED_SKY) {
			color = new Vec3d(color.x + INSTANCE.skyColorAdd, color.y, color.z);
		}
		return color;
	}

	public int manipulateRed(int position, int originalValue) {
		return originalValue;
	}

	public int manipulateGreen(int position, int originalValue) {
		if (isBloodmoonActive() && BloodmoonConfig.APPEARANCE.RED_LIGHT) {
			int height = position / 16;
			if (height < 16) {
				float mod = 1F / 16F * height;
				originalValue -= mod * lightSub * (sin / 2f + 1);
				return Math.max(originalValue, 0);
			}
		}
		return originalValue;
	}

	public int manipulateBlue(int position, int originalValue) {
		if (isBloodmoonActive() && BloodmoonConfig.APPEARANCE.RED_LIGHT) {
			int height = position / 16;
			if (height < 16) {
				float mod = 1F / 16F * height;
				originalValue -= mod * lightSub * 2.3f;
				return Math.max(originalValue, 0);
			}
		}
		return originalValue;
	}

	public void onClientTick(MinecraftClient client) {
		if (isBloodmoonActive()) {
			ClientWorld world = client.world;
			ClientPlayerEntity player = client.player;
			if (world != null && player != null) {
				if (world.getRegistryKey() != World.OVERWORLD) {
					bloodmoonActive = false;
					return;
				}

				float difTime = (int) (world.getTime() % 24000) - 12000;
				sin = Math.sin(difTime * sinMax);
				lightSub = (float) (sin * 150f);
				skyColorAdd = (float) (sin * 0.1f);
				moonColorRed = (float) (sin * 0.7f);
				fogRemove = (float) (sin * 6000f);
			} else if (bloodmoonActive) {
				bloodmoonActive = false;
			}
		}
	}

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}
}