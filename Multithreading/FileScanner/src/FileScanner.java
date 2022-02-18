import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class FileScanner {

	private final static String DEFAULT_LOC = "C:\\Program Files\\Microsoft";
	private final static int NUM_EVAL_RUNS = 5;

	public static void main(String[] args) throws FileNotFoundException {
		
		System.out.println("Please enter the location to scan:");
		Scanner scaner = new Scanner(System.in);
		//String location = scaner.nextLine();
		String location = DEFAULT_LOC;

		File fileLocation = new File(location);
		if(!fileLocation.exists()) {
			throw new FileNotFoundException("Location does not exist");
		}

		System.out.println("Scanning location " + location);
		
		
		System.out.println("Evaluating sequential scanning location");
		SequentialFileScanner sequentialFileScanner = new SequentialFileScanner(fileLocation);
		LocationStatistic sequentialFileScannerResult = sequentialFileScanner.scan();
		double sequentialTime = 0;
		for (int i = 0; i < NUM_EVAL_RUNS; i++) {
			long start = System.currentTimeMillis();
			sequentialFileScanner.scan();
			sequentialTime += System.currentTimeMillis() - start;
		}
		sequentialTime /= NUM_EVAL_RUNS;

		System.out.println("Evaluating parallel scanning location");
		ParallelFileScanner parallelFileScanner = new ParallelFileScanner(fileLocation);
		LocationStatistic parallelFileScannerResult = parallelFileScanner.scan();
		double parallelTime = 0;
		for (int i = 0; i < NUM_EVAL_RUNS; i++) {
			long start = System.currentTimeMillis();
			parallelFileScanner.scan();
			parallelTime += System.currentTimeMillis() - start;
		}
		parallelTime /= NUM_EVAL_RUNS;

		if (!sequentialFileScannerResult.equals(parallelFileScannerResult))
			throw new Error("sequentialResult and parallelResult do not match");
		

		System.out.format("Files: %s \n", parallelFileScannerResult.getFileCount());
		System.out.format("Folders: %s \n", parallelFileScannerResult.getFolderCount());
		System.out.format("Size: %s bytes\n", parallelFileScannerResult.getSize());

		System.out.format("Average Sequential Time: %.8f ms\n", sequentialTime);
		System.out.format("Average Parallel Time: %.8f ms\n", parallelTime);
		System.out.format("Speedup: %.2f \n", sequentialTime / parallelTime);
		System.out.format("Efficiency: %.2f%%\n",
				100 * (sequentialTime / parallelTime) / Runtime.getRuntime().availableProcessors());
		
		


	}

}

class LocationStatistic {
	private int fileCount;
	private int folderCount;
	private long size;
	
	public LocationStatistic () {
	}


	public int getFileCount() {
		return fileCount;
	}

	public void increaseFileCount() {
		this.fileCount++;
	}
	public void increaseFileCount(int count) {
		this.fileCount += count;
	}

	public int getFolderCount() {
		return folderCount;
	}

	public void increaseFolderCount() {
		this.folderCount++;
	}

	public void increaseFolderCount(int count) {
		this.folderCount += count;
	}

	public long getSize() {
		return size;
	}

	public void increaseSize(long size) {
		this.size += size;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fileCount;
		result = prime * result + folderCount;
		result = prime * result + (int) (size ^ (size >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocationStatistic other = (LocationStatistic) obj;
		if (fileCount != other.fileCount)
			return false;
		if (folderCount != other.folderCount)
			return false;
		if (size != other.size)
			return false;
		return true;
	}
	
}

class SequentialFileScanner {
	
	private File location;

	public SequentialFileScanner(File location) {
		this.location = location;
	}
	
	public LocationStatistic scan() {
		LocationStatistic statistic = new LocationStatistic();
		return scan(location, statistic);
	}

	
	private LocationStatistic scan(File location, LocationStatistic statistic) {
		File[] files = location.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (!files[i].isFile()) {
				statistic.increaseFolderCount();
				 scan(files[i], statistic);
			} else {
				statistic.increaseFileCount();
				statistic.increaseSize(files[i].length());
			}
		}
		
		return statistic;
	}
}

class ParallelFileScanner {
	
	private File location;

	public ParallelFileScanner(File location) {
		this.location = location;
	}
	
	public LocationStatistic scan() {
		ForkJoinPool pool = ForkJoinPool.commonPool();
		return pool.invoke(new RecursiveFileScanner(location, new LocationStatistic()));
	}
	
	private class RecursiveFileScanner extends RecursiveTask<LocationStatistic> {
		
		private File location;
		private LocationStatistic statistic;
		
		public RecursiveFileScanner (File location, LocationStatistic statistic) {
			this.location = location;
			this.statistic = statistic;
		}

		@Override
		protected LocationStatistic compute() {
			File[] files = location.listFiles();
			RecursiveFileScanner[] scannerArr = new RecursiveFileScanner[files.length];
			for (int i = 0; i < files.length; i++) {
				if (!files[i].isFile()) {
					synchronized (RecursiveFileScanner.class) {
						statistic.increaseFolderCount();
					}
					scannerArr[i] = new RecursiveFileScanner(files[i], new LocationStatistic());
					scannerArr[i].fork();
				} else {

					synchronized (RecursiveFileScanner.class) {
						statistic.increaseFileCount();
						statistic.increaseSize(files[i].length());
					}
				}
			}
			for (int i = 0; i < scannerArr.length; i++) {
				if (scannerArr[i] != null) {
					LocationStatistic tempStat = scannerArr[i].join();
					statistic.increaseFileCount(tempStat.getFileCount());
					statistic.increaseFolderCount(tempStat.getFolderCount());
					statistic.increaseSize(tempStat.getSize());
				}
			}
			
			return statistic;
		}
		
	}
}
