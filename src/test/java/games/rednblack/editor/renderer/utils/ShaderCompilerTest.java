package games.rednblack.editor.renderer.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * An array-texture shader reads every unit it is compiled for, and only those: the count is what
 * decides whether a region on the second atlas page draws at all.
 */
public class ShaderCompilerTest {

    private static final String FRAGMENT = "uniform sampler2D u_textures[MAX_TEXTURE_UNITS];\n"
            + ShaderCompiler.GET_TEXTURE_FROM_ARRAY_PLACEHOLDER + "\n"
            + "void main() { gl_FragColor = getTextureFromArray(v_texCoords); }\n";

    @Test
    public void everyUnitHasItsBranch() {
        String source = ShaderCompiler.unrollArrayTexture(FRAGMENT, 8);
        assertTrue(source.startsWith("#define MAX_TEXTURE_UNITS 8\n"));
        for (int i = 0; i < 8; i++) {
            assertTrue("unit " + i, source.contains("return texture2D(u_textures[" + i + "], uv);"));
        }
        assertFalse(source.contains("u_textures[8]"));
        assertFalse(source.contains(ShaderCompiler.GET_TEXTURE_FROM_ARRAY_PLACEHOLDER));
    }

    @Test
    public void aSingleUnitReadsOnlyTheFirstPage() {
        String source = ShaderCompiler.unrollArrayTexture(FRAGMENT, 1);
        assertEquals(1, source.split("texture2D\\(u_textures\\[").length - 1);
    }
}
