package com.AlanYu.Filter;

import android.util.Log;

import weka.classifiers.Classifier;
import weka.classifiers.meta.Vote;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author AlanYu
 */
@SuppressWarnings("deprecation")
public class DecisionMaker extends Vote {

    public static final int TRAINING = 0;
    public static final int TEST = 1;
    public static final int IS_OWNER = 0;
    public static final int IS_OTHER = 1;
    public static final int TRUE_POSITIVE = 0;
    public static final int TRUE_NEGATIVE = 1;
    public static final int FALSE_POSITIVE = 2;
    public static final int FALSE_NEGATIVE = 3;
    public static final int ATTRIBUTE_SIZE = 5;

    private J48Classifier j48;
    private kNNClassifier knn;
    private KStarClassifier kstar;
    private DecisionTableFilter dt;
    private RandomForestClassifier randomF;
    private Instances dataUnLabeled;
    private FastVector fvWekaAttributes;
    private String classifierName = "Vote";
    private Instances trainingData;
    private double threshold;
    private double confidence;

    public DecisionMaker() {
        init();
    }

    /**
     * init : will set five classifiers and set each option and setFeature setting
     */
    private void init() {
        j48 = new J48Classifier();
        knn = new kNNClassifier();
        kstar = new KStarClassifier();
        dt = new DecisionTableFilter();
        randomF = new RandomForestClassifier();
        this.setOption();
        this.setFeature();
    }


    public void addDataToTraining(Instances trainingData) {
        Log.d("DecisionMaker", "set trainging data");
        j48.setTrainingData(trainingData);
        knn.setTrainingData(trainingData);
        kstar.setTrainingData(trainingData);
        dt.setTrainingData(trainingData);
        randomF.setTrainingData(trainingData);
        this.trainingData = trainingData;
    }

    ;

    public void buildClassifier() {
        Log.d("DecisionMaker", "build classifier");
        j48.trainingData();
        knn.trainingData();
        kstar.trainingData();
        dt.trainingData();
        randomF.trainingData();
        Classifier cls[] = {j48.returnClassifier(), knn.returnClassifier(),
                kstar.returnClassifier(), dt.returnClassifier(),
                randomF.returnClassifier()};
        this.setClassifiers(cls);
    }

    ;

    /**
     * get predicted lable of unlabeled instances , for decision purpose
     *
     * @param unLabelData
     * @return
     */
    public int getFinalLabel(Instances unLabelData) {
        //majority voting
        return predictionInstances(unLabelData);
        // accumulates votes
//        return predictionByAccumulatingVotes(unLabelData);
    }

    /**
     * print every classifier evaluation result  ( per touch event evaluation )
     *
     * @param labeledData
     * @throws Exception
     */
    public void evaluationEachClassifier(Instances labeledData)
            throws Exception {
        int[] result;
        for (int i = 0; i < m_Classifiers.length; i++) {
            result = getStatisticsPerClassifier(getClassifier(i), labeledData);
            printStatistics(result);
        }
    }

    /**
     * print out the evaluatoin of the Majority Voting Policy
     *
     * @param labeledData
     */
    public void evaluationWithMajorityVoting(Instances labeledData) {
        int[] result = new int[4];
        for (int i = 0; i < labeledData.numInstances(); i++) {
            try {
                int tmpLabel = instanceMajorityVoting(labeledData.instance(i));
                int trueLabel = (int) labeledData.instance(i).classValue();
                result = evaluation(trueLabel, tmpLabel, result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Majority Voting");
        printStatistics(result);
    }

    /**
     * accumulate the evaluation result
     * acculatede
     *
     * @param trueLabel
     * @param tmpLabel
     * @param result
     * @return
     */
    public int[] evaluation(int trueLabel, int tmpLabel, int[] result) {
        if (tmpLabel == DecisionMaker.IS_OWNER) {
            if (trueLabel == DecisionMaker.IS_OWNER)
                result[TRUE_POSITIVE]++;
            else
                result[FALSE_POSITIVE]++;
        } else {
            if (trueLabel == DecisionMaker.IS_OTHER)
                result[TRUE_NEGATIVE]++;
            else
                result[FALSE_NEGATIVE]++;
        }
        return result;
    }

    /**
     * print Statistics of the result array which has tp tn fp fn value , and print out the precision , recall  , F-measure , FAR , FRR  , ERR value
     *
     * @param result integer array
     */
    public void printStatistics(int[] result) {
        int truePostive = result[TRUE_POSITIVE];
        int trueNegative = result[TRUE_NEGATIVE];
        int falsePostive = result[FALSE_POSITIVE];
        int falseNegative = result[FALSE_NEGATIVE];
        System.out.println("tp :" + truePostive + " tn:" + trueNegative
                + " fp:" + falsePostive + " fn:" + falseNegative);
        double precision = (double) truePostive / (truePostive + falsePostive);
        double recall = (double) truePostive / (truePostive + falseNegative);
        double fMeasure = (double) 2 * precision * recall
                / (precision + recall);
        Double[] result2 = new Double[3];
        result2[0] = precision;
        result2[1] = recall;
        result2[2] = fMeasure;
        System.out.println("\n----------Result-------Threshold:"+this.getThreshold()+"\n");
        System.out.println("Precisoin : " + Double.toString(result2[0])
                + "\nRecall: " + Double.toString(result2[1]) + "\nF-Measure:"
                + result2[2]);
        int total = trueNegative + truePostive + falseNegative + falsePostive;
        double far = (double) falsePostive / total;
        double frr = (double) falseNegative / total;
        System.out.println("\n FAR : " + Double.toString(far) + "\n FRR :" + Double.toString(frr));
    }

    /**
     * get integer array has tp tn fp fn value with labeled data
     *
     * @param classifier
     * @param labeledData instances with label
     * @return int array[] of evaluation result which has tp tn fp fn value
     */
    private int[] getStatisticsPerClassifier(Classifier classifier,
                                             Instances labeledData) {
        int[] result = new int[4];
        int classType;
        for (int i = 0; i < labeledData.numInstances(); i++) {
            try {
                classType = (int) classifier.classifyInstance(labeledData
                        .instance(i));
                result = evaluation((int)labeledData.instance(i).classValue(), classType,result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * prediction policy voting every instance by classifier and accumulate every ticket and deciside the final label of per access
     *
     * @param unLabelData
     * @return
     */
    private int predictionByAccumulatingVotes(Instances unLabelData) {
        int[] votes = new int[2];
        for (int i = 0; i < unLabelData.numInstances(); i++) {
            for (int j = 0; j < m_Classifiers.length; j++) {
                try {
                    int classType = (int) getClassifier(j).classifyInstance(unLabelData.instance(i));
                    votes[classType]++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return thresholdPolicy(votes[DecisionMaker.IS_OWNER],votes[DecisionMaker.IS_OTHER]);
    }

    /**
     * Prediction policy of continous unlabeled data and return classType by comparation with threshold policy
     * this policy return label whether the precision is bigger than threshold or not
     *
     * @param unLabelData
     * @return
     */
    private int predictionInstances(Instances unLabelData) {
        int ownerLabelNumber = 0;
        int otherLabelNumber = 0;
        int classtype = 0;
        Log.d("in predicting label","number of unlable instances: "+unLabelData.numInstances());
        for (int i = 0; i < unLabelData.numInstances(); i++) {
            try {
                classtype = this.voteForInstance(unLabelData.instance(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (classtype == DecisionMaker.IS_OWNER)
                ownerLabelNumber++;
            else
                otherLabelNumber++;
        }

        return thresholdPolicy(ownerLabelNumber,otherLabelNumber);
//        System.out.println("Predicting Label : Number of owner votes:" + ownerLabelNumber + "Number of other votes : " + otherLabelNumber);
//        System.out.println("True label  = " + unLabelData.instance(0).classValue());
        /* with threshold policy */


        /* with winner take all policy */
//        if(ownerLabelNumber >= otherLabelNumber)
//            return IS_OWNER;
//        else
//            return IS_OTHER;
    }

    private int thresholdPolicy(int ownerLabelNumber, int otherLabelNumber) {
        double confidence  = (double) ownerLabelNumber
                / (ownerLabelNumber + otherLabelNumber);

        this.setConfidence(confidence);
        if (confidence > this.getThreshold())
            return IS_OWNER;
        else
            return IS_OTHER;

    }


    public FastVector getWekaAttributes() {
        return j48.getFvWekaAttributes();
    }

    /**
     * prediction policy of per instance by Majority Voting
     *
     * @param currentInstance
     * @return
     */
    public int voteForInstance(Instance currentInstance) {
        dataUnLabeled = new Instances("TestInstances",
                this.getFvWekaAttributes(), 10);
        dataUnLabeled.setClassIndex(dataUnLabeled.numAttributes() - 1);
        dataUnLabeled.add(currentInstance);
        currentInstance.setDataset(dataUnLabeled);
        int classType = 0;
        try {
            classType = this.instanceMajorityVoting(currentInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(dataUnLabeled.numInstances()>0)
        dataUnLabeled.remove(0);

        return classType;

    }

    protected void setOption() {
        try {
            String[] options = weka.core.Utils.splitOptions("-R"
                    + Double.toString(Vote.MAJORITY_VOTING_RULE));
            this.setOptions(options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void setFeature() {

        Log.d("Seting Feature ", "seting feature ");
        // add numeric attribute
        Attribute attribute1 = new Attribute("x");
        Attribute attribute2 = new Attribute("y");
        Attribute attribute3 = new Attribute("pressure");
        Attribute attribute4 = new Attribute("size");

        //for preprocess data
//        Attribute attribute5 = new Attribute("time");
//        Attribute attribute6 = new Attribute("velocity");
//        Attribute attribute7 = new Attribute("type");
        // nominal attribute along with its values

        // declare class attribute
        FastVector<String> fvClassVal = new FastVector<String>(2);
        fvClassVal.addElement("owner");
        fvClassVal.addElement("other");
        Attribute classAttribute = new Attribute("the class", fvClassVal);

        // Declare feature vector
        setFvWekaAttributes(new FastVector(ATTRIBUTE_SIZE));
        getFvWekaAttributes().addElement(attribute1);
        getFvWekaAttributes().addElement(attribute2);
        getFvWekaAttributes().addElement(attribute3);
        getFvWekaAttributes().addElement(attribute4);

//        getFvWekaAttributes().addElement(attribute5);
 //        getFvWekaAttributes().addElement(attribute6);
//        getFvWekaAttributes().addElement(attribute7);
//        getFvWekaAttributes().addElement(attribute5);
        getFvWekaAttributes().addElement(classAttribute);


    }

    /**
     * print result array of precision of each class
     *
     * @param prediction
     */
    public void printResult(double[] prediction) {
        System.out.println("\n Result " + this.classifierName
                + "\n =========================\n");
        for (int i = 0; i < prediction.length; i++) {
            System.out.println("Probability of class "
                    + trainingData.classAttribute().value(i) + " : "
                    + Double.toString(prediction[i]));
        }
    }

    public FastVector getFvWekaAttributes() {
        return fvWekaAttributes;
    }

    public void setFvWekaAttributes(FastVector fvWekaAttributes) {
        this.fvWekaAttributes = fvWekaAttributes;
    }

    /**
     * prediction policy of per instance using Majority Voting
     *
     * @param instance
     * @return
     * @throws Exception
     */
    public int instanceMajorityVoting(Instance instance) throws Exception {

        double[] probs = new double[instance.classAttribute().numValues()];
        double[] votes = new double[probs.length];

        // each classifier
        for (int i = 0; i < m_Classifiers.length; i++) {

            // classifie one instance and find the biggest probability label
            probs = getClassifier(i).distributionForInstance(instance);
            int maxIndex = 0;
            for (int j = 0; j < probs.length; j++) {
                if (probs[j] > probs[maxIndex])
                    maxIndex = j;
            }

            // Consider the cases when multiple classes happen to have the same
            // probability
            for (int j = 0; j < probs.length; j++) {
                if (probs[j] == probs[maxIndex])
                    votes[j]++;
            }
        }

        int tmpMajorityIndex = 0;
        for (int k = 1; k < votes.length; k++) {
            if (votes[k] > votes[tmpMajorityIndex])
                tmpMajorityIndex = k;
        }

        // Consider the cases when multiple classes receive the same amount of
        // votes
        // Vector<Integer> majorityIndexes = new Vector<Integer>();
        // for (int k = 0; k < votes.length; k++) {
        // if (votes[k] == votes[tmpMajorityIndex])
        // majorityIndexes.add(k);
        // }
        // Resolve the ties according to a uniform random distribution
        // int majorityIndex =
        // majorityIndexes.get(m_Random.nextInt(majorityIndexes.size()));

        // set probs to 0
        // for (int k = 0; k < probs.length; k++)
        // probs[k] = 0;
        // probs[tmpMajorityIndex] = 1; // the class that have been voted the
        // most
        // // receives 1

        return tmpMajorityIndex;
    }
    public double getThreshold() {
        return threshold;
    }
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    public double getConfidence() {
        return confidence;
    }
    public void setConfidence(double confidence) {this.confidence=confidence; }

}
