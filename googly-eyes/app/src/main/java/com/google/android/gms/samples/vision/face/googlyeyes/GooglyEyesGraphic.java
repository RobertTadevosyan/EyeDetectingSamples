/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.face.googlyeyes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.samples.vision.face.googlyeyes.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

/**
 * Graphics class for rendering Googly Eyes on a graphic overlay given the current eye positions.
 */
class GooglyEyesGraphic extends GraphicOverlay.Graphic {
    private static final float EYE_RADIUS_PROPORTION = 0.40f;
    private static final float IRIS_RADIUS_PROPORTION = EYE_RADIUS_PROPORTION / 2.0f;

    private Paint mEyeWhitesPaint;
    private Paint mEyeIrisPaint;
    private Paint mEyeOutlinePaint;
    private Paint mEyeLidPaint;

    // Keep independent physics state for each eye.
    private EyePhysics mLeftPhysics = new EyePhysics();
    private EyePhysics mRightPhysics = new EyePhysics();

    private volatile PointF mLeftPosition;
    private volatile boolean mLeftOpen;

    private volatile PointF mRightPosition;
    private volatile boolean mRightOpen;

    private Random random = new Random();

    private float leftOpenScore;
    private float rightOpenScore;
    private GraphicOverlay overlay;
    private Bitmap icon;

    //==============================================================================================
    // Methods
    //==============================================================================================

    GooglyEyesGraphic(GraphicOverlay overlay) {
        super(overlay);
        this.overlay = overlay;
        icon = BitmapFactory.decodeResource(overlay.getContext().getResources(),
                R.drawable.eye);
//        icon = convertToMutable(icon);

        mEyeWhitesPaint = new Paint();
        int color = Color.argb(random.nextInt(255), random.nextInt(255), random.nextInt(255), random.nextInt(50) + 10);
        mEyeWhitesPaint.setColor(ContextCompat.getColor(overlay.getContext(), android.R.color.holo_blue_light));
        mEyeWhitesPaint.setStyle(Paint.Style.FILL);

        mEyeLidPaint = new Paint();
        mEyeLidPaint.setColor(Color.YELLOW);
        mEyeLidPaint.setStyle(Paint.Style.FILL);

        mEyeIrisPaint = new Paint();
        mEyeIrisPaint.setColor(Color.BLACK);
        mEyeIrisPaint.setStyle(Paint.Style.FILL);

        mEyeOutlinePaint = new Paint();
        mEyeOutlinePaint.setColor(Color.BLACK);
        mEyeOutlinePaint.setStyle(Paint.Style.STROKE);
        mEyeOutlinePaint.setStrokeWidth(5);
    }

    /**
     * Updates the eye positions and state from the detection of the most recent frame.  Invalidates
     * the relevant portions of the overlay to trigger a redraw.
     */
    void updateEyes(PointF leftPosition, boolean leftOpen,
                    PointF rightPosition, boolean rightOpen, Face face) {
        mLeftPosition = leftPosition;
        mLeftOpen = leftOpen;

        mRightPosition = rightPosition;
        mRightOpen = rightOpen;

        postInvalidate();

        leftOpenScore = face.getIsLeftEyeOpenProbability();
        rightOpenScore = face.getIsRightEyeOpenProbability();
//        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------");
//        System.out.println(face.getIsLeftEyeOpenProbability());
//        System.out.println(face.getIsRightEyeOpenProbability());
//        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------");
    }

    /**
     * Draws the current eye state to the supplied canvas.  This will draw the eyes at the last
     * reported position from the tracker, and the iris positions according to the physics
     * simulations for each iris given motion and other forces.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void draw(Canvas canvas) {
        PointF detectLeftPosition = mLeftPosition;
        PointF detectRightPosition = mRightPosition;
        if ((detectLeftPosition == null) || (detectRightPosition == null)) {
            return;
        }

        PointF leftPosition =
                new PointF(translateX(detectLeftPosition.x), translateY(detectLeftPosition.y));
        PointF rightPosition =
                new PointF(translateX(detectRightPosition.x), translateY(detectRightPosition.y));

        // Use the inter-eye distance to set the size of the eyes.
        float distance = (float) Math.sqrt(
                Math.pow(rightPosition.x - leftPosition.x, 2) +
                        Math.pow(rightPosition.y - leftPosition.y, 2));
        float eyeRadius = EYE_RADIUS_PROPORTION * distance;
        float irisRadius = IRIS_RADIUS_PROPORTION * distance;

        // Advance the current left iris position, and draw left eye.
        PointF leftIrisPosition =
                mLeftPhysics.nextIrisPosition(leftPosition, eyeRadius, irisRadius);
        drawEye(canvas, leftPosition, eyeRadius, leftIrisPosition, irisRadius, mLeftOpen, leftOpenScore);

        // Advance the current right iris position, and draw right eye.
        PointF rightIrisPosition =
                mRightPhysics.nextIrisPosition(rightPosition, eyeRadius, irisRadius);
        drawEye(canvas, rightPosition, eyeRadius, rightIrisPosition, irisRadius, mRightOpen, rightOpenScore);
    }

    /**
     * Draws the eye, either closed or open with the iris in the current position.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void drawEye(Canvas canvas, PointF eyePosition, float eyeRadius,
                         PointF irisPosition, float irisRadius, boolean isOpen, float probablyOpen) {

//        float [] pointsX  = new float[360];
//        float [] pointsY  = new float[360];
//        float [] newArray = new float[pointsX.length*2];
        if (isOpen) {


            //
            //          *
            //     *         *
            // *                 *
            //     *         *
            //          *
            //

//            Picture picture = new Picture();


//
//            icon.setWidth((int) (eyeRadius * probablyOpen));
//            icon.setHeight((int) (eyeRadius * probablyOpen));
            Bitmap img = Bitmap.createScaledBitmap(icon, Integer.valueOf((int) (eyeRadius / 2)),Integer.valueOf((int) (eyeRadius / 2)),false);
            canvas.drawBitmap(img, eyePosition.x - irisRadius/2, eyePosition.y - irisRadius / 2,mEyeWhitesPaint);

            canvas.drawCircle(eyePosition.x - irisRadius, eyePosition.y, eyeRadius / 20, mEyeWhitesPaint);
//            canvas.drawCircle(eyePosition.x - irisRadius / 2, eyePosition.y - eyeRadius * probablyOpen / 10, eyeRadius / 20, mEyeWhitesPaint);
//            canvas.drawCircle(eyePosition.x, eyePosition.y - eyeRadius  * probablyOpen / 5, eyeRadius / 20, mEyeWhitesPaint);
//            canvas.drawCircle(eyePosition.x + irisRadius / 2, eyePosition.y - eyeRadius * probablyOpen / 10, eyeRadius / 20, mEyeWhitesPaint);
//            canvas.drawCircle(eyePosition.x + irisRadius, eyePosition.y, eyeRadius * probablyOpen / 20, mEyeWhitesPaint);
//            canvas.drawCircle(eyePosition.x + irisRadius / 2, eyePosition.y + eyeRadius  * probablyOpen / 10, eyeRadius / 20, mEyeWhitesPaint);
//            canvas.drawCircle(eyePosition.x, eyePosition.y + eyeRadius  * probablyOpen * probablyOpen / 5, eyeRadius / 20, mEyeWhitesPaint);
//            canvas.drawCircle(eyePosition.x - irisRadius / 2, eyePosition.y + eyeRadius * probablyOpen / 10, eyeRadius / 20, mEyeWhitesPaint);

//-----------------------------------------------------------------------------------------------
//            for (int degree = 0; degree < 360; degree++){
//                float radians = (float) (degree * Math.PI/180);
//                float x = (float) (eyePosition.x + eyeRadius * Math.cos(radians));
//                float y = (float) (eyePosition.y + eyeRadius * Math.sin(radians));
//                pointsX[degree] = x;
//                pointsY[degree] = y;
//
//                int k = 0;
//                for (int i = 0;i<pointsX.length; ++i) {
//                    newArray[k] = pointsX[i];
//                    k++;
//                    newArray[k] = pointsY[i];
//                    k++;
//                }
//                canvas.drawLines(newArray, mEyeWhitesPaint);
//                System.out.println(pointsX[degree]);
//            }
// ----------------------------------------------------------------------------------------------
//            canvas.drawCircle(irisPosition.x, irisPosition.y, irisRadius, mEyeIrisPaint);
        } else {
//            canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeLidPaint);
            float y = eyePosition.y;
            float start = eyePosition.x - eyeRadius * 2;
            float end = eyePosition.x + eyeRadius / 2;
//            canvas.drawLine(start, y, end, y, mEyeOutlinePaint);
        }
//        canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeOutlinePaint);
    }

    public static Bitmap convertToMutable(Bitmap imgIn) {
        try {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            // get the width and height of the source bitmap.
            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
            Bitmap.Config type = imgIn.getConfig();

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
            imgIn.copyPixelsToBuffer(map);
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle();
            System.gc();// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            //load it back from temporary
            imgIn.copyPixelsFromBuffer(map);
            //close the temporary file and channel , then delete that also
            channel.close();
            randomAccessFile.close();

            // delete the temp file
            file.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imgIn;
    }
}
