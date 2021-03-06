package org.fuwjax.pipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InputOutputStream{
	private InputStream in = new InputStream(){
		@Override
		public int read() throws IOException {
			lock.lock();
			try{
				while(readCapacity() == 0){
					untilIo.await();
				}
				int b = forRead().get();
				untilIo.signalAll();
				return b;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return -1;
			}finally{
				lock.unlock();
			}
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			lock.lock();
			try{
				while(readCapacity() == 0){
					untilIo.await();
				}
				int count = Math.min(readCapacity(), len);
				forRead().get(b, off, count);
				untilIo.signalAll();
				return count;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return -1;
			}finally{
				lock.unlock();
			}
		}
		
		@Override
		public int available() throws IOException {
			return readCapacity();
		}
	};
	
	private OutputStream out = new OutputStream(){
		@Override
		public void write(int b) throws IOException {
			lock.lock();
			try{
				while(writeCapacity() == 0){
					untilIo.await();
				}
				forWrite().put((byte)b);
				untilIo.signalAll();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}finally{
				lock.unlock();
			}
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			int offset = off;
			int length = len;
			lock.lock();
			try{
				while(length > 0){
					while(writeCapacity() == 0){
						untilIo.await();
					}
					int count = Math.min(writeCapacity(), length);
					forWrite().put(b, offset, length);
					untilIo.signalAll();
					offset += count;
					length -= count;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}finally{
				lock.unlock();
			}
		}
	};
	
	public enum Mode{
		READ,
		WRITE;
	}
	
	private final Lock lock = new ReentrantLock();
	private final Condition untilIo = lock.newCondition();
	private final ByteBuffer buffer = ByteBuffer.allocate(4096);
	private volatile Mode mode = Mode.WRITE;
	
	private int readCapacity(){
		return mode == Mode.WRITE ? buffer.position() : buffer.remaining();
	}
	
	private int writeCapacity(){
		return mode == Mode.WRITE ? buffer.remaining() : buffer.capacity() - buffer.remaining();
	}
	
	private ByteBuffer forRead(){
		if(mode == Mode.WRITE){
			buffer.flip();
			mode = Mode.READ;
		}
		return buffer;
	}
	
	private ByteBuffer forWrite(){
		if(mode == Mode.READ){
			buffer.compact();
			mode = Mode.WRITE;
		}
		return buffer;
	}
	
	public InputStream input(){
		return in;
	}
	
	public OutputStream output(){
		return out;
	}
}
