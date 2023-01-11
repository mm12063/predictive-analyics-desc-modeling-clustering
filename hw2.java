import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.util.Pair;
import org.jfree.data.xy.XYDataset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class hw2 {

    public static void main(String[] args){
        boolean KMPP = false;
        boolean USE_COSINE = false;

        for (String arg: args)
            if (arg.equals("kmpp"))
                KMPP = true;
            else if (arg.equals("cosine"))
                USE_COSINE = true;
            else
                System.out.println("Unknown argument: "+arg+" <–ignored");

        // Get the docs data
        String main_dir_path = "data/";
        ArrayList<String> file_locations = DirProcessor.getFileLocations(main_dir_path);
        if (file_locations.size() == 0) {
            System.out.println("Can't access files.");
            System.exit(0);
        }

        //Pre-process the documents
        TextPreProcessor text_pre_processor = new TextPreProcessor();
        ArrayList<String> filenames = text_pre_processor.get_filenames(file_locations);
        LinkedHashMap<String, String> corpus_data = text_pre_processor.get_corpus_data(file_locations,filenames);

        // Directory names
        DirProcessor.set_dir_names(corpus_data);
        ArrayList<String> dir_names = DirProcessor.dir_names;

        // Process the corpus data
        String full_text_str = text_pre_processor.get_corpus_as_str(corpus_data);
        CoreDocument doc = text_pre_processor.get_pipeline_results(full_text_str);

        // Entities
        ArrayList<String> entities = text_pre_processor.get_entities_list(doc);
        entities = text_pre_processor.remove_sub_strings(entities);
        entities = text_pre_processor.clean_add_underscores(entities);

        // Lemmas
        ArrayList<String> lemmas = text_pre_processor.get_lemmas_list(doc);
        String lemmas_as_str = text_pre_processor.get_list_as_string(lemmas);
        lemmas_as_str = TextPreProcessor.remove_stopwords(lemmas_as_str);
        lemmas_as_str = lemmas_as_str.replaceAll("\\.","").replaceAll("\\?","");
        lemmas = Stream.of(lemmas_as_str.split(" ")).collect(Collectors.toCollection(ArrayList<String>::new));

        // N Grams
        ArrayList<String> n_grams = TextPreProcessor.get_n_grams_map(lemmas, full_text_str);
        n_grams.removeAll(List.of(""));

        // Create all terms list
        ArrayList<String> all_terms = new ArrayList<>();
        all_terms.addAll(n_grams);
        ArrayList<String> entities_ngrams_corss_ref = text_pre_processor.cross_ref_lists(entities,n_grams);
        all_terms = text_pre_processor.check_for_dupes_and_add(all_terms, entities_ngrams_corss_ref);

        // Refine final list of terms
        int min_appearances = 4;
        ArrayList<String> final_terms_list = text_pre_processor.refine_final_terms_list(all_terms, corpus_data, min_appearances);

        // Create empty matrix
        DocTermMatrixCreator doc_term_matrix_creator = new DocTermMatrixCreator();
        LinkedHashMap<String, double[]> matrix = doc_term_matrix_creator.create_matrix(filenames,final_terms_list);

        // Populate matrix
        LinkedHashMap<String, double[]> matrix_pop = doc_term_matrix_creator.populate_matrix(matrix, final_terms_list, corpus_data);
        System.out.println("Populated matrix");
        System.out.println(final_terms_list);
        doc_term_matrix_creator.print_matrix(matrix_pop);
        System.out.println();

        // Convert to TF IDF
        LinkedHashMap<String, double[]> matrix_tf_idf = doc_term_matrix_creator.convert_matrix_to_tf_idf(matrix_pop,final_terms_list,corpus_data);
        System.out.println("TF IDF Matrix");
        System.out.println(final_terms_list);
        doc_term_matrix_creator.print_matrix(matrix_tf_idf);

        // Create ./topics.txt file and populate with keywords
        int num_keywords_per_dir = 10;
        FileManager.create_keywords_file(dir_names, num_keywords_per_dir, matrix_tf_idf, final_terms_list);

        // Cluster
        int k = 3;
        SimilarityCalculator similarity_calculator = new SimilarityCalculator(KMPP,USE_COSINE);
        ArrayList<ArrayList<String>> clusters = similarity_calculator.calculate_k_means(matrix_tf_idf, k);

        // Evaluate
        Evaluator eval = new Evaluator();
        eval.set_cluster_dir_name(clusters);
        int[][] confusion_matrix = eval.create_confusion_matrix(clusters);
        eval.print_confusion_matrix(confusion_matrix);
        eval.print_metrics(confusion_matrix);

        // Export TF IDF matrix as CSV
        FileManager.tf_idf_to_csv(matrix_tf_idf, final_terms_list);
        DocTermMatrixCreator.add_cluster_labels_to_tf_idf_csv(eval.cluster_dir_name, clusters);

        // Dim Reduction
        DimensionalityReducer dim_reducer = new DimensionalityReducer();
        ArrayList<ArrayList<Pair<Double, Double>>> xy_data = dim_reducer.get_matrix_as_xy_data(matrix_tf_idf);
        ArrayList<ArrayList<Pair<Double, Double>>> xy_cluster_data = dim_reducer.get_clusters_as_xy_data(clusters, matrix_tf_idf);

        // Visualize pre-clustered
        Visualizer visualizer = new Visualizer("Pre-clustered data");
        String plot_title = "Directories' file data points";
        String x_axis_title = "X Axis";
        String y_axis_title = "Y Axis";
        XYDataset dataset = visualizer.create_basic_dataset(xy_data, "File data points");
        visualizer.createPlot(dataset, plot_title, x_axis_title, y_axis_title);
        visualizer.setVisible(true);

        // Visualize clustered data
        visualizer = new Visualizer("K-Means Clusters");
        plot_title = "Directories' file data clustered";
        dataset = visualizer.create_cluster_dataset(xy_cluster_data, dir_names);
        visualizer.createPlot(dataset, plot_title, x_axis_title, y_axis_title);
        visualizer.setVisible(true);
    }
}