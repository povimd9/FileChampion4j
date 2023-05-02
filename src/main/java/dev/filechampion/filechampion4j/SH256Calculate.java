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
 * This class is used to calculate the SHA256 checksum of a file.
 * It is optimized for large files and uses multiple threads to
 * calculate the checksum in parallel.
 */
public class SH256Calculate {
    private static final int MIN_CHUNK_SIZE = 1024 * 1024; // 0.5 MB
    private static final int MAX_CHUNK_SIZE = 5 * 1024 * 1024; // 2 MB
    private final byte[] inputData;
    private int byteSize;

    /**
     * Creates a new instance of the SH256Calculate class.
     * @param inputData (bytep[]) The input data to calculate the checksum for.
     * @throws IllegalArgumentException Thrown if the input data is null or empty.
     */
    public SH256Calculate(byte[] inputData) throws IllegalArgumentException {
        if (inputData == null || inputData.length == 0) {
            throw new IllegalArgumentException("Input data must contain at least one byte.");
        }
        this.inputData = inputData;
        this.byteSize = inputData.length;
    }

    /**
    * Calculates the SHA256 checksum for the input data.
    * @return (byte[]) The SHA256 checksum.
    * @throws NoSuchAlgorithmException Thrown if the SHA256 algorithm is not available.
    * @throws InterruptedException Thrown if the thread is interrupted.
    * @throws ExecutionException Thrown if the execution fails.
    * @throws IOException Thrown if an I/O error occurs.
    */
    public byte[] getChecksum() throws NoSuchAlgorithmException, InterruptedException, ExecutionException, IOException {
        if (byteSize < MIN_CHUNK_SIZE * 2) {
            return calculateSmallSHA256Checksum();
        }
        return calculateSHA256Checksum() ;
    }

    /**
     * Calculates the SHA256 checksum for small input data.
     * @return (byte[]) The SHA256 checksum.
     * @throws NoSuchAlgorithmException Thrown if the SHA256 algorithm is not available.
     */
    private byte[] calculateSmallSHA256Checksum() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(inputData);
    }

    /**
     * Calculates the SHA256 checksum for large input data.
     * @return (byte[]) The SHA256 checksum.
     * @throws NoSuchAlgorithmException Thrown if the SHA256 algorithm is not available.
     * @throws InterruptedException Thrown if the thread is interrupted.
     * @throws ExecutionException Thrown if the execution fails.
     * @throws IOException Thrown if an I/O error occurs.
     */
    private byte[] calculateSHA256Checksum() throws NoSuchAlgorithmException, InterruptedException, ExecutionException, IOException  {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
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
     * This class is used to calculate the SHA256 checksum for a chunk of data.
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

