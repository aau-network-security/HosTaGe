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

import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Region;

public class LinePoint {
	private double x = 0;
	private double y = 0;
	private Path path;
	private Region region;
    private Integer color;

    public LinePoint(){
    }

	public LinePoint(double x, double y){
		this.x = x;
		this.y = y;
	}
	public LinePoint(float x, float y){
		this.x = x;
		this.y = y;
	}
	public double getX() {
		return x;
	}
	public void setX(float x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(float y) {
		this.y = y;
	}
	
	public void setX(double x){
		this.x =  x;
	}
	
	public void setY(double y){
		this.y =  y;
	}
	public Region getRegion() {
		return region;
	}
	public void setRegion(Region region) {
		this.region = region;
	}
	public Path getPath() {
		return path;
	}
	public void setPath(Path path) {
		this.path = path;
	}
	
	@Override
	public String toString(){
		return "x= " + x + ", y= " + y;
	}

    public Integer getColor() {
        return color != null ? color : Color.parseColor("#33B5E5");
    }

    public void setColor(Integer color) {
        this.color = color;
    }
}
