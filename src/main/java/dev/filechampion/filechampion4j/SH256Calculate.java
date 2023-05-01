package dev.filechampion.filechampion4j;


import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SH256Calculate {

    private static final int MIN_CHUNK_SIZE = 1024 * 1024; // 0.5 MB
    private static final int MAX_CHUNK_SIZE = 5 * 1024 * 1024; // 2 MB
    private final byte[] inputData;

    public SH256Calculate(byte[] inputData) {
        this.inputData = inputData;
    }

    public byte[] getChecksum() throws Exception {
        return calculateSHA256Checksum();
    }

    private byte[] calculateSHA256Checksum() throws Exception {
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

