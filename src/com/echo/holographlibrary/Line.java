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

import java.util.ArrayList;

public class Line {
	private ArrayList<LinePoint> points = new ArrayList<LinePoint>();
	private int color;
	private boolean showPoints = true;
	// 6 has been the default prior to the addition of custom stroke widths
	private int strokeWidth = 6;
	// since this is a new addition, it has to default to false to be backwards compatible
	private boolean isUsingDips = false;


	public boolean isUsingDips() {
		return isUsingDips;
	}
	public void setUsingDips(boolean treatSizesAsDips) {
		this.isUsingDips = treatSizesAsDips;
	}
	public int getStrokeWidth() {
		return strokeWidth;
	}
	public void setStrokeWidth(int strokeWidth) {
		if (strokeWidth < 0) {
			throw new IllegalArgumentException("strokeWidth must not be less than zero");
		}
		this.strokeWidth = strokeWidth;
	}
	public int getColor() {
		return color;
	}
	public void setColor(int color) {
		this.color = color;
	}
	public ArrayList<LinePoint> getPoints() {
		return points;
	}
	public void setPoints(ArrayList<LinePoint> points) {
		this.points = points;
	}
	public void addPoint(LinePoint point){
		LinePoint p;
		for(int i = 0; i < points.size(); i++){
			p = points.get(i);
			if(point.getX() < p.getX()){
				points.add(i, point);
				return;
			}
		}
		points.add(point);
	}
	
	public void removePoint(LinePoint point){
		points.remove(point);
	}
	public LinePoint getPoint(int index){
		return points.get(index);
	}
	
	public LinePoint getPoint(float x, float y){
		LinePoint p;
		for(int i = 0; i < points.size(); i++){
			p = points.get(i);
			if(p.getX() == x && p.getY() == y)
				return p;
		}
		return null;
	}
	public int getSize(){
		return points.size();
	}
	public boolean isShowingPoints() {
		return showPoints;
	}
	public void setShowingPoints(boolean showPoints) {
		this.showPoints = showPoints;
	}
	
}
