package dev.jensh.omnivaultrecipeviewer.test;

import static dev.jensh.omnivaultrecipeviewer.test.RecipeViewerClientGameTest.check;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IScreenHelper;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** JEI against the open crafting terminal. Only loaded when JEI is. */
@SuppressWarnings("UnstableApiUsage")
final class JeiChecks {
	private static volatile IJeiRuntime runtime;

	private JeiChecks() {
	}

	/** Keeps hold of JEI's runtime for the checks. */
	public static final class RuntimeGrabber implements IModPlugin {
		@Override
		public Identifier getPluginUid() {
			return Identifier.fromNamespaceAndPath("omnivault_recipe_viewer_compat_test", "runtime");
		}

		@Override
		public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
			runtime = jeiRuntime;
		}
	}

	static void craftingTerminal(ClientGameTestContext context, TestSingleplayerContext sp) {
		context.waitFor(mc -> runtime != null);
		context.runOnClient(mc -> {
			IScreenHelper screens = runtime.getScreenHelper();
			int[] cell = RecipeViewerClientGameTest.firstCell(mc);
			check(screens.getClickableIngredientUnderMouse(mc.gui.screen(), cell[0], cell[1])
					.anyMatch(clickable -> clickable.getTypedIngredient().getIngredient() instanceof ItemStack stack && stack.is(Items.OAK_PLANKS)),
					"JEI should see the planks under the mouse in the network grid");
			int[] button = RecipeViewerClientGameTest.sideButton(mc);
			check(screens.getGuiExclusionAreas(mc.gui.screen()).anyMatch(area -> area.contains(button[0], button[1])),
					"JEI should keep clear of the terminal's side buttons");
		});
	}
}
