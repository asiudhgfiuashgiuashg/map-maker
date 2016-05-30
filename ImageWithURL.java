import javafx.scene.image.Image;

/**
 * This class keeps track of the URL of the Image, which the vanilla Image class doesn't do.
 * We need to keep track of the URL of the image because the best known way to scale Tile and Object Images upon zoom is
 *  to load the Images with their requestedHeight and requestedWidth multiplied by a scalar. Attempting to scale the imageviews with the existing images
 *  results in blurry images, but we want pixelated images.
 */
public class ImageWithURL extends Image {
	private final String url;
	public ImageWithURL(String url,
	     double requestedWidth,
	     double requestedHeight,
	     boolean preserveRatio,
	     boolean smooth,
	     boolean backgroundLoading) {
		super(url, requestedWidth, requestedHeight, preserveRatio, smooth, backgroundLoading);
		this.url = url;
	}

	public ImageWithURL(String url) {
		super(url);
		this.url = url;
	}

	public String getURL() {
		return url;
	}
}