package dev.jensh.omnivaultrecipeviewer.rei;

import com.nightbeam.omnivault.autocraft.AutocraftContent;
import com.nightbeam.omnivault.autocraft.menu.PatternTerminalMenu;
import com.nightbeam.omnivault.autocraft.pattern.PatternData;
import com.nightbeam.omnivault.network.CraftingTransfer;
import com.nightbeam.omnivault.network.NetworkContent;
import com.nightbeam.omnivault.network.client.TerminalScreen;
import com.nightbeam.omnivault.network.menu.CraftingTerminalMenu;
import com.nightbeam.omnivault.storage.ItemKey;
import dev.architectury.event.CompoundEventResult;
import dev.jensh.omnivaultrecipeviewer.Terminals;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.plugin.common.displays.crafting.CraftingDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * REI support: move a crafting recipe into a crafting terminal straight from the network, load any
 * recipe into the Pattern Encoding Terminal, and look up whatever is under the mouse in the network grid.
 */
public class OmniVaultReiPlugin implements REIClientPlugin {
	@Override
	public void registerCategories(CategoryRegistry registry) {
		registry.addWorkstations(BuiltinPlugin.CRAFTING,
				EntryStacks.of(NetworkContent.CRAFTING_TERMINAL.get()),
				EntryStacks.of(NetworkContent.WIRELESS_TERMINAL.get()),
				EntryStacks.of(AutocraftContent.PATTERN_TERMINAL.get()));
	}

	@Override
	public void registerTransferHandlers(TransferHandlerRegistry registry) {
		registry.register(new CraftingTerminalTransfer());
		registry.register(new PatternTerminalTransfer());
	}

	@Override
	public void registerScreens(ScreenRegistry registry) {
		registry.registerFocusedStack((screen, mouse) -> {
			ItemStack item = Terminals.hoveredItem(screen, mouse.x, mouse.y);
			if (!item.isEmpty()) {
				return CompoundEventResult.interruptTrue(EntryStacks.of(item));
			}
			Fluid fluid = Terminals.hoveredFluid(screen, mouse.x, mouse.y);
			if (fluid != null) {
				return CompoundEventResult.interruptTrue(EntryStacks.of(fluid));
			}
			return CompoundEventResult.pass();
		});
	}

	@Override
	public void registerExclusionZones(ExclusionZones zones) {
		zones.register(TerminalScreen.class, screen -> {
			Terminals.Area buttons = Terminals.sideButtons(screen);
			return List.of(new Rectangle(buttons.x(), buttons.y(), buttons.width(), buttons.height()));
		});
	}

	/** The item stacks a recipe slot accepts, capped at what OmniVault's packet carries. */
	static List<ItemStack> items(EntryIngredient ingredient) {
		List<ItemStack> stacks = new ArrayList<>();
		for (EntryStack<?> entry : ingredient) {
			if (entry.getType() == VanillaEntryTypes.ITEM && stacks.size() < CraftingTransfer.MAX_CANDIDATES) {
				ItemStack stack = entry.castValue();
				if (!stack.isEmpty()) {
					stacks.add(stack.copy());
				}
			}
		}
		return stacks;
	}

	private static boolean isCrafting(Display display) {
		return display instanceof CraftingDisplay && BuiltinPlugin.CRAFTING.equals(display.getCategoryIdentifier());
	}

	/** Crafting recipes into the crafting terminal's 3x3 grid, pulling from the network and the inventory. */
	private static final class CraftingTerminalTransfer implements TransferHandler {
		@Override
		public Result handle(Context context) {
			if (!(context.getMenu() instanceof CraftingTerminalMenu menu) || !isCrafting(context.getDisplay())) {
				return Result.createNotApplicable();
			}
			List<EntryIngredient> grid = ((CraftingDisplay) context.getDisplay()).getOrganisedInputEntries(3, 3);
			Map<ItemKey, Long> available = Terminals.available(menu, context.getMinecraft().player);
			List<List<ItemStack>> slots = new ArrayList<>();
			List<EntryIngredient> missing = new ArrayList<>();
			for (int i = 0; i < 9; i++) {
				EntryIngredient ingredient = i < grid.size() ? grid.get(i) : EntryIngredient.empty();
				List<ItemStack> candidates = items(ingredient);
				slots.add(candidates);
				if (!candidates.isEmpty() && Terminals.reachable(candidates, available, true) == null) {
					missing.add(ingredient);
				}
			}
			if (slots.stream().allMatch(List::isEmpty)) {
				return Result.createNotApplicable();
			}
			if (!missing.isEmpty()) {
				return Result.createFailed(Component.translatable(Terminals.MISSING)).tooltipMissing(missing);
			}
			if (context.isActuallyCrafting()) {
				Terminals.sendCrafting(menu, slots);
			}
			return Result.createSuccessful().blocksFurtherHandling();
		}
	}

	/**
	 * Any recipe into the Pattern Encoding Terminal. Crafting recipes fill the 3x3 grid in crafting
	 * mode; every other recipe becomes a processing recipe with its item inputs and outputs. Where a
	 * slot accepts several items, one the player can reach is chosen.
	 */
	private static final class PatternTerminalTransfer implements TransferHandler {
		@Override
		public Result handle(Context context) {
			if (!(context.getMenu() instanceof PatternTerminalMenu menu)) {
				return Result.createNotApplicable();
			}
			Display display = context.getDisplay();
			boolean crafting = isCrafting(display);
			Map<ItemKey, Long> available = Terminals.available(menu, context.getMinecraft().player);
			List<ItemStack> inputs = new ArrayList<>();
			List<EntryIngredient> inputEntries = crafting
					? ((CraftingDisplay) display).getOrganisedInputEntries(3, 3) : display.getInputEntries();
			for (EntryIngredient ingredient : inputEntries) {
				ItemStack choice = choose(ingredient, available);
				// A crafting grid keeps its empty cells so the shape survives; machines just list items.
				if (crafting || !choice.isEmpty()) {
					inputs.add(choice);
				}
			}
			List<ItemStack> outputs = new ArrayList<>();
			for (EntryIngredient ingredient : display.getOutputEntries()) {
				ItemStack choice = choose(ingredient, available);
				if (!choice.isEmpty()) {
					outputs.add(choice);
				}
			}
			if (inputs.stream().allMatch(ItemStack::isEmpty) || (!crafting && outputs.isEmpty())) {
				return Result.createFailed(Component.translatable(Terminals.PATTERN_UNSUPPORTED));
			}
			if (inputs.size() > PatternData.MAX_INPUTS || (!crafting && outputs.size() > PatternData.MAX_OUTPUTS)) {
				return Result.createFailed(Component.translatable(Terminals.PATTERN_TOO_BIG));
			}
			if (context.isActuallyCrafting()) {
				Terminals.sendPattern(menu, !crafting, inputs, crafting ? List.of() : outputs);
			}
			return Result.createSuccessful().blocksFurtherHandling();
		}

		private static ItemStack choose(EntryIngredient ingredient, Map<ItemKey, Long> available) {
			List<ItemStack> candidates = items(ingredient);
			ItemStack reachable = Terminals.reachable(candidates, available, false);
			return reachable != null ? reachable : candidates.isEmpty() ? ItemStack.EMPTY : candidates.getFirst();
		}
	}
}
