package dev.jensh.omnivaultrecipeviewer.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int ovrvc$leftPos();

	@Accessor("topPos")
	int ovrvc$topPos();
}
