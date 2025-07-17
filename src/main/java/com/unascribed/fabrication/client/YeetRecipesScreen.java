package com.unascribed.fabrication.client;

import com.unascribed.fabrication.FabConf;
import com.unascribed.fabrication.loaders.LoaderYeetRecipes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Iterator;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class YeetRecipesScreen extends Screen{

	final ScrollBar yeetRecipesBar = new ScrollBar(height/2f);
	final ScrollBar availableRecipesBar = new ScrollBar(yeetRecipesBar.displayHeight-20);

	private TextFieldWidget searchField;
	Pattern filter = Pattern.compile("");
	Screen parent;
	PrideFlagRenderer prideFlag;
	boolean didClick;
	boolean didRClick;
	double lastMouseX, lastMouseY;


	public YeetRecipesScreen(Screen parent, PrideFlagRenderer prideFlag, String title, String configKey) {
		super(Text.literal("Fabrication Yeet Recipes"));
		this.parent = parent;
		this.prideFlag = prideFlag;
	}
	@Override
	protected void init() {
		super.init();
		searchField = new TextFieldWidget(textRenderer, 0, 0, width, 20, searchField, Text.literal("Filter"));
		searchField.setChangedListener((s) -> {
			s = s.trim();
			filter = Pattern.compile(s, Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
		});
	}

	@Override
	public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		renderBackground(drawContext, mouseX, mouseY, delta);
		drawContext.fill(0, 22, width, height / 2+10, FabConf.isEnabled("general.dark_mode") ? 0x44FFFFFF : 0x55000000);
		searchField.render(drawContext, mouseX, mouseY, delta);
		if (client.world == null) {
			drawContext.drawText(textRenderer, "Load a world to see suggestions", 5, 25, -1, true);
		} else {
			float y = 22 - availableRecipesBar.getScaledScroll(client);
			for (RecipeEntry<?> clr : client.world.getRecipeManager().values()) {
				if (!filter.matcher(clr.id().toString()).find()) continue;
				if (y > 20) {
					drawContext.drawText(textRenderer, clr.id().toString(), 5, (int) y, -1, true);
				}
				if (didClick && y > 20 && mouseY > 22 && mouseY > y && mouseY < y + 12) {
					client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, 1.2f, 1f));
					LoaderYeetRecipes.recipesToYeet.add(clr.id());
					LoaderYeetRecipes.instance.set(clr.id().toString(), "true");
				}
				y += 12;
				if (y > height/2f) break;
			}
			availableRecipesBar.height = client.world.getRecipeManager().values().stream().filter(r->filter.matcher(r.id().toString()).find()).count() * 12 + 20;

		}
		float y = 12 + height/2f - yeetRecipesBar.getScaledScroll(client);
		Iterator<Identifier> iter = LoaderYeetRecipes.recipesToYeet.iterator();
		while (iter.hasNext()) {
			Identifier clr = iter.next();
			if (y > 10 + height/2f) {
				drawContext.drawText(textRenderer, clr.toString(), 5, (int) y, -1, true);
			}
			if (didRClick && mouseY > 10 + height/2 && mouseY > y && mouseY < y + 12) {
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, 1.2f, 1f));
				iter.remove();
				LoaderYeetRecipes.instance.remove(clr.toString());
			}
			y += 12;
			if (y > height) break;
		}
		yeetRecipesBar.height = LoaderYeetRecipes.recipesToYeet.size() * 12 + 20;

		if (didClick) didClick = false;
		if (didRClick) didRClick = false;
	}

	@Override
	public void tick() {
		super.tick();
		yeetRecipesBar.tick();
		availableRecipesBar.tick();
	}

	@Override
	public void renderBackground(DrawContext matrices, int mouseX, int mouseY, float delta) {
		FabricationConfigScreen.drawBackground(height, width, client, prideFlag, 0, matrices, 0, 0, 0, 0, 0);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			didClick = true;
		} else if (button == 1){
			didRClick = true;
		}
		searchField.mouseClicked(mouseX, mouseY, button);
		return super.mouseClicked(mouseX, mouseY, button);
	}
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizon, double amount) {
		if (mouseY > height/2d) {
			yeetRecipesBar.scroll(amount*20);
		} else {
			availableRecipesBar.scroll(amount*20);
		}
		return super.mouseScrolled(mouseX, mouseY, horizon, amount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		switch (keyCode) {
			case GLFW.GLFW_KEY_PAGE_UP: mouseScrolled(lastMouseX, lastMouseY, 0, 20); break;
			case GLFW.GLFW_KEY_PAGE_DOWN: mouseScrolled(lastMouseX, lastMouseY, 0, -20); break;
			case GLFW.GLFW_KEY_UP: mouseScrolled(lastMouseX, lastMouseY, 0, 2); break;
			case GLFW.GLFW_KEY_DOWN: mouseScrolled(lastMouseX, lastMouseY, 0, -2); break;
		}
		searchField.keyPressed(keyCode, scanCode, modifiers);
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		searchField.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		searchField.mouseMoved(mouseX, mouseY);
		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		searchField.mouseReleased(mouseX, mouseY, button);
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		searchField.charTyped(chr, modifiers);
		return super.charTyped(chr, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		searchField.keyReleased(keyCode, scanCode, modifiers);
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public void resize(MinecraftClient client, int width, int height) {
		yeetRecipesBar.displayHeight = height/2f;
		availableRecipesBar.displayHeight = yeetRecipesBar.displayHeight-20;
		super.resize(client, width, height);
	}

}
