import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.googlecode.javacv.CanvasFrame;
import static com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;

public class JavaCVFirstProgTest {
	
	static String path = "/Users/matt/Desktop/boldt.jpg";

    public static void main(String[] args) throws Exception {
        
        // read an image
        final IplImage image = cvLoadImage(path);
        System.out.println("width " + image.width() );
        
        // create image window named "My Image"
        /*
        final CanvasFrame canvas = new CanvasFrame("My Image");
        
        // request closing of the application when the image window is closed
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
                
        // show image on window
        canvas.showImage(image);*/

        //
        // MJW -- try it the java way
        BufferedImage biImage = image.getBufferedImage();
        ImageIcon iiImage = new ImageIcon( biImage ); 
        
        //biImage = ImageIO.read(new File(path));
        
        // frame stuff
        JFrame fr = new JFrame("Attempt");
        fr.setSize(400,400);
        fr.setDefaultCloseOperation(fr.EXIT_ON_CLOSE);
        
        JLabel imgLabel = new JLabel(iiImage);
        fr.getContentPane().add(imgLabel);
        
        fr.setVisible(true);
        
    }
}
