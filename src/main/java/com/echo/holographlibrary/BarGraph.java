/*
 *     Created by Daniel Nadeau
 *     daniel.nadeau01@gmail.com
 *     danielnadeau.blogspot.com
 * 
 *     Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.echo.holographlibrary;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class BarGraph extends View {

	private final static int VALUE_FONT_SIZE = 23, AXIS_LABEL_FONT_SIZE = 12;
	
    private ArrayList<Bar> mBars = new ArrayList<>();
    private final Paint mPaint = new Paint();
    private boolean mShowBarText = true;
    private boolean mShowAxis = true;
    private int mIndexSelected = -1;
    private OnBarClickedListener mListener;
    private Bitmap mFullImage;
    private boolean mShouldUpdate = false;

    private int popupImageID;

    private final Context mContext;


    /**
     * Constructor
     * @param context Context
     */
    public BarGraph(Context context) {
        super(context);
        mContext = context;
        this.setWillNotDraw(false);
    }

    /**
     * Constructor
     * @param context Context
     * @param attrs AttributeSet
     */
    public BarGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        this.setWillNotDraw(false);
    }
    
    public void setShowBarText(boolean show){
        mShowBarText = show;
    }
    
    public void setShowAxis(boolean show){
        mShowAxis = show;
    }
    
    public void setBars(ArrayList<Bar> points){
        this.mBars = points;
        mShouldUpdate = true;
        //postInvalidate();
    }
    
    public ArrayList<Bar> getBars(){
        return this.mBars;
    }

    @Override
    public void onDraw(Canvas ca) {
    	super.onDraw(ca);
        if (mFullImage == null || mShouldUpdate) {
            mFullImage = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(mFullImage);
            canvas.drawColor(Color.TRANSPARENT);

            /* A header on top of a single bar */
            /*
            Bitmap backMap = BitmapFactory.decodeResource(getResources(), this.popupImageID);
            backMap = backMap.copy(Bitmap.Config.ARGB_8888, true);
            byte[] chunk = backMap.getNinePatchChunk();
            NinePatchDrawable popup = new NinePatchDrawable(getResources(), backMap, chunk, new Rect(), null);
            popup.setBounds(0, 0, backMap.getWidth(), backMap.getHeight());
            */


            NinePatchDrawable popup = (NinePatchDrawable)this.getResources().getDrawable(this.popupImageID);
            
            float maxValue = 0;
            float padding = 7 * mContext.getResources().getDisplayMetrics().density;
            int selectPadding = (int) (4 * mContext.getResources().getDisplayMetrics().density);
            float bottomPadding = 30 * mContext.getResources().getDisplayMetrics().density;
            
            float usableHeight;
            if (mShowBarText) {
                this.mPaint.setTextSize(VALUE_FONT_SIZE * mContext.getResources().getDisplayMetrics().scaledDensity);
                Rect r3 = new Rect();
                this.mPaint.getTextBounds("$", 0, 1, r3);
                usableHeight = getHeight()-bottomPadding-Math.abs(r3.top-r3.bottom)-24 * mContext.getResources().getDisplayMetrics().density;
            } else {
                usableHeight = getHeight()-bottomPadding;
            }
             
            // Draw x-axis line
            if (mShowAxis){
                mPaint.setColor(Color.BLACK);
                mPaint.setStrokeWidth(2 * mContext.getResources().getDisplayMetrics().density);
                mPaint.setAlpha(50);
                mPaint.setAntiAlias(true);
                canvas.drawLine(0, getHeight()-bottomPadding+10* mContext.getResources().getDisplayMetrics().density, getWidth(), getHeight()-bottomPadding+10* mContext.getResources().getDisplayMetrics().density, mPaint);
            }
            float barWidth = (getWidth() - (padding*2)*mBars.size())/mBars.size();

            // Maximum y value = sum of all values.
            for (final Bar bar : mBars) {
                if (bar.getValue() > maxValue) {
                    maxValue = bar.getValue();
                }
            }

            Rect mRectangle = new Rect();
            
            int count = 0;
            for (final Bar bar : mBars) {
                // Set bar bounds
                int left = (int)((padding*2)*count + padding + barWidth*count);
                int top = (int)(getHeight()-bottomPadding-(usableHeight*(bar.getValue()/maxValue)));
                int right = (int)((padding*2)*count + padding + barWidth*(count+1));
                int bottom = (int)(getHeight()-bottomPadding);
                mRectangle.set(left, top, right, bottom);

                // Draw bar
                this.mPaint.setColor(bar.getColor());
                this.mPaint.setAlpha(255);
                canvas.drawRect(mRectangle, this.mPaint);

                // Create selection region
                Path path = new Path();
                path.addRect(new RectF(mRectangle.left-selectPadding, mRectangle.top-selectPadding, mRectangle.right+selectPadding, mRectangle.bottom+selectPadding), Path.Direction.CW);
                bar.setPath(path);
                bar.setRegion(new Region(mRectangle.left-selectPadding, mRectangle.top-selectPadding, mRectangle.right+selectPadding, mRectangle.bottom+selectPadding));

                // Draw x-axis label text
                if (mShowAxis){
                    this.mPaint.setTextSize(AXIS_LABEL_FONT_SIZE * mContext.getResources().getDisplayMetrics().scaledDensity);
                    int x = (int)(((mRectangle.left+ mRectangle.right)/2)-(this.mPaint.measureText(bar.getName())/2));
                    int y = (int) (getHeight()-3 * mContext.getResources().getDisplayMetrics().scaledDensity);
                    canvas.drawText(bar.getName(), x, y, this.mPaint);
                }

                // Draw value text
                if (mShowBarText){
                    this.mPaint.setTextSize(VALUE_FONT_SIZE * mContext.getResources().getDisplayMetrics().scaledDensity);
                    this.mPaint.setColor(Color.WHITE);
                    Rect r2 = new Rect();
                    this.mPaint.getTextBounds(bar.getValueString(), 0, 1, r2);
                    
                    int boundLeft = (int) (((mRectangle.left+ mRectangle.right)/2)-(this.mPaint.measureText(bar.getValueString())/2)-5 * mContext.getResources().getDisplayMetrics().density);
                    int boundTop = (int) (mRectangle.top+(r2.top-r2.bottom)-18 * mContext.getResources().getDisplayMetrics().density);
                    int boundRight = (int)(((mRectangle.left+ mRectangle.right)/2)+(this.mPaint.measureText(bar.getValueString())/2)+5 * mContext.getResources().getDisplayMetrics().density);
                    popup.setBounds(boundLeft, boundTop, boundRight, mRectangle.top);
                    popup.draw(canvas);
                    
                    canvas.drawText(bar.getValueString(), (int)(((mRectangle.left+ mRectangle.right)/2)-(this.mPaint.measureText(bar.getValueString()))/2), mRectangle.top-(mRectangle.top - boundTop)/2f+(float)Math.abs(r2.top-r2.bottom)/2f*0.7f, this.mPaint);
                }
                if (mIndexSelected == count && mListener != null) {
                    this.mPaint.setColor(Color.parseColor("#33B5E5"));
                    this.mPaint.setAlpha(100);
                    canvas.drawPath(bar.getPath(), this.mPaint);
                    this.mPaint.setAlpha(255);
                }
                count++;
            }
            mShouldUpdate = false;
        }
        
        ca.drawBitmap(mFullImage, 0, 0, null);
        
    }

    /**
     * Set the header image id, which is used for the header image over all bars.
     * @param id int
     */
    public void setPopupImageID(int id){
        this.popupImageID = id;
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(mFullImage != null)
            mFullImage = null;
        postInvalidate();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        Point point = new Point();
        point.x = (int) event.getX();
        point.y = (int) event.getY();
        
        int count = 0;
        for (Bar bar : mBars){
            Region r = new Region();
            r.setPath(bar.getPath(), bar.getRegion());
            if (r.contains(point.x, point.y) && event.getAction() == MotionEvent.ACTION_DOWN){
                mIndexSelected = count;
            } else if (event.getAction() == MotionEvent.ACTION_UP){
                if (r.contains(point.x, point.y) && mListener != null){
                    if (mIndexSelected > -1) mListener.onClick(mIndexSelected);
                    mIndexSelected = -1;
                }
            }
            else if(event.getAction() == MotionEvent.ACTION_CANCEL)
            	mIndexSelected = -1;
            
            count++;
        }
        
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
            mShouldUpdate = true;
            postInvalidate();
        }
        
        

        return true;
    }
    
    @Override
    protected void onDetachedFromWindow()
    {
    	if(mFullImage != null)
    		mFullImage.recycle();
    	
    	super.onDetachedFromWindow();
        postInvalidate();
    }

    /**
     * Sets the bar clicked listener.
     * @param listener OnBarClickedListener
     */
    public void setOnBarClickedListener(OnBarClickedListener listener) {
        this.mListener = listener;
    }

    /**
     * Called if the user clicked a bar.
     */
    public interface OnBarClickedListener {
        void onClick(int index);
    }
}
