/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.util;


import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class ApAMQueue<T> implements Queue<T>{

    private ArrayBlockingQueue<T> arrayBlockingQueue;
    
    public ApAMQueue(ArrayBlockingQueue<T> arrayBlockingQueue) {
        assert arrayBlockingQueue !=null;
       this.arrayBlockingQueue = arrayBlockingQueue;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public int size() {
        return arrayBlockingQueue.size();
    }

    @Override
    public boolean isEmpty() {
        return arrayBlockingQueue.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return arrayBlockingQueue.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return arrayBlockingQueue.iterator();
    }

    @Override
    public Object[] toArray() {
        return arrayBlockingQueue.toArray();
    }

    @SuppressWarnings("hiding")
	@Override
    public <T> T[] toArray(T[] a) {
        return arrayBlockingQueue.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return arrayBlockingQueue.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return arrayBlockingQueue.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return arrayBlockingQueue.retainAll(c);
    }

    @Override
    public boolean add(T e) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public boolean offer(T e) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public T remove() {
        return arrayBlockingQueue.remove();
    }

    @Override
    public T poll() {
        return arrayBlockingQueue.poll();
    }

    @Override
    public T element() {
        return arrayBlockingQueue.element();
    }

    @Override
    public T peek() {
        return arrayBlockingQueue.peek();
    }


    @Override
    public boolean remove(Object o) {
       return arrayBlockingQueue.remove(o);
    }

}
