package com.AlanYu.Filter;

import android.util.Log;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.core.Instance;
import weka.core.Instances;

public class kNNClassifier extends AbstractFilter {

    public kNNClassifier() {
        this.setFeature();
        this.setOption();
        this.classifierName = "kNN";
        trainingData = new Instances("Rel", this.getFvWekaAttributes(), 1000);
        trainingData.setClassIndex(CLASS_INDEX_TOUCH);
    }

    @Override
    protected void setOption() {
        Log.d("set Option", "in seting option in classifier");
        try {
            String[] options = weka.core.Utils.splitOptions("-I -W 0");
            ibk = new IBk(5);
            ibk.setOptions(options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void testData() {
        Log.d("testing data", "in testing data phase .....");
        Evaluation eTest;
        try {
            eTest = new Evaluation(trainingData);
            eTest.evaluateModel(ibk, testData);
            System.out.println(eTest.toSummaryString(
                    "\n Results\n=============\n", false));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void trainingData() {
        Log.d("TrainingData", "in traininData phase.....");
        try {
            ibk.buildClassifier(trainingData);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public int predictInstance(Instance currentInstance) {
        dataUnLabeled.add(currentInstance);
        double[] prediction;
        try {
            prediction = ibk.distributionForInstance(dataUnLabeled
                    .firstInstance());
            if (prediction[DecisionMaker.IS_OWNER] > prediction[DecisionMaker.IS_OTHER])
                return DecisionMaker.IS_OWNER;
            else
                return DecisionMaker.IS_OTHER;
        } catch (Exception e) {
            e.printStackTrace();
        }
        dataUnLabeled.remove(0);
        return 0;
    }

    @Override
    public Classifier returnClassifier() {
        return ibk;
    }

}
