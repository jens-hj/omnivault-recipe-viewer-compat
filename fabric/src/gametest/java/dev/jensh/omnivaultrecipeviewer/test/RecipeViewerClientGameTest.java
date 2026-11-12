package dev.jensh.omnivaultrecipeviewer.test;

import com.nightbeam.omnivault.autocraft.AutocraftContent;
import com.nightbeam.omnivault.autocraft.block.PatternTerminalBlockEntity;
import com.nightbeam.omnivault.autocraft.client.PatternTerminalScreen;
import com.nightbeam.omnivault.network.NetworkContent;
import com.nightbeam.omnivault.network.StorageNetwork;
import com.nightbeam.omnivault.network.block.DriveBlockEntity;
import com.nightbeam.omnivault.network.block.MenuDevice;
import com.nightbeam.omnivault.network.block.NexusCoreBlockEntity;
import com.nightbeam.omnivault.network.block.OrientedDeviceBlock;
import com.nightbeam.omnivault.network.cell.CellTier;
import com.nightbeam.omnivault.network.client.CraftingTerminalScreen;
import com.nightbeam.omnivault.network.client.TerminalScreen;
import com.nightbeam.omnivault.network.menu.CraftingTerminalMenu;
import com.nightbeam.omnivault.network.menu.TerminalMenu;
import com.nightbeam.omnivault.storage.ItemKey;
import dev.jensh.omnivaultrecipeviewer.mixin.ContainerScreenAccessor;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Builds a small OmniVault network, opens the crafting and pattern terminals on a real client and
 * drives the loaded recipe viewer (-Pviewer=rei or jei) against them.
 */
@SuppressWarnings("UnstableApiUsage")
public class RecipeViewerClientGameTest implements FabricClientGameTest {
	private static final String VIEWER = System.getProperty("omnivault_recipe_viewer_compat.viewer", "rei");

	static BlockPos core;
	static BlockPos craftingTerminal;
	static BlockPos patternTerminal;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			sp.getServer().runOnServer(RecipeViewerClientGameTest::build);
			context.waitTicks(20);
			sp.getServer().runOnServer(server -> {
				long inserted = network(server).storage().insert(ItemKey.of(new ItemStack(Items.OAK_PLANKS)), 64, false);
				check(inserted == 64, "network should take 64 planks, took " + inserted);
				player(server).getInventory().clearContent();
			});

			open(context, sp, craftingTerminal, CraftingTerminalScreen.class);
			if (VIEWER.equals("rei")) {
				ReiChecks.craftingTerminal(context, sp);
			} else {
				JeiChecks.craftingTerminal(context, sp);
			}
			context.takeScreenshot(VIEWER + "_crafting_terminal");
			context.runOnClient(mc -> mc.player.closeContainer());
			context.waitTicks(5);

			open(context, sp, patternTerminal, PatternTerminalScreen.class);
			if (VIEWER.equals("rei")) {
				ReiChecks.patternTerminal(context, sp);
			}
			context.takeScreenshot(VIEWER + "_pattern_terminal");
			context.runOnClient(mc -> mc.player.closeContainer());
		}
	}

	private static void build(MinecraftServer server) {
		ServerLevel level = server.overworld();
		BlockPos base = player(server).blockPosition().offset(-3, 1, 2);
		for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(7, 1, 2))) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
		core = base;
		level.setBlockAndUpdate(core, NetworkContent.NEXUS_CORE.get().defaultBlockState());
		for (int x = 1; x <= 6; x++) {
			level.setBlockAndUpdate(base.offset(x, 0, 0), NetworkContent.NEXUS_CABLE.get().defaultBlockState());
		}
		// Fronts don't carry the network, so the devices face away from the cable.
		BlockPos drive = base.offset(2, 0, 1);
		level.setBlockAndUpdate(drive, NetworkContent.DRIVE.get().defaultBlockState().setValue(OrientedDeviceBlock.FACING, Direction.SOUTH));
		((DriveBlockEntity) level.getBlockEntity(drive)).cells().setItem(0, new ItemStack(NetworkContent.CELLS.get(CellTier.T4K).get()));
		craftingTerminal = base.offset(4, 0, 1);
		level.setBlockAndUpdate(craftingTerminal, NetworkContent.CRAFTING_TERMINAL.get().defaultBlockState()
				.setValue(OrientedDeviceBlock.FACING, Direction.SOUTH));
		patternTerminal = base.offset(5, 0, 1);
		level.setBlockAndUpdate(patternTerminal, AutocraftContent.PATTERN_TERMINAL.get().defaultBlockState()
				.setValue(OrientedDeviceBlock.FACING, Direction.SOUTH));
	}

	private static void open(ClientGameTestContext context, TestSingleplayerContext sp, BlockPos pos, Class<?> screen) {
		sp.getServer().runOnServer(server -> ((MenuDevice) server.overworld().getBlockEntity(pos)).openMenu(player(server)));
		context.waitFor(mc -> screen.isInstance(mc.gui.screen())
				&& !((TerminalScreen<?>) mc.gui.screen()).getMenu().clientEntries().isEmpty());
		// Let the screen draw so its grid view is filled in.
		context.waitTicks(10);
	}

	static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	static StorageNetwork network(MinecraftServer server) {
		return ((NexusCoreBlockEntity) server.overworld().getBlockEntity(core)).network();
	}

	static PatternTerminalBlockEntity patternTerminal(MinecraftServer server) {
		return (PatternTerminalBlockEntity) server.overworld().getBlockEntity(patternTerminal);
	}

	static CraftingTerminalMenu craftingMenu(MinecraftServer server) {
		return (CraftingTerminalMenu) player(server).containerMenu;
	}

	/** The middle of the first cell of the open terminal's network grid, in GUI coordinates. */
	static int[] firstCell(Minecraft mc) {
		ContainerScreenAccessor bounds = (ContainerScreenAccessor) mc.gui.screen();
		return new int[]{bounds.ovrvc$leftPos() + TerminalMenu.GRID_X + 8, bounds.ovrvc$topPos() + TerminalMenu.GRID_Y + 8};
	}

	/** A point on the sort button left of the open terminal. */
	static int[] sideButton(Minecraft mc) {
		ContainerScreenAccessor bounds = (ContainerScreenAccessor) mc.gui.screen();
		return new int[]{bounds.ovrvc$leftPos() - 10, bounds.ovrvc$topPos() + 30};
	}

	static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
