package ru.fizteh.fivt.students.irinaGoltsman.shell;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Shell {
    public enum Code {
        OK,
        ERROR,
        SYSTEM_ERROR,
        EXIT
    }

    //print working directory, печатает абсолютный путь к текущей директории +
    public static Code pwdCommand() {
        Properties p = System.getProperties();
        String dir = p.getProperty("user.dir");
        System.out.println(dir);
        return (Code.OK);
    }

    //Печатает содержимое текущей директории.
    public static Code dirCommand() {
        Properties p = System.getProperties();
        String dir = p.getProperty("user.dir");
        File f1 = new File(dir);
        try {
            for (String child : f1.list()) {
                System.out.println(child);
            }
        } catch (Exception e) {
            System.err.println(e);
            return (Code.SYSTEM_ERROR);
        }
        return (Code.OK);
    }

    //Создание новой директории в текущей директории.
    public static Code mkdirCommand(String nameOfDir) {
        Properties p = System.getProperties();
        String dir = p.getProperty("user.dir");
        File f = new File(dir + File.separator + nameOfDir);
        if (!f.exists()) {
            boolean result = f.mkdir();
            if (!result) {
                System.err.println("mkdir: dir '" + nameOfDir + "' can not be made");
                return Code.ERROR;
            }
        } else {
            System.err.println("mkdir: dir '" + nameOfDir + "' already exists");
            return Code.ERROR;
        }
        return (Code.OK);
    }

    //Копирует указанную в параметра папку/файл в указанное место.
    public static Code cpCommand(String source, String destination) {
        Properties p = System.getProperties();
        String userDir = p.getProperty("user.dir");
        File from = new File(userDir + File.separator + source);
        File to = new File(userDir + File.separator + destination);
        if (!from.exists()) {
            System.err.println("cp: '" + source + "': No such file or directory");
            return Code.ERROR;
        }
        if (!to.exists()) {
            try {
                Files.copy(from.toPath(), to.toPath());
            } catch (Exception e) {
                System.err.println(e);
                return Code.SYSTEM_ERROR;
            }
            return Code.OK;
        }
        if (!to.isDirectory()) {
            System.err.println("cp: '" + destination + "': Is not a directory");
            return Code.ERROR;
        }
        File toInDir = new File(destination + File.separator + source);
        if (toInDir.exists()) {
            System.err.println("cp: '" + source + "': File with such name already exist in '" + destination + "'");
            return Code.ERROR;
        }
        try {
            Files.copy(from.toPath(), toInDir.toPath());
        } catch (Exception e) {
            System.err.println(e);
            return Code.SYSTEM_ERROR;
        }
        return Code.OK;
    }

    // mv <source> <destination> — переносит указанный файл/папку в новое место (файл на прежнем месте удаляется).
    // В частности переименовывает файл/папку, если source и destination находятся в одной папке
    public static Code mvCommand(String source, String destination) {
        Properties p = System.getProperties();
        String userDir = p.getProperty("user.dir");
        File from = new File(userDir + File.separator + source);
        if (!from.exists()) {
            System.err.println("wv: cannot move'" + source + "': No such file or directory'");
            return Code.ERROR;
        }
        if (destination.equals(".")) {
            return Code.OK;
        }
        File toDir = new File(userDir + File.separator + destination);
        if (!toDir.exists()) {
            if (!from.renameTo(toDir)) {
                System.err.println("wv: cannot rename'" + source + "': to '" + destination + "'");
                return Code.ERROR;
            } else {
                return Code.OK;
            }
        } else {
            Code cpReturnCode = cpCommand(source, destination);
            if (cpReturnCode != Code.OK) {
                return cpReturnCode;
            }
            return rmCommand(source);
        }
    }

    //Удаляет указанную в параметрах папку (рекурсивно) или файл.
    public static Code rmCommand(String path) {
        try {
            Properties p = System.getProperties();
            String userDir = p.getProperty("user.dir");
            File inputFile = new File(userDir + File.separator + path);
            if (!inputFile.exists()) {
                System.err.println("rm: cannot remove '" + path + "': No such file or directory");
                return Code.ERROR;
            }
            if (!inputFile.isAbsolute()) {
                System.err.print("Error in rm");
                System.exit(1);
            }
            if (inputFile.isDirectory()) {
                for (File childFile : inputFile.listFiles()) {
                    if (childFile != null) {
                        if (childFile.isDirectory()) {
                            cdCommand(path);
                            if (rmCommand(childFile.toString()) == Code.SYSTEM_ERROR) {
                                return Code.SYSTEM_ERROR;
                            }
                            cdCommand("..");
                        }
                        if (!childFile.delete()) {
                            System.err.println("rm: impossible to remove file '" + childFile.toString() + "'.");
                            return Code.ERROR;
                        }
                    }
                }
            }
            if (!inputFile.delete()) {
                System.err.println("rm: impossible to remove file '" + path + "'.");
                return Code.ERROR;
            } else {
                return Code.OK;
            }
        } catch (Exception e) {
            System.err.println(e);
            return Code.SYSTEM_ERROR;
        }
    }

    //Change directory, смена текущей директории. Поддерживаются ., .., относительные и абсолютные пути.
    public static Code cdCommand(String inputNameDir) {
        File newDir = new File(inputNameDir);
        String path = "";
        try {
            path = newDir.getCanonicalPath();
        } catch (Exception e) {
            System.err.println(e);
            return (Code.SYSTEM_ERROR);
        }
        try {  /*
            if (!newDir.isAbsolute()) {
                /*
                if (path.startsWith("/") || path.startsWith("\\")) {
                    System.err.println("cd: '" + inputNameDir + "': No such file or directory");
                    return (Code.ERROR);
                }

                Properties p = System.getProperties();
                String currentDir = p.getProperty("user.dir");
                path = currentDir + File.separator + inputNameDir;
                newDir = new File(path);
                if (!newDir.isAbsolute()) {
                    System.err.println("Error in cd");
                    System.exit(1);
                }
            }
       */
            if (!newDir.exists()) {
                System.err.println("cd: '" + inputNameDir + "': No such file or directory");
                return (Code.ERROR);
            }
            if (!newDir.isDirectory()) {
                System.err.println("cd: '" + inputNameDir + "': Is not a directory");
                return (Code.ERROR);
            }
            System.setProperty("user.dir", path);
        } catch (Exception e) {
            System.err.println(e);
            return (Code.SYSTEM_ERROR);
        }
        return (Code.OK);
    }

    //Обработка введённой команды.
    public static Code commandProcessing(String command) {
        StringTokenizer st = new StringTokenizer(command, " \t", false);
        ArrayList<String> parts = new ArrayList<String>();
        while (st.hasMoreElements()) {
            String tmp = (String) st.nextElement();
            if (!tmp.equals("")) {
                parts.add(tmp);
            }
        }
        if (parts.get(0).equals("pwd") && parts.size() == 1) {
            return pwdCommand();
        } else if (parts.get(0).equals("exit") && parts.size() == 1) {
            return (Code.EXIT);
        } else if (parts.get(0).equals("dir") && parts.size() == 1) {
            return dirCommand();
        } else if (parts.get(0).equals("mkdir") && parts.size() == 2) {
            return mkdirCommand(parts.get(1));
        } else if (parts.get(0).equals("cd") && parts.size() == 2) {
            return cdCommand(parts.get(1));
        } else if (parts.get(0).equals("rm") && parts.size() == 2) {
            return rmCommand(parts.get(1));
        } else if (parts.get(0).equals("cp") && parts.size() == 3) {
            return cpCommand(parts.get(1), parts.get(2));
        } else if (parts.get(0).equals("mv") && parts.size() == 3) {
            return mvCommand(parts.get(1), parts.get(2));
        } else {
            System.out.println("Incorrect input: " + command);
            return (Code.ERROR);
        }

    }

    public static void main(String[] args) {
        //Интерактивный режим.
        if (args.length == 0) {
            Scanner scan = new Scanner(System.in);
            System.out.print("$ ");
            while (true) {
                String command = "exit";
                if (scan.hasNextLine()) {
                    command = scan.nextLine();
                } else {
                    break;
                }
                String checkIsEmpty = command.replaceAll(" ", "");
                checkIsEmpty = checkIsEmpty.replaceAll("\t", "");
                if (checkIsEmpty.equals("")) {
                    System.out.print("$ ");
                    continue;
                }
                String[] commands = command.split(";");
                if (commands.length == 1) {
                    Code codeOfCommand = commandProcessing(commands[0]);
                    if (codeOfCommand == Code.SYSTEM_ERROR) {
                        System.exit(1);
                    }
                } else {
                    for (int i = 0; i < commands.length; i++) {
                        String checkEmpty = commands[i].replaceAll(" ", "");
                        checkEmpty = checkEmpty.replaceAll("\t", "");
                        if (checkEmpty.equals("")) {
                            continue;
                        }
                        Code codeOfCommand = commandProcessing(commands[i]);
                        if (codeOfCommand == Code.SYSTEM_ERROR) {
                            System.exit(1);
                        }
                    }
                }
                System.out.print("$ ");
            }
        } else {   //Пакетный режим.
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                str.append(args[i]);
                str.append(" ");
            }
            String input = str.toString();
            String[] commands = input.split(";");
            for (int i = 0; i < commands.length; i++) {
                String checkIsEmpty = commands[i].replaceAll(" ", "");
                checkIsEmpty = checkIsEmpty.replaceAll("\t", "");
                if (checkIsEmpty.equals("")) {
                    continue;
                }
                Code codeOfCommand = commandProcessing(commands[i]);
                if (codeOfCommand == Code.SYSTEM_ERROR || codeOfCommand == Code.ERROR) {
                    System.exit(1);
                }
                if (codeOfCommand == Code.EXIT) {
                    System.exit(0);
                }
            }
        }
    }
}