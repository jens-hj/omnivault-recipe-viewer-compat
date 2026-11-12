package dev.jensh.omnivaultrecipeviewer;

import com.nightbeam.omnivault.autocraft.AutocraftPackets;
import com.nightbeam.omnivault.autocraft.menu.PatternTerminalMenu;
import com.nightbeam.omnivault.network.CraftingTransfer;
import com.nightbeam.omnivault.network.client.TerminalScreen;
import com.nightbeam.omnivault.network.menu.CraftingTerminalMenu;
import com.nightbeam.omnivault.network.menu.TerminalMenu;
import com.nightbeam.omnivault.platform.Services;
import com.nightbeam.omnivault.storage.FluidKey;
import com.nightbeam.omnivault.storage.ItemKey;
import dev.jensh.omnivaultrecipeviewer.mixin.ContainerScreenAccessor;
import dev.jensh.omnivaultrecipeviewer.mixin.TerminalScreenAccessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/** What every recipe viewer needs from an OmniVault terminal, independent of the viewer's API. */
public final class Terminals {
	public static final String MOD_ID = "omnivault_recipe_viewer_compat";
	public static final String MISSING = "gui." + MOD_ID + ".missing";
	public static final String PATTERN_UNSUPPORTED = "gui." + MOD_ID + ".pattern_unsupported";
	public static final String PATTERN_TOO_BIG = "gui." + MOD_ID + ".pattern_too_big";

	private Terminals() {
	}

	/** A screen rectangle. */
	public record Area(int x, int y, int width, int height) {
	}

	/** What the player can reach: the network as the terminal last saw it, plus their inventory. */
	public static Map<ItemKey, Long> available(TerminalMenu menu, Player player) {
		Map<ItemKey, Long> available = new HashMap<>();
		for (TerminalMenu.ClientEntry entry : menu.clientEntries().values()) {
			available.merge(entry.key(), entry.count(), Long::sum);
		}
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			ItemKey key = ItemKey.of(stack);
			if (key != null) {
				available.merge(key, (long) stack.getCount(), Long::sum);
			}
		}
		return available;
	}

	/** The first candidate the player can reach, or null. With {@code take}, one of it is used up. */
	public static @Nullable ItemStack reachable(List<ItemStack> candidates, Map<ItemKey, Long> available, boolean take) {
		for (ItemStack candidate : candidates) {
			ItemKey key = ItemKey.of(candidate);
			if (key != null && available.getOrDefault(key, 0L) > 0) {
				if (take) {
					available.merge(key, -1L, Long::sum);
				}
				return candidate;
			}
		}
		return null;
	}

	/** Fills the crafting grid of an open crafting terminal; nine entries of acceptable stacks. */
	public static void sendCrafting(CraftingTerminalMenu menu, List<List<ItemStack>> slots) {
		Services.PLATFORM.sendToServer(new CraftingTransfer(menu.containerId, slots));
	}

	/** Loads a recipe into the Pattern Encoding Terminal. */
	public static void sendPattern(PatternTerminalMenu menu, boolean processing, List<ItemStack> inputs, List<ItemStack> outputs) {
		Services.PLATFORM.sendToServer(new AutocraftPackets.PatternTransfer(menu.containerId, processing, inputs, outputs));
	}

	/** The network item under the mouse in a terminal's grid, or empty. */
	public static ItemStack hoveredItem(Screen screen, double mouseX, double mouseY) {
		if (screen instanceof TerminalScreen<?> && screen instanceof TerminalScreenAccessor terminal) {
			TerminalMenu.ClientEntry entry = terminal.ovrvc$entryAt(mouseX, mouseY);
			if (entry != null) {
				return entry.key().view().copyWithCount(1);
			}
		}
		return ItemStack.EMPTY;
	}

	/** The network fluid under the mouse in a terminal's grid (fluid view), or null. */
	public static @Nullable Fluid hoveredFluid(Screen screen, double mouseX, double mouseY) {
		if (screen instanceof TerminalScreen<?> && screen instanceof TerminalScreenAccessor terminal) {
			Map.Entry<FluidKey, Long> entry = terminal.ovrvc$fluidAt(mouseX, mouseY);
			if (entry != null) {
				return entry.getKey().fluid();
			}
		}
		return null;
	}

	/** The grid cell under the mouse. */
	public static Area cell(Screen screen, double mouseX, double mouseY) {
		ContainerScreenAccessor bounds = (ContainerScreenAccessor) screen;
		int column = (int) Math.floor((mouseX - bounds.ovrvc$leftPos() - TerminalMenu.GRID_X) / 18);
		int row = (int) Math.floor((mouseY - bounds.ovrvc$topPos() - TerminalMenu.GRID_Y) / 18);
		return new Area(bounds.ovrvc$leftPos() + TerminalMenu.GRID_X + column * 18, bounds.ovrvc$topPos() + TerminalMenu.GRID_Y + row * 18, 16, 16);
	}

	/** The column of sort/view buttons that hangs off the left edge of every terminal. */
	public static Area sideButtons(Screen screen) {
		ContainerScreenAccessor bounds = (ContainerScreenAccessor) screen;
		return new Area(bounds.ovrvc$leftPos() - 22, bounds.ovrvc$topPos() + 18, 22, 88);
	}
}
