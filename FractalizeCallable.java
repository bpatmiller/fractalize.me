import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class FractalizeCallable implements Callable<BufferedImage> {
	private BufferedImage layer;
	private int col;
	private int index;

	public FractalizeCallable(int index, BufferedImage layer, int col) {
		this.index = index;
		this.layer = layer;
		this.col = col;
	}

	@Override
	public BufferedImage call() {
		byte[] pixels;
		List<Complex> S, L;
		double a_n;
		Complex offset, z, z1;
		int samecount, k;

		BufferedImage output = new BufferedImage(Fractalize.xres, Fractalize.yres, BufferedImage.TYPE_4BYTE_ABGR);

		// System.out.print(this.index + "||");
		pixels = ((DataBufferByte) this.layer.getRaster().getDataBuffer()).getData();
		S = Fractalize.bytes2set(pixels, Fractalize.xres, Fractalize.yres, Fractalize.scale, Fractalize.cutoff);
		
		if (S!=null) {
			// System.out.print("set size: "+S.size());
			
			// recenter S (important for mathematical purposes)
			offset = Fractalize.norm(S).scale(Fractalize.scale);
			Fractalize.normalize(S,offset);
			
			// compute leja points
			// System.out.print("//computing leja pts:");
			L = Fractalize.leja(S, Math.min(Fractalize.lejas, S.size()/2));
			a_n = Fractalize.an(L);
			// System.out.print("//done w leja");

			// iterate pixel by pixel
			for (int x=0; x<Fractalize.xres; ++x) {
				for(int y=0; y<Fractalize.yres; ++y) {
					samecount = 0;
					z = Fractalize.xy2complex(
						x,y,
						(int)((double)Fractalize.xres*Fractalize.ratio),
						(int)((double)Fractalize.yres*Fractalize.ratio),
						Fractalize.scale
					);
					z = z.minus(offset);
					k=0;
					while(k<Fractalize.maxiters && z.abs()<2) {
						++k;
						z1 = z;
						z = Fractalize.P(z, S, L, a_n);
						if ((z1.minus(z).abs())<0.0001) ++samecount;
						if (samecount==3) {
							k=Fractalize.maxiters;
						}
					}
					if (k>=Fractalize.maxiters) output.setRGB(x,y,this.col);
				}
			}
		}

		return output;
	}
}