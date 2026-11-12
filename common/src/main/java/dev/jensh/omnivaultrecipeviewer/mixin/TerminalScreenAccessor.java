package dev.jensh.omnivaultrecipeviewer.mixin;

import com.nightbeam.omnivault.network.client.TerminalScreen;
import com.nightbeam.omnivault.network.menu.TerminalMenu;
import com.nightbeam.omnivault.storage.FluidKey;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The network grid is drawn by hand, not with slots; these tell us what is under the mouse. */
@Mixin(value = TerminalScreen.class, remap = false)
public interface TerminalScreenAccessor {
	@Invoker("entryAt")
	TerminalMenu.ClientEntry ovrvc$entryAt(double mouseX, double mouseY);

	@Invoker("fluidAt")
	Map.Entry<FluidKey, Long> ovrvc$fluidAt(double mouseX, double mouseY);
}
