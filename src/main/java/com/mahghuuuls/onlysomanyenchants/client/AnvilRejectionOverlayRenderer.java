package com.mahghuuuls.onlysomanyenchants.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ContainerRepair;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
public final class AnvilRejectionOverlayRenderer {

    private static final int PANEL_WIDTH = 176;
    private static final int HORIZONTAL_PADDING = 6;
    private static final int VERTICAL_PADDING = 4;
    private static final int LINE_STEP = 10;
    private static final int MAX_TEXT_WIDTH = PANEL_WIDTH - (HORIZONTAL_PADDING * 2);

    private static final int BG_COLOR = 0xE61C1010;
    private static final int BORDER_COLOR = 0xFFAA3B3B;
    private static final int TEXT_COLOR = 0xFFFF6B6B;

    private AnvilRejectionOverlayRenderer() {
    }

    public static void renderIfActive(GuiRepair gui, ContainerRepair anvil) {
        if (gui == null || anvil == null) {
            return;
        }
        if (anvil.maximumCost > 0 || anvil.getSlot(2).getHasStack()) {
            return;
        }
        if (!ClientAnvilFeedbackState.isActiveFor(anvil.windowId)) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc != null ? mc.fontRenderer : null;
        if (font == null) {
            return;
        }

        String message = I18n.format(
                "onlysomanyenchants.anvil.cap_rejected",
                Integer.valueOf(ClientAnvilFeedbackState.getAttemptedCount()),
                Integer.valueOf(ClientAnvilFeedbackState.getMaxAllowed())
        );

        List<String> lines = font.listFormattedStringToWidth(message, MAX_TEXT_WIDTH);
        if (lines.isEmpty()) {
            return;
        }

        int bannerHeight = (lines.size() * LINE_STEP) + (VERTICAL_PADDING * 2) - 1;
        int guiTop = (gui.height - 166) / 2;
        int top;
        if (guiTop >= bannerHeight + 6) {
            top = -bannerHeight - 4;
        } else if ((gui.height - guiTop - 166) >= bannerHeight + 6) {
            top = 166 + 4;
        } else {
            top = -guiTop + 2;
        }
        int left = 0;
        int right = PANEL_WIDTH;
        int bottom = top + bannerHeight;

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Gui.drawRect(left - 1, top - 1, right + 1, bottom + 1, BORDER_COLOR);
        Gui.drawRect(left, top, right, bottom, BG_COLOR);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineWidth = font.getStringWidth(line);
            int textX = (PANEL_WIDTH - lineWidth) / 2;
            int textY = top + VERTICAL_PADDING + (i * LINE_STEP);
            font.drawStringWithShadow(line, textX, textY, TEXT_COLOR);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
    }
}
