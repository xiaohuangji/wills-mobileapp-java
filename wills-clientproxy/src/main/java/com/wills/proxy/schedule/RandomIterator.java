package com.wills.proxy.schedule;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

 
/**
 * @author huangsiping
 *
 * @param <T>
 */
public class RandomIterator<T> implements Iterable<T> {

	private final static Random random = new Random();
	List<T> coll;
	
	public RandomIterator(List<T> coll) { this.coll = coll; }

	public Iterator<T> iterator() {
		return new Iterator<T>() {
			public boolean hasNext() {
				return true;
			}
			public T next() {
				T ret = null;
				if (coll != null) {
					int length = coll.size();
					int rIndex = random.nextInt(length);
					ret = coll.get(rIndex);
				}
				return ret;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}