package pt.ulisboa.tecnico.cnv.loadbalancer.utils;

import pt.ulisboa.tecnico.cnv.mss.CaptureTheFlagMetric;
import pt.ulisboa.tecnico.cnv.mss.FifteenPuzzleMetric;
import pt.ulisboa.tecnico.cnv.mss.GameOfLifeMetric;
import weka.classifiers.lazy.IBk;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.neighboursearch.KDTree;

import java.util.ArrayList;
import java.util.List;

public class KNN {

    private final IBk knn;
    private Attribute targetAttribute;
    private boolean isTrained = false;

    public KNN() {
        int k = 5;
        this.knn = new IBk(k);
        this.knn.setNearestNeighbourSearchAlgorithm(new KDTree());
        this.knn.setMeanSquared(true);
        try {
            this.knn.setOptions(new String[] { "-I" });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void train(Instances trainData) throws Exception {
        this.knn.buildClassifier(trainData);
        this.targetAttribute = trainData.classAttribute();
        this.isTrained = true;
    }

    public boolean isNotTrained() {
        return !isTrained;
    }

    public long predict(Instances data) {
        if (!isTrained) {
            throw new IllegalStateException("KNN model is not trained yet.");
        }
        if (!this.targetAttribute.equals(data.classAttribute())) {
            throw new IllegalArgumentException("Data target attribute does not match the trained target attribute.",
                    new Throwable("The model was trained to predict " + this.targetAttribute.name()
                            + ", but was asked to predict " + data.classAttribute().name()));
        }
        try {
            double prediction = this.knn.classifyInstance(data.firstInstance());
            return Double.valueOf(prediction).longValue();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static Instances buildInstancesFP(List<FifteenPuzzleMetric> metrics) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("size", false));
        attributes.add(new Attribute("shuffles", false));
        attributes.add(new Attribute("instructionCount", false));

        // Create Instances object
        Instances dataset = new Instances("FifteenPuzzleData", attributes, metrics.size());
        dataset.setClassIndex(dataset.numAttributes() - 1);

        // Populate instances
        for (FifteenPuzzleMetric metric : metrics) {
            double[] values = new double[dataset.numAttributes()];
            values[0] = metric.getSize();
            values[1] = metric.getShuffles();
            values[2] = metric.getInstructionCount();
            dataset.add(new DenseInstance(1.0, values));
        }
        return dataset;
    }

    public static Instances buildInstancesGOF(List<GameOfLifeMetric> metrics) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("mapFile", metrics.stream().map(m -> m.getMapFile()).distinct().toList()));
        attributes.add(new Attribute("iterations", false));
        attributes.add(new Attribute("instructionCount", false));

        Instances dataset = new Instances("GameOfLifeData", attributes, metrics.size());
        dataset.setClassIndex(dataset.numAttributes() - 1);

        for (GameOfLifeMetric metric : metrics) {
            double[] values = new double[dataset.numAttributes()];
            values[0] = dataset.attribute(0).addStringValue(metric.getMapFile());
            values[1] = metric.getIterations();
            values[2] = metric.getInstructionCount();

            dataset.add(new DenseInstance(1.0, values));
        }
        return dataset;
    }

    /**
     * Build dataset for Capture The Flag metrics.
     * 
     * @param metrics List of CaptureTheFlagMetric objects containing the metrics.
     * @param target  The target attribute to predict (either instruction count or
     *                basic block count).
     * @return Instances object containing the dataset ready for training or
     *         prediction.
     */
    public static Instances buildInstancesCTF(List<CaptureTheFlagMetric> metrics) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        List<String> flagPlacements = List.of("A", "B", "C");

        attributes.add(new Attribute("gridSize", false));
        attributes.add(new Attribute("blue", false));
        attributes.add(new Attribute("red", false));
        attributes.add(new Attribute("flagPlacement", flagPlacements));
        attributes.add(new Attribute("instructionCount"));

        Instances dataset = new Instances("CaptureTheFlagData", attributes, metrics.size());
        dataset.setClassIndex(dataset.numAttributes() - 1);

        for (CaptureTheFlagMetric metric : metrics) {
            double[] values = new double[dataset.numAttributes()];
            values[0] = metric.getGridSize();
            values[1] = metric.getBlue();
            values[2] = metric.getRed();
            values[3] = flagPlacements.indexOf(Character.toString(metric.getFlagPlacement()));
            values[4] = metric.getInstructionCount();

            dataset.add(new DenseInstance(1.0, values));
        }
        return dataset;
    }

}
