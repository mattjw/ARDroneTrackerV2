/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

import org.junit.Test;

import java.io.File;

import static com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static org.junit.Assert.*;

/**
 * Test reading of a video from a file using C API.
 */
public final class JavaCVVidFileCapTest {

    public static void main( String[] args ) throws Exception {
    	
    	System.out.println("why is run?");
    	
        final File file = new File("/Users/matt/Desktop/bike.avi");
        assertTrue("Input video file exists: "+file.getAbsolutePath(), file.exists());

        final CvCapture capture = cvCreateFileCapture(file.getPath());
        assertNotNull(capture);
        try {
            final long nbFrames = (long) cvGetCaptureProperty(capture, CV_CAP_PROP_FRAME_COUNT);
            //assertEquals(119, nbFrames);//assertEquals(119, nbFrames);

            final double fps = (long) cvGetCaptureProperty(capture, CV_CAP_PROP_FPS);
            assertEquals(14, fps, 0.0001);//assertEquals(15, fps, 0.0001);

            IplImage frame;
            long count = 0;
            while (cvGrabFrame(capture) != 0 && (frame = cvRetrieveFrame(capture)) != null) {
                assertEquals(320, frame.width());
                assertEquals(240, frame.height());
                count++;
            }

            //assertEquals(nbFrames, count);
        } finally {
            cvReleaseCapture(capture);
        }
    }
}
