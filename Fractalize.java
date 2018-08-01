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
	public static PixelGrid pixelGrid;

	public static int threadCount = Runtime.getRuntime().availableProcessors();
	public static int lejas = 80;
	public static int cutoff = 50;
	public static double scale = 1.0;
	public static int maxiters = 16;
	public static int xres, yres;


	public static double piz(List<Complex> lpts, List<Complex> S, int s) {
		if (lpts.size() == 0)
			return S.get(s).abs();
		double k = 1;
		for (Complex a : lpts)
			k *= S.get(s).minus(a).abs();

		return k;
	}

	public static ArrayList<Complex> leja(ArrayList<Complex> S, int n) {
		if (n>S.size()/2) n = S.size()/2;
		int sl = S.size();
		ArrayList<Complex> lpts = new ArrayList<>(n);
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

	public static double an(List<Complex> lpts) {
		double p = 1;
		for (int j=0; j<lpts.size()-1; ++j) {
			p*=(lpts.get(lpts.size()-1).minus(lpts.get(j))).abs();
		}
		return p;
	}

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

	public static Complex xy2complex(int x, int y, int xres, int yres, double scale) {
		return new Complex(scale*(((double)x/(double)xres)-0.5), scale*(((double)y/(double)yres)-0.5));
	}
  

  	public static boolean colorsMatch(int x, int y, int i, int j) {
  		return ( kImage.getRGB(x,y) == kImage.getRGB(i,j) );
  	}

	public static int expand(int x, int y, int group){
		int count = 0;
		int col;
		int red=0, green=0, blue=0;
		Stack<int[]> pixelStack = new Stack<int[]>();
		pixelStack.push(new int[]{x, y});
		int[] curr = null;

		while (!pixelStack.isEmpty()){
			curr = pixelStack.pop();
			int i = curr[0];
			int j = curr[1];
			count++;

			col = startImage.getRGB(i,j);
			red = ((col >> 16) & 0xFF) + red;
			green = ((col >> 8) & 0xFF) + green;
			blue = (col & 0xFF) + blue;

			pixelGrid.setVisited(i,j);
			pixelGrid.setPixelGroup(i, j, group);

			if ( pixelGrid.isExpandable(i-1,j) && colorsMatch(i,j,i-1,j) ) {
				pixelStack.push(new int[]{i-1, j});
			}
			if ( pixelGrid.isExpandable(i+1,j) && colorsMatch(i,j,i+1,j) ) {
				pixelStack.push(new int[]{i+1, j});
			}
			if ( pixelGrid.isExpandable(i,j-1) && colorsMatch(i,j,i,j-1) ) {
				pixelStack.push(new int[]{i, j-1});
			}
			if ( pixelGrid.isExpandable(i,j+1) && colorsMatch(i,j,i,j+1) ) {
				pixelStack.push(new int[]{i, j+1});
			}

		}
		blue/=count;
		green/=count;
		red/=count;

		pixelGrid.addColor(new Color(red, green, blue).getRGB());
		pixelGrid.addGroupSize( count );

		return count;
	}

	public static int splitLayers(int cutoff) {
		pixelGrid = new PixelGrid(startImage.getWidth(), startImage.getHeight());
		
		int count = 0;
		int cutoffCount = 0;
		for (int x=0; x<startImage.getWidth(); ++x) {
			for (int y=0; y<startImage.getHeight(); ++y) {
				if ( pixelGrid.isExpandable(x,y) ) {
					if ( expand(x, y, count) > cutoff ) {
						cutoffCount++;
					}
					count++;							
				}
			}
		}
		System.out.println("done making " + cutoffCount + " separate image layers.");
		return count;
	}

	public static ArrayList<Complex> pixelsToSet(ArrayList<int[]> pixelsList, int xres, int yres,double scale, int cutoff) {
		ArrayList<Complex> S = new ArrayList<>();
		for (int[] pixel : pixelsList) {
				S.add( xy2complex(pixel[0],pixel[1],xres,yres,scale) );
		}
		return S;
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

	public static void main(String[] args) throws IOException {
		int colors = 5;
		String fname = "in.png";
		int frames = 10;


		// read/segment image
	    startImage = ImageIO.read(new File("in/"+fname));
		KMeans kmeans = new KMeans();
		kImage = kmeans.run(startImage,"out.png",colors,"i");

		int segments = splitLayers(cutoff);				
		
		//compute resolution
		xres = startImage.getWidth();
		yres = startImage.getHeight();
		BufferedImage image = new BufferedImage(xres, yres, BufferedImage.TYPE_3BYTE_BGR);

		//create thread pool
		ExecutorService service = Executors.newFixedThreadPool(threadCount);
		System.out.printf("Running with %d threads.\n", threadCount);
			//create jobs for threads
			Queue<FractalizeCallable> jobs = new LinkedList<>();
			for (int index=0; index<segments; ++index) {
				if (pixelGrid.getGroupSize(index) > cutoff) {
					jobs.add(new FractalizeCallable(
									pixelsToSet( pixelGrid.getGroupContents(index),
									xres,
									yres,
									scale,
									cutoff ),
									pixelGrid.getGroupColor(index) ));
				}
			
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

				ImageIO.write(image, "png", new File( "out/" + fname + '_' + 0 ));
				System.out.println("done");
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