import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Stack;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.Color;
import java.awt.Graphics2D;

public class Fractalize {

	public static BufferedImage startImage;
	public static BufferedImage kImage;
	public static int[][] groups;
	public static int[] groupConv;
	public static List<int[]> layerColors = new ArrayList<>();
	public static List<Integer> layerColors2;

	// calculates PI(z-z_j)
	public static double piz(List<Complex> lpts, List<Complex> S, int s) {
		if (lpts.size() == 0)
			return S.get(s).abs();

		//compute product
		double k = 1;
		for (Complex a : lpts)
			k *= S.get(s).minus(a).abs();

		return k;
	}

	// generate n leja points given a set S
	public static List<Complex> leja(List<Complex> S, int n) {
		if (n>S.size()/2) n = S.size()/2;
		int sl = S.size();
		List<Complex> lpts = new ArrayList<>(n);
		for (int j=0; j<n; ++j) {
			double max = 0;
			int smax = 0;
			double curr = 0;
			for (int s=0; s<sl; ++s) {
				curr = piz(lpts, S, s);
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
		for (int j=0; j<lpts.size()-1; ++j) {
			p*=(lpts.get(lpts.size()-1).minus(lpts.get(j))).abs();
		}
		return p;
	}

	// compute P(z)
	public static Complex P(Complex z, List<Complex> E, List<Complex> lpts, double asubn) {
		double n = (double)lpts.size();
		double s = 1/n;

		double zRe = z.re(), zIm = z.im();
		double pRe = zRe - lpts.get(0).re(), pIm = zIm - lpts.get(0).im();
		double pRe2, pIm2;

		for (int j=1; j<n; ++j) {
			double re = zRe - lpts.get(j).re();
			double im = zIm - lpts.get(j).im();
			pRe2 = pRe * re - pIm * im;
			pIm2 = pRe * im + pIm * re;
			pRe = pRe2;
			pIm = pIm2;
		}
		
		double scale = Math.exp(0.5) / asubn;
		pRe2 = (pRe * zRe - pIm * zIm) * scale;
		pIm2 = (pRe * zIm + pIm * zRe) * scale;
		pRe = pRe2;
		pIm = pIm2;
		return new Complex(pRe, pIm);
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

    //sobel edge detection
    public static BufferedImage sobel(BufferedImage image) {
    	int w = image.getWidth();
    	int h = image.getHeight();

    	//convert input to INT_ARGB
    	BufferedImage input = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    	Graphics2D igfx = input.createGraphics();
    	igfx.drawImage(image, null, null);
    	igfx.dispose();

		BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		
		//create input buffer
		Raster rSrc = input.getRaster();
		DataBufferInt dSrc = (DataBufferInt)rSrc.getDataBuffer();
		int[] src = dSrc.getData();

		//create output buffer
		WritableRaster rDst = output.getRaster();
		DataBufferInt dDst = (DataBufferInt)rDst.getDataBuffer();
		int[] dst = dDst.getData();

		//apply sobel operator
    	int maxGval = 0;
    	int[][] edgeColors = new int[w][h];
    	int maxGradient = -1;

    	int[][] srcMat = {{0,0,0}, {0,0,0}, {0,0,0}};

    	for (int x=1; x<w-1; ++x) {
    		for (int y=1; y<h-1; ++y) {
    			//sample input image
    			srcMat[0][0] = getGrayScale(src[(y-1)*w+(x-1)]);
    			srcMat[0][1] = getGrayScale(src[(y  )*w+(x-1)]);
    			srcMat[0][2] = getGrayScale(src[(y+1)*w+(x-1)]);

    			srcMat[1][0] = getGrayScale(src[(y-1)*w+(x  )]);
    			srcMat[1][1] = getGrayScale(src[(y  )*w+(x  )]);
    			srcMat[1][2] = getGrayScale(src[(y+1)*w+(x  )]);

    			srcMat[2][0] = getGrayScale(src[(y-1)*w+(x+1)]);
    			srcMat[2][1] = getGrayScale(src[(y  )*w+(x+1)]);
    			srcMat[2][2] = getGrayScale(src[(y+1)*w+(x+1)]);

    			//compute gradient
    			int gx =  ((-1 * srcMat[0][0]) + ( 0 * srcMat[0][1]) + ( 1 * srcMat[0][2])) 
                        + ((-2 * srcMat[1][0]) + ( 0 * srcMat[1][1]) + ( 2 * srcMat[1][2]))
                        + ((-1 * srcMat[2][0]) + ( 0 * srcMat[2][1]) + ( 1 * srcMat[2][2]));

                int gy =  ((-1 * srcMat[0][0]) + (-2 * srcMat[0][1]) + (-1 * srcMat[0][2]))
                        + (( 0 * srcMat[1][0]) + ( 0 * srcMat[1][1]) + ( 0 * srcMat[1][2]))
                        + (( 1 * srcMat[2][0]) + ( 2 * srcMat[2][1]) + ( 1 * srcMat[2][2]));

                double gval = Math.sqrt((gx * gx) + (gy * gy));
                int g = (int) gval;

                if(maxGradient < g)
                    maxGradient = g;

                edgeColors[x][y] = g;
    		}
    	}

    	double scale = 255.0 / maxGradient;

    	//render output image
        for (int x=1; x<w-1; ++x) {
            for (int y=1; y<h-1; ++y) {
                int edgeColor = edgeColors[x][y];
                edgeColor = (int)(edgeColor * scale);
                edgeColor = 0xff000000 | (edgeColor << 16) | (edgeColor << 8) | edgeColor;

                dst[y*w+x] = edgeColor;
            }
        }

        //clear edge pixels
		int black = (new Color(0,0,0)).getRGB();
		for (int x=0; x<w; ++x) {
			dst[x] = black;
			dst[(h-1)*w+x] = black;
		}
		for (int y=0; y<h; ++y) {
			dst[y*w] = black;
			dst[y*w + w-1] = black;
		}

		Graphics2D gfx = image.createGraphics();
        gfx.drawImage(output, null, null);
        gfx.dispose();
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

	public static List<BufferedImage> splitLayers(double ratio, int cutoff) {
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
		List<BufferedImage> layers = new ArrayList<>();
		layerColors2 = new ArrayList<>();
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
		System.out.println("done making separate image layers");
		
		for (int i=0; i<layers.size(); ++i) {
			layers.set(i, sobel(layers.get(i)));
		}
		System.out.println("done applying sobel operator");
		return layers;
	}

	public static List<Complex> bytes2set(byte[] pixels, int xres, int yres,double scale, int cutoff) {
		List<Complex> S = new ArrayList<>();
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

	public static Complex norm(List<Complex> S) {
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

	public static void normalize(List<Complex> S, Complex z) {
		for (int i=0; i<S.size(); ++i) {
			S.set(i, S.get(i).minus(z));
		}
	}

	public static int threadCount = Runtime.getRuntime().availableProcessors();
	public static int lejas = 80;
	public static int cutoff = 50;
	public static double scale = 1.0;
	public static double ratio = 1.0;
	public static int maxiters = 16;
	public static int xres, yres;
	public static void main(String[] args) throws IOException {
		// variables for the program
		int colors = 4;
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
		List<BufferedImage> layersList = splitLayers(ratio, cutoff);
		int segments=layersList.size();
		System.out.println("segments: "+ segments);
						
		// draw the group distribution
		BufferedImage groupsImg = new BufferedImage(startImage.getWidth(), startImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		int col;
		for (int x=0; x<startImage.getWidth(); ++x) {
			for (int y=0; y<startImage.getHeight(); ++y) {
				col = Color.getHSBColor((float)groups[x][y]/(float)segments ,(float)0.6,(float)0.6).getRGB();
				//col = layerColors2.get(groupConv[groups[x][y]-1]);
				groupsImg.setRGB(x,y,col);
			}
		}
		ImageIO.write(groupsImg, "png", new File("out/groups.png"));

		//compute resolution
		xres = (int)(startImage.getWidth()*ratio);
		yres = (int)(startImage.getHeight()*ratio);
		BufferedImage image = new BufferedImage(xres, yres, BufferedImage.TYPE_3BYTE_BGR);

		//create thread pool
		ExecutorService service = Executors.newFixedThreadPool(threadCount);
		System.out.printf("Running with %d threads.\n", threadCount);

		//create jobs for threads
		Queue<FractalizeCallable> jobs = new LinkedList<>();
		for (int index=0; index<segments; ++index) {
			jobs.add(new FractalizeCallable(
				index,
				layersList.get(index),
				layerColors2.get(index)
			));
		}

		final int batchSize = threadCount;
		
		//prepare graphics
		Graphics2D gfx = image.createGraphics();
		gfx.clearRect(0, 0, image.getWidth(), image.getHeight());

		int jobsCompleted = 0;
		ANSI.Progress progress = new ANSI.Progress(32, jobs.size());
		
		try {
			while (!jobs.isEmpty()) {
				//construct a batch of jobs
				Queue<FractalizeCallable> batch = new LinkedList<>();
				for (int i=0; i<batchSize && !jobs.isEmpty(); ++i)
					batch.add(jobs.poll());

				//collect output images
				List<Future<BufferedImage>> results = service.invokeAll(batch);
				
				//update progress bar
				jobsCompleted += batchSize;
				progress.updateValue(jobsCompleted);

				//composite output images
				for (Future<BufferedImage> fimg : results)
					gfx.drawImage(fimg.get(), null, null);
			}

			ANSI.clearLine();

			ImageIO.write(image, "png", new File( "out/" + fname ));
			System.out.println("fractal - done");
		}
		catch (InterruptedException e) {
			System.out.println("Execution interrupted.");
			e.printStackTrace();
		}
		catch (ExecutionException e) {
			System.out.println("Execution failed.");
			e.printStackTrace();
		}
		finally {
			service.shutdown();
			gfx.dispose();
		}
	}
}