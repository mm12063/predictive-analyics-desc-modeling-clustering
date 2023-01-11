import edu.stanford.nlp.util.Pair;

import java.text.DecimalFormat;
import java.util.*;

public class KnnProcessor {
    static HashMap<String,String> CLASS_CAT_NAME = new HashMap<>();
    static HashMap<String,String> ACTUAL_LABELS = new HashMap<>();
    static DecimalFormat decimal_formatter = new DecimalFormat("###.##");
    ArrayList<String> terms;
    LinkedHashMap<String, String> training_labels;
    boolean USE_FUZZY;

    public KnnProcessor(ArrayList<String> terms, LinkedHashMap<String, String> training_labels, boolean USE_FUZZY){
        this.terms = terms;
        this.training_labels = training_labels;
        this.USE_FUZZY = USE_FUZZY;

        CLASS_CAT_NAME.put("C1","Airline Safety");
        CLASS_CAT_NAME.put("C4","Hoof and Mouth Disease");
        CLASS_CAT_NAME.put("C7","Mortgage Rates");

        ACTUAL_LABELS.put("unknown01","C1");
        ACTUAL_LABELS.put("unknown02","C1");
        ACTUAL_LABELS.put("unknown03","C1");
        ACTUAL_LABELS.put("unknown04","C1");
        ACTUAL_LABELS.put("unknown05","C4");
        ACTUAL_LABELS.put("unknown06","C4");
        ACTUAL_LABELS.put("unknown07","C7");
        ACTUAL_LABELS.put("unknown08","C7");
        ACTUAL_LABELS.put("unknown09","C1/C4");
        ACTUAL_LABELS.put("unknown10","C1/C4");
    }

    public Pair<String,String> run_knn(String unknown_file_location, LinkedHashMap<String, double[]> tf_idf, int k){

        TextPreProcessor text_pre_processor = new TextPreProcessor();
        String filename = TextPreProcessor.get_filename(unknown_file_location);
        LinkedHashMap<String, String> file_data = text_pre_processor.get_file_data(unknown_file_location,filename);

        String file_text = file_data.entrySet().iterator().next().getValue();
        double[] file_vec = DocTermMatrixCreator.get_term_freq_per_file(terms, file_text);

        ArrayList<Pair<String, Double>> distances = calc_distances(tf_idf, file_vec);

        String file = filename.split("__")[1];
        System.out.println(file+"'s "+k+" closest neighbour(s):");
        print_k_neighbours(training_labels, distances, k);

        // Find winning class
        LinkedHashMap<String, Integer> results_counter = get_results_counter(training_labels, distances, k);
        String winning_class = Collections.max(results_counter.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
        String winning_cat_name = CLASS_CAT_NAME.get(winning_class);
        System.out.println();

        if (USE_FUZZY)
            System.out.println(file +" is (fuzzy) categorized as: "+ get_fuzzy_results(results_counter));
        else
            System.out.println(file +" is categorized as: "+ winning_cat_name + " ("+winning_class+")");

        System.out.println();
        System.out.println("=======================");
        System.out.println();

        return new Pair<>(file,winning_class);
    }

    private String get_fuzzy_results(LinkedHashMap<String, Integer> results_counter){
        double divisor = 0.0;
        double percentage;
        for (Map.Entry<String, Integer> result : results_counter.entrySet())
            divisor += result.getValue();
        StringBuilder result_str = new StringBuilder();
        for (Map.Entry<String, Integer> result : results_counter.entrySet()) {
            result_str.append("\n").append(result.getKey()).append(": ");
            percentage = result.getValue()/divisor * 100;
            result_str.append(decimal_formatter.format(percentage));
            result_str.append("%");
            result_str.append(" (").append(CLASS_CAT_NAME.get(result.getKey())).append(")");
        }
        return result_str.toString();
    }

    private double[] l2_norm(double[] file_vec){
        double total = 0;

        for (double val : file_vec)
            total += Math.pow(val, 2);
        total = Math.sqrt(total);

        for (int i = 0; i < file_vec.length; i++)
            file_vec[i] = file_vec[i] / total;

        return file_vec;
    }

    private ArrayList<Pair<String, Double>> calc_distances(LinkedHashMap<String, double[]> tf_idf, double[] file_vec) {
        // Using Cosine Sim, so normalise to unit vectors
        double[] file_vec_for_cosine = l2_norm(file_vec);

        ArrayList<Pair<String, Double>> distances = new ArrayList<>();
        for (Map.Entry<String, double[]> row : tf_idf.entrySet()){
            double[] row_values = row.getValue();
            double distance = 0.0;
            for (int i = 0; i < row.getValue().length; i++)
                distance += Math.pow((row_values[i] - file_vec_for_cosine[i]), 2);
            distance = Math.sqrt(distance);
            distances.add(new Pair<>(row.getKey(),distance));
        }
        // Sort distances smallest to largest
        distances.sort(Comparator.comparing(Pair::second));
        return distances;
    }

    private LinkedHashMap<String, Integer> init_results_counter(LinkedHashMap<String, String> training_labels) {
        LinkedHashMap<String, Integer> results_counter = new LinkedHashMap<>();
        for (Map.Entry<String, String> train_file : training_labels.entrySet())
            if (!results_counter.containsKey(train_file.getValue()))
                results_counter.put(train_file.getValue(),0);

        return results_counter;
    }

    private LinkedHashMap<String, Integer> get_results_counter(LinkedHashMap<String, String> training_labels,
                                                               ArrayList<Pair<String, Double>> distances,
                                                               int k) {
        LinkedHashMap<String, Integer> results_counter = init_results_counter(training_labels);
        for (int i = 0; i < k; i++)
            if (k <= distances.size()){
                String neighbour_label = training_labels.get(distances.get(i).first);
                results_counter.put(neighbour_label, results_counter.get(neighbour_label) +1);
            }
        return results_counter;
    }

    private void print_k_neighbours(LinkedHashMap<String, String> training_labels,
                                                               ArrayList<Pair<String, Double>> distances,
                                                               int k) {
        for (int i = 0; i < k; i++)
            if (k <= distances.size()){
                String neighbour_label = training_labels.get(distances.get(i).first);
                System.out.println(distances.get(i).first +" –> "+ neighbour_label);
            }
    }
}
