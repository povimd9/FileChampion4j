package dev.filechampion.filechampion4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Synchronizes and stores the name, index, and timesamp of credentials in the cache.
 */
public class IndexSynchronizer {
    private List<char[]> credList;
    private Map<String, Map<Integer, Long>> listIndex;
    private ReadWriteLock lock;
    private static final Logger LOGGER = Logger.getLogger(IndexSynchronizer.class.getName());

    /**
     * IndexSynchronizer constructor.
     */
    public IndexSynchronizer() {
        credList = new ArrayList<>();
        listIndex = new ConcurrentHashMap<>();
        lock = new ReentrantReadWriteLock();
    }

    /**
     * Adds a new secret to the cache.
     * @param item (char[]) secret value as char array for mutability.
     * @param secret (String) name of the secret.
     */
    public void addItem(char[] item, String secret) {
        lock.writeLock().lock();
        try {
            credList.add(item);
            Map<Integer, Long> secretEntry = new ConcurrentHashMap<>();
            secretEntry.put(credList.size() - 1, System.currentTimeMillis());
            listIndex.put(secret, secretEntry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get a secret value from the cache.
     * @param secret (String) name of the secret.
     * @return (char[]) secret value as char array for mutability, or null if not found.
     */
    public char[] getSecretValue(String secret) {
        lock.readLock().lock();
        try {
            Map<Integer, Long> secretEntry = listIndex.get(secret);
            if (secretEntry != null) {
                int index = secretEntry.keySet().iterator().next();
                secretEntry.put(index, System.currentTimeMillis());
                logFine(new StringBuilder("Retrieved secret: ").append(secret));
                return credList.get(index);
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Remove secrets that have not been accessed in the last maxAgeMillis milliseconds, and synchronize the cache.
     * @param maxAgeMillis (long) maximum age of a secret in milliseconds.
     */
    public void checkAndRemoveStaleSecrets(long maxAgeMillis) {
        lock.writeLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<String, Map<Integer, Long>>> iterator = listIndex.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Map<Integer, Long>> entry = iterator.next();
                Map<Integer, Long> secretEntry = entry.getValue();
                long timestamp = secretEntry.values().iterator().next();
                if (currentTime - timestamp > maxAgeMillis) {
                    int index = secretEntry.keySet().iterator().next();
                    Arrays.fill(credList.get(index), '\u0000');
                    credList.remove(index);
                    iterator.remove();
                    updateListIndex(index);
                    logFine(new StringBuilder("Removed stale secret: ").append(entry.getKey()));
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Update the index of all secrets in the cache after a secret has been removed.
     * @param removedIndex (int) index of the removed secret.
     */
    private void updateListIndex(int removedIndex) {
        for (Map.Entry<String, Map<Integer, Long>> entry : listIndex.entrySet()) {
            Map<Integer, Long> secretEntry = entry.getValue();
            int currentIndex = secretEntry.keySet().iterator().next();
            if (currentIndex > removedIndex) {
                secretEntry.put(currentIndex - 1, secretEntry.remove(currentIndex));
            }
        }
    }

    ////////////////////
    // Helper methods //
    ////////////////////

    /**
     * LOGGER.fine wrapper
     * @param message (StringBuilder) - message to log
     */
    private void logFine(StringBuilder message) {
        if (LOGGER.isLoggable(Level.FINE )) {
            LOGGER.fine(message.toString());
        }
    }
}
