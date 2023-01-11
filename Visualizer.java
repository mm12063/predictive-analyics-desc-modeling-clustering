import java.awt.Color;
import java.util.ArrayList;
import edu.stanford.nlp.util.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

import javax.swing.*;

public class Visualizer extends ApplicationFrame {

    private static final long serialVersionUID = 1L;

    static {
        ChartFactory.setChartTheme(new StandardChartTheme("JFree/Shadow",
                true));
    }

    public Visualizer(String title) {
        super(title);
        this.setSize(1000, 500);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void createPlot(XYDataset dataset, String title, String x_axis_label, String y_axis_label){
        JFreeChart chart = ChartFactory.createScatterPlot(
                title,
                x_axis_label,
                y_axis_label,
                dataset
        );

        // Changes background color
        XYPlot plot = (XYPlot)chart.getPlot();
        plot.setBackgroundPaint(new Color(255,228,196));

        // Create Panel
        ChartPanel panel = new ChartPanel(chart);
        setContentPane(panel);
    }

    public XYDataset create_basic_dataset(ArrayList<ArrayList<Pair<Double, Double>>> xy_data, String data_title) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (ArrayList<Pair<Double, Double>> xy_datum : xy_data) {
            XYSeries cluster_points = new XYSeries(data_title);
            for (Pair<Double, Double> xy : xy_datum)
                cluster_points.add(xy.first, xy.second);
            dataset.addSeries(cluster_points);
        }
        return dataset;
    }

    public XYDataset create_cluster_dataset(ArrayList<ArrayList<Pair<Double, Double>>> xy_data, ArrayList<String> dir_names) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        int idx = 0;
        for (ArrayList<Pair<Double, Double>> xy_datum : xy_data) {
            String dir = dir_names.get(idx);
            XYSeries cluster_points = new XYSeries("Dir " + dir + " Cluster");
            for (Pair<Double, Double> xy : xy_datum)
                cluster_points.add(xy.first, xy.second);
            dataset.addSeries(cluster_points);
            idx++;
        }
        return dataset;
    }
}