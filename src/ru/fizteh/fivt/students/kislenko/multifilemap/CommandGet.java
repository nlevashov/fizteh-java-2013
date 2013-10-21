package ru.fizteh.fivt.students.kislenko.multifilemap;

import ru.fizteh.fivt.students.kislenko.shell.Command;

import java.io.IOException;

public class CommandGet implements Command<MultiFileHashMapState> {
    public String getName() {
        return "get";
    }

    public int getArgCount() {
        return 1;
    }

    public void run(MultiFileHashMapState state, String[] args) throws IOException {
        if (state.getCurrentTableController() == null) {
            System.out.println("no table");
            throw new IOException("Database haven't initialized.");
        }
        String tableName = state.getWorkingTableName().split("\\.")[0];
        int tableNumber = Integer.parseInt(tableName);
        byte b = args[0].getBytes()[0];
        int dirNumber = b % 16;
        if (tableNumber != dirNumber) {
            throw new IOException("Incorrect key's hash code.");
        }
        if (state.hasKey(args[0])) {
            System.out.println("found\n" + state.getValue(args[0]));
        } else {
            System.out.println("not found");
        }
    }
}