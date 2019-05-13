package guichaguri.betterfps.patches.misc;

import guichaguri.betterfps.transformers.annotations.Copy;
import guichaguri.betterfps.transformers.annotations.Copy.Mode;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GLSync;
import org.lwjgl.opengl.OpenGLException;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL44.GL_MAP_PERSISTENT_BIT;
import static org.lwjgl.opengl.GL44.glBufferStorage;

public class FontRendererPatch extends FontRenderer {

    @Copy
    private static final int INIT_FLAGS = GL_MAP_PERSISTENT_BIT | GL_MAP_WRITE_BIT;

    @Copy
    private static final int MAP_FLAGS = GL_MAP_UNSYNCHRONIZED_BIT | INIT_FLAGS;

    @Copy
    private static final int VBO_LENGTH = 64;

    @Copy
    private static final int BUFFER_COUNT = 1024;

    @Copy
    private static final int BUFFER_SIZE = VBO_LENGTH * BUFFER_COUNT;

    @Copy
    private static final long TIMEOUT = TimeUnit.MILLISECONDS.toNanos(16);

    @Copy
    private GLSync[] syncs;

    @Copy
    private int vbo;

    @Copy
    private ByteBuffer buffer;

    @Copy
    private int offset;

    @Copy
    private boolean requireSync;

    @Copy
    private boolean initialized;

    public FontRendererPatch(GameSettings a, ResourceLocation b, TextureManager c, boolean d) {
        super(a, b, c, d);
    }

    @Copy(Mode.APPEND)
    public void readGlyphSizes() {
        if (!initialized) {
            init();
        }
    }

    @Copy
    private void init() {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferStorage(GL_ARRAY_BUFFER, BUFFER_SIZE, INIT_FLAGS);
        ByteBuffer buffer = glMapBufferRange(GL_ARRAY_BUFFER, 0, BUFFER_SIZE, MAP_FLAGS, null);
        if (buffer == null) {
            throw new OpenGLException(glGetError());
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        this.vbo = vbo;
        this.buffer = buffer;
        this.initialized = true;
        this.syncs = new GLSync[BUFFER_COUNT];
        for (int i = 0; i < BUFFER_COUNT; ++i) {
            syncs[i] = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        }
    }

    @Copy(Mode.REPLACE)
    @Override
    protected void finalize() throws Throwable {
        try {
            for (GLSync sync : syncs) {
                glDeleteSync(sync);
            }
        } finally {
            super.finalize();
        }
    }

    @Copy
    private ByteBuffer getBuffer() {
        ByteBuffer buffer = this.buffer;
        buffer.position(offset).limit(offset + VBO_LENGTH);
        if (requireSync) {
            glClientWaitSync(syncs[offset / VBO_LENGTH], 0, TIMEOUT);
        }
        offset += VBO_LENGTH;
        if (offset >= BUFFER_SIZE) {
            requireSync = true;
            offset = 0;
        }
        return buffer;
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

        final int offset = this.offset;
        final ByteBuffer buffer = getBuffer();
        buffer
            .putFloat(posX + x2).putFloat(posY) // pos
            .putFloat(u / 256F).putFloat(v / 256F) // tex
            .putFloat(posX - x2).putFloat(posY + 7.99F) // pos
            .putFloat(u / 256F).putFloat((v + 15.98F) / 256F) // tex
            .putFloat(posX + x1 / 2F + x2).putFloat(posY) // pos
            .putFloat((u + x1) / 256F).putFloat(v / 256F) // tex
            .putFloat(posX + x1 / 2F - x2).putFloat(posY + 7.99F) // pos
            .putFloat((u + x1) / 256F).putFloat((v + 15.98F) / 256F); // tex

        glPushMatrix();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);

        glVertexPointer(2, GL_FLOAT, 16, offset);
        glTexCoordPointer(2, GL_FLOAT, 16, offset + 8);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glPopMatrix();

        return (f1 - f) / 2F + 1F;
    }

    @Override
    @Copy(Mode.REPLACE)
    public float renderDefaultChar(int ch, boolean italic) {
        int x = (ch & 15) << 3;
        int y = (ch >> 1) & ~7;

        int offX = italic ? 1 : 0;
        renderEngine.bindTexture(locationFontTexture);
        int width = charWidth[ch];
        float w = width - 0.01F;

        final float posX = this.posX;
        final float posY = this.posY;

        final int offset = this.offset;
        final ByteBuffer buffer = getBuffer();
        buffer
            .putFloat(posX + offX).putFloat(posY)
            .putFloat(x / 128F).putFloat(y / 128F)
            .putFloat(posX - offX).putFloat(posY + 7.99F)
            .putFloat(x / 128F).putFloat((y + 7.99F) / 128F)
            .putFloat(posX + w - 1F + offX).putFloat(posY)
            .putFloat((x + w - 1F) / 128F).putFloat(y / 128F)
            .putFloat(posX + w - 1F - offX).putFloat(posY + 7.99F)
            .putFloat((x + w - 1F) / 128F).putFloat((y + 7.99F) / 128F);

        glPushMatrix();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);

        glVertexPointer(2, GL_FLOAT, 16, offset);
        glTexCoordPointer(2, GL_FLOAT, 16, offset + 8);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glPopMatrix();

        return width;
    }

}
