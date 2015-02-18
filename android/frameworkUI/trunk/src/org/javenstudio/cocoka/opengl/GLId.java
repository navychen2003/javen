package org.javenstudio.cocoka.opengl;

import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

// This mimics corresponding GL functions.
public class GLId {
    private static int sNextId = 1;

    public synchronized static void glGenTextures(int n, int[] textures, int offset) {
        while (n-- > 0) {
            textures[offset + n] = sNextId++;
        }
    }

    public synchronized static void glGenBuffers(int n, int[] buffers, int offset) {
        while (n-- > 0) {
            buffers[offset + n] = sNextId++;
        }
    }

    public synchronized static void glDeleteTextures(GL11 gl, int n, int[] textures, int offset) {
        gl.glDeleteTextures(n, textures, offset);
    }

    public synchronized static void glDeleteBuffers(GL11 gl, int n, int[] buffers, int offset) {
        gl.glDeleteBuffers(n, buffers, offset);
    }

    public synchronized static void glDeleteFramebuffers(
            GL11ExtensionPack gl11ep, int n, int[] buffers, int offset) {
        gl11ep.glDeleteFramebuffersOES(n, buffers, offset);
    }
    
}
