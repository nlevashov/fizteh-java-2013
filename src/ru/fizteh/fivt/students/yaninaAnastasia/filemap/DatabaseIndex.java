package ru.fizteh.fivt.students.yaninaAnastasia.filemap;

import ru.fizteh.fivt.storage.structured.Index;
import ru.fizteh.fivt.storage.structured.Storeable;

import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DatabaseIndex implements Index {
    DatabaseTable indexTable;
    int column;
    String indexName;
    HashMap<Object, String> indexes;
    ReadWriteLock lock = new ReentrantReadWriteLock(true);

    DatabaseIndex(DatabaseTable table, int column, String name, HashMap<Object, String> indexes) {
        this.indexTable = table;
        this.column = column;
        this.indexName = name;
        this.indexes = indexes;
    }

    public String getName() {
        return indexName;
    }

    public Storeable get(String key) {
        if ((key == null) || key.isEmpty()) {
            throw new IllegalArgumentException("The key is illegal");
        }
        Object myKey = null;
        Storeable result = null;
        lock.readLock().lock();
        try {
            if (indexTable.getColumnType(column).equals(String.class)) {
                myKey = key;
            } else {
                myKey = DatabaseTableProvider.typesParser(key, indexTable.getColumnType(column));
            }
            if ((indexes.get(myKey) == null) || (indexes.get(myKey).isEmpty())) {
                throw new IllegalArgumentException("The required index is illegal");
            }
            result = indexTable.get(indexes.get(myKey));
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }
}
