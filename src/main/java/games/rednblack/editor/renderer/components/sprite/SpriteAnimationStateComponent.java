package games.rednblack.editor.renderer.components.sprite;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import games.rednblack.editor.renderer.components.BaseComponent;
import games.rednblack.editor.renderer.data.FrameRange;

import java.util.Comparator;
import java.util.Objects;

public class SpriteAnimationStateComponent implements BaseComponent {
    public Array<TextureAtlas.AtlasRegion> allRegions;
	public Animation<TextureRegion> currentAnimation;
	public float time = 0.0f;

    public  boolean paused = false;

    private FrameRange lastFrameRange;
    private int lastFPS;
    private Animation.PlayMode lastPlayMode;

    public SpriteAnimationStateComponent() {
    }

    public void setAllRegions(Array<TextureAtlas.AtlasRegion> allRegions) {
        this.allRegions = sortAndGetRegions(allRegions);
    }

	public Animation<TextureRegion> get() {
		return currentAnimation;
	}

    public void set(SpriteAnimationComponent sac) {
        set(sac.frameRangeMap.get(sac.currentAnimation), sac.fps, sac.playMode);
    }

    public void set(FrameRange range, int fps, Animation.PlayMode playMode) {
        if (Objects.equals(range, lastFrameRange) && fps == lastFPS && Objects.equals(playMode, lastPlayMode))
            return;

        Array<TextureAtlas.AtlasRegion> textureRegions = new Array<>(range.endFrame - range.startFrame + 1);
        for (int r = range.startFrame; r <= range.endFrame; r++) {
            textureRegions.add(allRegions.get(r));
        }

        currentAnimation =  new Animation<TextureRegion>(1f/fps, textureRegions, playMode);
        time = 0.0f;

        lastFrameRange = range;
        lastFPS = fps;
        lastPlayMode = playMode;
    }

    private Array<TextureAtlas.AtlasRegion> sortAndGetRegions(Array<TextureAtlas.AtlasRegion> regions) {
        regions.sort(new SortRegionsComparator());

        return regions;
    }

    @Override
    public void reset() {
        allRegions = null;
        currentAnimation = null;
        time = 0.0f;
        paused = false;
    }

    private static class SortRegionsComparator implements Comparator<TextureAtlas.AtlasRegion> {
        @Override
        public int compare(TextureAtlas.AtlasRegion o1, TextureAtlas.AtlasRegion o2) {
            return Integer.compare(o1.index, o2.index);
        }
    }
}
