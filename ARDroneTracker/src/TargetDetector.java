import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;


public class TargetDetector {
	
	public TargetDetector() {}
	
	
	///////////////////////////////////////
	//////////// PUBLIC INTERFACE /////////
	///////////////////////////////////////
	
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
		return tgt_r;
	}
	
	// A method to return the 'processed' image annotated with debug info
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
    
	// This method is main point of entry for receiving a image to be processed
	public void receiveImage( BufferedImage image ) {
    	rawImage = image;  // MJW
    	
        int height = image.getHeight();
        int width = image.getWidth();
        processedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // handle left target
        double[][] buff_left = new double[HEIGHT][WIDTH]; 
        computeDifference(buff_left, TGT_LEFT_R, TGT_LEFT_G, TGT_LEFT_B);  // side-effect: input buf modified
        double[][] convLeft = convolve(buff_left);
        DetectionInfo leftDet = findTarget(convLeft);
        
        // handle right target
        double[][] buff_right = new double[HEIGHT][WIDTH]; 
        computeDifference(buff_right, TGT_RIGHT_R, TGT_RIGHT_G, TGT_RIGHT_B);  // side-effect: input buf modified
        double[][] convRight = convolve(buff_right);
        DetectionInfo rightDet = findTarget(convRight);
        
        // some processing on detection info
        double averageExtent = (leftDet.tgt_r + rightDet.tgt_r) / 2.0;
        
        double centroid_x = (leftDet.tgt_x + rightDet.tgt_x) / 2.0;
        double centroid_y = (leftDet.tgt_y + rightDet.tgt_y) / 2.0;
        
        double totalExtent = leftDet.tgt_r + rightDet.tgt_r;
        
        double dist = Math.sqrt( sq(leftDet.tgt_x - rightDet.tgt_x) + sq(leftDet.tgt_y - rightDet.tgt_y) );
        double distNorm = dist / totalExtent;
        
        boolean targetSuccess = leftDet.success && rightDet.success;
        targetSuccess = targetSuccess && (distNorm <= 0.42);
        		
        
        // output to ivars
        this.tgt_x = centroid_x;
        this.tgt_y = centroid_y;
        this.success = targetSuccess;
        this.tgt_r = averageExtent;
        
        visualise(convLeft, convRight);
	}
	
	
    ///////////////////////////////////////
    //////////// KAS PROC /////////////////
    ///////////////////////////////////////
    
    private BufferedImage rawImage;
    private String imgpath = "./data";
    private int WIDTH = 320;
    private int HEIGHT = 240;
    
    // colour properties of the target...
    // (Not necessarily red/green/blue values. Actually,
    // RGB ratios.)
    
    // left square (green)
    private double TGT_LEFT_R = 0.400;
    private double TGT_LEFT_G = 1.85;
    private double TGT_LEFT_B = 0.7;
    
    // right square (red)
    private double TGT_RIGHT_R = 1.661;
    private double TGT_RIGHT_G = 0.429;
    private double TGT_RIGHT_B = 0.911;
    
    // thresholds
    private double DIST_THR = 0.06;  // = 0.10; //.12 //11; // 0.07;   colour distance thresh
    private double CONV_THR = 0.5;  // = 0.5; //0.1   // false pos thresh
    
    // other params
    private final int CONV_R = 10;  // convolution circle mask' radius or diam

    // Results
    private double tgt_x;
    private double tgt_y;
    private double tgt_r;  // target radius/diameter/extent
    private BufferedImage processedImage;
    private boolean success;

    // Temporary buffers

    private double sq(double x) {
        return x * x;
    }

    public static BufferedImage getImageFromArray(double[][] pixels, int width, int height) {
        int[] tmp = new int[3 * height * width];
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        double max = 0.0;
        for (int row = 0; row < height; ++row) {
            for (int col = 0; col < width; ++col) {
                max = Math.max(max, pixels[row][col]);
            }
        }
        double scale = 1.0 / max;
        for (int row = 0; row < height; ++row) {
            for (int col = 0; col < width; ++col) {
                image.setRGB(col, row, 0x00010101 * (int)(255.0 * scale * pixels[row][col]));
            }
        }
        return image;
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
                	System.out.println( String.format("tgtr  %.3f   tgtg  %.3f    tgtb %.3f  %f", TGT_R, TGT_G, TGT_B, DIST_THR) );
                }
                
                // Retain only central cam circle/cone
                // the corners of the camera are especially poor quality, so
                // we discard these corners by retaining only a central cone
                if( Math.sqrt( sq(i-160) + sq(j-120) ) > 160 )   // bigger then 160 px from centre culled
                	buf[j][i] = 0.0;  // 0 means flagged as NOT interesting for detection
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
    	
        double[][] mask = new double[CONV_R*2+1][CONV_R*2+1];
        int c = 0;
        for (int dj = -CONV_R; dj <= CONV_R; ++dj) {
            for (int di = -CONV_R; di <= CONV_R; ++di) {
                if (Math.sqrt(sq((double)dj) + sq((double)di)) <= CONV_R) {
                    mask[dj + CONV_R][di + CONV_R] = 1.0;
                    ++c;
                }
            }
        }

        for (int j = 0; j < HEIGHT; ++j) {
            for (int i = 0; i < WIDTH; ++i) {
                double sum = 0.0;
                for (int dj = -CONV_R; dj <= CONV_R; ++dj) {
                    for (int di = -CONV_R; di <= CONV_R; ++di) {
                        int ii = Math.max(0, Math.min(WIDTH - 1, i + di));
                        int jj = Math.max(0, Math.min(HEIGHT - 1, j + dj));
                        sum += mask[dj + CONV_R][di + CONV_R] * buf[jj][ii];
                    }
                }

                conv[j][i] = sum / (double)c;

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
    private DetectionInfo findTarget( double[][] conv ) {
        double tgt_x = 0.0;
        double tgt_y = 0.0;
        double tgt_r;
        boolean success;
        
        double tconv = 0.0;
        for (int j = 0; j < HEIGHT; ++j) {
            for (int i = 0; i < WIDTH; ++i) {
                tgt_x += i * conv[j][i];
                tgt_y += j * conv[j][i];
                tconv += conv[j][i];
            }
        }
        tgt_x /= tconv;
        tgt_y /= tconv;


        tgt_r = Math.max(1, 3 * Math.sqrt(tconv));
        success = (tgt_r > 20);  // prev = 10
        
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
                processedImage.setRGB(i, j, new Color((float)val2, (float)val1, (float)gray).getRGB());
                
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