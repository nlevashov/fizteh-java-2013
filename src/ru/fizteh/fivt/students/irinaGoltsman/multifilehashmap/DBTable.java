package ru.fizteh.fivt.students.irinaGoltsman.multifilehashmap;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.irinaGoltsman.shell.Code;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class DBTable implements Table {

    private File tableDirectory;
    private HashMap<String, Storeable> tableOfChanges = new HashMap<>();
    private HashMap<String, Storeable> originalTable = new HashMap<>();
    private Set<String> removedKeys = new HashSet<>();
    private List<Class<?>> columnTypes;
    private TableProvider tableProvider;

    private void checkTableDir(File tableDir) throws IOException {
        if (!tableDir.exists()) {
            throw new IOException(String.format("DBTable: table dir %s does not exist", tableDir));
        }
        File[] listFiles = tableDir.listFiles();
        if (listFiles == null) {
            throw new IOException(String.format("DBTable: file %s is not a dir", tableDir));
        }
        if (listFiles.length == 0) {
            throw new IOException("empty dir");
        }
        for (File dirFile : listFiles) {
            if (dirFile.isDirectory()) {
                if (!dirFile.getName().matches("(0|1|2|3|4|5|6|7|8|9|10|11|12|13|14|15)\\.dir")) {
                    throw new IOException(String.format("illegal name of dir %s inside table %s",
                            dirFile.getName(), tableDir.getName()));
                } else {
                    File[] listFilesInsideDir = dirFile.listFiles();
                    if (listFilesInsideDir.length == 0){
                        throw new IOException("empty dir " + dirFile.getName());
                    }
                    for (File datFiles : listFilesInsideDir) {
                        if (!datFiles.getName().matches("(0|1|2|3|4|5|6|7|8|9|10|11|12|13|14|15)\\.dat")) {
                            throw new IOException(String.format("illegal name of file %s inside dir %s inside table %s",
                                    datFiles.getName(), dirFile.getName(), tableDir.getName()));
                        }
                    }
                }
            } else {
                if (!dirFile.getName().equals("signature.tsv")) {
                    throw new IOException("illegal file " + dirFile.getName());
                }
            }
        }
    }

    public DBTable(File inputTableDirectory, TableProvider provider, List<Class<?>> types)
            throws IOException {
        checkTableDir(inputTableDirectory);
        tableDirectory = inputTableDirectory;
        tableProvider = provider;
        columnTypes = types;
        HashMap<String, String> tmpTable = new HashMap<>();
        Code returnCOde = FileManager.readDBFromDisk(tableDirectory, tmpTable);
        if (returnCOde != Code.OK) {
            throw new IOException("Error while reading table: " + this.getName());
        }
        List<String> keys = new ArrayList<>(tmpTable.keySet());
        List<String> values = new ArrayList<>(tmpTable.values());
        for (int i = 0; i < values.size(); i++) {
            try {
                Storeable rowValue = tableProvider.deserialize(this, values.get(i));
                originalTable.put(keys.get(i), rowValue);
            } catch (ParseException e) {
                throw new IOException(e);
            }
        }
    }

    public DBTable(File inputTableDirectory, TableProvider provider)
            throws IOException {
        checkTableDir(inputTableDirectory);
        tableDirectory = inputTableDirectory;
        tableProvider = provider;
        columnTypes = FileManager.readTableSignature(tableDirectory);
        HashMap<String, String> tmpTable = new HashMap<>();
        Code returnCOde = FileManager.readDBFromDisk(tableDirectory, tmpTable);
        if (returnCOde != Code.OK) {
            throw new IOException("Error while reading table: " + this.getName());
        }
        List<String> keys = new ArrayList<>(tmpTable.keySet());
        List<String> values = new ArrayList<>(tmpTable.values());
        for (int i = 0; i < values.size(); i++) {
            try {
                Storeable rowValue = tableProvider.deserialize(this, values.get(i));
                originalTable.put(keys.get(i), rowValue);
            } catch (ParseException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public String getName() {
        return tableDirectory.getName();
    }

    @Override
    public Storeable get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("remove: key is null");
        }
        Storeable value = tableOfChanges.get(key);
        if (value == null) {
            if (removedKeys.contains(key)) {
                return null;
            }
            value = originalTable.get(key);
        }
        return value;
    }

    //Проверяет соответствие типов в переданном Storeable с типами таблицы
    private void checkEqualityTypes(Storeable storeable) throws ColumnFormatException {
        for (int numberOfType = 0; numberOfType < columnTypes.size(); numberOfType++) {
            Object type;
            try {
                type = storeable.getColumnAt(numberOfType);
            } catch (IndexOutOfBoundsException e) {
                throw new ColumnFormatException("table put: types of storeable mismatch");
            }
            if (type != null) {
                if (!columnTypes.get(numberOfType).equals(type.getClass())) {
                    throw new ColumnFormatException("table put: types of storeable mismatch");
                }
            }
        }
        try {  //Проверка на то, что число колонок в storeable не больше допустимого
            storeable.getColumnAt(columnTypes.size());
        } catch (IndexOutOfBoundsException e) {
            return;
        }
        throw new ColumnFormatException("storeable has more columns then must have");
    }

    @Override
    public Storeable put(String key, Storeable value) throws ColumnFormatException {
        if (value == null || key == null) {
            throw new IllegalArgumentException("put: key or value is null");
        }
        if (key.trim().isEmpty()) {
            throw new IllegalArgumentException("put: key is empty");
        }
        if (key.matches(".*\\s+.*")) {
            throw new IllegalArgumentException("put: key contains white space");
        }
        checkEqualityTypes(value);
        Storeable originalValue = originalTable.get(key);
        Storeable oldValue = tableOfChanges.put(key, value);
        //Значит здесь впервые происходит перезаписывание старого значения.
        if (!removedKeys.contains(key) && oldValue == null) {
            oldValue = originalValue;
        }
        if (originalValue != null) {
            removedKeys.add(key);
        }
        return oldValue;
    }

    @Override
    public Storeable remove(String key) {
        if (key == null) {
            throw new IllegalArgumentException("table remove: key is null");
        }
        Storeable value = tableOfChanges.get(key);
        if (value == null) {
            if (!removedKeys.contains(key)) {
                value = originalTable.get(key);
                if (value != null) {
                    removedKeys.add(key);
                }
            }
        } else {
            tableOfChanges.remove(key);
            if (originalTable.containsKey(key)) {
                removedKeys.add(key);
            }
        }
        return value;
    }

    @Override
    public int size() {
        return tableOfChanges.size() + originalTable.size() - removedKeys.size();
    }

    //@return Количество сохранённых ключей.
    @Override
    public int commit() throws IOException {
        int count = countTheNumberOfChanges();
        for (String delString : removedKeys) {
            originalTable.remove(delString);
        }
        originalTable.putAll(tableOfChanges);
        List<String> keys = new ArrayList<>(originalTable.keySet());
        List<Storeable> values = new ArrayList<>(originalTable.values());
        HashMap<String, String> serializedTable = new HashMap();
        for (int i = 0; i < values.size(); i++) {
            String serializedValue = tableProvider.serialize(this, values.get(i));
            serializedTable.put(keys.get(i), serializedValue);
        }
        FileManager.writeTableOnDisk(tableDirectory, serializedTable);
        tableOfChanges.clear();
        removedKeys.clear();
        return count;
    }

    @Override
    public int rollback() {
        int count = countTheNumberOfChanges();
        tableOfChanges.clear();
        removedKeys.clear();
        return count;
    }

    @Override
    public int getColumnsCount() {
        return columnTypes.size();
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        if (columnIndex >= columnTypes.size() || columnIndex < 0) {
            throw new IndexOutOfBoundsException("invalid column index: " + columnIndex);
        }
        return columnTypes.get(columnIndex);
    }

    public int countTheNumberOfChanges() {
        int countOfChanges = 0;
        for (String currentKey : removedKeys) {
            if (tableOfChanges.containsKey(currentKey)) {
                Storeable currentValue = tableOfChanges.get(currentKey);
                if (checkStoreableForEquality(originalTable.get(currentKey), currentValue)) {
                    continue;
                }
            }
            countOfChanges++;
        }
        for (String currentKey : tableOfChanges.keySet()) {
            if (!originalTable.containsKey(currentKey)) {
                countOfChanges++;
            }
        }
        return countOfChanges;
    }

    private boolean checkStoreableForEquality(Storeable first, Storeable second) {
        String string1 = tableProvider.serialize(this, first);
        String string2 = tableProvider.serialize(this, second);
        return string1.equals(string2);
    }
}
