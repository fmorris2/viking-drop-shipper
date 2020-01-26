package main.org.vikingsoftware.dropshipper.core.utils;

import java.util.LinkedList;

public class CircularQueue<T> extends LinkedList<T> {
	private static final long serialVersionUID = 1L;
	
	private int maxSize;
	public CircularQueue(int maxSize) {
		super();
		this.maxSize = maxSize;
	}
	
	@Override
	public boolean add(T element) {
		if(size() >= maxSize) {
			poll();
		}
		
		return super.add(element);
	}

}
