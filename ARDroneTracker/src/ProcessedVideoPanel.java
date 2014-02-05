/*
 * VideoPanel.java
 * 
 * Created on 21.05.2011, 18:42:10
 */



import com.codeminders.ardrone.ARDrone;
import com.codeminders.ardrone.DroneVideoListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JPanel;

/**
 * 
 */
@SuppressWarnings("serial")
public class ProcessedVideoPanel extends JPanel implements DroneVideoListener
{
    private AtomicReference<BufferedImage> atomImage          = new AtomicReference<BufferedImage>();  // used for output when displaying the video stream. this variable is the frame that'll be displayed 
    private AtomicBoolean                  preserveAspect = new AtomicBoolean(true);  
    private BufferedImage                  noConnection   = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
    
    private TargetDetector detectorObj;
    
    /** Creates new form VideoPanel */
    public ProcessedVideoPanel()
    {
        initComponents();
        Graphics2D g2d = (Graphics2D) noConnection.getGraphics();
        Font f = g2d.getFont().deriveFont(24.0f);
        g2d.setFont(f);
        g2d.drawString("No video connection", 40, 110);
        atomImage.set(noConnection);
        
        // Set up the target detector algo
        detectorObj = new TargetDetector();
    }

    public void setDrone(ARDrone drone)
    {
        drone.addImageListener(this);
    }

    public void setPreserveAspect(boolean preserve)
    {
        preserveAspect.set(preserve);
    }


    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();
        drawDroneImage(g2d, width, height);
    }

    private void drawDroneImage(Graphics2D g2d, int width, int height)
    {
        BufferedImage im = atomImage.get();
        if(im == null)
        {
            return;
        }
        int xPos = 0;
        int yPos = 0;
        if(preserveAspect.get())
        {
            g2d.setColor(Color.BLACK);
            g2d.fill3DRect(0, 0, width, height, false);
            float widthUnit = ((float) width / 4.0f);
            float heightAspect = (float) height / widthUnit;
            float heightUnit = ((float) height / 3.0f);
            float widthAspect = (float) width / heightUnit;

            if(widthAspect > 4)
            {
                xPos = (int) (width - (heightUnit * 4)) / 2;
                width = (int) (heightUnit * 4);
            } else if(heightAspect > 3)
            {
                yPos = (int) (height - (widthUnit * 3)) / 2;
                height = (int) (widthUnit * 3);
            }
        }
        if(im != null)
        {
            g2d.drawImage(im, xPos, yPos, width, height, null);
        }
    }

    private void initComponents()
    {
        setLayout(new java.awt.GridLayout(4, 6));
    }
    
    
    
    ////////////////////////////////////////
    //////////// TARGET DETECTION //////////
    ////////////////////////////////////////
    
    public TargetDetector getDetector()
    {
    	return detectorObj;
    }
    
    private int frameSkipCount = 0;
    private final int nFrameSkip = 2;  
    // frameSkipCount: cycles through 0, 1, 2, ..., nFrameSkip-1, 0, 1, 2, ...
    //                 frame will not be processed until unless frameSkipCount==0;
    // nFrameSkip: skip `nFrameSkip` frames before re-processing
	
	@Override
    public void frameReceived(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize)
    {
		if( frameSkipCount == 0 )
		{
	        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);  // create blank frame
	        im.setRGB(startX, startY, w, h, rgbArray, offset, scansize);  // copy pixels across 
	        
	        detectorObj.receiveImage( im );
	        BufferedImage processedImage = detectorObj.getProcessedImage();
	        
	        atomImage.set( processedImage );
	        repaint();
		}
        
		frameSkipCount = (frameSkipCount+1) % nFrameSkip;
    }

}
