package dev.jensh.omnivaultrecipeviewer.test;

import static dev.jensh.omnivaultrecipeviewer.test.RecipeViewerClientGameTest.check;

import com.nightbeam.omnivault.autocraft.block.PatternTerminalBlockEntity;
import com.nightbeam.omnivault.network.menu.CraftingTerminalMenu;
import com.nightbeam.omnivault.storage.ItemKey;
import java.util.List;
import java.util.Optional;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.displays.cooking.DefaultSmeltingDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapedDisplay;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** REI against the open terminals. Only loaded when REI is. */
@SuppressWarnings("UnstableApiUsage")
final class ReiChecks {
	private ReiChecks() {
	}

	private static EntryIngredient of(ItemLike item) {
		return EntryIngredient.of(EntryStacks.of(item));
	}

	/** Four planks in a 2x2: a crafting table. */
	private static Display craftingTable() {
		EntryIngredient planks = of(Items.OAK_PLANKS);
		return new DefaultCustomShapedDisplay(List.of(planks, planks, planks, planks), List.of(of(Items.CRAFTING_TABLE)), Optional.empty(), 2, 2);
	}

	/** Runs the transfer the way REI's "+" button does: first handler that succeeds, or the first that blocks. */
	private static TransferHandler.Result transfer(Minecraft mc, Display display, boolean craft) {
		TransferHandler.Context context = TransferHandler.Context.create(craft, false, (AbstractContainerScreen<?>) mc.gui.screen(), display);
		TransferHandler.Result firstError = null;
		for (TransferHandler handler : TransferHandlerRegistry.getInstance()) {
			if (!handler.checkApplicable(context).isApplicable()) {
				continue;
			}
			TransferHandler.Result result = handler.handle(context);
			if (result.isSuccessful() || (result.isApplicable() && result.isBlocking())) {
				return result;
			}
			if (result.isApplicable() && firstError == null) {
				firstError = result;
			}
		}
		check(firstError != null, "no REI transfer handler applies to " + mc.gui.screen().getClass().getSimpleName());
		return firstError;
	}

	static void craftingTerminal(ClientGameTestContext context, TestSingleplayerContext sp) {
		context.waitFor(mc -> !PluginManager.areAnyReloading());
		context.runOnClient(mc -> {
			check(transfer(mc, craftingTable(), false).isSuccessful(), "REI should offer to move a crafting table recipe");
			TransferHandler.Result missing = transfer(mc,
					new DefaultCustomShapedDisplay(List.of(of(Items.DIAMOND)), List.of(of(Items.STICK)), Optional.empty(), 1, 1), false);
			check(missing.isApplicable() && !missing.isSuccessful(), "a recipe with unreachable items must fail");
			check(transfer(mc, craftingTable(), true).isSuccessful(), "REI should move a crafting table recipe");
		});
		context.waitTicks(10);
		sp.getServer().runOnServer(server -> {
			CraftingTerminalMenu menu = RecipeViewerClientGameTest.craftingMenu(server);
			check(menu.getSlot(menu.resultIndex()).getItem().is(Items.CRAFTING_TABLE), "the grid should hold a crafting table recipe, result is "
					+ menu.getSlot(menu.resultIndex()).getItem());
			long planks = RecipeViewerClientGameTest.network(server).storage().getCount(ItemKey.of(new ItemStack(Items.OAK_PLANKS)));
			check(planks == 60, "four planks should come from the network, " + planks + " left");
		});
		context.waitTicks(10);
		context.runOnClient(mc -> {
			int[] cell = RecipeViewerClientGameTest.firstCell(mc);
			EntryStack<?> focused = ScreenRegistry.getInstance().getFocusedStack(mc.gui.screen(), new Point(cell[0], cell[1]));
			check(focused != null && focused.getValue() instanceof ItemStack stack && stack.is(Items.OAK_PLANKS),
					"REI should see the planks under the mouse in the network grid, saw " + focused);
			int[] button = RecipeViewerClientGameTest.sideButton(mc);
			check(ScreenRegistry.getInstance().exclusionZones().getExclusionZones(mc.gui.screen()).stream()
					.anyMatch(zone -> zone.contains(button[0], button[1])), "REI should keep clear of the terminal's side buttons");
		});
	}

	static void patternTerminal(ClientGameTestContext context, TestSingleplayerContext sp) {
		context.runOnClient(mc -> check(transfer(mc,
				new DefaultSmeltingDisplay(List.of(of(Items.RAW_GOLD)), List.of(of(Items.GOLD_INGOT)), Optional.empty(), 1f, 200), true).isSuccessful(),
				"REI should load a smelting recipe into the pattern terminal"));
		context.waitTicks(10);
		sp.getServer().runOnServer(server -> {
			PatternTerminalBlockEntity terminal = RecipeViewerClientGameTest.patternTerminal(server);
			check(terminal.processing(), "a smelting recipe is a processing pattern");
			check(terminal.inputs().getItem(0).is(Items.RAW_GOLD) && terminal.outputs().getItem(0).is(Items.GOLD_INGOT),
					"pattern should be raw gold -> gold ingot, is " + terminal.inputs().getItem(0) + " -> " + terminal.outputs().getItem(0));
		});
		context.runOnClient(mc -> check(transfer(mc, craftingTable(), true).isSuccessful(),
				"REI should load a crafting recipe into the pattern terminal"));
		context.waitTicks(10);
		sp.getServer().runOnServer(server -> {
			PatternTerminalBlockEntity terminal = RecipeViewerClientGameTest.patternTerminal(server);
			check(!terminal.processing(), "a crafting recipe is a crafting pattern");
			for (int slot : new int[]{0, 1, 3, 4}) {
				check(terminal.inputs().getItem(slot).is(Items.OAK_PLANKS), "grid slot " + slot + " should hold planks");
			}
			check(terminal.inputs().getItem(2).isEmpty() && terminal.inputs().getItem(8).isEmpty(), "the 2x2 shape should survive");
		});
	}
}
