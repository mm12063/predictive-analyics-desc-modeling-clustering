import Jama.SingularValueDecomposition;
import Jama.Matrix;
import edu.stanford.nlp.util.Pair;
import java.util.*;

public class DimensionalityReducer {

    public Matrix reduce_with_svd(LinkedHashMap<String, double[]> matrix_tf_idf, int num_dims){

        int m = matrix_tf_idf.size();
        int n = matrix_tf_idf.entrySet().iterator().next().getValue().length;
        double[][] vals = new double[m][n];
        int counter = 0;
        for (Map.Entry<String, double[]> row : matrix_tf_idf.entrySet()) {
            System.arraycopy(row.getValue(), 0, vals[counter], 0, row.getValue().length);
            counter++;
        }

        Matrix A = new Matrix(vals);
        A = A.transpose(); // Because JAMA doesn't like wide matrices...
        SingularValueDecomposition svd = new SingularValueDecomposition(A);

        return reduce_matrix_to_k_dims(A, svd, num_dims);
    }

    public Matrix reduce_matrix_to_k_dims(Matrix matrix, SingularValueDecomposition svd, int k){
        int n = matrix.getColumnDimension();
        Matrix Stk = get_matrix_subset(svd.getS().transpose(),k,k);
        Matrix Vk = get_matrix_subset(svd.getV(),n,k);
        return Vk.times(Stk);
    }

    private Matrix get_matrix_subset(Matrix orig_matrix, int m, int k){
        Matrix new_matrix = new Matrix(m,k);
        for (int i = 0; i < m; i++)
            for (int j = 0; j < k; j++)
                new_matrix.set(i,j,orig_matrix.get(i,j));
        return new_matrix;
    }

    private LinkedHashMap<String, Pair<Double,Double>> marry_xy_with_filenames(LinkedHashMap<String, double[]> matrix,
                                                                               Matrix xy){
        LinkedHashMap<String, Pair<Double,Double>> xy_with_filenames = new LinkedHashMap<>();
        int counter = 0;
        for (Map.Entry<String, double[]> row : matrix.entrySet()) {
            xy_with_filenames.put(row.getKey(), new Pair<>(xy.get(counter,0), xy.get(counter,1)));
            counter++;
        }
        return xy_with_filenames;
    }

    public ArrayList<ArrayList<Pair<Double, Double>>> get_matrix_as_xy_data(LinkedHashMap<String, double[]> matrix) {
        int num_of_dims = 2;
        Matrix xy = reduce_with_svd(matrix, num_of_dims);
        ArrayList<ArrayList<Pair<Double, Double>>> xy_data = new ArrayList<>();
        xy_data.add(new ArrayList<>());
        for (int i = 0; i < xy.getRowDimension(); i++)
            xy_data.get(0).add(new Pair<>(xy.get(i,0),xy.get(i,1)));

        return xy_data;
    }

    public ArrayList<ArrayList<Pair<Double, Double>>> get_clusters_as_xy_data(
            ArrayList<ArrayList<String>> clusters, LinkedHashMap<String, double[]> matrix) {

        int num_of_dims = 2;
        Matrix xy = reduce_with_svd(matrix, num_of_dims);
        LinkedHashMap<String, Pair<Double,Double>> xy_with_filenames = marry_xy_with_filenames(matrix, xy);

        ArrayList<ArrayList<Pair<Double, Double>>> clusters_as_xy_data = new ArrayList<>();
        int counter = 0;
        for (ArrayList<String> cluster: clusters){
            clusters_as_xy_data.add(new ArrayList<>());
            for (String cluster_point: cluster)
                clusters_as_xy_data.get(counter).add(xy_with_filenames.get(cluster_point));
            counter++;
        }
        return clusters_as_xy_data;
    }
}
