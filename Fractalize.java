import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.Color;

public class Fractalize {

	public static BufferedImage startImage;
	public static int[][] groups;

	// mostly for debugging purposes, generates a big "H"
	public static ArrayList<Complex> unitH( int n ) {
		ArrayList<Complex> S = new ArrayList<Complex>();
		for (int i=-n; i<=n; ++i) {
			for (int j=-n; j<=n; ++j) {
				if ( (Math.abs((float)i/(float)n) > 0.6) || (((float)j/(float)n>=-0.3) && ((float)j/(float)n<=0.3)))
					S.add(new Complex((double)i/(double)n, (double)j/(double)n));
			}
		}
		return S;
	}
	// generates a filled unit circle
	public static ArrayList<Complex> unitCircle( int n ) {
		ArrayList<Complex> S = new ArrayList<Complex>();
		for (int i=-n; i<=n; ++i) {
			for (int j=-n; j<=n; ++j) {
				if ( new Complex((double)i/(double)n, (double)j/(double)n).abs()<0.5 )
					S.add(new Complex((double)i/(double)n, (double)j/(double)n));
			}
		}
		return S;
	}
	// calculates PI(z-z_j)
	public static double piz( List<Complex> lpts, ArrayList<Complex> S, int s) {
		int ll = lpts.size();
		if (ll==0) {
			return (S.get(s)).abs();
		} else if (ll==1) {
			return ((S.get(s)).minus(lpts.get(0))).abs();
		}
		double k = (((S.get(s)).minus(lpts.get(ll-1))).abs());
		lpts.remove(ll-1);
		return ( k * (piz(lpts, S,s)));
	}

	// generate n leja points given a set S
	public static List<Complex> leja( ArrayList<Complex> S, int n) {
		if (n>S.size()/2) n = S.size()/2;
		int sl = S.size();
		List<Complex> lpts = new ArrayList<Complex>(n);
		//System.out.println();
		//System.out.print("progress out of "+n+":");
		for (int j=0; j<n; ++j) {
			double max = 0;
			int smax = 0;
			double curr = 0;
			for (int s=0; s<sl; ++s) {
				List<Complex> dummylpts = new ArrayList(lpts);
				curr = piz(dummylpts, S, s);
				if (curr > max) {
					max = curr;
					smax = s;
				}
			}
			lpts.add(S.get(smax));
			S.remove(smax);
			--sl;
			//System.out.print(j+"|");
		}
		return lpts;
	}

	// compute a_n
	public static double an(List<Complex> lpts) {
		double p = 1;
		int ll = lpts.size();
		Complex zn1 = lpts.get(ll-1);
		for (int j=0; j<ll-1; ++j) {
			p*=(zn1.minus(lpts.get(j))).abs();
		}
		return p;
	}

	// compute P(z)
	public static Complex P(Complex z, ArrayList<Complex> E, List<Complex> lpts, double asubn) {
		double n = (double)lpts.size();
		double s = 1/n;
		Complex p = z.minus(lpts.get(0));
		for (int j=1; j<n; ++j) {
			p = p.times(z.minus(lpts.get(j)));
		}
		return (p.times(z)).scale(Math.exp(0.5)/asubn);
	}

	// translate x,y coordinates to a complex number
	public static Complex xy2complex(int x, int y, int xres, int yres, double scale) {
		return new Complex(scale*(((double)x/(double)xres)-0.5), scale*(((double)y/(double)yres)-0.5));
	}

	public static int complex2x(Complex z, int xres, double scale) {
		int x = (int)(((z.re()/scale)+0.5)*(double)xres);
		if (x>=0 && x<xres) return x;
		return 0;
	}

	public static int complex2y(Complex z, int yres, double scale) {
		int y = (int)(((z.im()/scale)+0.5)*(double)yres);
		if (y>=0 && y<yres) return y;
		return 0;
	}

    public static int getGrayScale(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = (rgb) & 0xff;

        //from https://en.wikipedia.org/wiki/Grayscale, calculating luminance
        int gray = (int)(0.2126 * r + 0.7152 * g + 0.0722 * b);
        //int gray = (r + g + b) / 3;

        return gray;
    }

    // sobel edge detection
    public static BufferedImage sobel(BufferedImage image) {
        int x = image.getWidth();
        int y = image.getHeight();

        int maxGval = 0;
        int[][] edgeColors = new int[x][y];
        int maxGradient = -1;

        for (int i = 1; i < x - 1; i++) {
            for (int j = 1; j < y - 1; j++) {

                int val00 = getGrayScale(image.getRGB(i - 1, j - 1));
                int val01 = getGrayScale(image.getRGB(i - 1, j));
                int val02 = getGrayScale(image.getRGB(i - 1, j + 1));

                int val10 = getGrayScale(image.getRGB(i, j - 1));
                int val11 = getGrayScale(image.getRGB(i, j));
                int val12 = getGrayScale(image.getRGB(i, j + 1));

                int val20 = getGrayScale(image.getRGB(i + 1, j - 1));
                int val21 = getGrayScale(image.getRGB(i + 1, j));
                int val22 = getGrayScale(image.getRGB(i + 1, j + 1));

                int gx =  ((-1 * val00) + (0 * val01) + (1 * val02)) 
                        + ((-2 * val10) + (0 * val11) + (2 * val12))
                        + ((-1 * val20) + (0 * val21) + (1 * val22));

                int gy =  ((-1 * val00) + (-2 * val01) + (-1 * val02))
                        + ((0 * val10) + (0 * val11) + (0 * val12))
                        + ((1 * val20) + (2 * val21) + (1 * val22));

                double gval = Math.sqrt((gx * gx) + (gy * gy));
                int g = (int) gval;

                if(maxGradient < g) {
                    maxGradient = g;
                }

                edgeColors[i][j] = g;
            }
        }

        double scale = 255.0 / maxGradient;

        for (int i = 1; i < x - 1; i++) {
            for (int j = 1; j < y - 1; j++) {
                int edgeColor = edgeColors[i][j];
                edgeColor = (int)(edgeColor * scale);
                edgeColor = 0xff000000 | (edgeColor << 16) | (edgeColor << 8) | edgeColor;

                image.setRGB(i, j, edgeColor);
            }
        }

		int black = (new Color(0,0,0)).getRGB();
        for (int i=0; i<image.getWidth(); i+=image.getWidth()-1) {
        	for (int j=0; j<image.getHeight(); ++j) {
        		image.setRGB(i,j,black);
        	}
        }
		for (int i=0; i<image.getHeight(); i+=image.getHeight()-1) {
        	for (int j=0; j<image.getWidth(); ++j) {
        		image.setRGB(j,i,black);
        	}
        }

        return image;
    }

	public static void expand(int x, int y, int n, int depth){
		--depth;
		if (depth>0) {
			groups[x][y]=n;
			boolean bottom = false;
			boolean top = false;
			boolean left = false;
			boolean right = false;
			// check for bounds:
			if( x<=0 ) left = true;
			if( x>=groups.length-1) right = true;
			if( y<=0 ) bottom = true ;
			if( y>=groups[0].length-1) top = true;

			if( !left ) {
				if( (startImage.getRGB(x-1,y)==startImage.getRGB(x,y)) && (groups[x-1][y]==0) ) expand(x-1,y,n,depth);
				if ( !bottom ) {
					if( (startImage.getRGB(x-1,y-1)==startImage.getRGB(x,y)) && (groups[x-1][y-1]==0) ) expand(x-1,y-1,n,depth);
				}
				if ( !top ) {
					if( (startImage.getRGB(x-1,y+1)==startImage.getRGB(x,y)) && (groups[x-1][y+1]==0) ) expand(x-1,y+1,n,depth);
				}
			}
			if( !right ) {
				if( (startImage.getRGB(x+1,y)==startImage.getRGB(x,y)) && (groups[x+1][y]==0) ) expand(x+1,y,n,depth);
				if ( !bottom ) {
					if( (startImage.getRGB(x+1,y-1)==startImage.getRGB(x,y)) && (groups[x+1][y-1]==0) ) expand(x+1,y-1,n,depth);
				}
				if ( !top ) {
					if( (startImage.getRGB(x+1,y+1)==startImage.getRGB(x,y)) && (groups[x+1][y+1]==0) ) expand(x+1,y+1,n,depth);
				}
			}
			if( !bottom ) {
				if( (startImage.getRGB(x,y-1)==startImage.getRGB(x,y)) && (groups[x][y-1]==0) ) expand(x,y-1,n,depth);
			}
			if( !top ) {
				if( (startImage.getRGB(x,y+1)==startImage.getRGB(x,y)) && (groups[x][y+1]==0) ) expand(x,y+1,n,depth);
			}
		}	
	}

	public static ArrayList<BufferedImage> splitLayers(double ratio) {
		groups = new int[startImage.getWidth()][startImage.getHeight()];
		int count = 0;

		// make a 2d int array for which pixel is in which group
		for (int x=0; x<startImage.getWidth(); ++x) {
			for (int y=0; y<startImage.getHeight(); ++y) {
				if (groups[x][y]==0) {
					++count;
					expand(x,y,count,2000);
				} else {
					expand(x,y,groups[x][y],2000);
				}

			}
		}

		// use that int array to split the input image into layers
		ArrayList<BufferedImage> layers = new ArrayList<BufferedImage>(count);
		for (int i=0; i<count; ++i) {
			layers.add(new BufferedImage((int)(startImage.getWidth()*ratio),(int)(startImage.getHeight()*ratio), BufferedImage.TYPE_3BYTE_BGR));
		}

		for (int x=0; x<startImage.getWidth(); ++x) {
			for (int y=0; y<startImage.getHeight(); ++y) {
				(layers.get(groups[x][y]-1)).setRGB(x,y,new Color(200,0,0).getRGB());
			}
		}

		for (int i=0; i<count; ++i) {
			layers.set(i, sobel(layers.get(i)));
		}
		return layers;
	}

	public static ArrayList<Complex> bytes2set(byte[] pixels, int xres, int yres,double scale, int cutoff) {
		ArrayList S = new ArrayList();
		int step;
		if (pixels.length/(xres*yres)==3) {
		for (int i=0; i<pixels.length; i+=3) {
			if ( (pixels[i]+pixels[i+1]+pixels[i+2])/3.0 <=-0.5) {
				int x = (i/3)%xres;
				int y = (i/3)/xres;
				S.add( xy2complex(x,y,xres,yres,scale) );
			}
		}		} else {
		for (int i=0; i<pixels.length; i+=4) {
			if ( (pixels[i+1]+pixels[i+2]+pixels[i+3])/3.0 <=-0.5) {
				int x = (i/4)%xres;
				int y = (i/4)/xres;
				S.add( xy2complex(x,y,xres,yres,scale) );
			}
		}		}

		if (S.size()>=cutoff) return S;
		return null;
	}

	public static Complex norm(ArrayList<Complex> S) {
		double x=0;
		double y=0;
		for (int i=0; i<S.size(); ++i) {
			x+=S.get(i).re();
			y+=S.get(i).im();
		}
		x/=S.size();
		y/=S.size();
		return new Complex(x,y);
	}

	public static void normalize(ArrayList<Complex> S, Complex z) {
		for (int i=0; i<S.size(); ++i) {
			S.set(i, S.get(i).minus(z));
		}
	}

	public static void main( String[] args ) throws IOException {
		// final variables for the program
		final double scale = 1.0;
		final double ratio = 1.0;
		final int maxiters = 62;
		final int lejas = 180;
		//final double lejaRatio = 0.1;
		final int colors = 2;
		final int cutoff = 30;

		// read input file
		KMeans kmeans = new KMeans();
		startImage = kmeans.run("in/in.png","out.png",colors,"i");
		ImageIO.write(startImage, "png", new File("out/kmeans.png"));
		ArrayList<BufferedImage> layersList = splitLayers(ratio);
		BufferedImage[] layers = new BufferedImage[layersList.size()];
		layers = layersList.toArray(layers);
		int segments = layers.length;

		BufferedImage image = new BufferedImage((int)(startImage.getWidth()*ratio),(int)(startImage.getHeight()*ratio), BufferedImage.TYPE_3BYTE_BGR);
		// draw the group distribution
		for (int x=0; x<startImage.getWidth(); ++x) {
			for (int y=0; y<startImage.getHeight(); ++y) {
				int cc = new Color(0,0,0).getHSBColor((float)groups[x][y]/(float)segments ,(float)0.6,(float)0.6).getRGB();
				image.setRGB(x,y,cc);

			}
		}
		ImageIO.write(image, "png", new File("out/groups.png"));
		

		// draw out the layers of the image
		/*for (int index=0; index<segments; ++index) {
			image = layers[index];
			ImageIO.write(image, "png", new File("layer"+index+".png"));
		}*/

		int xres = (int)(startImage.getWidth()*ratio);
		int yres = (int)(startImage.getHeight()*ratio);
		int col;
		boolean conv = true;
		byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		ArrayList<Complex> S;
		List<Complex> L;
		image = new BufferedImage(xres,yres,BufferedImage.TYPE_3BYTE_BGR);
		Complex offset;
	    startImage = ImageIO.read(new File("in/in.png"));

		for (int index=0; index<segments; ++index) {
			System.out.print(index + "||");
			pixels = ((DataBufferByte) layers[index].getRaster().getDataBuffer()).getData();
			S = bytes2set(pixels, xres, yres, scale, cutoff);
			if(S!=null) {
				offset = norm(S).scale(scale);
				normalize(S,offset);
				//System.out.println(offset);
				System.out.print("set size: "+S.size());
				System.out.print("//computing leja pts:");
				// compute leja points
				L = leja(S,lejas);
				double asubn = an(L);
				System.out.println("//done w leja");
				// init vars for fractal
				int k1 = 0;
				Color myColor;
				// iterate pixel by pixel
				for (int x=0; x<xres; ++x) {
					for(int y=0; y<yres; ++y) {
						conv = true;
						Complex z = xy2complex(x,y,xres,yres,scale);
						z = z.minus(offset);
						//z = z.plus(offset);
						for (int k=0; k<maxiters; ++k) {
							k1 = k;
							z = P(z, S, L, asubn);
							if (z.abs()>2) {
								conv = false;
							}
						}
						if (conv) {
							myColor = Color.getHSBColor((float)index/(float)segments,(float)0.6,(float)0.6);
							//col = startImage.getRGB( complex2x(offset, xres, scale), complex2y(offset, yres, scale));
							col = myColor.getRGB();
							image.setRGB(x,y,col);
						}
					}
				}
			}
			ImageIO.write(image, "png", new File("out/fractal.png"));
			}
			System.out.println("fractal - done");
			// write fractal to file
			//ImageIO.write(image, "png", new File("fractal.png"));
			System.out.println("image output (fractal) - done");
	}
}