import java.util.ArrayList;

public class PixelGrid {
	/*
	pixels is a 2d array that numbers each pixel by group
	visited is a 2d boolean array that tells if a given pixel
	  has been "visited" by the expand process
	colors is a 1d array the contains colors which correspond
	  to each group
	*/
	int[][] pixels;
	boolean[][] visited;
	ArrayList<Integer> colors;
	int w;
	int h;

	public PixelGrid(int w, int h) {
		pixels = new int[w][h];
		visited = new boolean[w][h];
		colors = new ArrayList<Integer>();

		this.w = w;
		this.h = h;
	}

	public boolean isExpandable(int x, int y) {
		if ( ( ! visited[x][y] ) && x>=0 && x<w && y>=0 && y<h) {
			return true;
		}
		else {
			return false;
		}
	}


	public int getPixelGroup(int x, int y) {
		return pixels[x][y];
	}

	public int getGroupSize(int group) {
		int count = 0;
		for (int x=0; x<w; x++) {
			for (int y=0; y<h; y++) {
				if ( getPixelGroup(x,y) == group ) {
					count++;
				}
			}
		}
		return count;
	}

	public int getGroupColor(int group) {
		return colors.get(group);
	}
}