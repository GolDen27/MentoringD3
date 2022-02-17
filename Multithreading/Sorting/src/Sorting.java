import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;


public class Sorting {

	private static int ARRAY_LENGTH = 100000000;
	private static int NUM_EVAL_RUNS = 5;
	
	public static void main(String[] args) {
		System.out.println("Generating array");
		int array[] = generateArray(ARRAY_LENGTH);

		System.out.println("Evaluating sequential sorting");
		SequentialMergeSorting sequentialMergeSorting = new SequentialMergeSorting(array);
		int[] sequentialMergeSortingResult = sequentialMergeSorting.sort();
		double sequentialTime = 0;
		for (int i = 0; i < NUM_EVAL_RUNS; i++) {
			long start = System.currentTimeMillis();
			sequentialMergeSorting.sort();
			sequentialTime += System.currentTimeMillis() - start;
		}
		sequentialTime /= NUM_EVAL_RUNS;

		System.out.println("Evaluating parallel sorting");
		ParallelMergeSorting parallelMergeSorting = new ParallelMergeSorting(array);
		int[] parallelMergeSortingResult = parallelMergeSorting.sort();
		double parallelTime = 0;
		for (int i = 0; i < NUM_EVAL_RUNS; i++) {
			long start = System.currentTimeMillis();
			parallelMergeSorting.sort();
			parallelTime += System.currentTimeMillis() - start;
		}
		parallelTime /= NUM_EVAL_RUNS;

		if (!Arrays.equals(sequentialMergeSortingResult, parallelMergeSortingResult))
			throw new Error("sequentialResult and parallelResult do not match");

		System.out.format("Average Sequential Time: %.8f ms\n", sequentialTime);
		System.out.format("Average Parallel Time: %.8f ms\n", parallelTime);
		System.out.format("Speedup: %.2f \n", sequentialTime / parallelTime);
		System.out.format("Efficiency: %.2f%%\n",
				100 * (sequentialTime / parallelTime) / Runtime.getRuntime().availableProcessors());

	}
	
	private static int[] generateArray (int length) {
		int array[] = new int[length];
		Random rnd = new Random();
		for (int i = 0; i < length; i++) {
			array[i] = rnd.nextInt();
		}
		return array;
	}

}

class SequentialMergeSorting {
	private int[] array;
	
	public SequentialMergeSorting (int[] array) {
		this.array = new int[array.length];
		System.arraycopy(array, 0, this.array, 0, array.length);
	}
	
	public int[] sort() {
		int[] result = sort(array);
		return result;
	}
	
	private int[] sort(int[] array) {
		if(array.length >= 2) {
			int[][] splitArr = split(array);
			
			int[] left = splitArr[0];
			int[] right = splitArr[1];
			
			int[] leftSort = sort(left);
			int[] rightSort = sort(right);

			return merge(leftSort, rightSort);
		} else {
			return array;
		}
	}
	
	private int[][] split(int[] array) {
		int[][] result = new int[2][];
		int[] left = Arrays.copyOfRange(array, 0, (array.length+1)/2);
		int[] right = Arrays.copyOfRange(array, (array.length+1)/2, array.length);
		result[0] = left;
		result[1] = right;
		return result;
	}
	
	private int[] merge(int[] left, int right[]) {
		int[] result = new int[left.length+right.length];
		int leftIndex = 0, rightIndex = 0;
		
		for (int i = 0; i < result.length; i++) {
			int lo;
			if (leftIndex < left.length && rightIndex < right.length) {
				if (left[leftIndex] < right[rightIndex]) {
					lo = left[leftIndex];
					leftIndex++;
				} else {
					lo = right[rightIndex];
					rightIndex++;
				}
			} else {
				if (leftIndex < left.length) {
					lo = left[leftIndex];
					leftIndex++;
				} else {
					lo = right[rightIndex];
					rightIndex++;
				}
			}
			result[i] = lo;
		}
		
		return result;
	}
}

class ParallelMergeSorting {
	private int[] array;

	public ParallelMergeSorting (int[] array) {
		this.array = new int[array.length];
		System.arraycopy(array, 0, this.array, 0, array.length);
	}
	
	public int[] sort() {

		ForkJoinPool pool = ForkJoinPool.commonPool();
		int[] result = pool.invoke(new RecursiveMergeSorting(array));

		return result;
	}
	
	private class RecursiveMergeSorting extends RecursiveTask<int[]> {
		
		private static final long serialVersionUID = 1L;
		private int[] array;

		public RecursiveMergeSorting(int array[]) {
			this.array = array;
		}

		@Override
		protected int[] compute() {
			if (array.length >= 2) {
				int[][] splitArr = split(array);

				int[] left = splitArr[0];
				int[] right = splitArr[1];
				
				RecursiveMergeSorting leftSort = new RecursiveMergeSorting(left);
				RecursiveMergeSorting rightSort = new RecursiveMergeSorting(right);

				rightSort.fork();
				
				return merge(leftSort.compute(), rightSort.join());
			} else {
				return array;
			}
		}
		
		private int[][] split(int[] array) {
			int[][] result = new int[2][];
			int[] left = Arrays.copyOfRange(array, 0, (array.length + 1) / 2);
			int[] right = Arrays.copyOfRange(array, (array.length + 1) / 2, array.length);
			result[0] = left;
			result[1] = right;
			return result;
		}

		private int[] merge(int[] left, int right[]) {
			int[] result = new int[left.length + right.length];
			int leftIndex = 0, rightIndex = 0;

			for (int i = 0; i < result.length; i++) {
				int lo;
				if (leftIndex < left.length && rightIndex < right.length) {
					if (left[leftIndex] < right[rightIndex]) {
						lo = left[leftIndex];
						leftIndex++;
					} else {
						lo = right[rightIndex];
						rightIndex++;
					}
				} else {
					if (leftIndex < left.length) {
						lo = left[leftIndex];
						leftIndex++;
					} else {
						lo = right[rightIndex];
						rightIndex++;
					}
				}
				result[i] = lo;
			}

			return result;
		}
	}
	
}
