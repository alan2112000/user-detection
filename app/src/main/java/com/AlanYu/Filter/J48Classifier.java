package com.AlanYu.Filter;

import android.util.Log;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

public class J48Classifier extends AbstractFilter {

    public J48Classifier() {
        this.setFeature();
        this.setOption();
        this.classifierName = "j48";
        trainingData = new Instances("Rel", this.getFvWekaAttributes(), 1000);
        trainingData.setClassIndex(CLASS_INDEX_TOUCH);
    }

    @Override
    public void setOption() {
        Log.d("set Option", "in seting option in classifier");
        String[] options = null;
        tree = new J48();
        try {
            options = weka.core.Utils.splitOptions("-C 0.25 -M 2");
            tree.setOptions(options);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void testData() {
        Log.d("testing data", "in testing data phase .....");
        Evaluation eTest;
        try {
            eTest = new Evaluation(trainingData);
            eTest.evaluateModel(tree, testData);
            System.out.println(eTest.toSummaryString(
                    "\n Results\n=============\n", false));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void trainingData() {
        Log.d("TrainingData", "in J48 traininData phase....No of instances :");
        try {
            tree.buildClassifier(trainingData);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public int predictInstance(Instance currentInstance) {
        dataUnLabeled.add(currentInstance);
        double[] prediction;
        try {
            prediction = tree.distributionForInstance(dataUnLabeled
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
        return tree;
    }

}
