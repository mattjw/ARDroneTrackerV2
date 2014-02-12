import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;


public class TargetDetector {
	
	public TargetDetector() {}
	
	///////////////////////////////////////
	//////////// PUBLIC INTERFACE /////////
	///////////////////////////////////////
	
    private double tgt_x;
    private double tgt_y;
    private double tgt_r;  // target radius/diameter/extent
    private BufferedImage processedImage;
    private boolean success;
	
	public boolean isTargetFound()
	{
		return success;
	}
	
	public double getTargetX()
    {
    	return tgt_x; 
    }
	
	public double getTargetY()
    {
    	return tgt_y; 
    }
    
	public double getTargetExtent()
	{
		// Some sort of measure of the size of the target. Unit unknown.
		// Let's call it the target's "extent". 
//		return tgt_r;
		double x = tgt_r;
		double X1 = -0.035171;
		double X2 =  -9.1622;
		double X3 = 75.811;
		double y = (x * X1 + X3) / (x - X2);
		return y;
	}
	
	// A method to return the 'processed' image 
	// This is the image annotated with debug info
	public BufferedImage getProcessedImage() {
		return processedImage;
	}
	
	// A method to set detection params
	public void setParams( float TGT_LEFT_R, float TGT_LEFT_G, float TGT_LEFT_B,
						   float TGT_RIGHT_R, float TGT_RIGHT_G, float TGT_RIGHT_B,
				 	 	   float DIST_THR )
    {
    	this.TGT_LEFT_R = TGT_LEFT_R;
    	this.TGT_LEFT_G = TGT_LEFT_G;
    	this.TGT_LEFT_B = TGT_LEFT_B;
    	this.TGT_RIGHT_R = TGT_RIGHT_R;
    	this.TGT_RIGHT_G = TGT_RIGHT_G;
    	this.TGT_RIGHT_B = TGT_RIGHT_B;
    	this.DIST_THR = DIST_THR;
    }
    
	private double countNeighbours(int i, int j, double [][] where, int XR)
	{
//		int XR = 5;
		double sum = 0.0;
        for (int dj = -XR; dj <= XR; ++dj) {
            for (int di = -XR; di <= XR; ++di) {
//            	int ii = i + di;
//            	int jj = j + dj;                  
            	int ii = Math.max(0, Math.min(WIDTH - 1, i + di));
                int jj = Math.max(0, Math.min(HEIGHT - 1, j + dj));
            	sum += where[jj][ii];
            }
        }
        return sum;		
	}
	
	private void crossCheck(double[][] left, double[][] right, double x, double y, double tgtr)
	{

		double R = 70;
//		int XR = (int)Math.min(10, Math.max(1, Math.round(tgtr / 18.0)));
		int XR = 10;
//		double THR = 0.11 * (double)sq(2*XR + 1);
		double THR = 0.08 * (double)sq(2*XR + 1);

		for (int j = 0; j < HEIGHT; ++j) {
            for (int i = 0; i < WIDTH; ++i) {
            	double nn = 0.0;
            	double dist = Math.sqrt(sq(j - y) + sq(i - x));
            	if (true || (dist < R) || !(x >= 0) || !(y >= 0))
            	{
            			
            	if (left[j][i] > 0.1)
            	{
            		nn = countNeighbours(i, j, right, XR);
            	}
            	if (right[j][i] > 0.1)
            	{
            		nn = countNeighbours(i, j, left, XR);
            	}
            	}
        		if (nn < THR)
        		{
        			left[j][i] = 0.0;
        			right[j][i] = 0.0;
        			continue;
        		}
            }
        }
    }
	
	// This method is main point of entry for receiving a image to be processed
	public void receiveImage( BufferedImage image ) {
    	rawImage = image;  // MJW
    	
        int height = image.getHeight();
        int width = image.getWidth();
        processedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // handle left target
        double[][] buff_left = new double[HEIGHT][WIDTH]; 
        computeDifference(buff_left, TGT_LEFT_R, TGT_LEFT_G, TGT_LEFT_B);  // side-effect: input buf modified
//        double[][] convLeft = convolve(buff_left);
//        double[][] convLeft = buff_left;
     
//        concentrateOn(convLeft, leftDet.tgt_x, leftDet.tgt_y, 20);
//        leftDet = findTarget(convLeft);
        
        // handle right target
        double[][] buff_right = new double[HEIGHT][WIDTH]; 
        computeDifference(buff_right, TGT_RIGHT_R, TGT_RIGHT_G, TGT_RIGHT_B);  // side-effect: input buf modified
//        double[][] convRight = convolve(buff_right);
//        double[][] convRight = buff_right;
//        
//		double [][] new_left = new double[HEIGHT][WIDTH];
//		double [][] new_right = new double[HEIGHT][WIDTH];
        crossCheck(buff_left, buff_right, this.tgt_x, this.tgt_y, this.tgt_r);
        crossCheck(buff_left, buff_right, this.tgt_x, this.tgt_y, this.tgt_r);


//        crossCheck(buff_left, buff_right);

//        crossCheck(convRight, convLeft);

        DetectionInfo det = findTarget(buff_left, buff_right);
//        DetectionInfo rightDet = findTarget(buff_right);
        
        // some processing on detection info
        double averageExtent = det.tgt_r;
        
        double centroid_x = det.tgt_x;
        double centroid_y = det.tgt_y;
        
     
        boolean targetSuccess = det.success;
        
        // output to ivars
        this.tgt_x = centroid_x;
        this.tgt_y = centroid_y;
        this.success = targetSuccess;
        this.tgt_r = averageExtent;
        
        visualise(buff_left, buff_right);
	}
	
	
    ///////////////////////////////////////
    //////////// KAS PROC /////////////////
    ///////////////////////////////////////
    
    private BufferedImage rawImage;
    private int WIDTH = 320;
    private int HEIGHT = 240;
    
    // colour properties of the target...
    // (Not necessarily red/green/blue values. Actually,
    // RGB ratios.)
    
    // left square (green)
    private double TGT_LEFT_R = 0.13;//0.400;
    private double TGT_LEFT_G = 1.85;//1.85;
    private double TGT_LEFT_B = 1.02;//0.7;
    
    // right square (red)
    private double TGT_RIGHT_R = 1.8;//1.661;
    private double TGT_RIGHT_G = 0.35;//0.429;
    private double TGT_RIGHT_B = 0.8;//0.911;
    
    // thresholds
    private double DIST_THR = 0.5;//0.06;  // = 0.10; //.12 //11; // 0.07;   colour distance thresh
    private double CONV_THR = 0.5;  // = 0.5; //0.1   // false pos thresh
    
    // other params
    private final int CONV_R = 10;  // convolution circle mask' radius or diam

    private double sq(double x) {
        return x * x;
    }

    /*
     * implicit params: rawImage
     * explicit param: buf
     * side-effects: buf
     */
    private void computeDifference(double[][] buf, double TGT_R, double TGT_G, double TGT_B) {
        for (int j = 0; j < HEIGHT; ++j) {
            for (int i = 0; i < WIDTH; ++i) {
                Color col = new Color(rawImage.getRGB(i, j));
                
                double r = col.getRed()   / 255.0;
                double g = col.getGreen() / 255.0;
                double b = col.getBlue()  / 255.0;
                
                double br = (r + g + b) / 3.0;
                r = r/br;
                g = g/br;
                b = b/br;
                double diff = Math.sqrt(sq(r - TGT_R) + sq(g - TGT_G) + sq(b - TGT_B)) / Math.sqrt(3.0);
                
                if (diff < DIST_THR ) {
                    buf[j][i] = 1.0;
                }
                
                // debug
                if( (i==160) && (j==120) )
                {
                	System.out.println( String.format("r  %.3f   g  %.3f    b %.3f  %f", r, g, b, DIST_THR) );
//                	System.out.println( String.format("tgtr  %.3f   tgtg  %.3f    tgtb %.3f  %f", TGT_R, TGT_G, TGT_B, DIST_THR) );
                }
                
                // Retain only central cam circle/cone
                // the corners of the camera are especially poor quality, so
                // we discard these corners by retaining only a central cone
//                if( Math.sqrt( sq(i-160) + sq(j-120) ) > 160 )   // bigger then 160 px from centre culled
//                	buf[j][i] = 0.0;  // 0 means flagged as NOT interesting for detection
            }
        }
    }
    
    /*
     * explicit param: buf
     * return: conv
     * side effeects: none
     */
    private double[][] convolve( double[][] buf ) {
        double[][] conv = new double[HEIGHT][WIDTH];
    	
//        double[][] mask = new double[CONV_R*2+1][CONV_R*2+1];
//        int c = 0;
//        for (int dj = -CONV_R; dj <= CONV_R; ++dj) {
//            for (int di = -CONV_R; di <= CONV_R; ++di) {
//                if (Math.sqrt(sq((double)dj) + sq((double)di)) <= CONV_R) {
//                    mask[dj + CONV_R][di + CONV_R] = 1.0;
//                    ++c;
//                }
//            }
//        }

        for (int j = CONV_R; j < HEIGHT-CONV_R; ++j) {
            for (int i = CONV_R; i < WIDTH-CONV_R; ++i) {
            	if (buf[j][i] < 0.1)
            	{
            		continue;
            	}
                double sum = 0.0;
                for (int dj = -CONV_R; dj <= CONV_R; ++dj) {
                    for (int di = -CONV_R; di <= CONV_R; ++di) {
                      //  int ii = Math.max(0, Math.min(WIDTH - 1, i + di));
                      //  int jj = Math.max(0, Math.min(HEIGHT - 1, j + dj));
                    	int ii = i + di;
                    	int jj = j + dj;
                        //sum += mask[dj + CONV_R][di + CONV_R] * buf[jj][ii];
                    	sum += buf[jj][ii];
                    }
                }

                //conv[j][i] = sum / (double)c;
                conv[j][i] = sum / (double)(sq(CONV_R*2+1));

                if (conv[j][i] < CONV_THR) {
                    conv[j][i] = 0.0;
                }
            }
        }
        
        return conv;
    }
    
    /*
     * explciit: conv
     * implicit: none
     * side-effect: none
     */
    private DetectionInfo findTarget( double[][] left, double[][] right) {
        double tgt_x = 0.0;
        double tgt_y = 0.0;
        double tgt_r;
        boolean success;
        
        double tconv = 0.0;
        for (int j = 0; j < HEIGHT; ++j) {
            for (int i = 0; i < WIDTH; ++i) {
            	double val = (left[j][i] + right[j][i]);
                tgt_x += i * val;
                tgt_y += j * val;
                tconv += val;
            }
        }
        tgt_x /= tconv;
        tgt_y /= tconv;


        tgt_r = Math.max(1, 3 * Math.sqrt(tconv));
        success = (tgt_r > 55);  // prev = 10
        
        // encapsulate output
        DetectionInfo out = new DetectionInfo();
        out.tgt_r = tgt_r;
        out.tgt_x = tgt_x;
        out.tgt_y = tgt_y;
        out.success = success;
        return out;
    }

    /*
     * side-effects: processedImage
     */
    private void visualise(double[][] conv1, double[][] conv2) {

    	processedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        double maxconv1 = 0.0;
        double maxconv2 = 0.0;
        for (int j = 0; j < HEIGHT; ++j) {
            for (int i = 0; i < WIDTH; ++i) {
                maxconv1 = Math.max(maxconv1, conv1[j][i]);
                maxconv2 = Math.max(maxconv2, conv2[j][i]);
            }
        }
        for (int j = 0; j < HEIGHT; ++j) {
            for (int i = 0; i < WIDTH; ++i) {
                Color col = new Color(rawImage.getRGB(i, j));
                
                double r = col.getRed()   / 255.0;
                double g = col.getGreen() / 255.0;
                double b = col.getBlue()  / 255.0;
                double gray = Math.min(1.0, 0.21 * r + 0.71 * g + 0.07 * b);
                double val1 = conv1[j][i] / maxconv1;
                double val2 = conv2[j][i] / maxconv2;
//                int cc = Color.HSBtoRGB((float)val1, (float)val2, (float)gray);
//                processedImage.setRGB(i, j, cc);
//                int cc = Color.HSBtoRGB((float)val1, (float)val2, (float)gray);
                double rr = gray;
                double gg = gray;
                double bb = gray;
                if (val1 > 0.1)
                {
                	rr = 0.0; gg = 1.0; bb = 0.0;
                }
                if (val2 > 0.1)
                {
                	rr = 1.0; gg = 0.0; bb = 0.0;
                }
                if (!this.success)
                {
                	bb = 1.0;//this.tgt_r / 255.0;
                }
                processedImage.setRGB(i, j, new Color((float)rr, (float)gg, (float)bb).getRGB());
                
                if( (i==160) && (j==120) )
                {
                	processedImage.setRGB(i, j, 0xFFFFFF );
                	// for reference purposes, set the center pixel to white
                }
            }
        }

        Graphics2D g = processedImage.createGraphics();
        g.setColor(Color.WHITE);
        g.drawOval((int)Math.floor(tgt_x - tgt_r/4), (int)Math.floor(tgt_y - tgt_r/4), (int)Math.round(tgt_r/2.0), (int)Math.round(tgt_r/2.0));
    }
    
    class DetectionInfo 
    {
    	public boolean success;
    	public double tgt_r, tgt_x, tgt_y;  // r = radius. x, y = loc
    	
    }
}