package ru.fizteh.fivt.students.drozdowsky.databaseTest;

import org.junit.*;
import org.junit.Test;
import ru.fizteh.fivt.students.drozdowsky.Commands.ShellController;
import ru.fizteh.fivt.students.drozdowsky.database.MFHMProviderFactory;
import ru.fizteh.fivt.students.drozdowsky.database.MultiFileHashMap;

import java.io.File;
import java.io.IOException;

public class MFHMProviderFactoryTest {
    static MFHMProviderFactory factory;
    static File databaseDir;

    @Before
    public void setUp() {
        factory = new MFHMProviderFactory();
        String workingDir = System.getProperty("user.dir");
        databaseDir = new File(workingDir + "/" + "test");
        databaseDir.mkdir();
    }

    @Test
    public void legalCreateTest() {
        MultiFileHashMap provider = factory.create(databaseDir.getAbsolutePath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createNullTestShouldFail() {
        MultiFileHashMap provider = factory.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidNameShouldFail() {
        MultiFileHashMap badProvider = factory.create("aba/aba");
    }

    @After
    public void tearDown() {
        try {
            ShellController.deleteDirectory(databaseDir);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
