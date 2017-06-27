/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
package com.sun.glass.ui.jfxgl;

import java.util.Arrays;

public class EventQueue {

	private Runnable[] queue = new Runnable[32];
	private int start;
	private int count;

	public synchronized void postEvent(Runnable event) {
		if (count == queue.length) {
			Runnable[] newQueue = new Runnable[(queue.length * 3) / 2];
			System.arraycopy(queue, start, newQueue, 0, queue.length - start);
			System.arraycopy(queue, 0, newQueue, queue.length - start, start);
			queue = newQueue;
			start = 0;
		}
		queue[modulo(start + count)] = event;
		count++;
		notifyAll();
	}

	public synchronized Runnable getNextEvent()
	throws InterruptedException {
		while (count == 0) {
			wait();
		}
		Runnable event = queue[start];
		queue[start] = null;
		start = modulo(start + 1);
		count--;
		return event;
	}

	public synchronized void shutdown() {
		Arrays.fill(queue, null);
		count = 0;
	}
	
	private int modulo(int index) {
		if (index >= queue.length) {
			index -= queue.length;
		}
		return index;
	}
}
