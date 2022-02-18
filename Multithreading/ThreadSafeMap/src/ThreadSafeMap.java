import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadSafeMap {

	public static void main(String[] args) {
		Map <Integer, Integer> map = new HashMap<Integer, Integer>();
		//BaseMap workingMap = new NonThreadSafeMap(map);
		BaseMap workingMap = new CustomThreadSafeMap(map);
		Thread addThread = new AddThread(workingMap);
		Thread sumThread = new SumThread(workingMap);
		addThread.start();
		sumThread.start();
	}

}

interface BaseMap {
	void addElement(int key, int value);
	int sumElments();
}

class NonThreadSafeMap implements BaseMap {
	
	private Map <Integer, Integer> map;
	
	public NonThreadSafeMap (Map <Integer, Integer> map) {
		this.map = map;
	}

	@Override
	public void addElement(int key, int value) {
		map.put(key, value);
	}

	@Override
	public int sumElments() {
		int sum = 0;
		for(Map.Entry<Integer, Integer> entry : map.entrySet()) {
			sum += entry.getValue();
		}
		return sum;
	}
	
}

class CustomThreadSafeMap implements BaseMap {
	
	private Map <Integer, Integer> map;
	Lock lock = new ReentrantLock();
	
	public CustomThreadSafeMap (Map <Integer, Integer> map) {
		this.map = map;
	}

	@Override
	public void addElement(int key, int value) {

		lock.lock();
		map.put(key, value);
		lock.unlock();
	}

	@Override
	public int sumElments() {
		int sum = 0;
		
		lock.lock();

		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			sum += entry.getValue();
		}
		
		lock.unlock();
		
		return sum;
	}
	
}

class AddThread extends Thread {
	private BaseMap map;
	
	public AddThread(BaseMap map) {
		this.map = map;
	}
	
	@Override
	public void run() {
		int i = 0;
		Random rnd = new Random();
		while (true) {
			map.addElement(i, rnd.nextInt(100));
			i++;
		}
	}
}

class SumThread extends Thread {
	private BaseMap map;

	public SumThread(BaseMap map) {
		this.map = map;
	}

	@Override
	public void run() {
		while (true) {
			map.sumElments();
		}
	}
}
