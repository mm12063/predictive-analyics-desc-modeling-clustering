import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class DirProcessor {
    public static ArrayList<String> dir_names = new ArrayList<>();

    public static ArrayList<String> getFileLocationsHelper(File[] listOfFiles, ArrayList<String> file_locations){
        if (listOfFiles != null){
            for (File file : listOfFiles) {
                if (file.isDirectory()) {
                    getFileLocationsHelper(file.listFiles(), file_locations);
                } else {
                    if (file.getName().charAt(0) != '.') {
                        file_locations.add(file.getAbsolutePath());
                    }
                }
            }
        }
        return file_locations;
    }

    public static String get_file_path(String filename){
        String path = "";
        try {
            File file = new File(filename);
            path = file.getAbsolutePath();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return path;
    }

    public static ArrayList<String> getFileLocations(String main_pth){
        File folder = new File(main_pth);
        if (!folder.isDirectory()){
            System.out.println("Not a valid directory location.");
            return new ArrayList<>();
        }
        File[] listOfFiles = folder.listFiles();
        return getFileLocationsHelper(listOfFiles, new ArrayList<>());
    }

    public static void set_dir_names(LinkedHashMap<String, String> corpus_data){
        for (Map.Entry<String, String> file_data : corpus_data.entrySet()) {
            String dir_name = file_data.getKey().split("_")[0];
            if (!dir_names.contains(dir_name))
                dir_names.add(dir_name);
        }
    }
}
