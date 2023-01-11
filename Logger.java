import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class Logger {
    int log_file_counter = 0;
    String filename = "log_file_"+log_file_counter+".log";
    final String DIR_NAME = "logs/";
    String full_path = DIR_NAME+filename;
    String log_title = "";
    Boolean log_title_set = false;

    public Logger(){
        create_dir();
        create_file();
    }

    public void create_dir() {
        File dir = new File(this.DIR_NAME);
        if(dir.exists() || dir.mkdirs()) {
            System.out.println("Log dir created.");
        }
    }

    private String get_time_date(){
        DateTimeFormatter tf_formatter = DateTimeFormatter.ofPattern("HH:mm:ss | dd-MM-yyyy");
        LocalDateTime time_date = LocalDateTime.now();
        return tf_formatter.format(time_date);
    }

    public void create_file() {
        try {
            File file = new File(full_path);
            if (file.exists()) {
                log("\r=== "+get_time_date() + " ============================================\n");
                log("TITLE: "+this.log_title);
                this.log_title_set = false;
            }
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
                log_file_counter += 1;
            }
            else
                System.out.println("File already exists.");
        } catch (IOException e) {
            System.out.println("Cannot create file.");
            e.printStackTrace();
        }
    }

    public void delete_file(String filename) {
        File file = new File(filename);
        if (file.delete()) {
            System.out.println();
            System.out.println("File deleted: " + file.getName());
            log_file_counter -= 1;
        } else
            System.out.println("Unable to delete file: "+ file.getName());
    }

    public void log(String content){
        System.out.println(content);
        try {
            content += "\r";
            Files.write(Paths.get(full_path), content.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Cannot write to file.");
            e.printStackTrace();
        }
    }

    public void log(int num){
        System.out.println(num);
        String content = String.valueOf(num);
        log(content);
    }

    public void log(double num){
        String content = String.format("%.3f", num);
        System.out.println(content);
        log(content);
    }

    public void log(ArrayList<String> list) {
        StringBuilder full_string = new StringBuilder();
        for (String el: list)
            full_string.append(el).append("\n");
        log(full_string.toString());
    }

    public void log(LinkedHashMap<String, double[]> matrix) {
        StringBuilder full_string = new StringBuilder();

        for (Map.Entry<String, double[]> row : matrix.entrySet())
            full_string.append(row.getKey())
                    .append(" ")
                    .append(Arrays.toString(row.getValue()))
                    .append("\n");

        log(full_string.toString());
    }

    public void log_nested_list(ArrayList<ArrayList<String>> lists) {
        StringBuilder full_string = new StringBuilder();
        full_string.append("[");
        for (int i = 0; i < lists.size(); i++) {
            for (int j = 0; j < lists.get(i).size(); j++) {
                if (j ==0)
                    full_string.append("[");
                full_string.append(lists.get(i).get(j));
                if (j != lists.get(i).size()-1)
                    full_string.append(", ");
                if (j == lists.get(i).size()-1)
                    full_string.append("]");
            }
            if (i != lists.size()-1)
                full_string.append(",\n");
        }
        full_string.append("]");
        log(full_string.toString());
    }
}
