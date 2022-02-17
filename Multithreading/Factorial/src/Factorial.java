import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class Factorial {

	private static int NUM_EVAL_RUNS = 100000;
	private static int FACTORIAL = 20; // MAX 20

	public static void main(String[] args) {

		System.out.println("Evaluating sequential factorial computation");
		SequentialFactorial sequentialFactorial = new SequentialFactorial(FACTORIAL);
		long sequentialFactorialResult = sequentialFactorial.computeFactorial();
		double sequentialTime = 0;
		for (int i = 0; i < NUM_EVAL_RUNS; i++) {
			long start = System.currentTimeMillis();
			sequentialFactorial.computeFactorial();
			sequentialTime += System.currentTimeMillis() - start;
		}
		sequentialTime /= NUM_EVAL_RUNS;

		System.out.println("Evaluating parallel factorial computation");
		ParallelFactorial parallelFactorial = new ParallelFactorial(FACTORIAL);
		long parallelFactorialResult = parallelFactorial.computeFactorial();
		double parallelTime = 0;
		for (int i = 0; i < NUM_EVAL_RUNS; i++) {
			long start = System.currentTimeMillis();
			sequentialFactorial.computeFactorial();
			parallelTime += System.currentTimeMillis() - start;
		}
		parallelTime /= NUM_EVAL_RUNS;

		if (sequentialFactorialResult != parallelFactorialResult)
			throw new Error("sequentialResult and parallelResult do not match");

		System.out.format("Average Sequential Time: %.8f ms\n", sequentialTime);
		System.out.format("Average Parallel Time: %.8f ms\n", parallelTime);
		System.out.format("Speedup: %.2f \n", sequentialTime / parallelTime);
		System.out.format("Efficiency: %.2f%%\n",
				100 * (sequentialTime / parallelTime) / Runtime.getRuntime().availableProcessors());

	}

}

class SequentialFactorial {
	int num;

	public SequentialFactorial(int num) {
		this.num = num;
	}

	public long computeFactorial() {
		long result = 1;
		for (int i = 1; i <= num; i++) {
			result *= i;
		}
		return result;
	}
}

class ParallelFactorial {

	int num;

	public ParallelFactorial(int num) {
		this.num = num;
	}

	public long computeFactorial() {
		ForkJoinPool pool = ForkJoinPool.commonPool();
		Long total = pool.invoke(new RecursiveFactorialComputation(1, num));

		return total;
	}

	private class RecursiveFactorialComputation extends RecursiveTask<Long> {
		
		private static final long serialVersionUID = 1L;
		
		private int lo, hi;

		public RecursiveFactorialComputation(int lo, int hi) {
			this.lo = lo;
			this.hi = hi;
		}

		@Override
		protected Long compute() {
			if ((hi - lo) > 5) {
				int mid = (hi + lo) / 2;
				RecursiveFactorialComputation left = new RecursiveFactorialComputation(lo, mid);
				RecursiveFactorialComputation right = new RecursiveFactorialComputation(mid + 1, hi);

				left.fork();

				return right.compute() * left.join();
			} else {
				long result = 1;
				for (int i = lo; i <= hi; i++)
					result *= i;
				return result;
			}
		}
	}
}
