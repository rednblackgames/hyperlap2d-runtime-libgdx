package games.rednblack.editor.renderer.utils;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import games.rednblack.editor.renderer.utils.value.DynamicValue;

public class ABAtlasRegion extends TextureAtlas.AtlasRegion {

    private final TextureAtlas.AtlasRegion regionB;
    private DynamicValue<Boolean> useRegionB;

    public ABAtlasRegion(TextureAtlas.AtlasRegion regionA, TextureAtlas.AtlasRegion regionB) {
        this(regionA, regionB, null);
    }

    public ABAtlasRegion(TextureAtlas.AtlasRegion regionA, TextureAtlas.AtlasRegion regionB, DynamicValue<Boolean> useRegionB) {
        super(regionA);
        this.regionB = regionB;
        this.useRegionB = useRegionB;
    }

    public void setUseRegionB(DynamicValue<Boolean> useRegionB) {
        this.useRegionB = useRegionB;
    }

    @Override
    public float getRotatedPackedHeight() {
        if (useRegionB != null && useRegionB.get())
            return regionB.getRotatedPackedHeight();
        else
            return super.getRotatedPackedHeight();
    }

    @Override
    public float getRotatedPackedWidth() {
        if (useRegionB != null && useRegionB.get())
            return regionB.getRotatedPackedWidth();
        else
            return super.getRotatedPackedWidth();
    }

    @Override
    public float getU() {
        if (useRegionB != null && useRegionB.get())
            return regionB.getU();
        else
            return super.getU();
    }

    @Override
    public float getU2() {
        if (useRegionB != null && useRegionB.get())
            return regionB.getU2();
        else
            return super.getU2();
    }

    @Override
    public float getV() {
        if (useRegionB != null && useRegionB.get())
            return regionB.getV();
        else
            return super.getV();
    }

    @Override
    public float getV2() {
        if (useRegionB != null && useRegionB.get())
            return regionB.getV2();
        else
            return super.getV2();
    }

    @Override
    public int getRegionHeight() {
        if (useRegionB != null && useRegionB.get())
            return regionB.getRegionHeight();
        else
            return super.getRegionHeight();
    }

    @Override
    public int getRegionWidth() {
        if (useRegionB != null && useRegionB.get())
            return regionB.getRegionWidth();
        else
            return super.getRegionWidth();
    }

    @Override
    public int getRegionX() {
        if (useRegionB != null && useRegionB.get())
            return regionB.getRegionX();
        else
            return super.getRegionX();
    }

    @Override
    public int getRegionY() {
        if (useRegionB != null && useRegionB.get())
            return regionB.getRegionY();
        else
            return super.getRegionY();
    }

    @Override
    public Texture getTexture() {
        if (useRegionB != null && useRegionB.get())
            return regionB.getTexture();
        else
            return super.getTexture();
    }

    @Override
    public void setRegion(Texture texture) {
        if (useRegionB != null && useRegionB.get())
            regionB.setRegion(texture);
        else
            super.setRegion(texture);
    }

    @Override
    public void setRegion(TextureRegion region) {
        if (useRegionB != null && useRegionB.get())
            regionB.setRegion(region);
        else
            super.setRegion(region);
    }

    @Override
    public void setRegion(int x, int y, int width, int height) {
        if (useRegionB != null && useRegionB.get())
            regionB.setRegion(x, y, width, height);
        else
            super.setRegion(x, y, width, height);
    }

    @Override
    public void setRegion(float u, float v, float u2, float v2) {
        if (useRegionB != null && useRegionB.get())
            regionB.setRegion(u, v, u2, v2);
        else
            super.setRegion(u, v, u2, v2);
    }

    @Override
    public void setRegion(TextureRegion region, int x, int y, int width, int height) {
        if (useRegionB != null && useRegionB.get())
            regionB.setRegion(region, x, y, width, height);
        else
            super.setRegion(region, x, y, width, height);
    }

    @Override
    public void setRegionHeight(int height) {
        if (useRegionB != null && useRegionB.get())
            regionB.setRegionHeight(height);
        else
            super.setRegionHeight(height);
    }

    @Override
    public void setRegionWidth(int width) {
        if (useRegionB != null && useRegionB.get())
            regionB.setRegionWidth(width);
        else
            super.setRegionWidth(width);
    }

    @Override
    public void setRegionX(int x) {
        if (useRegionB != null && useRegionB.get())
            regionB.setRegionX(x);
        else
            super.setRegionX(x);
    }

    @Override
    public void setRegionY(int y) {
        if (useRegionB != null && useRegionB.get())
            regionB.setRegionY(y);
        else
            super.setRegionY(y);
    }

    @Override
    public void setTexture(Texture texture) {
        if (useRegionB != null && useRegionB.get())
            regionB.setTexture(texture);
        else
            super.setTexture(texture);
    }

    @Override
    public void setU(float u) {
        if (useRegionB != null && useRegionB.get())
            regionB.setU(u);
        else
            super.setU(u);
    }

    @Override
    public void setU2(float u2) {
        if (useRegionB != null && useRegionB.get())
            regionB.setU2(u2);
        else
            super.setU2(u2);
    }

    @Override
    public void setV(float v) {
        if (useRegionB != null && useRegionB.get())
            regionB.setV(v);
        else
            super.setV(v);
    }

    @Override
    public void setV2(float v2) {
        if (useRegionB != null && useRegionB.get())
            regionB.setV2(v2);
        else
            super.setV2(v2);
    }

    @Override
    public boolean isFlipX() {
        if (useRegionB != null && useRegionB.get())
            return regionB.isFlipX();
        else
            return super.isFlipX();
    }

    @Override
    public boolean isFlipY() {
        if (useRegionB != null && useRegionB.get())
            return regionB.isFlipY();
        else
            return super.isFlipY();
    }

    @Override
    public boolean equals(Object o) {
        if (useRegionB != null && useRegionB.get())
            return regionB.equals(o);
        else
            return super.equals(o);
    }

    @Override
    public int hashCode() {
        if (useRegionB != null && useRegionB.get())
            return regionB.hashCode();
        else
            return super.hashCode();
    }

    @Override
    public int[] findValue(String name) {
        if (useRegionB != null && useRegionB.get())
            return regionB.findValue(name);
        else
            return super.findValue(name);
    }

    @Override
    public TextureRegion[][] split(int tileWidth, int tileHeight) {
        if (useRegionB != null && useRegionB.get())
            return regionB.split(tileWidth, tileHeight);
        else
            return super.split(tileWidth, tileHeight);
    }

    @Override
    public void flip(boolean x, boolean y) {
        if (useRegionB != null && useRegionB.get())
            regionB.flip(x, y);
        else
            super.flip(x, y);
    }

    @Override
    public void scroll(float xAmount, float yAmount) {
        if (useRegionB != null && useRegionB.get())
            regionB.scroll(xAmount, yAmount);
        else
            super.scroll(xAmount, yAmount);
    }

    @Override
    public String toString() {
        if (useRegionB != null && useRegionB.get())
            return regionB.toString();
        else
            return super.toString();
    }
}