import edu.stanford.nlp.util.Pair;

import java.util.*;

public class Evaluator {
    LinkedHashMap<String, Integer> cluster_dir_name = new LinkedHashMap<>();

    private double[] get_precision(int [][] confusion_matrix){
        double[] precisions = new double[confusion_matrix.length];
        for (int i = 0; i < confusion_matrix.length; i++) {
            int sum = 0;
            for (int j = 0; j < confusion_matrix[i].length; j++)
                sum += confusion_matrix[j][i];
            double precision = 0;
            if (sum != 0)
                precision = (double) confusion_matrix[i][i] / sum;
            precisions[i] = precision;
        }
        return precisions;
    }

    private double[] get_recall(int [][] confusion_matrix){
        double[] recalls = new double[confusion_matrix.length];
        for (int i = 0; i < confusion_matrix.length; i++) {
            int sum = 0;
            for (int j = 0; j < confusion_matrix[i].length; j++)
                sum += confusion_matrix[i][j];
            double recall = 0;
            if (sum != 0)
                recall = (double) confusion_matrix[i][i] / sum;
            recalls[i] = recall;
        }
        return recalls;
    }

    public double get_accuracy(int [][] confusion_matrix){
        int accuracy_divisor = 0;
        int accuracy_dividend = 0;
        for (int i = 0; i < confusion_matrix.length; i++) {
            int sum = 0;
            for (int j = 0; j < confusion_matrix[i].length; j++)
                sum += confusion_matrix[i][j];
            accuracy_divisor += sum;
            accuracy_dividend += confusion_matrix[i][i];
        }
        return (double)accuracy_dividend/accuracy_divisor;
    }

    private double[] get_f1(ArrayList<Double[]> metrics){
        double[] f_1s = new double[metrics.size()];
        int idx = 0;
        for (Double[] row: metrics) {
            double divisor = row[0] + row[1];
            double f_1 = 0.0;
            if (divisor > 0)
                f_1 = (2 * row[0] * row[1]) / divisor;
            f_1s[idx]  = f_1;
            idx++;
        }
        return f_1s;
    }

    public void print_metrics(int [][] confusion_matrix){
        // Init metric array
        ArrayList<Double[]> metrics = new ArrayList<>();
        for (int j = 0; j < cluster_dir_name.size(); j++)
            metrics.add(new Double[cluster_dir_name.size()]);

        // Precision and Recall
        double[] precision = get_precision(confusion_matrix);
        double[] recall = get_recall(confusion_matrix);
        for (int i = 0; i < confusion_matrix.length; i++) {
            metrics.get(i)[0] = precision[i];
            metrics.get(i)[1] = recall[i];
        }

        // F_1
        double[] f_1s = get_f1(metrics);
        for (int i = 0; i < metrics.size(); i++)
            metrics.get(i)[2] = f_1s[i];

        // Print results
        int counter = 0;
        System.out.println("Metrics:");
        System.out.printf("%5s %4s %1s %1s %2s %5s %n",  "","precision","","recall", "", "f1-Score");
        for (Map.Entry<String, Integer> dir : cluster_dir_name.entrySet()) {
            System.out.printf("%2s %4s %.3f %4s %.3f %4s %.3f %n",
                    dir.getKey(), "",
                    metrics.get(counter)[0], "",
                    metrics.get(counter)[1], "",
                    metrics.get(counter)[2]);
            counter++;
        }
        System.out.printf("%5s %19s %.3f %n", "Accuracy:","", get_accuracy(confusion_matrix));
    }

    public int [][] create_confusion_matrix(ArrayList<ArrayList<String>> clusters){
        int [][] results_array = new int[clusters.size()][clusters.size()];

        for (int i = 0; i < clusters.size(); i++) {
            for (String point: clusters.get(i)){
                String dir_name = point.split("__")[0];
                if (cluster_dir_name.get(dir_name) == i) {
                    results_array[i][i]++;
                } else {
                    results_array[i][cluster_dir_name.get(dir_name)]++;
                }
            }
        }
        return results_array;
    }

    public int [][] create_confusion_matrix(HashMap<String,String> actual, HashMap<String,String> test_results){

        int [][] results_array = new int[KnnProcessor.CLASS_CAT_NAME.size()][KnnProcessor.CLASS_CAT_NAME.size()];
        String[] potential_classes;
        boolean match;

        for (Map.Entry<String, String> a : actual.entrySet()) {
            String actual_class = a.getValue();
            if (actual_class.contains("/"))
                potential_classes = actual_class.split("/");
            else
                potential_classes = new String[]{actual_class};

            String test_class = test_results.get(a.getKey());
            match = Arrays.asList(potential_classes).contains(test_class);

            if (match)
                actual_class = get_matching_class(potential_classes,test_class);
            else
                actual_class = potential_classes[0];

            int actual_idx = cluster_dir_name.get(actual_class);
            int test_idx = cluster_dir_name.get(test_class);

            if (match)
                results_array[actual_idx][actual_idx]++;
            else
                results_array[actual_idx][test_idx]++;
        }

        return results_array;
    }

    private String get_matching_class(String[] potential_classes, String test_class) {
        String matching_class = "";
        for (String potential_class : potential_classes) {
            if (potential_class.equals(test_class))
                matching_class = potential_class;
        }
        return matching_class;
    }

    private boolean any_available_to_assign(int[] cluster_assigned){
        for (int clust: cluster_assigned)
            if (clust == 0)
                return true;
        return false;
    }

    public void set_cluster_dir_name(ArrayList<ArrayList<String>> clusters) {
        // Init
        LinkedHashMap<String, Integer> actual = new LinkedHashMap<>();
        actual.put("C1",0);
        actual.put("C4",0);
        actual.put("C7",0);

        String [] actual_arr = new String[]{"C1","C4","C7"};

        LinkedHashMap<String, int[]> cluster_results = new LinkedHashMap<>();
        String[] cluster_to_dirs = new String[actual.size()];

        for (int i = 0; i < clusters.size(); i++) {
            cluster_results.put("Cluster_"+i, new int[actual.size()]);
            String cluster_to_update = "Cluster_"+i;

            for (String point : clusters.get(i)) {
                String dir_name = point.split("__")[0];
                int new_val = actual.get(dir_name) + 1;
                actual.put(dir_name, new_val);
            }

            int index = 0;
            for (Map.Entry<String, Integer> a : actual.entrySet()) {
                cluster_results.get(cluster_to_update)[index] = a.getValue();
                index++;
            }

            // Reset
            actual.put("C1",0);
            actual.put("C4",0);
            actual.put("C7",0);
        }

        int[] cluster_assigned = new int[cluster_results.size()];
        int[] col_assigned = new int[actual.size()];

        while (any_available_to_assign(cluster_assigned)) {
            Pair<String, int[]> curr_winner = new Pair<>("",new int[2]);
            for (int i = 0; i < actual.size(); i++) {
                if (col_assigned[i] == 0) {
                    int highest = 0, j = 0;
                    for (Map.Entry<String, int[]> result_arr : cluster_results.entrySet()) {
                        if (cluster_assigned[j] == 0){
                            if (result_arr.getValue()[i] >= highest){
                                highest = result_arr.getValue()[i];
                                curr_winner.first = result_arr.getKey();
                                curr_winner.second = new int[]{i,j};
                            }
                        }
                        j++;
                    }
                }
            }

            col_assigned[curr_winner.second[0]] = 1;
            cluster_assigned[curr_winner.second[1]] = 1;
            cluster_to_dirs[curr_winner.second[1]] = actual_arr[curr_winner.second[0]];
        }

        for (int i = 0; i < cluster_to_dirs.length; i++)
            cluster_dir_name.put(cluster_to_dirs[i],i);
    }

    public void print_confusion_matrix(int [][] conf_mat){
        int counter = 0;
        System.out.println("Confusion Matrix:");
        for (Map.Entry<String, Integer> dir : cluster_dir_name.entrySet()) {
            System.out.print(dir.getKey()+ " ");
            for (int val : conf_mat[counter])
                System.out.print("|" + val);
            System.out.print("|");
            System.out.println();
            counter++;
        }
        System.out.println();
    }

    public String get_confusion_matrix_as_string(int [][] conf_mat){
        StringBuilder cf_str = new StringBuilder();
        int counter = 0;
        cf_str.append("Confusion Matrix:").append("\n");
        for (Map.Entry<String, Integer> dir : cluster_dir_name.entrySet()) {
            cf_str.append(dir.getKey()).append(" ");
            for (int val : conf_mat[counter])
                cf_str.append("|").append(val);
            cf_str.append("|");
            cf_str.append("\n");
            counter++;
        }
        return cf_str.toString();
    }

    public Pair<Double,ArrayList<Integer>> get_best_ks(HashMap<Integer, Pair<Double,String>> k_accuracies){
        double highest_accuracy = 0;
        for (Map.Entry<Integer, Pair<Double,String>> results_data : k_accuracies.entrySet())
            if (results_data.getValue().first > highest_accuracy)
                highest_accuracy = results_data.getValue().first;

        Pair<Double,ArrayList<Integer>> best_ks = new Pair<>();
        best_ks.first = highest_accuracy;
        best_ks.second = new ArrayList<>();
        for (Map.Entry<Integer, Pair<Double,String>> results_data : k_accuracies.entrySet())
            if (results_data.getValue().first == highest_accuracy)
                best_ks.second.add(results_data.getKey());

        return best_ks;
    }

    public String create_results_file_content(Pair<Double,ArrayList<Integer>> best_ks,
                                              HashMap<Integer, Pair<Double,String>> k_accuracies){
        double best_acc = best_ks.first;
        StringBuilder results_content = new StringBuilder();
        results_content.append("The following value(s) of K, achieved the highest accuracy value of ")
                .append(best_acc).append(".\n\n");
        for (int k: best_ks.second){
            results_content.append("Value of k: ").append(k).append("\n");
            results_content.append(k_accuracies.get(k).second);
            results_content.append("Accuracy: ").append(best_acc).append("\n\n");
        }

        return results_content.toString();
    }

}
