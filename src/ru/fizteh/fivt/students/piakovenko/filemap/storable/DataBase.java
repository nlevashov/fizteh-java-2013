package ru.fizteh.fivt.students.piakovenko.filemap.storable;



import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.piakovenko.filemap.Exit;
import ru.fizteh.fivt.students.piakovenko.filemap.Get;
import ru.fizteh.fivt.students.piakovenko.filemap.GlobalFileMapState;
import ru.fizteh.fivt.students.piakovenko.filemap.Put;
import ru.fizteh.fivt.students.piakovenko.filemap.storable.JSON.JSONSerializer;
import ru.fizteh.fivt.students.piakovenko.shell.Remove;
import ru.fizteh.fivt.students.piakovenko.shell.Shell;

import java.io.*;
import java.lang.Math;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created with IntelliJ IDEA.
 * User: Pavel
 * Date: 12.10.13
 * Time: 22:45
 * To change this template use File | Settings | File Templates.
 */
public class DataBase implements Table {
    private String name;
    private RandomAccessFile raDataBaseFile = null;
    private DataBaseMap map = null;
    private Shell shell = null;
    private File dataBaseStorage = null;
    private int changed;
    private List<Class<?>> storeableClasses;
    private final String nameOfFileWithTypes = "signature.tsv";
    private TableProvider parent = null;
    private ThreadLocal<Transaction> transaction;
    protected final Lock lock = new ReentrantLock(true);

    private class Transaction {
        private Map<String, Storeable> transactionMap = null;
        private Map<String, Boolean> removedMap = null;

        public Transaction() {
            transactionMap = new HashMap<String, Storeable>();
            removedMap = new HashMap<String, Boolean>();
        }

        public void put(String key, Storeable value) {
            transactionMap.put(key, value);
        }

        public Storeable get(String key) {
            if (transactionMap.containsKey(key)) {
                return transactionMap.get(key);
            }
            return map.get(key);
        }


        public int commit() {
            return map.commit(transactionMap);
        }

        public int size() {
            return map.currentSize(transactionMap);
        }

        public int rollback() {
            int changes = map.changesCount(transactionMap);
            transactionMap.clear();
            return changes;
        }


    }

    public void checkAlienStoreable(Storeable storeable) {
        for (int index = 0; index < getColumnsCount(); ++index) {
            try {
                Object o = storeable.getColumnAt(index);
                if (o == null) {
                    continue;
                }
                if (!o.getClass().equals(getColumnType(index))) {
                    throw new ColumnFormatException("Alien storeable with incompatible types");
                }
            } catch (IndexOutOfBoundsException e) {
                throw new ColumnFormatException("Alien storeable with less columns");
            }
        }
        try {
            storeable.getColumnAt(getColumnsCount());
        } catch (IndexOutOfBoundsException e) {
            return;
        }
        throw new ColumnFormatException("Alien storeable with more columns");
    }

    private boolean isValidNameDirectory(String name){
        if (name.length() < 5 || name.length() > 6)
            return false;
        int number = Integer.parseInt(name.substring(0, name.indexOf('.')), 10);
        if (number > 15 || number < 0)
            return false;
        if (!name.substring(name.indexOf('.') + 1).equals("dir"))
            return false;
        return true;
    }

    private boolean isValidNameFile(String name){
        if (name.length() < 5 || name.length() > 6)
            return false;
        int number = Integer.parseInt(name.substring(0, name.indexOf('.')), 10);
        if (number > 15 || number < 0)
            return false;
        if (!name.substring(name.indexOf('.') + 1).equals("dat"))
            return false;
        return true;
    }

    private int ruleNumberDirectory (String key) {
        int b = Math.abs(key.getBytes()[0]);
        return b % 16;
    }

    private int ruleNumberFile (String key) {
        int b = Math.abs(key.getBytes()[0]);
        return b / 16 % 16;
    }

    private void readFromFile() throws IOException  {
        long length = raDataBaseFile.length();
        while (length > 0) {
            int l1 = raDataBaseFile.readInt();
            if (l1 <= 0) {
                throw new IOException("Length of new key less or equals zero");
            } else if (l1 > 1024 * 1024) {
                throw new IOException("Key greater than 1 MB");
            }
            length -= 4;
            int l2 = raDataBaseFile.readInt();
            if (l2 <= 0) {
                throw new IOException("Length of new value less or equals zero");
            } else if (l2 > 1024 * 1024) {
                throw new IOException("Value greater than 1 MB");
            }
            length -= 4;
            byte [] key = new byte [l1];
            byte [] value = new byte [l2];
            if (raDataBaseFile.read(key) < l1) {
                throw new IOException("Key: read less, that it was pointed to read");
            } else {
                length -= l1;
            }
            if (raDataBaseFile.read(value) < l2) {
                throw new IOException("Value: read less, that it was pointed to read");
            } else {
                length -= l2;
            }
            try {
                map.primaryPut(new String(key, StandardCharsets.UTF_8), JSONSerializer.deserialize(this, new String(value, StandardCharsets.UTF_8)));
            } catch (ParseException e) {
                System.err.println("readFromFile: problem with desereliaze" + e.getMessage());
                System.exit(1);
            }
        }
    }

    private void readFromFile (File storage, int numberOfDirectory) throws IOException {
        RandomAccessFile ra = null;
        try {
            ra = new RandomAccessFile(storage, "rw");
            int numberOfFile =  Integer.parseInt(storage.getName().substring(0, storage.getName().indexOf('.')), 10);
            long length = ra.length();
            while (length > 0) {
                int l1 = ra.readInt();
                if (l1 <= 0) {
                    throw new IOException("Length of new key less or equals zero");
                } else if (l1 > 1024 * 1024) {
                    throw new IOException("Key greater than 1 MB");
                }
                length -= 4;
                int l2 = ra.readInt();
                if (l2 <= 0) {
                    throw new IOException("Length of new value less or equals zero");
                } else if (l2 > 1024 * 1024) {
                    throw new IOException("Value greater than 1 MB");
                }
                length -= 4;
                byte [] key = new byte [l1];
                byte [] value = new byte [l2];
                if (ra.read(key) < l1) {
                    throw new IOException("Key: read less, that it was pointed to read");
                } else {
                    length -= l1;
                }
                if (ra.read(value) < l2) {
                    throw new IOException("Value: read less, that it was pointed to read");
                } else {
                    length -= l2;
                }
                String keyString = new String(key, StandardCharsets.UTF_8);
                String valueString = new String(value, StandardCharsets.UTF_8);
                if (ruleNumberFile(keyString) != numberOfFile || ruleNumberDirectory(keyString) != numberOfDirectory) {
                    throw new IOException("Wrong place of key value! Key: " + keyString + " Value: " + valueString);
                } else {
                    try {
                        map.primaryPut(keyString, JSONSerializer.deserialize(this, valueString));
                    } catch (ParseException e) {
                        System.err.println("readFromFile: problem with deserializer" + e.getMessage());
                        System.exit(1);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error! " + e.getMessage());
            System.exit(1);
        } finally {
            if (ra != null) {
                ra.close();
            }
        }
    }

    private void saveToFile () throws IOException {
        long length  = 0;
        raDataBaseFile.seek(0);
        for (String key: map.getMap().keySet()) {
            byte [] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte [] valueBytes = JSONSerializer.serialize(this, map.getMap().get(key)).getBytes(StandardCharsets.UTF_8);
            raDataBaseFile.writeInt(keyBytes.length);
            raDataBaseFile.writeInt(valueBytes.length);
            raDataBaseFile.write(keyBytes);
            raDataBaseFile.write(valueBytes);
            length += 4 + 4 + keyBytes.length + valueBytes.length;
        }
        raDataBaseFile.setLength(length);
    }

    private void saveToFile(File f, String key, String value) throws IOException {
        RandomAccessFile ra = null;
        try {
            ra = new RandomAccessFile(f, "rw");
            ra.seek(ra.length());
            byte [] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte [] valueBytes = value.getBytes(StandardCharsets.UTF_8);
            ra.writeInt(keyBytes.length);
            ra.writeInt(valueBytes.length);
            ra.write(keyBytes);
            ra.write(valueBytes);
        } finally {
            ra.close();
        }
    }

    private void saveToDirectory() throws IOException{
        if (dataBaseStorage.exists()) {
            Remove.removeRecursively(dataBaseStorage);
        }
        if (!dataBaseStorage.mkdirs()){
            throw new IOException("Unable to create this directory - " + dataBaseStorage.getCanonicalPath());
        }
        for (String key : map.getMap().keySet()) {
            Integer numberOfDirectory = ruleNumberDirectory(key);
            Integer numberOfFile = ruleNumberFile(key);
            File directory = new File (dataBaseStorage, numberOfDirectory.toString() + ".dir");
            if (!directory.exists()) {
                if (!directory.mkdirs()){
                    throw new IOException("Unable to create this directory - " + directory.getCanonicalPath());
                }
            }
            File writeFile = new File(directory, numberOfFile.toString() + ".dat" );
            if (!writeFile.exists()) {
                writeFile.createNewFile();
            }
            saveToFile(writeFile, key, JSONSerializer.serialize(this, map.getMap().get(key)));
        }
    }

    private void loadDataBase (File dataBaseFile) throws IOException {
        raDataBaseFile = new RandomAccessFile(dataBaseFile, "rw");
        try {
            readFromFile();
        } catch (IOException e) {
            System.err.println("Error! " + e.getCause());
            System.exit(1);
        }
    }

    private void readFromDirectory(File dir, int numberOfDirectory) throws IOException {
        for (File f: dir.listFiles()) {
            if (!isValidNameFile(f.getName())) {
                throw new IOException("Wrong name of file!");
            }
            readFromFile(f, numberOfDirectory);
        }
    }

    private void loadFromDirectory (File directory) throws IOException {
        for (File f : directory.listFiles()) {
            if (!isValidNameDirectory(f.getName())) {
                throw new IOException("Wrong name of directory!");
            }
            int numberOfDirectory = Integer.parseInt(f.getName().substring(0, f.getName().indexOf('.')), 10);
            readFromDirectory(f, numberOfDirectory);
        }
    }

    private void readClasses() throws IOException {
        File fileWithClasses = null;
        if (dataBaseStorage.isDirectory()) {
            fileWithClasses = new File(nameOfFileWithTypes);
        } else {
            fileWithClasses = new File (dataBaseStorage.getParent(), nameOfFileWithTypes);
        }
        if (!fileWithClasses.exists()) {
            throw new IOException("no file with classes!");
        }
        BufferedReader reader = new BufferedReader(new FileReader(fileWithClasses));
        String types = reader.readLine();
        Class<?> temp = null;
        for (String type : types.trim().split("\\s")){
            temp = ColumnTypes.fromNameToType(type);
            if (temp == null) {
                throw new IOException("wrong type!");
            } else {
                storeableClasses.add(temp);
            }
        }
    }

    public DataBase (Shell sl, File storage, TableProvider _parent, List<Class<?>> columnTypes) {
        map = new DataBaseMap();
        shell  = sl;
        dataBaseStorage = storage;
        name = storage.getName();
        changed = 0;
        parent = _parent;
        storeableClasses = columnTypes;
        transaction = new ThreadLocal<Transaction>() {
           @Override
           protected Transaction initialValue() {
            return new Transaction();
           }
        };
    }

    public DataBase (Shell sl, File storage, TableProvider _parent) {
        map = new DataBaseMap();
        shell  = sl;
        dataBaseStorage = storage;
        name = storage.getName();
        changed = 0;
        parent = _parent;
        transaction = new ThreadLocal<Transaction>() {
            @Override
            protected Transaction initialValue() {
                return new Transaction();
            }
        };
        try {
            readClasses();
        } catch (IOException e) {
            System.out.println("Database: Oops! don't have file with types!");
            System.exit(1);
        }
    }

    public DataBase () {
        map = new DataBaseMap();
        shell  = new Shell();
        dataBaseStorage = new File (System.getProperty("fizteh.db.dir"));
        name = dataBaseStorage.getName();
        changed = 0;
    }

    public void load () throws IOException {
        if (dataBaseStorage.isFile()) {
            loadDataBase(dataBaseStorage);
        } else {
            loadFromDirectory(dataBaseStorage);
        }
    }

    public String getName () {
        return name;
    }

    public void initialize (GlobalFileMapState state) {
        shell.addCommand(new Exit(state));
        shell.addCommand(new Put(state));
        shell.addCommand(new Get(state));
        shell.addCommand(new ru.fizteh.fivt.students.piakovenko.filemap.Remove(state));
        try {
            load();
        } catch (IOException e) {
            System.err.println("Error! " + e.getMessage());
            System.exit(1);
        }
    }

    public void saveDataBase () throws IOException {
        if (dataBaseStorage.isFile()) {
            try {
                saveToFile();
            } finally {
                raDataBaseFile.close();
            }
        } else {
            saveToDirectory();
        }
    }

    public Storeable get (String key) throws IllegalArgumentException {
        if (key == null || (key.isEmpty() || key.trim().isEmpty())) {
            throw new IllegalArgumentException("Table name cannot be null");
        }
       return transaction.get().get(key);
    }

    public Storeable put (String key, Storeable value) throws IllegalArgumentException {
        if ((key == null) || (key.trim().isEmpty())) {
            throw new IllegalArgumentException("Key can not be null");
        }
        if (key.matches("\\s*") || key.split("\\s+").length != 1) {
            throw new IllegalArgumentException("Key contains whitespaces");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        checkAlienStoreable(value);
        Storeable oldValue = transaction.get().get(key);
        transaction.get().put(key, value);
        return oldValue;
    }

    public Storeable remove(String key) throws IllegalArgumentException {
        if (key == null || (key.isEmpty() || key.trim().isEmpty())) {
            throw new IllegalArgumentException("Key name cannot be null");
        }
        Storeable oldValue = transaction.get().get(key);
        transaction.get().put(key, null);
        return oldValue;
    }

    public File returnFiledirectory() {
        return dataBaseStorage;
    }

    public int size() {
        System.out.println(transaction.get().size());
        return transaction.get().size();
    }

    public int commit () {

        try {
            lock.lock();
            int tempChanged = transaction.get().commit();
            saveDataBase();
            System.out.println(tempChanged);
            return tempChanged;
        } catch (IOException e) {
            System.err.println("Error! " + e.getMessage());
            System.exit(1);
        }  finally {
            lock.unlock();
        }
        return 0;
    }

    public int rollback () {
        int tempChanged = transaction.get().rollback();
        System.out.println(tempChanged);
        return tempChanged;
    }

    public int getColumnsCount() {
        return storeableClasses.size();
    }

    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        return storeableClasses.get(columnIndex);
    }

}
