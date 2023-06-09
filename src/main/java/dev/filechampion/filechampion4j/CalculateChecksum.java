package dev.filechampion.filechampion4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to calculate the checksums of a file.
 * It is optimized for large files and uses multiple threads to
 * calculate the checksum in parallel.
 */
public class CalculateChecksum {
    private static final int MIN_CHUNK_SIZE = 1024 * 1024; // 0.5 MB
    private static final int MAX_CHUNK_SIZE = 3 * 1024 * 1024; // 3 MB
    private MessageDigest md;
    private final byte[] inputData;
    private int byteSize;

    /**
     * Creates a new instance of the this class.
     * @param inputData (bytep[]) The input data to calculate the checksum for.
     * @throws IllegalArgumentException Thrown if the input data is null or empty.
     */
    public CalculateChecksum(byte[] inputData) throws IllegalArgumentException {
        if (inputData == null || inputData.length == 0) {
            throw new IllegalArgumentException("Input data must contain at least one byte.");
        }
        this.inputData = inputData;
        this.byteSize = inputData.length;
    }

    /**
    * Calculates the checksum for the input data.
    * @param hashAlgorithm (String) The hash algorithm to use. Must be one of: MD5, SHA-1, SHA-256, SHA-512.
    * @return (byte[]) The calculated checksum.
    * @throws NoSuchAlgorithmException Thrown if the algorithm is not available.
    * @throws InterruptedException Thrown if the thread is interrupted.
    * @throws ExecutionException Thrown if the execution fails.
    * @throws IOException Thrown if an I/O error occurs.
    */
    public byte[] getChecksum(String hashAlgorithm) throws NoSuchAlgorithmException, InterruptedException, ExecutionException, IOException {
        md = MessageDigest.getInstance(hashAlgorithm);
        if (byteSize < MIN_CHUNK_SIZE * 2) {
            return calculateSmallChecksum();
        }
        return calculateChecksum() ;
    }

    /**
     * Calculates the checksum for small input data.
     * @return (byte[]) The calculated checksum.
     */
    private byte[] calculateSmallChecksum() {
        return md.digest(inputData);
    }

    /**
     * Calculates the checksum for large input data.
     * @return (byte[]) The calculated checksum.
     * @throws InterruptedException Thrown if the thread is interrupted.
     * @throws ExecutionException Thrown if the execution fails.
     * @throws IOException Thrown if an I/O error occurs.
     */
    private byte[] calculateChecksum() throws InterruptedException, ExecutionException, IOException  {
        ByteArrayInputStream bais = new ByteArrayInputStream(inputData);
        int numProcessors = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(numProcessors, numProcessors,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        long fileSize = inputData.length;
        long chunkSize = Math.max(MIN_CHUNK_SIZE, Math.min(fileSize / numProcessors, MAX_CHUNK_SIZE));
        int numChunks = (int) Math.ceil((double) fileSize / chunkSize);
        byte[] buffer = new byte[(int) chunkSize];
        List<Future<?>> futures = new ArrayList<>(numChunks);
        for (int i = 0; i < numChunks; i++) {
            int bytesRead = bais.read(buffer, 0, (int) Math.min(chunkSize, fileSize - i * chunkSize));
            Future<?> future = executor.submit(new ChecksumTask(md, buffer, bytesRead));
            futures.add(future);
        }
        // wait for all futures to complete
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();
        bais.close();
        return md.digest();
    }

    /**
     * This class is used to calculate the checksum for a chunk of data.
     */
    private static class ChecksumTask implements Runnable {
        private MessageDigest md;
        private byte[] buffer;
        private int bytesRead;

        private ChecksumTask(MessageDigest md, byte[] buffer, int bytesRead) {
            this.md = md;
            this.buffer = buffer;
            this.bytesRead = bytesRead;
        }

        @Override
        public void run() {
            md.update(buffer, 0, bytesRead);
        }
    }
}

