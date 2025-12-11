package games.rednblack.editor.renderer.lights;

public class RayHandlerOptions {
	boolean gammaCorrection = false;
	boolean isDiffuse = false;

	boolean pseudo3d = false;
	boolean shadowColorInterpolation = false;
	int samples = 0;

	public void setDiffuse (boolean diffuse) {
		isDiffuse = diffuse;
	}

	public void setSamples(int samples) {
		this.samples = samples;
	}

	public void setGammaCorrection (boolean gammaCorrection) {
		this.gammaCorrection = gammaCorrection;
	}

	public void setPseudo3d (boolean pseudo3d) {
		setPseudo3d(pseudo3d, false);
	}

	public void setPseudo3d (boolean pseudo3d, boolean shadowColorInterpolation) {
		this.pseudo3d = pseudo3d;
		this.shadowColorInterpolation = shadowColorInterpolation;
	}
}
