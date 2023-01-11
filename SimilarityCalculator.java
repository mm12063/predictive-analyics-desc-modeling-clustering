import java.util.*;

public class SimilarityCalculator {
    boolean KMPP;
    boolean USE_COSINE;

    public SimilarityCalculator(boolean KMPP, boolean USE_COSINE){
        this.KMPP = KMPP;
        this.USE_COSINE = USE_COSINE;
    }

    public ArrayList<ArrayList<String>> calculate_k_means(LinkedHashMap<String, double[]>  matrix_tf_idf, int k) {
        // Normalise to unit vectors
        if (this.USE_COSINE) {
            for (Map.Entry<String, double[]> row : matrix_tf_idf.entrySet()) {
                double total = 0;
                for (int i = 0; i < row.getValue().length; i++) {
                    total += Math.pow(row.getValue()[i], 2);
                }
                total = Math.sqrt(total);
                for (int i = 0; i < row.getValue().length; i++) {
                    row.getValue()[i] = row.getValue()[i] / total;
                }
            }
        }

        // Get initial centroid keys
        ArrayList<String> centroid_keys = get_init_centroid_keys(this.KMPP, matrix_tf_idf, k);

        // Initialise the clusters array
        ArrayList<ArrayList<String>> clusters = new ArrayList<>(new ArrayList<>());
        for (int i = 0; i < centroid_keys.size(); i++) {
            clusters.add(new ArrayList<>());
            clusters.get(i).add(centroid_keys.get(i));
        }

        // Create the initial centroids
        ArrayList<ArrayList<Double>> centroids = new ArrayList<>();
        for (String key: centroid_keys) {
            ArrayList<Double> temp = new ArrayList<>();
            for (int i = 0; i < matrix_tf_idf.get(key).length; i++) {
                temp.add(matrix_tf_idf.get(key)[i]);
            }
            centroids.add(temp);
        }

        // Main loop
        ArrayList<Double> distances = new ArrayList<>();
        ArrayList<ArrayList<String>> clusters_prev = new ArrayList<>(new ArrayList<>());
        int unchanged = 0;
        while(unchanged <= 2) {

            // Duplicate the cluster arraylist for comparison later
            clusters_prev.clear();
            for (int i = 0; i < clusters.size(); i++) {
                clusters_prev.add(new ArrayList<>());
                for (String c: clusters.get(i))
                    clusters_prev.get(i).add(c);
            }

            for (Map.Entry<String, double[]> row : matrix_tf_idf.entrySet()) {
                if (!centroid_keys.contains(row.getKey())) {

                    // Calculate the distances
                    distances.clear();
                    for (ArrayList<Double> centroid : centroids) {
                        double distance = 0;
                        for (int i = 0; i < row.getValue().length; i++) {
                            distance += Math.pow((row.getValue()[i] - centroid.get(i)), 2);
                        }
                        distance = Math.sqrt(distance);
                        distances.add(distance);
                    }

                    // Compare distances and assign to a cluster
                    Double smallest = distances.get(0);
                    int smallest_idx = 0;
                    for (int i = 0; i < distances.size(); i++) {
                        if (distances.get(i) < smallest) {
                            smallest = distances.get(i);
                            smallest_idx = i;
                        }
                    }

                    ArrayList<Integer> centroids_to_update = new ArrayList<>();
                    centroids_to_update.add(smallest_idx);
                    if (!clusters.get(smallest_idx).contains(row.getKey())) {
                        clusters.get(smallest_idx).add(row.getKey());
                        for (int l = 0; l < clusters.size(); l++) {
                            if (l != smallest_idx & clusters.get(l).contains(row.getKey())) {
                                clusters.get(l).remove(String.valueOf(row.getKey()));
                                centroids_to_update.add(l);
                            }
                        }
                    }

                    // Recalculate centroids...
                    for (int centroid_id : centroids_to_update) {
                        for (int i = 0; i < row.getValue().length; i++) {
                            double total = 0.0;
                            for (int j = 0; j < clusters.get(centroid_id).size(); j++) {
                                String key = clusters.get(centroid_id).get(j);
                                total += matrix_tf_idf.get(key)[i];
                            }
                            total = total / clusters.get(centroid_id).size();
                            centroids.get(centroid_id).set(i, total);
                        }
                    }
                }
            }
            centroid_keys.clear();
            if (clusters_prev.equals(clusters))
                unchanged++;
            else
                unchanged = 0;
        }
        return clusters;
    }

    private ArrayList<String> get_init_centroid_keys(boolean KMPP, LinkedHashMap<String, double[]>  matrix_tf_idf, int k){
        if (KMPP)
            return get_kmpp_centroid_keys(matrix_tf_idf, k);
        else
            return get_k_centroid_keys(matrix_tf_idf, k);
    }

    public ArrayList<String> get_k_centroid_keys(LinkedHashMap<String, double[]>  matrix_tf_idf, int k){
        ArrayList<Integer> rand_nums = get_random_nums(matrix_tf_idf,k);
        ArrayList<String> keys = new ArrayList<>();

        int counter = 0;
        for (Map.Entry<String, double[]> row : matrix_tf_idf.entrySet()) {
            if (rand_nums.contains(counter))
                keys.add(row.getKey());
            counter++;
        }
        return keys;
    }

    public ArrayList<String> get_kmpp_centroid_keys(LinkedHashMap<String, double[]>  matrix_tf_idf, int k) {

        // Randomly get first centroid
        Random rand = new Random();
        int max = matrix_tf_idf.size() - 1;
        int next = rand.nextInt(max) + 1;
        String init_centroid_key = (String) matrix_tf_idf.keySet().toArray()[next];

        ArrayList<String> centroids = new ArrayList<>();
        centroids.add(init_centroid_key);

        // Calc the distances from the init centroid
        while(centroids.size() < k) {
            LinkedHashMap<String, LinkedHashMap<String, Double>> centroid_distances = new LinkedHashMap<>();

            for (Map.Entry<String, double[]> row : matrix_tf_idf.entrySet()) {
                LinkedHashMap<String, Double> centroid_distances_from_current_point = new LinkedHashMap<>();
                for (String centroid: centroids){
                    double distance = 0.0;
                    for (int i = 0; i < row.getValue().length; i++) {
                        distance += Math.pow((row.getValue()[i] - matrix_tf_idf.get(centroid)[i]), 2);
                    }
                    distance = Math.sqrt(distance);
                    centroid_distances_from_current_point.put(centroid, distance);
                }
                centroid_distances.put(row.getKey(), centroid_distances_from_current_point);
            }

            // Find the min dist for each point
            LinkedHashMap<String, Double> distances = new LinkedHashMap<>();
            for (Map.Entry<String, LinkedHashMap<String, Double>> cent_for_pointx : centroid_distances.entrySet()) {
                String point_name = cent_for_pointx.getKey();
                double min_dist = cent_for_pointx.getValue().entrySet().iterator().next().getValue();
                for (Map.Entry<String, Double> cent_dist : cent_for_pointx.getValue().entrySet())
                    if (cent_dist.getValue() < min_dist)
                        min_dist = cent_dist.getValue();
                distances.put(point_name, min_dist);
            }

            // Get next centroid based on distance probabilities
            Double summed_distances = 0.0;
            LinkedHashMap<String, Double> distances_as_probs = new LinkedHashMap<>();
            for (Map.Entry<String, Double> distance : distances.entrySet())
                summed_distances += distance.getValue();

            for (Map.Entry<String, Double> row : distances.entrySet())
                distances_as_probs.put(row.getKey(), row.getValue() / summed_distances);

            double rand_val = Math.random();
            double cumaltive_prob = 0.0;
            String next_centroid = "";
            for (Map.Entry<String, Double> row : distances_as_probs.entrySet()) {
                cumaltive_prob += row.getValue();
                if (rand_val < cumaltive_prob) {
                    next_centroid = row.getKey();
                    break;
                }
            }
            centroids.add(next_centroid);
        }
        return centroids;
    }

    public ArrayList<Integer> get_random_nums(LinkedHashMap<String, double[]>  matrix_tf_idf, int k){
        Random rand = new Random();
        int max = matrix_tf_idf.size()-1;
        ArrayList<Integer> rand_values = new ArrayList<>();

        if (max < k)
            throw new IllegalArgumentException("K needs to be less than the dataset size.");

        while (rand_values.size() < k) {
            Integer next = rand.nextInt(max) + 1;
            if (!rand_values.contains(next))
                rand_values.add(next);
        }
        return rand_values;
    }
}
