package games.rednblack.editor.renderer.resources;

/**
 * Created by azakhary on 9/9/2014.
 */
public interface IAssetLoader {
    void loadAtlasPacks();
    void loadParticleEffects();
    void loadSpriteAnimations();
    void loadFonts();
    void loadBitmapFonts();
    void loadShaders();

    void loadExternalTypes();
}
