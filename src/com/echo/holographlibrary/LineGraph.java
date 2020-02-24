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
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Point;
import android.graphics.PorterDuffXfermode;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class LineGraph extends View {

    /**
     * The axis converter will be called, if the line graph loads / refreshed its axis.
     */
    public interface AxisDataConverter {
        /**
         * returns the x axis value for a given x coord.
         * @param x double, the x coord.
         * @return String, formatted value for the x position.
         */
        public String convertDataForX_Position(double x);
        /**
         * returns the y axis value for a given y coord.
         * @param y double, the y coord.
         * @return String, formatted value for the y position.
         */
        public String convertDataForY_Position(double y);
    }

    private final static int AXIS_LABEL_FONT_SIZE = 8;

    private ArrayList<Line> lines = new ArrayList<Line>();
	Paint paint = new Paint();
	private double minY = 0, minX = 0;
	private double maxY = 0, maxX = 0;
	private double rangeYRatio = 0;
	private double rangeXRatio = 0;
	private boolean isMaxYUserSet = false;
	private boolean isMaxXUserSet = false;
	private int lineToFill = -1;
	private int indexSelected = -1;
	private OnPointClickedListener listener;
	private Bitmap fullImage;
	private boolean shouldUpdate = false;

    static final float bottomPadding = 40, topPadding = 16;
    static final float leftPadding = 50, rightPadding = 16;
    static final float sidePadding = rightPadding + leftPadding;

    /**
     * Step = Axis Period
     */
    private float xAxisStep = 4;
    private float yAxisStep = 4;

    private AxisDataConverter converter;

    private Context mContext;

    public void setxAxisStep(float step){
        this.xAxisStep = step;
    }
    public void setYAxisStep(float step){
        this.yAxisStep = step;
    }
    public float getyAxisStep(){return  this.yAxisStep;}
    public float getxAxisStep(){return this.xAxisStep;}

    /**
     * The axis converter will be called if the line graph refresh / load its axis labels.
     * @param conv AxisDataConverter
     */
    public void setConverter(AxisDataConverter conv){
        this.converter = conv;
    }

    /**
     * Constructor
     * @param context Context
     */
	public LineGraph(Context context){
		super(context);
        this.mContext = context;
        this.setWillNotDraw(false);
    }

    /**
     * Constructor
     * @param context Context
     * @param attrs AttributeSet
     */
	public LineGraph(Context context, AttributeSet attrs) {
		super(context, attrs);
        this.mContext = context;
        this.setWillNotDraw(false);
    }

    /**
     * Removes all lines.
     */
	public void removeAllLines(){
		while (lines.size() > 0){
			lines.remove(0);
		}
		shouldUpdate = true;
		//postInvalidate();
	}

    /**
     * Add a line.
     * @param line {@link Line Line}
     */
	public void addLine(Line line) {
		lines.add(line);
		shouldUpdate = true;
		//postInvalidate();
	}

    /**
     * Adds a new point to a {@link Line lin}
     * @param lineIndex the index of the line
     * @param x double, the x coord.
     * @param y double, the y coord.
     */
	public void addPointToLine(int lineIndex, double x, double y){
		addPointToLine(lineIndex, (float) x, (float) y);
	}
	public void addPointToLine(int lineIndex, float x, float y){
		LinePoint p = new LinePoint(x, y);

		addPointToLine(lineIndex, p);
	}
	
	public double getRangeYRatio(){
		return rangeYRatio;
	}
	
	public void setRangeYRatio(double rr){
		this.rangeYRatio = rr;
	}
	public double getRangeXRatio(){
		return rangeXRatio;
	}
	
	public void setRangeXRatio(double rr){
		this.rangeXRatio = rr;
	}
	public void addPointToLine(int lineIndex, LinePoint point){
		Line line = getLine(lineIndex);
		line.addPoint(point);
		lines.set(lineIndex, line);
		resetLimits();
		shouldUpdate = true;
		//postInvalidate();
	}
	
	public void addPointsToLine(int lineIndex, LinePoint[] points){
		Line line = getLine(lineIndex);
		for(LinePoint point : points){
			line.addPoint(point);
		}
		lines.set(lineIndex, line);
		resetLimits();
		shouldUpdate = true;
		//postInvalidate();
	}
	
	public void removeAllPointsAfter(int lineIndex, double x){
		removeAllPointsBetween(lineIndex, x, getMaxX());
	}
	public void removeAllPointsBefore(int lineIndex, double x){
		removeAllPointsBetween(lineIndex, getMinX(), x);
	}
	
	public void removeAllPointsBetween(int lineIndex, double startX, double finishX){
		Line line = getLine(lineIndex);
		LinePoint[] pts = new LinePoint[line.getPoints().size()];
		pts = line.getPoints().toArray(pts);
		for(LinePoint point : pts){
			if(point.getX() >= startX && point.getX() <= finishX)
				line.removePoint(point);
		}
		lines.set(lineIndex, line);
		resetLimits();
		shouldUpdate = true;
		//postInvalidate();
	}
	public void removePointsFromLine(int lineIndex, LinePoint[] points){
		Line line = getLine(lineIndex);
		for(LinePoint point : points){
			line.removePoint(point);
		}
		lines.set(lineIndex, line);
		resetLimits();
		shouldUpdate = true;
		//postInvalidate();
	}
	public void removePointFromLine(int lineIndex, float x, float y){
		LinePoint p = null;
		Line line = getLine(lineIndex);
		p = line.getPoint(x, y);
		removePointFromLine(lineIndex, p);
	}
	public void removePointFromLine(int lineIndex, LinePoint point){
		Line line = getLine(lineIndex);
		line.removePoint(point);
		lines.set(lineIndex, line);
		resetLimits();
		shouldUpdate = true;
		//postInvalidate();
	}

    /**
     * Resets the y axis limits
     */
	public void resetYLimits(){
        double range = getMaxY() - getMinY();
		setRangeY(getMinY()-range*getRangeYRatio(), getMaxY()+range*getRangeYRatio());
        isMaxYUserSet = false;
    }

    /**
     * Resets the x axis limits
     */
	public void resetXLimits(){
        double range = getMaxX() - getMinX();
		setRangeX(getMinX()-range*getRangeXRatio(), getMaxX()+range*getRangeXRatio());
        isMaxXUserSet = false;
    }
	public void resetLimits() {
		resetYLimits();
		resetXLimits();
	}
	public ArrayList<Line> getLines() {
		return lines;
	}
	public void setLineToFill(int indexOfLine) {
		this.lineToFill = indexOfLine;
		shouldUpdate = true;
		//postInvalidate();
	}
	public int getLineToFill(){
		return lineToFill;
	}
	public void setLines(ArrayList<Line> lines) {
		this.lines = lines;
	}
	public Line getLine(int index) {
		return lines.get(index);
	}
	public int getSize(){
		return lines.size();
	}

    /**
     * Sets the y axis range (minimal and maximal value for the y axis)
     * @param min float
     * @param max float
     */
	public void setRangeY(float min, float max) {
		minY = min;
		maxY = max;
		isMaxYUserSet = true;
	}

    /**
     * Sets the y axis range (minimal and maximal value for the y axis)
     * @param min double
     * @param max double
     */
	public void setRangeY(double min, double max){
		minY = min;
		maxY = max;
		isMaxYUserSet = true;
	}

    /**
     * Sets the x axis range (minimal and maximal value for the x axis)
     * @param min float
     * @param max float
     */
	public void setRangeX(float min, float max) {
		minX = min;
		maxX = max;
        isMaxXUserSet = true;
    }
    /**
     * Sets the x axis range (minimal and maximal value for the x axis)
     * @param min double
     * @param max double
     */
	public void setRangeX(double min, double max){
		minX = min;
		maxX = max;
        isMaxXUserSet = true;
    }
	public double getMaxY(){
        if (isMaxYUserSet)return maxY;
        double max = lines.get(0).getPoint(0).getY();
		for (Line line : lines){
			for (LinePoint point : line.getPoints()){
				max = point.getY() > max ? point.getY() : max;
			}
		}
		maxY = max;
		return maxY;	
	}

	public double getMinY(){
        if (isMaxYUserSet)return minY;
        double min = lines.get(0).getPoint(0).getY();
		for (Line line : lines){
			for (LinePoint point : line.getPoints()){
				min = point.getY() < min ? point.getY() : min;
			}
		}
		minY = min;
		return minY;
	}
	public double getMinLimY(){
		return minY;
	}
	public double getMaxLimY(){
		return maxY;
	}
	public double getMinLimX(){
        if (isMaxXUserSet) {
            return minX;
        }
        else {
            return getMinX();
        }
	}
	public double getMaxLimX(){
		if (isMaxXUserSet) {
			return maxX;
		}
		else {
			return getMaxX();
		}
	}
	public double getMaxX(){
        double max = lines.size() > 0 ? lines.get(0).getPoint(0).getX() : 0;
		for (Line line : lines){
			for (LinePoint point : line.getPoints()){
				max =Math.max(point.getX(), max);// point.getX() > max ? point.getX() : max;
			}
		}
		maxX = max;
		return maxX;
		
	}
	public double getMinX(){
        double min = lines.size() > 0 ? lines.get(0).getPoint(0).getX() : 0;
		for (Line line : lines){
			for (LinePoint point : line.getPoints()){
				min =Math.min(point.getX(), min);// point.getX() < min ? point.getX() : min;
			}
		}
		minX = min;
		return minX;
	}

    /**
     * Returns the title for the x coordinate by calling the converter.
     * @param x  double, the x coord.
     * @return String, the title
     */
    private String getX_AxisTitle(double x){
        if (this.converter == null)return "" + (long)x;
        return this.converter.convertDataForX_Position(x);
    }
    /**
     * Returns the title for the y coordinate by calling the converter.
     * @param y  double, the y coord.
     * @return String, the title
     */
    private String getY_AxisTitle(double y){
        if (this.converter == null)return "" + (long)y;
        return this.converter.convertDataForY_Position(y);
    }

    @Override
	public void onDraw(Canvas ca) {
        super.onDraw(ca);

        if (this.lines == null || this.lines.size() == 0) return;

        if (fullImage == null || shouldUpdate) {
			fullImage = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(fullImage);
			
			paint.reset();
			Path path = new Path();


			float usableHeight = getHeight() - bottomPadding - topPadding;
			float usableWidth = getWidth() - 2*sidePadding;

            double maxY = getMaxLimY();
            double minY = getMinLimY();
            double maxX = getMaxLimX();
            double minX = getMinLimX();


	        // DRAW THE BACKGROUND
            //this.drawBackground(canvas);

            paint.setAntiAlias(true);

            // DRAW THE AXIS
            this.drawAxis(canvas);


            // DRAW LINES
			for (Line line : lines){
				int count = 0;
				float lastXPixels = 0, newYPixels = 0;
				float lastYPixels = 0, newXPixels = 0;
				
				paint.setColor(line.getColor());
				paint.setStrokeWidth(getStrokeWidth(line));
				
				for (LinePoint p : line.getPoints()){
                    float yPercent =(float) ((p.getY()-minY)/(maxY - minY));
                    float xPercent = (float)((p.getX()-minX)/(maxX - minX));
					if (count == 0){
						lastXPixels = sidePadding + (xPercent*usableWidth);
						lastYPixels = getHeight() - bottomPadding - (usableHeight*yPercent);
					} else {
						newXPixels = sidePadding + (xPercent*usableWidth);
						newYPixels = getHeight() - bottomPadding - (usableHeight*yPercent);
						canvas.drawLine(lastXPixels, lastYPixels, newXPixels, newYPixels, paint);
						lastXPixels = newXPixels;
						lastYPixels = newYPixels;
					}
					count++;
				}
			}
			
			
			int pointCount = 0;
			// DRAW POINTS
			for (Line line : lines){

				paint.setColor(line.getColor());
				paint.setStrokeWidth(getStrokeWidth(line));
				paint.setStrokeCap(Paint.Cap.ROUND);
				
				if (line.isShowingPoints()){
					for (LinePoint p : line.getPoints()){
                        float yPercent =(float) ((p.getY()-minY)/(maxY - minY));
						float xPercent =(float) ((p.getX()-minX)/(maxX - minX));
						float xPixels = sidePadding + (xPercent*usableWidth);
                        float yPixels = getHeight() - bottomPadding - (usableHeight*yPercent);

						int outerRadius;
						if (line.isUsingDips()) {
							outerRadius = getPixelForDip(line.getStrokeWidth() + 4);
						}
						else {
							outerRadius = line.getStrokeWidth() + 4;
						}
						int innerRadius = outerRadius / 2;

						paint.setColor(p.getColor());
						canvas.drawCircle(xPixels,(float) yPixels, outerRadius, paint);
						paint.setColor(Color.WHITE);
						canvas.drawCircle(xPixels,(float) yPixels, innerRadius, paint);
						
						Path path2 = new Path();
						path2.addCircle(xPixels,(float) yPixels, 30, Direction.CW);
						p.setPath(path2);
						p.setRegion(new Region((int)(xPixels-30), (int)(yPixels-30), (int)(xPixels+30), (int)(yPixels+30)));
						
						if (indexSelected == pointCount && listener != null){
                            paint.setColor(p.getColor());
							paint.setAlpha(100);
							canvas.drawPath(p.getPath(), paint);
							paint.setAlpha(255);
						}
						
						pointCount++;
					}
				}
			}
			
			shouldUpdate = false;
		}
		
		ca.drawBitmap(fullImage, 0, 0, null);

	}

    /**
     * Draw the x & y axis
     * @param canvas Canvas
     */
    private void drawAxis(Canvas canvas){
        //double maxX = getMaxLimX();
        //double minX = getMinLimX();
        //float usableWidth = getWidth() - 2*sidePadding;
        //float usableHeight = getHeight() - bottomPadding - topPadding;

        float yPixels = getHeight() - (bottomPadding - (10));

        // DRAW SEPERATOR
        paint.setColor(Color.BLACK);
        paint.setAlpha(50);
        paint.setAntiAlias(true);
        // x Axis
        canvas.drawLine(leftPadding ,yPixels, getWidth()-sidePadding, yPixels, paint);
        // y Axis
        canvas.drawLine(leftPadding ,topPadding, leftPadding, yPixels, paint);

        paint.setAlpha(255);

        this.drawAxisLabel(canvas);
    }

    /**
     * Draw the x & y axis labels.
     * @param canvas Canvas
     */
    private void drawAxisLabel(Canvas canvas){
        this.paint.setTextSize(AXIS_LABEL_FONT_SIZE * mContext.getResources().getDisplayMetrics().scaledDensity);

        double maxX = getMaxLimX();
        double minX = getMinLimX();

        float usableWidth = getWidth() - 2*sidePadding;
        float usableHeight = getHeight() - bottomPadding - topPadding;
        float yPixels = getHeight() - (bottomPadding - (10));


        // Draw y-axis label text
        double step = Math.max(1., (maxY - minY) / (Math.max(1., yAxisStep)));
        double v = 0;
        for (double y = minY; y <= maxY; y+=step){
            double yPercent = (y-minY)/(maxY - minY);

            double newYPixels = topPadding + (yPercent*usableHeight);
            canvas.drawLine((float)leftPadding,(float)newYPixels,(float)leftPadding-5.f,(float)newYPixels, paint);
            String title = this.getY_AxisTitle(maxY - v);
            float textwidth = (this.paint.measureText(title));
            canvas.drawText(title, 5.f ,(float)newYPixels + (textwidth/2), this.paint);

            v+=step;
        }

        this.paint.setTextSize(AXIS_LABEL_FONT_SIZE * mContext.getResources().getDisplayMetrics().scaledDensity);

        // Draw x-axis label text
        step = Math.max(1, (maxX - minX) / (Math.max(1, xAxisStep)));

        for (double x = minX; x <= maxX; x+=step){
            double xPercent = (x-minX)/(maxX - minX);

            double newXPixels = sidePadding + (xPercent*usableWidth);
            canvas.drawLine((float)newXPixels,(float) yPixels + 5,(float) newXPixels,(float) yPixels, paint);
            String title = this.getX_AxisTitle(x);
            float textwidth = (this.paint.measureText(title));

            float x_Coord = Math.max(0.f, (float) (newXPixels - (textwidth / 2)));
            x_Coord = Math.min(x_Coord, getWidth() - rightPadding);
            float y_Coord =  (float) (yPixels + ((bottomPadding - 10) / 1));
            canvas.drawText(title, x_Coord , y_Coord, this.paint);
        }
    }

    /**
     * Draw the lined background.
     * @param canvas Canvas
     */
    private void drawBackground(Canvas canvas){

        paint.reset();


        float usableHeight = getHeight() - bottomPadding - topPadding;
        float usableWidth = getWidth() - 2*sidePadding;

        double maxY = getMaxLimY();
        double minY = getMinLimY();
        double maxX = getMaxLimX();
        double minX = getMinLimX();

        Path path = new Path();

        // DRAW THE BACKGROUND
			int lineCount = 0;
			for (Line line : lines){
				int count = 0;
				float firstXPixels = 0, lastXPixels = 0, newYPixels = 0;
				float lastYPixels = 0, newXPixels = 0;

				if (lineCount == lineToFill){
					paint.setColor(Color.BLACK);
					paint.setAlpha(30);
					paint.setStrokeWidth(2);
					for (int i = 10; i-getWidth() < getHeight(); i = i+20){
						canvas.drawLine(i, getHeight()-bottomPadding, 0, getHeight()-bottomPadding-i, paint);
					}


					paint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
					for (LinePoint p : line.getPoints()){
						float yPercent =(float) ((p.getY()-minY)/(maxY - minY));
						float xPercent =(float) ((p.getX()-minX)/(maxX - minX));
						if (count == 0){
							lastXPixels = sidePadding + (xPercent*usableWidth);
							lastYPixels = getHeight() - bottomPadding - (usableHeight*yPercent);
							firstXPixels = lastXPixels;
							path.moveTo(lastXPixels, lastYPixels);
						} else {
							newXPixels = sidePadding + (xPercent*usableWidth);
							newYPixels = getHeight() - bottomPadding - (usableHeight*yPercent);
							path.lineTo(newXPixels, newYPixels);
							Path pa = new Path();
							pa.moveTo(lastXPixels, lastYPixels);
							pa.lineTo(newXPixels, newYPixels);
							pa.lineTo(newXPixels, 0);
							pa.lineTo(lastXPixels, 0);
							pa.close();
							canvas.drawPath(pa, paint);
							lastXPixels = newXPixels;
							lastYPixels = newYPixels;
						}
						count++;
					}

					path.reset();

					path.moveTo(0, getHeight()-bottomPadding);
					path.lineTo(sidePadding, getHeight()-bottomPadding);
					path.lineTo(sidePadding, 0);
					path.lineTo(0, 0);
					path.close();
					canvas.drawPath(path, paint);

					path.reset();

					path.moveTo(getWidth(), getHeight()-bottomPadding);
					path.lineTo(getWidth()-sidePadding, getHeight()-bottomPadding);
					path.lineTo(getWidth()-sidePadding, 0);
					path.lineTo(getWidth(), 0);
					path.close();

					canvas.drawPath(path, paint);

				}

				lineCount++;
			}

        this.paint.reset();
    }

	private int getStrokeWidth(Line line) {
		int strokeWidth;
		if (line.isUsingDips()) {
			strokeWidth = getPixelForDip(line.getStrokeWidth());
		}
		else {
			strokeWidth = line.getStrokeWidth();
		}
		return strokeWidth;
	}

	private int getPixelForDip(int dipValue) {
		return (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP,
				dipValue,
				getResources().getDisplayMetrics());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

	    Point point = new Point();
	    point.x = (int) event.getX();
	    point.y = (int) event.getY();
	    
	    int count = 0;
	    int lineCount = 0;
	    int pointCount = 0;
	    
	    Region r = new Region();
	    for (Line line : lines){
	    	pointCount = 0;
	    	for (LinePoint p : line.getPoints()){
	    		
	    		if (p.getPath() != null && p.getRegion() != null){
	    			r.setPath(p.getPath(), p.getRegion());
			    	if (r.contains((int)point.x,(int) point.y) && event.getAction() == MotionEvent.ACTION_DOWN){
			    		indexSelected = count;
			    	} else if (event.getAction() == MotionEvent.ACTION_UP){
			    		if (r.contains((int)point.x,(int) point.y) && listener != null){
			    			listener.onClick(lineCount, pointCount);
			    		}
			    		indexSelected = -1;
			    	}
	    		}
		    	
		    	pointCount++;
			    count++;
	    	}
	    	lineCount++;
	    	
	    }
	    
	    if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP){
	    	shouldUpdate = true;
	    	postInvalidate();
	    }
	    
	    

	    return true;
	}

    @Override
    protected void onDetachedFromWindow()
    {
        if(fullImage != null)
            fullImage.recycle();

        super.onDetachedFromWindow();
        //postInvalidate();
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        //postInvalidate();
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(fullImage != null)
            fullImage = null;
        postInvalidate();
    }

    /**
     * Set the point click listener, which will be called on clicking a point.
     * @param listener OnPointClickedListener
     */
    public void setOnPointClickedListener(OnPointClickedListener listener) {
		this.listener = listener;
	}

    /**
     * OnPointClickedListener will be called, if the user clicks a point.
     */
	public interface OnPointClickedListener {
		abstract void onClick(int lineIndex, int pointIndex);
	}
}
