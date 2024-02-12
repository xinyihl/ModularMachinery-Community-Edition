package github.kasuminova.mmce.client.gui.widget.preview;

import com.cleanroommc.client.preview.renderer.scene.ISceneRenderHook;
import com.cleanroommc.client.preview.renderer.scene.ImmediateWorldSceneRenderer;
import com.cleanroommc.client.preview.renderer.scene.WorldSceneRenderer;
import com.cleanroommc.client.util.BlockInfo;
import com.cleanroommc.client.util.TrackedDummyWorld;
import com.cleanroommc.client.util.world.LRDummyWorld;
import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.util.RenderPos;
import github.kasuminova.mmce.client.gui.util.RenderSize;
import github.kasuminova.mmce.client.gui.widget.base.DynamicWidget;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import hellfirepvp.modularmachinery.client.ClientScheduler;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import javax.vecmath.Vector3f;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class MachineStructurePreviewWidget extends DynamicWidget {
    private final DynamicMachine machine;
    private final WorldSceneRenderer renderer = new ImmediateWorldSceneRenderer(
            new LRDummyWorld(new TrackedDummyWorld(), new TrackedDummyWorld())
    );

    private final LinkedList<Integer> mouseEventStamps = new LinkedList<>();
    protected Vector3f center = new Vector3f();
    protected float rotationYaw = 25;
    protected float rotationPitch = -135;
    protected float zoom = 5;
    protected boolean dragging;
    protected int lastMouseX;
    protected int lastMouseY;
    protected int currentMouseX;
    protected int currentMouseY;
    private long mouseEventStamp = -1;

    public MachineStructurePreviewWidget(final DynamicMachine machine) {
        this.machine = machine;
        initRenderer();
    }

    /**
     * 防止坐标过低导致方块无法放置至虚拟世界。
     */
    private static int getYOffset(final BlockPos pos) {
        int y = pos.getY();
        if (y < 0) {
            return -y;
        }
        return 0;
    }

    private void initPattern(final DynamicMachine machine, final boolean resetZoom) {
        TrackedDummyWorld world = renderer.getLRDummyWorld().getAnotherWorld();

        Map<BlockPos, BlockInfo> converted = new HashMap<>();
        TaggedPositionBlockArray pattern = machine.getPattern();
        BlockPos min = pattern.getMin();
        BlockPos max = pattern.getMax();
        int yOffset = getYOffset(min);
        pattern.getPattern().forEach((pos, info) -> {
            IBlockState sampleState = info.getSampleState(ClientScheduler.getClientTick());

            TileEntity te = null;
            Block block = sampleState.getBlock();
            if (block.hasTileEntity(sampleState)) {
                te = block.createTileEntity(world, sampleState);
            }
            converted.put(pos.add(0, yOffset, 0), new BlockInfo(sampleState, te));
        });

        world.addBlocks(converted);
        setRenderedCore(min.add(0, yOffset, 0), max.add(0, yOffset, 0), converted.keySet(), null, resetZoom);
    }

    private void initRenderer() {
        initPattern(machine, true);
        renderer.setOnLookingAt(ray -> {
        });
        renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
        renderer.useCacheBuffer(true);
    }

    @Override
    public void update(final WidgetGui gui) {
        super.update(gui);
        if (ClientScheduler.getClientTick() % 30 == 0) {
            renderer.getLRDummyWorld().setAnotherWorld(new TrackedDummyWorld());
            initPattern(machine, false);
            renderer.refreshCache();
        }
    }

    @Override
    public void render(final WidgetGui gui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
        final int guiLeft = (gui.getWidth() - gui.getXSize()) / 2;
        final int guiTop = (gui.getHeight() - gui.getYSize()) / 2;

        RenderPos realRenderPos = renderPos.add(new RenderPos(guiLeft, guiTop));

        renderer.render(
                realRenderPos.posX(), realRenderPos.posY(),
                renderSize.width(), renderSize.height(),
                mousePos.mouseX(), mousePos.mouseY()
        );

        currentMouseX = mousePos.mouseX();
        currentMouseY = mousePos.mouseY();

        int stampTotal = mouseEventStamps.stream().mapToInt(stamp -> stamp).sum();
        int stampAvg = mouseEventStamps.isEmpty() ? 0 : stampTotal / mouseEventStamps.size();
        GuiScreen g = gui.getGui();
        Minecraft mc = g.mc;
        g.drawString(mc.fontRenderer, "Mouse Event Stamp Avg: " + stampAvg + "ms",
                renderPos.posX() + 1, renderPos.posY(),
                0xFFFFFFFF);
    }

    @Override
    public boolean onMouseDWheel(final MousePos mousePos, final RenderPos renderPos, final int wheel) {
        if (isMouseOver(mousePos)) {
            zoom = (float) MathHelper.clamp(zoom + (wheel < 0 ? 1.5 : -1.5), 0.1, 999);
            renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
            return true;
        }
        return super.onMouseDWheel(mousePos, renderPos, wheel);
    }

    @Override
    public boolean onMouseClicked(final MousePos mousePos, final RenderPos renderPos, final int mouseButton) {
        dragging = true;
        lastMouseX = mousePos.mouseX();
        lastMouseY = mousePos.mouseY();
        return true;
    }

    @Override
    public boolean onMouseClickMove(final MousePos mousePos, final RenderPos renderPos, final int mouseButton) {
        if (mouseEventStamp == -1) {
            mouseEventStamp = System.currentTimeMillis();
        }
        mouseEventStamps.addFirst((int) (System.currentTimeMillis() - mouseEventStamp));
        mouseEventStamp = System.currentTimeMillis();
        if (mouseEventStamps.size() > 20) {
            mouseEventStamps.removeLast();
        }

        if (dragging) {
            int mouseX = mousePos.mouseX();
            int mouseY = mousePos.mouseY();

            rotationPitch += mouseX - lastMouseX + 360;
            rotationPitch = rotationPitch % 360;
            rotationYaw = (float) MathHelper.clamp(rotationYaw + (mouseY - lastMouseY), -89.9, 89.9);
            lastMouseY = mouseY;
            lastMouseX = mouseX;
            renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseReleased(final MousePos mousePos, final RenderPos renderPos) {
        dragging = false;
        return false;
    }

    public MachineStructurePreviewWidget setRenderedCore(BlockPos min, BlockPos max, Collection<BlockPos> blocks, ISceneRenderHook renderHook, boolean resetZoom) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos vPos : blocks) {
            minX = Math.min(minX, vPos.getX());
            minY = Math.min(minY, vPos.getY());
            minZ = Math.min(minZ, vPos.getZ());
            maxX = Math.max(maxX, vPos.getX());
            maxY = Math.max(maxY, vPos.getY());
            maxZ = Math.max(maxZ, vPos.getZ());
        }
        center = new Vector3f((minX + maxX) / 2f + 0.5F, (minY + maxY) / 2f + 0.5F, (minZ + maxZ) / 2f + 0.5F);
        renderer.addRenderedBlocksToAnotherWorld(blocks, renderHook);

        if (resetZoom) {
            zoom = (float) (3.5 * Math.sqrt(Math.max(Math.max(Math.max(maxX - minX + 1, maxY - minY + 1), maxZ - minZ + 1), 1)));
            renderer.setCameraLookAt(center, zoom, Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
        }
        return this;
    }

    @Override
    public void onGUIClosed(final WidgetGui gui) {
        super.onGUIClosed(gui);
        renderer.deleteCacheBuffer();
    }
}
