package dev.filechampion.filechampion4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class IndexSynchronizer {
    private List<char[]> credList;
    private Map<String, Map<Integer, Long>> listIndex;
    private ReadWriteLock lock;
    private static final Logger LOGGER = Logger.getLogger(IndexSynchronizer.class.getName());

    public IndexSynchronizer() {
        credList = new ArrayList<>();
        listIndex = new ConcurrentHashMap<>();
        lock = new ReentrantReadWriteLock();
    }

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

    public void removeItem(String secret) {
        lock.writeLock().lock();
        try {
            Map<Integer, Long> secretEntry = listIndex.remove(secret);
            if (secretEntry != null) {
                int index = secretEntry.keySet().iterator().next();
                credList.remove(index);
                updateListIndex(index);
                logFine(new StringBuilder(secret).append(" removed from index."));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

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

    private void updateListIndex(int removedIndex) {
        for (Map.Entry<String, Map<Integer, Long>> entry : listIndex.entrySet()) {
            Map<Integer, Long> secretEntry = entry.getValue();
            int currentIndex = secretEntry.keySet().iterator().next();
            if (currentIndex > removedIndex) {
                secretEntry.put(currentIndex - 1, secretEntry.remove(currentIndex));
            }
        }
    }

    public List<char[]> getCredList() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(credList);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, Map<Integer, Long>> getListIndex() {
        lock.readLock().lock();
        try {
            return new HashMap<>(listIndex);
        } finally {
            lock.readLock().unlock();
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
