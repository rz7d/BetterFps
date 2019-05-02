package guichaguri.betterfps.patches.misc;

import org.lwjgl.opengl.GL11;

import guichaguri.betterfps.transformers.annotations.Copy;
import guichaguri.betterfps.transformers.annotations.Copy.Mode;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;

public class FontRendererPatch extends FontRenderer {

    public FontRendererPatch(GameSettings a, ResourceLocation b, TextureManager c, boolean d) {
        super(a, b, c, d);
    }

    @Override
    @Copy(Mode.REPLACE)
    public float renderUnicodeChar(char ch, boolean italic) {
        int width = this.glyphWidth[ch] & 255;
        if (width == 0)
            return 0F;

        int tex = ch >> 8;
        this.loadGlyphTexture(tex);
        int k = width >>> 4;
        int l = width & 15;
        float f = k;
        float f1 = l + 1;
        float u = (ch & 15) * 16F + f;
        float v = (ch & 255) / 16F * 16F;
        float x1 = f1 - f - 0.02F;
        float x2 = italic ? 1F : 0F;

        final float posX = this.posX;
        final float posY = this.posY;

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX);
        buffer
                .pos(posX + x2, posY, 0.)
                .tex(u / 256., v / 256.)
                .endVertex();
        buffer
                .pos(posX - x2, posY + 7.99, 0.)
                .tex(u / 256., (v + 15.98) / 256.)
                .endVertex();
        buffer
                .pos(posX + x1 / 2. + x2, posY, 0.)
                .tex((u + x1) / 256., v / 256.)
                .endVertex();
        buffer
                .pos(posX + x1 / 2. - x2, posY + 7.99, 0.)
                .tex((u + x1) / 256., (v + 15.98) / 256.)
                .endVertex();
        tessellator.draw();
        return (f1 - f) / 2F + 1F;
    }

    @Override
    @Copy(Mode.REPLACE)
    public float renderDefaultChar(int ch, boolean italic) {
        int x = (ch & 15) << 3;
        int y = (ch >> 1) & ~7;

        int offset = italic ? 1 : 0;
        renderEngine.bindTexture(locationFontTexture);
        int width = charWidth[ch];
        float w = width - 0.01F;

        final float posX = this.posX;
        final float posY = this.posY;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX);
        buffer
                .pos(posX + offset, posY, 0.)
                .tex(x / 128., y / 128.)
                .endVertex();
        buffer
                .pos(posX - offset, posY + 7.99, 0.)
                .tex(x / 128., (y + 7.99) / 128.)
                .endVertex();
        buffer
                .pos(posX + w - 1. + offset, posY, 0.)
                .tex((x + w - 1.) / 128., y / 128.)
                .endVertex();
        buffer
                .pos(posX + w - 1. - offset, posY + 7.99, 0.)
                .tex((x + w - 1.) / 128., (y + 7.99) / 128.)
                .endVertex();
        tessellator.draw();

        return width;
    }

}
