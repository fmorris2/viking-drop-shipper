package main.org.vikingsoftware.dropshipper.core.data.misc;

public class Pair<T,V> {
	
	public final T left;
	public final V right;
	
	public Pair(final T left, final V right) {
		this.left = left;
		this.right = right;
	}
}
