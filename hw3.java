import com.google.protobuf.compiler.PluginProtos;
import edu.stanford.nlp.util.Pair;

import java.util.*;

public class hw3 {
    public static void main(String[] args){
        boolean USE_FUZZY = false;

        for (String arg: args)
            if (arg.equals("fuzzy"))
                USE_FUZZY = true;
            else
                System.out.println("Unknown argument: "+arg+" <–ignored");

        // Get the 'unknown' docs data
        String main_dir_path = "unknown/";
        ArrayList<String> file_locations = DirProcessor.getFileLocations(main_dir_path);
        if (file_locations.size() == 0) {
            System.out.println("Can't access files.");
            System.exit(0);
        }

        // Convert csv to matrix and pass to KNN Processor
        String tf_idf_csv_location = "tf_idf.csv";
        List<String> csv_data = FileManager.get_csv_data(tf_idf_csv_location);
        ArrayList<String> terms = DocTermMatrixCreator.get_terms_from_csv(csv_data);
        LinkedHashMap<String, double[]> tf_idf = DocTermMatrixCreator.get_matrix_from_csv(csv_data);
        LinkedHashMap<String, String> training_labels = DocTermMatrixCreator.get_training_labels_from_csv(csv_data);

        KnnProcessor knn_proc = new KnnProcessor(terms,training_labels, USE_FUZZY);

        // Set the directory classes
        Evaluator evaluator = new Evaluator();
        evaluator.cluster_dir_name.put("C1",0);
        evaluator.cluster_dir_name.put("C4",1);
        evaluator.cluster_dir_name.put("C7",2);

        HashMap<Integer, Pair<Double,String>> k_accuracies = new HashMap<>();

        int MAX_K = 24;
        for (int k = 1; k <= MAX_K; k++) {
            System.out.println();
            System.out.println("=======================");
            System.out.println("||  Results for K="+k+"  ||");
            System.out.println("=======================");
            HashMap<String, String> test_results = new HashMap<>();
            Pair<String, String> file_result;
            for (String file_location : file_locations) {
                file_result = knn_proc.run_knn(file_location, tf_idf, k); // MAIN KNN METHOD
                test_results.put(file_result.first, file_result.second);
            }

            int [][] conf_matrix = evaluator.create_confusion_matrix(KnnProcessor.ACTUAL_LABELS, test_results);
            evaluator.print_confusion_matrix(conf_matrix);
            evaluator.print_metrics(conf_matrix);
            System.out.println();

            String cm_str = evaluator.get_confusion_matrix_as_string(conf_matrix);
            double accuracy = evaluator.get_accuracy(conf_matrix);
            k_accuracies.put(k,new Pair<>(accuracy,cm_str));
        }

        // Create results.txt
        String result_filename = "results.txt";
        Pair<Double, ArrayList<Integer>> best_ks = evaluator.get_best_ks(k_accuracies);
        String best_k_results_file_content = evaluator.create_results_file_content(best_ks, k_accuracies);
        FileManager.create_file(result_filename);
        FileManager.write_to_file(result_filename, best_k_results_file_content);

    }
}
