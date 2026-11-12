package dev.jensh.omnivaultrecipeviewer.jei;

import com.nightbeam.omnivault.network.client.TerminalScreen;
import dev.jensh.omnivaultrecipeviewer.Terminals;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * OmniVault ships its own JEI plugin for moving recipes into terminals. This adds what it leaves out:
 * looking up the item or fluid under the mouse in the network grid, and keeping JEI clear of the
 * terminal's side buttons.
 */
@JeiPlugin
public class OmniVaultJeiExtrasPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return Identifier.fromNamespaceAndPath(Terminals.MOD_ID, "jei");
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		IPlatformFluidHelper<?> fluids = registration.getJeiHelpers().getPlatformFluidHelper();
		registration.addGenericGuiContainerHandler(TerminalScreen.class, new TerminalHandler<>(fluids));
	}

	private record TerminalHandler<F>(IPlatformFluidHelper<F> fluids) implements IGuiContainerHandler<TerminalScreen<?>> {
		@Override
		public List<Rect2i> getGuiExtraAreas(TerminalScreen<?> screen) {
			Terminals.Area buttons = Terminals.sideButtons(screen);
			return List.of(new Rect2i(buttons.x(), buttons.y(), buttons.width(), buttons.height()));
		}

		@Override
		public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory,
				TerminalScreen<?> screen, double mouseX, double mouseY) {
			Terminals.Area cell = Terminals.cell(screen, mouseX, mouseY);
			ItemStack item = Terminals.hoveredItem(screen, mouseX, mouseY);
			if (!item.isEmpty()) {
				return factory.createBuilder(item).buildWithArea(cell.x(), cell.y(), cell.width(), cell.height());
			}
			Fluid fluid = Terminals.hoveredFluid(screen, mouseX, mouseY);
			if (fluid != null) {
				F stack = fluids.create(fluid.builtInRegistryHolder(), fluids.bucketVolume());
				return factory.createBuilder(fluids.getFluidIngredientType(), stack).buildWithArea(cell.x(), cell.y(), cell.width(), cell.height());
			}
			return Optional.empty();
		}
	}
}
