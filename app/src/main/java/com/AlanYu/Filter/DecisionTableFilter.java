package com.AlanYu.Filter;

import android.util.Log;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.rules.DecisionTable;
import weka.core.Instance;
import weka.core.Instances;

public class DecisionTableFilter extends AbstractFilter {

    public DecisionTableFilter() {
        this.setFeature();
        this.setOption();
        this.classifierName = "Decision Table";
        trainingData = new Instances("Rel", this.getFvWekaAttributes(), 1000);
        trainingData.setClassIndex(CLASS_INDEX_TOUCH);
    }

    @Override
    protected void setOption() {
        Log.d("set Option", "in seting option in classifier");
        String[] options = null;
        try {
            dt = new DecisionTable();
            options = weka.core.Utils.splitOptions("-X 1");
            dt.setOptions(options);
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
            // eTest.evaluateModel(dt, testData);
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
            dt.buildClassifier(trainingData);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public int predictInstance(Instance currentInstance) {
        dataUnLabeled.add(currentInstance);
        double[] prediction;
        try {
            prediction = dt.distributionForInstance(dataUnLabeled
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
        return dt;
    }

}
