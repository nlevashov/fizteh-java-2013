package ru.fizteh.fivt.students.kochetovnicolai.fileMap;

import ru.fizteh.fivt.storage.strings.TableProviderFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class DistributedTableProviderFactory implements TableProviderFactory {
    HashMap<String, DistributedTableProvider> providers;

    public DistributedTableProviderFactory() {
        providers = new HashMap<>();
    }

    @Override
    public DistributedTableProvider create(String dir) throws IllegalArgumentException {
        File path = new File(dir);
        if (dir == null) {
            throw new IllegalArgumentException("directory couldn't be null");
        }
        try {
            path = path.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid directory");
        }
        String directory = path.getAbsolutePath();
        if (!providers.containsKey(directory)) {
            providers.put(directory, new DistributedTableProvider(path));
        }
        return providers.get(directory);
    }
}
