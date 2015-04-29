package com.zhong.eyetrackingtest;


import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "MainActivity";
    private static final String    TESTTAG             = "MainActivityTEST";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);

    private MenuItem               mItemFace50;
    private MenuItem               mItemFace40;
    private MenuItem               mItemFace30;
    private MenuItem               mItemFace20;

    private Mat                    mRgba;
    private Mat                    mGray;
    private Mat teplateR;
	private Mat teplateL;
//    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    private CascadeClassifier      mEyeDetector;

    private float                  mRelativeFaceSize   = 0.4f;
    private int                    mAbsoluteFaceSize   = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;
    private int learn_frames = 0;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    try {
                    	
                    	File frontalFaceCascadeFile=MyImgProcUtil.loadCascadeFile(MainActivity.this, R.raw.haarcascade_frontalface_alt, "haarcascade_frontalface_alt.xml");
                    	File eyeCascadeFile=MyImgProcUtil.loadCascadeFile(MainActivity.this, R.raw.haarcascade_eye, "haarcascade_eye.xml");
                    	
                    	mJavaDetector = new CascadeClassifier(frontalFaceCascadeFile.getAbsolutePath());
                    	mEyeDetector=new CascadeClassifier(eyeCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()||mEyeDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                            mEyeDetector=null;
                        } else{
                        	Log.i(TAG, "Loaded cascade classifier success");
                        }

//                        cascadeDir.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
    				mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
    				mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

//        Log.i(TESTTAG, "rows---->"+mGray.rows());
//        Log.i(TESTTAG, "cols---->"+mGray.cols());
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();
        if (mJavaDetector != null){
            	mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
//        else if (mDetectorType == NATIVE_DETECTOR) {
//            if (mNativeDetector != null)
//                mNativeDetector.detect(mGray, faces);
//        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();
        /*
         * 
         
        for (int i = 0; i < facesArray.length; i++) {
//        	Log.e(TESTTAG, "r.x  y---->"+facesArray[0].x+"   "+facesArray[0].y);
			Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
					FACE_RECT_COLOR, 3);
			Rect r = facesArray[i];
			double xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
			double yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
			Rect eyearea_right = new Rect(r.x + r.width / 8,
					(int) (r.y + (r.height / 3.5)), (r.width - 2 * r.width / 8)/2,
					(int) (r.height/2-r.height/3.5));
			Rect eyearea_left = new Rect((int)xCenter,
					(int) (r.y + (r.height / 3.5)), (r.width - 2 * r.width / 8)/2,
					(int) (r.height/2-r.height/3.5));
			Imgproc.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(),
					new Scalar(255, 0, 0, 255), 2);
			Imgproc.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(),
					new Scalar(255, 0, 0, 255), 2);
			
			MatOfRect eyer = new MatOfRect();
			MatOfRect eyel = new MatOfRect();
//			mEyeDetector.detectMultiScale(mGray.submat(eyearea_right), eyer);
//			mEyeDetector.detectMultiScale(mGray.submat(eyearea_left), eyel);
			Mat sub=mGray.submat(facesArray[i]);
			
			mEyeDetector.detectMultiScale(mGray.submat(facesArray[i]), eyer, 1.15, 2,2, new Size(30, 30),
					new Size());
			
			Rect[] eyers=eyer.toArray();
			Log.i(TESTTAG, "eyers---->"+eyers.length);
			for(int j=0;j<eyers.length;j++){
				Imgproc.rectangle(mRgba,eyers[j].tl(),eyers[j].br(),
						FACE_RECT_COLOR, 2);
			}
        }
        
        */
        for (int i = 0; i < facesArray.length; i++) {
        	Log.e(TESTTAG, "r.x  y---->"+facesArray[0].x+"   "+facesArray[0].y);
        	
			Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
					FACE_RECT_COLOR, 3);
			double xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
			double yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
			Point center = new Point(xCenter, yCenter);
			Imgproc.circle(mRgba, center, 5, new Scalar(255, 0, 0, 255), 3);
//			Imgproc.putText(mRgba, "[" + center.x + "," + center.y + "]",
//					new Point(center.x + 20, center.y + 20),
//					Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255,
//							255));
			Rect r = facesArray[i];
			// compute the eye area
//			Rect eyearea = new Rect(r.x + r.width / 8,
//					(int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
//					(int) (r.height / 3.0));
			Rect eyearea = new Rect(r.x + r.width / 8,
					(int) (r.y + (r.height / 3.5)), r.width - 2 * r.width / 8,
					(int) (r.height *0.214));
			Rect eyearea_right = new Rect(r.x + r.width / 8,
					(int) (r.y + (r.height / 3.5)), (r.width - 2 * r.width / 8)/2,
					(int) (r.height/2-r.height/3.5));
			Rect eyearea_left = new Rect((int)xCenter,
					(int) (r.y + (r.height / 3.5)), (r.width - 2 * r.width / 8)/2,
					(int) (r.height/2-r.height/3.5));
//			Rect eyearea_right = new Rect(r.x + r.width / 16,
//					(int) (r.y + (r.height / 4.5)),
//					(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
//			Rect eyearea_left = new Rect(r.x + r.width / 16
//					+ (r.width - 2 * r.width / 16) / 2,
//					(int) (r.y + (r.height / 4.5)),
//					(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
			
			Imgproc.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(),
					new Scalar(255, 0, 0, 255), 2);
			Imgproc.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(),
					new Scalar(255, 0, 0, 255), 2);
//			Imgproc.rectangle(mRgba, eyearea.tl(), eyearea.br(),
//					new Scalar(255, 0, 0, 255), 2);
			if (learn_frames < 5) {
				teplateR=MyImgProcUtil.get_template(mEyeDetector, eyearea_right, 24, mGray, mRgba);
				teplateL=MyImgProcUtil.get_template(mEyeDetector, eyearea_left, 24, mGray, mRgba);
				learn_frames++;
			} else {
				// Learning finished, use the new templates for template
				// matching
				 MyImgProcUtil.match_eye(eyearea_right, teplateR, 0, mGray, mRgba);
				 MyImgProcUtil.match_eye(eyearea_left, teplateL, 0, mGray, mRgba);
			}
			// cut eye areas and put them to zoom windows
//			Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2,
//					mZoomWindow2.size());
//			Imgproc.resize(mRgba.submat(eyearea_right), mZoomWindow,
//					mZoomWindow.size());
		}
        
        return mRgba;
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50)
            setMinFaceSize(0.5f);
        else if (item == mItemFace40)
            setMinFaceSize(0.4f);
        else if (item == mItemFace30)
            setMinFaceSize(0.3f);
        else if (item == mItemFace20)
            setMinFaceSize(0.2f);
        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    public void onRecreateClick(View v)
    {
    	learn_frames = 0;
    }
    
    private boolean cameraflag=true;
    public void onSwitchBtnClick(View v)
    {
    	mOpenCvCameraView.disableView();
    	if(cameraflag){
    		mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
    		cameraflag=false;
        }else{
        	mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        	cameraflag=true;
        }
    	mOpenCvCameraView.enableView();
    }
    	
}
