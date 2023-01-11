import edu.stanford.nlp.util.Pair;

import java.text.DecimalFormat;
import java.util.*;

public class DocTermMatrixCreator {

    public LinkedHashMap<String, double[]> create_matrix(ArrayList<String> filenames, ArrayList<String> terms_list){
        LinkedHashMap<String, double[]> matrix = new LinkedHashMap<>();
        for (String filename: filenames)
            matrix.put(filename,new double[terms_list.size()]);
        return matrix;
    }

    public int[] get_term_frequencies_across_corpus(ArrayList<String> all_terms, LinkedHashMap<String, String> cleaned_corpus_text) {
        int[] appearances = new int[all_terms.size()];
        for (int i = 0; i < all_terms.size(); i++) {
            int counter = 0;
            for (Map.Entry<String, String> file_data : cleaned_corpus_text.entrySet()) {
                String term_to_check = all_terms.get(i).replace("_", " ").toLowerCase();
                String text = cleaned_corpus_text.get(file_data.getKey()).toLowerCase();
                int times_in_file = (text.length() - text.replace(term_to_check, "")
                        .length()) / term_to_check.length();
                if (times_in_file > 0)
                    counter++;
            }
            appearances[i] = counter;
        }
        return appearances;
    }

    public static double[] get_term_freq_per_file(ArrayList<String> all_terms, String text) {
        double[] appearances = new double[all_terms.size()];
        for (int i = 0; i < all_terms.size(); i++) {
            String term_to_check = all_terms.get(i).replace("_", " ").toLowerCase();
            text = text.toLowerCase();
            int times_in_file = (text.length() - text.replace(term_to_check, "")
                    .length()) / term_to_check.length();
            if (times_in_file > 0)
                appearances[i] = times_in_file;
        }
        return appearances;
    }

    public void print_matrix(LinkedHashMap<String, double[]> matrix){
        for (Map.Entry<String, double[]> row : matrix.entrySet())
            System.out.println(row.getKey() +" "+ Arrays.toString(row.getValue()));
    }

    public LinkedHashMap<String, double[]> populate_matrix(LinkedHashMap<String, double[]> matrix, ArrayList<String> all_terms, LinkedHashMap<String, String> cleaned_corpus_text){
        for (Map.Entry<String, double[]> row : matrix.entrySet()) {
            for (int i = 0; i < all_terms.size(); i++) {
                String term_to_check = all_terms.get(i).replace("_"," ").toLowerCase();
                String text = cleaned_corpus_text.get(row.getKey()).toLowerCase();
                row.getValue()[i] = (double)(text.length() - text.replace(term_to_check, "").length()) / term_to_check.length();
            }
        }
        return matrix;
    }

    public LinkedHashMap<String, double[]> convert_matrix_to_tf_idf(LinkedHashMap<String, double[]> matrix, ArrayList<String> all_terms, LinkedHashMap<String, String> cleaned_corpus_text){
        int corpus_size = cleaned_corpus_text.size();
        double term_freq, inverse_doc_freq;

        int[] appearances = this.get_term_frequencies_across_corpus(all_terms, cleaned_corpus_text);

        // Init matrix
        LinkedHashMap<String, double[]> tf_idf_matrix = new LinkedHashMap<>();
        for (Map.Entry<String, double[]> entry : matrix.entrySet())
            tf_idf_matrix.put(entry.getKey(),new double[all_terms.size()]);

        for (int i = 0; i < all_terms.size(); i++)
            for (Map.Entry<String, double[]> row : matrix.entrySet()) {
                String text = cleaned_corpus_text.get(row.getKey());
                int doc_size = text.split(" ").length;

                double appearances_in_doc = row.getValue()[i];
                if (appearances_in_doc > 0) {
                    term_freq = appearances_in_doc / doc_size;
                    inverse_doc_freq = Math.log10((double) corpus_size / appearances[i] + 1);
                    double tf_idf = term_freq * inverse_doc_freq;
                    tf_idf_matrix.get(row.getKey())[i] = tf_idf;
                }
            }

        return tf_idf_matrix;
    }

    public ArrayList<Pair<Double, Integer>> generate_dir_keywords(String dir_name, LinkedHashMap<String, double[]> matrix) {
        ArrayList<Pair<Double, Integer>> all_keywords = new ArrayList<>();
        ArrayList<Integer> added = new ArrayList<>();
        for (Map.Entry<String, double[]> row : matrix.entrySet())
            if (row.getKey().split("__")[0].equals(dir_name))
                for (int i = 0; i < row.getValue().length; i++)
                    if (row.getValue()[i] > 0 && !added.contains(i)) {
                        all_keywords.add(new Pair<>(row.getValue()[i], i));
                        added.add(i);
                    }

        all_keywords.sort(Comparator.comparing(p -> -p.first()));
        return all_keywords;
    }

    public String create_keyword_str(ArrayList<Pair<Double, Integer>> all_keywords, int num_keywords, ArrayList<String> all_terms) {
        DecimalFormat df = new DecimalFormat("###.####");
        int counter = 0;
        StringBuilder str = new StringBuilder();
        for (Pair<Double,Integer> values: all_keywords){
            counter++;
            str.append(all_terms.get(values.second)).append(" – ").append(df.format(values.first)).append("\n");
            if (counter >= num_keywords)
                break;
        }
        return str.toString();
    }

    public String get_matrix_as_csv(ArrayList<String> all_terms, LinkedHashMap<String, double[]> matrix){
        StringBuilder csv = new StringBuilder();

        csv.append(" ").append(",");
        for (String term: all_terms)
            csv.append(term).append(",");

        csv.append("\n");

        for (Map.Entry<String, double[]> row : matrix.entrySet()){
            csv.append(row.getKey()).append(",");
            for (int i = 0; i < row.getValue().length; i++) {
                csv.append(row.getValue()[i]).append(",");
            }
            csv.append("\n");
        }
        csv.append("\n");

        return csv.toString();
    }

    public static LinkedHashMap<String, String> get_training_labels_from_csv(List<String> csv_data){
        LinkedHashMap<String, String> training_labels = new LinkedHashMap<>();

        for (int i = 1; i < csv_data.size(); i++) {
            String[] row_parts = csv_data.get(i).split(",");
            if (row_parts[0].length()>0)
                training_labels.put(row_parts[0],row_parts[row_parts.length-1]);
        }
        return training_labels;
    }

    public static LinkedHashMap<String, double[]> get_matrix_from_csv(List<String> csv_data){
        LinkedHashMap<String, double[]> matrix = new LinkedHashMap<>();
        for (int i = 1; i < csv_data.size(); i++) {
            String[] row_parts = csv_data.get(i).split(",");
            if (row_parts[0].length()>0) {
                double[] temp = new double[row_parts.length-2];
                int idx = 0;
                for (int j = 1; j < row_parts.length-1; j++) {
                    temp[idx] = Double.parseDouble(row_parts[j]);
                    idx++;
                }
                matrix.put(row_parts[0],temp);
            }
        }
        return matrix;
    }

    public static ArrayList<String> get_terms_from_csv(List<String> csv_data){
        String[] first_row_parts = csv_data.get(0).split(",");
        return new ArrayList<>(Arrays.asList(first_row_parts).subList(1, first_row_parts.length - 1));
    }

    private static String get_cluster_label(LinkedHashMap<String, Integer> cluster_dir_labels, int idx) {
        for (Map.Entry<String, Integer> cluster : cluster_dir_labels.entrySet()){
            if (cluster.getValue() == idx)
                return cluster.getKey();
        }
        return "Error";
    }

    public static void add_cluster_labels_to_tf_idf_csv(LinkedHashMap<String, Integer> cluster_dir_labels,
                                                        ArrayList<ArrayList<String>> clusters){
        String tf_idf_filename = "tf_idf.csv";
        List<String> csv_data = FileManager.get_csv_data(tf_idf_filename);

        for (int i = 1; i < csv_data.size(); i++) {
            String[] row_parts = csv_data.get(i).split(",");
            String filename = row_parts[0];
            String label = "";
            for (int j = 0; j < clusters.size(); j++)
                if (clusters.get(j).contains(filename))
                    label = get_cluster_label(cluster_dir_labels, j);
            csv_data.set(i, csv_data.get(i)+label+"\n");
        }

        StringBuilder csv_content = new StringBuilder();
        csv_data.set(0, csv_data.get(0)+"LABEL"+"\n");
        for (String row: csv_data)
            csv_content.append(row);

        FileManager.create_file(tf_idf_filename);
        FileManager.write_to_file(tf_idf_filename, csv_content.toString());
    }
}
