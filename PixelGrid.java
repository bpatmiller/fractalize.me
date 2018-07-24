public class PixelGrid {
	int[][] pixels;

	public void PixelGrid(int w, int h) {
		pixels = new int[w][h];
	}

	public int getGroup(int x, int y) {
		return pixels[x][y];
	}
}