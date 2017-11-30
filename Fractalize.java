import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Stack;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.Color;

public class Fractalize {

	public static BufferedImage startImage;
	public static BufferedImage kImage;
	public static int[][] groups;
	public static int[] groupConv;
	public static ArrayList<int[]> layerColors = new ArrayList<int[]>();
	public static ArrayList<Integer> layerColors2;

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
		}
		return lpts;
	}

	// compute a_n
	public static double an(List<Complex> lpts) {
		double p = 1;
		Complex zn1 = lpts.get(lpts.size()-1);
		for (int j=0; j<lpts.size()-1; ++j) {
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

	public static void expand(int x, int y, int sub){
		int[] temp = new int[3];

		int ct = 0;
		int col, red, green, blue;
		Stack<int[]> yeet = new Stack<int[]>();
		yeet.push(new int[]{x, y, sub});
		int[] curr = null;

		while (!yeet.isEmpty()){
			curr = yeet.pop();
			// color stuff
			++ct;
			col = startImage.getRGB(curr[0],curr[1]);
			temp[0] = ((col >> 16) & 0xFF) + temp[0];
			temp[1] = ((col >> 8) & 0xFF) + temp[1];
			temp[2] = (col & 0xFF) + temp[2];
			// end color stuff
			groups[curr[0]][curr[1]] = sub;

			if ( followable(curr[0]-1,curr[1],sub) && kImage.getRGB(curr[0],curr[1])==kImage.getRGB(curr[0]-1,curr[1]) ) yeet.push(new int[]{curr[0]-1, curr[1], sub});	
			if ( followable(curr[0]+1,curr[1],sub) && kImage.getRGB(curr[0],curr[1])==kImage.getRGB(curr[0]+1,curr[1]) ) yeet.push(new int[]{curr[0]+1, curr[1], sub});	
			if ( followable(curr[0],curr[1]-1,sub) && kImage.getRGB(curr[0],curr[1])==kImage.getRGB(curr[0],curr[1]-1) ) yeet.push(new int[]{curr[0], curr[1]-1, sub});	
			if ( followable(curr[0],curr[1]+1,sub) && kImage.getRGB(curr[0],curr[1])==kImage.getRGB(curr[0],curr[1]+1) ) yeet.push(new int[]{curr[0], curr[1]+1, sub});
		}
		temp[0] = temp[0]/ct;
		temp[1] = temp[1]/ct;
		temp[2] = temp[2]/ct;
		// color stuff
		layerColors.add(temp);
		// end color stuff
	}

	public static boolean followable(int x,int y,int sub) {
		if (x>=0 && x<groups.length && y>=0 && y<groups[0].length && groups[x][y]!=sub) return true;
		return false;
	}

	public static void replaceGroups(int sub, int n) {
		for (int x=0; x<groups.length; ++x) {
			for (int y=0; y<groups[0].length; ++y) {
				if(groups[x][y]==sub) groups[x][y]=n;
			}
		}
	}

	public static ArrayList<BufferedImage> splitLayers(double ratio, int cutoff) {
		groups = new int[startImage.getWidth()][startImage.getHeight()];
		int count = 0;

		// make a 2d int array for which pixel is in which group
		for (int x=0; x<startImage.getWidth(); ++x) {
			for (int y=0; y<startImage.getHeight(); ++y) {
				if (groups[x][y]==0) {
						//System.out.println("new gp" + x + "," + y);
						++count;
						expand(x,y,-1);
						replaceGroups(-1,count);
				}
			}
		}

		// calculate group sizes
		int[] groupSizes = new int[count];
		groupConv = new int[count];
		int temp = 0;
		int rd = (new Color(200,0,0)).getRGB();

		for (int x=0; x<startImage.getWidth(); ++x) {
			for (int y=0; y<startImage.getHeight(); ++y) {
				groupSizes[groups[x][y]-1]+=1;
			}
		}

		// add only valid groups
		ArrayList<BufferedImage> layers = new ArrayList<BufferedImage>();
		layerColors2 = new ArrayList<Integer>();
		for (int i=0; i<count; ++i) {
			if (groupSizes[i]>=cutoff) {
				layers.add(new BufferedImage((int)(startImage.getWidth()*ratio),(int)(startImage.getHeight()*ratio), BufferedImage.TYPE_3BYTE_BGR));
				layerColors2.add( new Color( layerColors.get(i)[0],layerColors.get(i)[1],layerColors.get(i)[2] ).getRGB() );
				groupConv[i] = temp;
				++temp;
			}
		}

		// write valid groups to layer array
		for (int x=0; x<startImage.getWidth(); ++x) {
			for (int y=0; y<startImage.getHeight(); ++y) {
				if (groupSizes[groups[x][y]-1] >= cutoff) {
					layers.get( groupConv[groups[x][y]-1] ).setRGB(x,y,rd); 
				}
			}
		}
		System.out.println("done making seperate image layers");
		
		for (int i=0; i<layers.size(); ++i) {
			layers.set(i, sobel(layers.get(i)));
		}
		System.out.println("done applying sobel operator");
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
		if (S.size()>cutoff) return S;
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
		// variables for the program
		double scale = 1.0;
		double ratio = 1.0;
		int maxiters = 16;
		int lejas = 80;
		int colors = 4;
		int cutoff = 50;
		String fname = "in.png";

		for (int k=0; k<args.length; ++k) {
				 if (args[k].equals("--scale")) scale = Double.parseDouble(args[k+1]);
			else if (args[k].equals("--ratio")) ratio = Double.parseDouble(args[k+1]);
			else if (args[k].equals("--maxiters")) maxiters = Integer.parseInt(args[k+1]);
			else if (args[k].equals("--lejas")) lejas = Integer.parseInt(args[k+1]);
			else if (args[k].equals("--colors")) colors = Integer.parseInt(args[k+1]);
			else if (args[k].equals("--cutoff")) cutoff = Integer.parseInt(args[k+1]);
			else if (args[k].equals("-i")) fname = args[k+1];
		}

		// read/segment image
	    startImage = ImageIO.read(new File("in/"+fname));
		KMeans kmeans = new KMeans();
		kImage = kmeans.run(startImage,"out.png",colors,"i");
		ArrayList<BufferedImage> layersList = splitLayers(ratio, cutoff);
		int segments=layersList.size();
		System.out.println("segments: "+ segments);

		int col;
		BufferedImage image;
		// draw the group distribution
		image = new BufferedImage((int)(startImage.getWidth()),(int)(startImage.getHeight()), BufferedImage.TYPE_3BYTE_BGR);		
		for (int x=0; x<startImage.getWidth(); ++x) {
			for (int y=0; y<startImage.getHeight(); ++y) {
				col = new Color(0,0,0).getHSBColor((float)groups[x][y]/(float)segments ,(float)0.6,(float)0.6).getRGB();
				//col = layerColors2.get(groupConv[groups[x][y]-1]);
				image.setRGB(x,y,col);
			}
		}
		ImageIO.write(image, "png", new File("out/groups.png"));

		int xres = (int)(startImage.getWidth()*ratio);
		int yres = (int)(startImage.getHeight()*ratio);		
		image = new BufferedImage(xres, yres, BufferedImage.TYPE_4BYTE_ABGR);

		byte[] pixels;
		ArrayList<Complex> S;
		List<Complex> L;
		double asubn;
		Complex offset;
		Complex z;
		Complex z1;
		int samecount;
		int k;

		for (int index=0; index<segments; ++index) {
			System.out.print(index + "||");
			pixels = ((DataBufferByte) layersList.get(index).getRaster().getDataBuffer()).getData();
			S = bytes2set(pixels, xres, yres, scale, cutoff);
			if(S!=null) {
				System.out.print("set size: "+S.size());
				// recenter S (important for mathematical purposes)
				offset = norm(S).scale(scale);
				normalize(S,offset);
				
				// compute leja points
				System.out.print("//computing leja pts:");
				L = leja(S, Math.min(lejas, S.size()/2));
				asubn = an(L);
				System.out.println("//done w leja");
				// set color
				col = layerColors2.get(index);

				// iterate pixel by pixel
				for (int x=0; x<xres; ++x) {
					for(int y=0; y<yres; ++y) {
						samecount = 0;
						z = xy2complex(x,y,(int)((double)xres*ratio),(int)((double)yres*ratio),scale);
						z = z.minus(offset);
						k=0;
						while(k<maxiters && z.abs()<2) {
							++k;
							z1 = z;
							z = P(z, S, L, asubn);
							if ((z1.minus(z).abs())<0.0001) ++samecount;
							if (samecount==3) {
								k=maxiters;
							}
						}
						if (k>=maxiters) image.setRGB(x,y,col);
					}
				}
			}
		}
		ImageIO.write(image, "png", new File( "out/" + fname ));
		System.out.println("fractal - done");
	}
}