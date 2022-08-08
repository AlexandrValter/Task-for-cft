import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SortingManager {
    private String sort;
    private String type;
    private String outputFile;
    private final List<String> inputFiles = new ArrayList<>();
    private int index;
    private BufferedReader[] bufferedReaders;
    private String value;
    private Writer fileWriter;

    public void readParam(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (args[i].equals("-a") || args[i].equals("-d")) {
                    sort = args[i];
                }
                if (args[i].equals("-s") || args[i].equals("-i")) {
                    type = args[i];
                    outputFile = args[++i];
                }
            } else {
                if (type == null) {
                    throw new NullPointerException("Необходимо указать тип данных (-s или -i)");
                }
                if (!args[i].isBlank() && !args[i].isEmpty()) {
                    inputFiles.add(args[i]);
                }
            }
        }
        try {
            checkParam();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void checkParam() throws IOException {
        if (!inputFiles.isEmpty()) {
            List<BufferedReader> tempBufferedReaders = new ArrayList<>();
            for (String file : inputFiles) {
                if (Files.exists(Path.of(file))) {
                    tempBufferedReaders.add(new BufferedReader(new FileReader(file, StandardCharsets.UTF_8)));
                }
            }
            bufferedReaders = tempBufferedReaders.toArray(BufferedReader[]::new);
        } else {
            throw new RuntimeException("Необходимо указать имена файлов с данными для слияния");
        }
        if (!outputFile.isEmpty()) {
            fileWriter = new FileWriter(outputFile, StandardCharsets.UTF_8, true);
        } else {
            throw new RuntimeException("Необходимо указать имя файла для записи результата слияния");
        }
        index = 0;
        if (type != null) {
            mergeFiles();
        } else {
            throw new NullPointerException("Необходимо указать тип данных (-s или -i)");
        }
    }

    private void mergeFiles() throws IOException {
        String[] array = new String[bufferedReaders.length];
        for (int i = 0; i < bufferedReaders.length; i++) {
            Optional<String> element = getElement();
            if (element.isPresent()) {
                while (!validateElement(element.get()) || !checkSorting(element.get())) {
                    element = getElement();
                }
                array[i] = element.get();
            }
            index++;
        }
        while (Arrays.stream(array).anyMatch(Objects::nonNull)) {
            index = findValue(array);
            fileWriter.write(value + "\n");
            Optional<String> nextElement = getElement();
            if (nextElement.isPresent() && validateElement(nextElement.get()) && !checkSorting(nextElement.get())) {
                array[index] = nextElement.get();
                value = nextElement.get();
            } else {
                findCorrectElement(array);
            }
        }
        fileWriter.close();
    }

    private void findCorrectElement(String[] array) throws IOException {
        Optional<String> nextElement;
        while (true) {
            nextElement = getElement();
            if (nextElement.isPresent()) {
                if (validateElement(nextElement.get())) {
                    if (!checkSorting(nextElement.get())) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
        if (nextElement.isEmpty()) {
            array[index] = null;
            for (int i = 0; i < bufferedReaders.length; i++) {
                if (bufferedReaders[i] != null) {
                    index = i;
                }
            }
            value = null;
        } else {
            array[index] = nextElement.get();
            value = nextElement.get();
        }
    }

    private Optional<String> getElement() throws IOException {
        if (index < bufferedReaders.length) {
            if (bufferedReaders[index] != null) {
                if (bufferedReaders[index].ready()) {
                    return Optional.of(bufferedReaders[index].readLine());
                } else {
                    bufferedReaders[index].close();
                    bufferedReaders[index] = null;
                }
            }
        }
        return Optional.empty();
    }

    private int findValue(String[] arr) {
        int index = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null) {
                if (value == null) {
                    value = arr[i];
                    index = i;
                } else if (checkSorting(arr[i])) {
                    value = arr[i];
                    index = i;
                }
            }
        }
        return index;
    }

    private boolean checkSorting(String element) {
        if (value == null) {
            return true;
        } else if (type.equals("-s")) {
            if (sort != null && sort.equals("-d")) {
                return value.compareTo(element) <= 0;
            } else {
                return value.compareTo(element) >= 0;
            }
        } else {
            if (sort != null && sort.equals("-d")) {
                return Long.parseLong(value) <= Long.parseLong(element);
            } else {
                return Long.parseLong(value) >= Long.parseLong(element);
            }
        }
    }

    private boolean validateElement(String element) {
        if (element.contains(" ") || element.isBlank()) {
            return false;
        } else {
            if (type.equals("-i")) {
                try {
                    Long.parseLong(element);
                    return true;
                } catch (NumberFormatException e) {
                    System.out.printf("Введенное значение '%s' не является числом\n", element);
                    return false;
                }
            }
        }
        return true;
    }
}