public class ANSI {
	public static final char ESCAPE = 27;
	public static final char CSI = '[';

	/**
	 * Clears the current line on the console.
	 */
	public static synchronized void clearLine() {
		printCSISeq("1K"); //clear to first col
		printCSISeq("1G"); //move cursor to first col
	}

	/**
	 * Moves the cursor relative to its current position.
	 */
	public static synchronized void moveCursor(int x, int y) {
		if (y < 0)
			printCSISeq(String.format("%dA", Math.abs(y))); //cursor up
		if (y > 0)
			printCSISeq(String.format("%dB", y)); //cursor down
		if (x < 0)
			printCSISeq(String.format("%dD", Math.abs(x))); //cursor back
		if (x > 0)
			printCSISeq(String.format("%dC", x)); //cursor forward
	}

	private static <T> void print(T item) {
		System.out.print(item);
	}

	private static void printCSISeq(String seq) {
		print(ESCAPE);
		print(CSI);
		print(seq);
	}

	/**
	 * A single-line progress bar.
	 * Has configurable width and maximum value.
	 */
	public static class Progress {
		public final int width;
		public final double max;
		private double val = 0;
		private boolean once = false;
		
		public Progress(int width) {
			this(width, 1);
		}

		public Progress(int width, int max) {
			this.width = width;
			this.max = max;
		}

		/**
		 * Update the value of the progress bar and print it.
		 */
		public synchronized void updateValue(double val) {
			double existing = getValue();
			if (val > existing) {
				setValue(val);
				print();
			}
		}

		public synchronized void setValue(double val) {
			if (val > max)
				val = max;
			this.val = val;
		}

		public synchronized double getValue() {
			return val;
		}

		/**
		 * Redraw the progress bar
		 */
		public synchronized void print() {
			int progress = (int)Math.round(width*val/max);

			//don't squash the line above the progress bar
			if (once)
				moveCursor(0, -1);
			else
				once = true;

			clearLine();
			ANSI.print("[");
			printCSISeq("38;5;130m"); //color
			
			//draw bar
			for (int i=0; i<progress-1; i++)
				ANSI.print("=");
			ANSI.print(">");
			for (int i=0; i<width-progress; i++)
				ANSI.print(" ");

			printCSISeq("0m"); //reset color
			ANSI.print("]");

			ANSI.print(String.format(" %.0f%%", 100*val/max)); //display percent
			ANSI.print("\n");
		}
	}
}