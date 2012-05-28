package org.whired.inspexi.master;

public class BufferQueue<E> {
	private E[] elems;
	private int idx = 0;

	@SuppressWarnings("unchecked")
	public BufferQueue(int capacity) {
		elems = (E[]) new Object[capacity];
	}

	public boolean offer(E e) {
		if (idx < elems.length) {
			elems[idx++] = e;
		}
		else {
			elems = shiftLeft();
			elems[elems.length - 1] = e;
		}
		return true;
	}

	public E peekFirst() {
		return elems[0];
	}

	public E peekLast() {
		return elems[idx];
	}

	public E pollFirst() {
		E val = elems[0];
		elems = shiftLeft();
		idx--;
		return val;
	}

	public E pollLast() {
		return elems[idx--];
	}

	public E get(int index) {
		return elems[index];
	}

	public int size() {
		return idx;
	}

	@SuppressWarnings("unchecked")
	private E[] shiftLeft() {
		E[] newElems = (E[]) new Object[elems.length];
		System.arraycopy(elems, 1, newElems, 0, newElems.length - 1);
		return newElems;
	}
}
