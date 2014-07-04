package com.AlanYu.database;

import android.database.Cursor;

public class TouchDataNode {

	private String id;
	private String x;
	private String y;
	private String size;
	private String pressure;
	private String timestamp;
	private String label;
	private String actionType;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getX() {
		return Integer.valueOf(x);
	}

	public void setX(String x) {
		this.x = x;
	}

	public int getY() {
		return Integer.valueOf(y);
	}

	public void setY(String y) {
		this.y = y;
	}

	public double getSize() {
		return Double.valueOf(this.size);
	}

	public void setSize(String size) {
		this.size = size;
	}

	public double getPressure() {
		return Double.valueOf(pressure);
	}

	public void setPressure(String pressure) {
		this.pressure = pressure;
	}

	public double getTimestamp() {
		return Double.valueOf(timestamp);
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getActionType() {
		return Integer.valueOf(this.actionType);
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public TouchDataNode() {
	}

}
