/*
 * 	   Created by Daniel Nadeau
 * 	   daniel.nadeau01@gmail.com
 * 	   danielnadeau.blogspot.com
 * 
 * 	   Licensed to the Apache Software Foundation (ASF) under one
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.ui.activity.MainActivity;

public class PieGraph extends View {

    private final static int TITLE_FONT_SIZE = 14;
    private final static int SUBTITLE_FONT_SIZE = 30;

    static final String ALL_TITLE = MainActivity.getContext().getString(R.string.pie_all);


    private ArrayList<PieSlice> slices = new ArrayList<PieSlice>();
	private Paint paint = new Paint();
	private Path path = new Path();
	
	private int indexSelected = -1;
	private int thickness;
	private OnSliceClickedListener listener;
	
	private boolean drawCompleted = false;


    private String title;
    private String subtitle;

    private Context mContext;

    /**
     * Constructor
     * @param context Context
     */
	public PieGraph(Context context) {
		super(context);
        this.mContext = context;
		thickness = (int) (25f * context.getResources().getDisplayMetrics().density);
        this.setWillNotDraw(false);
	}

    /**
     * Constructor
     * @param context Context
     * @param attrs AttributeSet
     */
	public PieGraph(Context context, AttributeSet attrs) {
		super(context, attrs);
        this.mContext = context;
		thickness = (int) (25f * context.getResources().getDisplayMetrics().density);
        this.setWillNotDraw(false);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.TRANSPARENT);
		paint.reset();
		paint.setAntiAlias(true);
		float midX, midY, radius, innerRadius;
		path.reset();
		
		float currentAngle = 270;
		float currentSweep = 0;
		int totalValue = 0;
		float padding = 2;
		
		midX = getWidth()/2;
		midY = getHeight()/2;
		if (midX < midY){
			radius = midX;
		} else {
			radius = midY;
		}
		radius -= padding;
		innerRadius = radius - thickness;
		
		for (PieSlice slice : slices){
			totalValue += slice.getValue();
		}
		
		int count = 0;
		for (PieSlice slice : slices){
			Path p = new Path();
			paint.setColor(slice.getColor());
			currentSweep = (slice.getValue()/totalValue)*(360);
			p.arcTo(new RectF(midX-radius, midY-radius, midX+radius, midY+radius), currentAngle+padding, currentSweep - padding);
			p.arcTo(new RectF(midX-innerRadius, midY-innerRadius, midX+innerRadius, midY+innerRadius), (currentAngle+padding) + (currentSweep - padding), -(currentSweep-padding));
			p.close();
			
			slice.setPath(p);
			slice.setRegion(new Region((int)(midX-radius), (int)(midY-radius), (int)(midX+radius), (int)(midY+radius)));
			canvas.drawPath(p, paint);
			
			if (indexSelected == count && listener != null){
				path.reset();
				//paint.setColor(slice.getColor());
				paint.setColor(Color.parseColor("#33B5E5"));
				paint.setAlpha(100);
				
				if (slices.size() > 1) {
					path.arcTo(new RectF(midX-radius-(padding*2), midY-radius-(padding*2), midX+radius+(padding*2), midY+radius+(padding*2)), currentAngle, currentSweep+padding);
					path.arcTo(new RectF(midX-innerRadius+(padding*2), midY-innerRadius+(padding*2), midX+innerRadius-(padding*2), midY+innerRadius-(padding*2)), currentAngle + currentSweep + padding, -(currentSweep + padding));
					path.close();
				} else {
					path.addCircle(midX, midY, radius+padding, Direction.CW);
				}
				
				canvas.drawPath(path, paint);
				paint.setAlpha(255);
			}
			
			currentAngle = currentAngle+currentSweep;
			
			count++;
		}


        this.drawTitle(canvas);
        this.drawSubtitle(canvas);
		
		drawCompleted = true;
		
	}

    /**
     * Draws the title in the middle of the pie.
     * @param canvas Canvas
     */
    private void drawTitle(Canvas canvas){
        String title = this.title;

        if (title != null && title.length() != 0){
            this.paint.reset();
            paint.setColor(Color.BLACK);
            paint.setAlpha(50);
            paint.setAntiAlias(true);
            paint.setAlpha(255);

            this.paint.setTextSize(TITLE_FONT_SIZE * mContext.getResources().getDisplayMetrics().scaledDensity);

            int yCenter = this.getHeight() / 2;
            int xCenter = this.getWidth() / 2;

            float textwidth = (this.paint.measureText(title));
            Rect bounds = new Rect();
            this.paint.getTextBounds(title,0,title.length(), bounds);
            canvas.drawText(title,xCenter - (textwidth / 2),yCenter - bounds.height(), this.paint);

            this.paint.reset();
        }
    }

    /**
     * Draws the Subtitle in the middle of the pie.
     * @param canvas Canvas
     */
    private void drawSubtitle(Canvas canvas){
        String title = this.subtitle;
        if (title != null && title.length() != 0){
            this.paint.reset();
            paint.setColor(Color.BLACK);
            paint.setAlpha(50);
            paint.setAntiAlias(true);
            paint.setAlpha(255);

            this.paint.setTextSize(SUBTITLE_FONT_SIZE * mContext.getResources().getDisplayMetrics().scaledDensity);

            int yCenter = this.getHeight() / 2;
            int xCenter = this.getWidth() / 2;

            float textwidth = (this.paint.measureText(title));
            Rect bounds = new Rect();
            this.paint.getTextBounds(title,0,title.length(), bounds);
            canvas.drawText(title,xCenter - (textwidth / 2),yCenter + bounds.height(), this.paint);

            this.paint.reset();
        }
    }
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (drawCompleted) {
		
			Point point = new Point();
			point.x = (int) event.getX();
			point.y = (int) event.getY();
			
			int count = 0;
            boolean drawedTitle = false;
			for (PieSlice slice : slices){
				Region r = new Region();
				r.setPath(slice.getPath(), slice.getRegion());
				if (r.contains(point.x, point.y) && event.getAction() == MotionEvent.ACTION_DOWN){
					indexSelected = count;
                    this.title = slice.getTitle();
                    this.subtitle = "" + (long) slice.getValue();
                    drawedTitle = true;
				} else if (event.getAction() == MotionEvent.ACTION_UP){
					if (r.contains(point.x, point.y) && listener != null){
						if (indexSelected > -1){
							listener.onClick(indexSelected);
						}
						indexSelected = -1;
					}
					
				}
				else if(event.getAction() == MotionEvent.ACTION_CANCEL) {
                    indexSelected = -1;
                }
				count++;
			}
			if ( event.getAction() == MotionEvent.ACTION_DOWN && !drawedTitle ) {
                // refresh title & subtitle
                this.addSlice(null);
            }
			if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
				postInvalidate();
			}
	    }
	    

	    return true;
	}
	
	public ArrayList<PieSlice> getSlices() {
		return slices;
	}
	public void setSlices(ArrayList<PieSlice> slices) {
		this.slices = slices;
		//postInvalidate();
	}
	public PieSlice getSlice(int index) {
		return slices.get(index);
	}

    /**
     * Add a pie slice.
     * @param slice {@link PieSlice PieSlice}
     */
	public void addSlice(PieSlice slice) {
        if (slice != null)
		    this.slices.add(slice);

        long countedValue = 0;

        Iterator<PieSlice> iter = this.slices.iterator();

        while (iter.hasNext()){
            PieSlice s = iter.next();
            countedValue+= s.getValue();
        }

        this.title = ALL_TITLE;
        this.subtitle = "" + countedValue;
		//postInvalidate();
	}

	public int getThickness() {
		return thickness;
	}
	public void setThickness(int thickness) {
		this.thickness = thickness;
		//postInvalidate();
	}

    /**
     * Remove all slices.
     */
	public void removeSlices(){
        Iterator<PieSlice> iter = slices.iterator();

        while (iter.hasNext()) {
            iter.next();
            iter.remove();
		}
        this.title = "";
        this.subtitle = "";

		//postInvalidate();
	}

    /**
     * Set the OnSliceClickedListener, which will be called if the user clicks a slice.
     * @param listener
     */
    public void setOnSliceClickedListener(OnSliceClickedListener listener) {
        this.listener = listener;
    }

    /**
     * OnSliceClickedListener will be called if the user clicks a pie slice.
     */
	public interface OnSliceClickedListener {
		void onClick(int index);
	}

}
