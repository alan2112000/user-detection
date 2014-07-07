package com.AlanYu.Filter;

import android.database.Cursor;
import android.util.Log;

import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.KStar;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public abstract class AbstractFilter {

    protected FastVector fvWekaAttributes;
    protected Classifier cModel;
    protected Instances trainingData;
    protected Instances testData;
    protected Instances dataUnLabeled;
    protected J48 tree;
    protected IBk ibk;
    protected KStar kstar;
    protected DecisionTable dt;
    protected RandomForest randomF;
    public final static int CLASS_INDEX_TOUCH = 4;
    public final static int CLASS_INDEX_AC = 3;
    public final static int ATTRIBUTE_SIZE = 5;
    protected String classifierName = null;

    protected abstract void setOption();

    public abstract void testData();

    public abstract void trainingData();

    public abstract int predictInstance(Instance currentInstance);

    public abstract Classifier returnClassifier();

    public FastVector getFvWekaAttributes() {
        return fvWekaAttributes;
    }

    protected void setFvWekaAttributes(FastVector fvWekaAttributes) {
        this.fvWekaAttributes = fvWekaAttributes;
    }

    public Instances getTrainingData() {
        return trainingData;
    }

    public void setTrainingData(Instances trainingData) {
        this.trainingData = trainingData;
    }

    public Instances getTestData() {
        return testData;
    }

    public void setTestData(Instances testData) {
        this.testData = testData;
    }

    public Instances getDataUnLabeled() {
        return dataUnLabeled;
    }

    public void setDataUnLabeled(Instances dataUnLabeled) {
        this.dataUnLabeled = dataUnLabeled;
    }

    protected void setFeature() {

        Log.d("Seting Feature ", "seting feature ");
        // add numeric attribute
        Attribute attribute1 = new Attribute("x");
        Attribute attribute2 = new Attribute("y");
        Attribute attribute3 = new Attribute("pressure");
        Attribute attribute4 = new Attribute("size");


        //for preprocess data type , remember to modify the CLASS_INDEX AND CLASS_ATTRIBUTES
//        Attribute attribute5 = new Attribute("time");
//        Attribute attribute6 = new Attribute("velocity");
//        Attribute attribute7 = new Attribute("type");
        // nominal attribute along with its values

        // declare class attribute
        FastVector fvClassVal = new FastVector(2);
        fvClassVal.addElement("owner");
        fvClassVal.addElement("other");
        Attribute classAttribute = new Attribute("the class", fvClassVal);

        // Declare feature vector
        fvWekaAttributes = new FastVector(ATTRIBUTE_SIZE);
        fvWekaAttributes.addElement(attribute1);
        fvWekaAttributes.addElement(attribute2);
        fvWekaAttributes.addElement(attribute3);
        fvWekaAttributes.addElement(attribute4);
//        fvWekaAttributes.addElement(attribute5);
//        fvWekaAttributes.addElement(attribute6);
//        fvWekaAttributes.addElement(attribute7);
        fvWekaAttributes.addElement(classAttribute);

        dataUnLabeled = new Instances("TestInstances", getFvWekaAttributes(),
                10);
        dataUnLabeled.setClassIndex(dataUnLabeled.numAttributes() - 1);
    }

    protected void printResult(double[] prediction) {
        System.out.println("\n Result " + this.classifierName + "\n =========================\n");
        for (int i = 0; i < prediction.length; i++) {
            System.out.println("Probability of class "
                    + trainingData.classAttribute().value(i) + " : "
                    + Double.toString(prediction[i]));
        }
    }

    protected void setInstances(Cursor cursor) {

        if (cursor.moveToFirst())
            do {
                Instance iExample = new DenseInstance(ATTRIBUTE_SIZE);
                Log.d("abstractFilter", "setting instance value ");
                // iExample.setValue((Attribute) getFvWekaAttributes()
                // .elementAt(0),
                // Double.valueOf(cursor.getString(cursor.getColumnIndex("X"))));
                iExample.setValue((Attribute) getFvWekaAttributes()
                        .elementAt(1), Double.valueOf(cursor.getString(cursor
                        .getColumnIndex("Y"))));
                iExample.setValue((Attribute) getFvWekaAttributes()
                        .elementAt(2), Double.valueOf(cursor.getString(cursor
                        .getColumnIndex("PRESSURE"))));
                iExample.setValue((Attribute) getFvWekaAttributes()
                        .elementAt(3), Double.valueOf(cursor.getString(cursor
                        .getColumnIndex("SIZE"))));
                iExample.setValue((Attribute) getFvWekaAttributes()
                        .elementAt(4), Double.valueOf(cursor.getString(cursor
                        .getColumnIndex("LABEL"))));
                Log.d("abstract Filter  ", "add to training set  ");
                trainingData.add(iExample);
                testData.add(iExample);
            } while (cursor.moveToNext());
        cursor.close();
    }

    public void addInstanceToTrainingData(Instance instance) {
        trainingData.add(instance);
        this.predictInstance(instance);
    }

    public void addInstanceToTestData(Instance instance) {
        testData.add(instance);
    }
}
