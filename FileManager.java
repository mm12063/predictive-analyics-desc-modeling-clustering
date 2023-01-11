import edu.stanford.nlp.util.Pair;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class FileManager {

    public static void create_keywords_file(ArrayList<String> dirs,
                                            int num_keywords,
                                            LinkedHashMap<String, double[]> matrix_tf_idf,
                                            ArrayList<String> all_terms){
        DocTermMatrixCreator doc_term_matrix_creator = new DocTermMatrixCreator();
        String filename = "topics.txt";
        FileManager.create_file(filename);

        for (String dir_name: dirs) {
            ArrayList<Pair<Double, Integer>> keywords = doc_term_matrix_creator.generate_dir_keywords(dir_name,matrix_tf_idf);
            String keyword_string = dir_name + " Keywords: \n";
            keyword_string += doc_term_matrix_creator.create_keyword_str(keywords,num_keywords,all_terms);
            FileManager.write_to_file(filename,keyword_string);
        }
    }

    public static void matrices_to_csv(LinkedHashMap<String, double[]> pop_matrix,
                                            LinkedHashMap<String, double[]> matrix_tf_idf,
                                            ArrayList<String> all_terms){
        String filename = "matrices.csv";
        FileManager.create_file(filename);

        DocTermMatrixCreator doc_term_matrix_creator = new DocTermMatrixCreator();

        String pop_matrix_as_csv = doc_term_matrix_creator.get_matrix_as_csv(all_terms,pop_matrix);
        String tf_idf_matrix_as_csv = doc_term_matrix_creator.get_matrix_as_csv(all_terms,matrix_tf_idf);

        String csv_string_full = pop_matrix_as_csv + "\n" + tf_idf_matrix_as_csv;
        FileManager.write_to_file(filename,csv_string_full);
    }

    public static void tf_idf_to_csv(LinkedHashMap<String, double[]> matrix_tf_idf,
                                       ArrayList<String> all_terms){
        String filename = "tf_idf.csv";
        FileManager.create_file(filename);

        DocTermMatrixCreator doc_term_matrix_creator = new DocTermMatrixCreator();
        String tf_idf_matrix_as_csv = doc_term_matrix_creator.get_matrix_as_csv(all_terms,matrix_tf_idf);

        FileManager.write_to_file(filename,tf_idf_matrix_as_csv);
    }

    public static void create_file(String filename) {
        try {
            File file = new File(filename);
            if (file.exists())
                delete_file(filename);
            if (file.createNewFile())
                System.out.println("File created: " + file.getName());
            else
                System.out.println("File already exists.");
        } catch (IOException e) {
            System.out.println("Cannot create file.");
            e.printStackTrace();
        }
    }

    public static void write_to_file(String filename, String content) {
        try {
            content += "\n";
            Files.write(Paths.get(filename), content.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Cannot write to file.");
            e.printStackTrace();
        }
    }

    public static void delete_file(String filename) {
        File file = new File(filename);
        if (file.delete()) {
            System.out.println();
            System.out.println("File deleted: " + file.getName());
        } else
            System.out.println("Unable to delete file: "+ file.getName());
    }

    public static List<String> get_csv_data(String csv_location){
        Path path;
        List<String> file_data = new ArrayList<>();
        try {
            String abs_path = DirProcessor.get_file_path(csv_location);
            path = Paths.get(abs_path);
            file_data = Files.readAllLines(path);
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
        return file_data;
    }
}
