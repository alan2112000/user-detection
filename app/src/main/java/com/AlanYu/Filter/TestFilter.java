package com.AlanYu.Filter;

import android.database.Cursor;
import android.util.Log;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class TestFilter {


    private FastVector fvWekaAttributes;
    private Classifier cModel;
    private Instances trainingData;
    private Instances testData;
    private Classifier cls;
    private J48 tree;
    private IBk ibk;
    private Instances dataUnLabeled;

    public TestFilter() {
    }


    public FastVector getFvWekaAttributes() {
        return fvWekaAttributes;
    }

    public void setFvWekaAttributes(FastVector fvWekaAttributes) {
        setFeature();
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

    public void loadData() {

    }

	/*
     * (non-Javadoc)
	 * 
	 * @see com.AlanYu.Filter.AbstractFilter#testData()
	 */

    public float testData() {

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
        return 0;
    }

    public void setOption() {
        Log.d("set Option", "in seting option in classifier");
        try {
            String[] options = weka.core.Utils.splitOptions("-I -K 5 ");
            ibk = new IBk(5);
            ibk.setOptions(options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void trainingData() {

        Log.d("TrainingData", "in traininData phase.....");
        try {
            ibk.buildClassifier(trainingData);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void generateData() {

        Instance iExample = new DenseInstance(4);
        iExample.setValue((Attribute) fvWekaAttributes.elementAt(0), 1.0);
        iExample.setValue((Attribute) fvWekaAttributes.elementAt(1), 0.5);
        iExample.setValue((Attribute) fvWekaAttributes.elementAt(2), "gray");
        iExample.setValue((Attribute) fvWekaAttributes.elementAt(3), "positive");

        // add the instance
        testData.add(iExample);
    }

    public void setFeature() {

        Log.d("Seting Feature ", "seting feature ");

        //add numeric attribute
        Attribute attribute1 = new Attribute("x");
        Attribute attribute2 = new Attribute("y");
        Attribute attribute3 = new Attribute("pressure");
        Attribute attribute4 = new Attribute("size");

        // nominal attribute along with its values

        // declare class attribute
        FastVector fvClassVal = new FastVector(8);
        fvClassVal.addElement("owner");
        fvClassVal.addElement("eraser");
        fvClassVal.addElement("fucker");
        fvClassVal.addElement("gary");
        fvClassVal.addElement("peg");
        fvClassVal.addElement("weiling");
        fvClassVal.addElement("joanne");
        fvClassVal.addElement("mako");
        Attribute classAttribute = new Attribute("the class", fvClassVal);

        // Declare feature vector
        fvWekaAttributes = new FastVector(5);
        fvWekaAttributes.addElement(attribute1);
        fvWekaAttributes.addElement(attribute2);
        fvWekaAttributes.addElement(attribute3);
        fvWekaAttributes.addElement(attribute4);
        fvWekaAttributes.addElement(classAttribute);

        // isTrainingSet = new Instances("Rel", fvWekaAttributes, 1000);
        // isTrainingSet.setClassIndex(5);


    }

    public void setInstances(Cursor cursor) {

        if (cursor.moveToFirst())
            do {
                Instance iExample = new DenseInstance(5);
                Log.d("testFilter", "setting instance value ");
//				iExample.setValue((Attribute) getFvWekaAttributes()
//						.elementAt(0), Double.valueOf(cursor.getString(cursor.getColumnIndex("X"))));
                iExample.setValue((Attribute) getFvWekaAttributes()
                        .elementAt(1), Double.valueOf(cursor.getString(cursor.getColumnIndex("Y"))));
                iExample.setValue((Attribute) getFvWekaAttributes()
                        .elementAt(2), Double.valueOf(cursor.getString(cursor.getColumnIndex("PRESSURE"))));
                iExample.setValue((Attribute) getFvWekaAttributes()
                        .elementAt(3), Double.valueOf(cursor.getString(cursor.getColumnIndex("SIZE"))));
                iExample.setValue((Attribute) getFvWekaAttributes()
                        .elementAt(4), Double.valueOf(cursor.getString(cursor.getColumnIndex("LABEL"))));
                Log.d("testFilter ", "add to training set  ");
                trainingData.add(iExample);
                testData.add(iExample);
            } while (cursor.moveToNext());
        cursor.close();
    }

    public void predictInstance(Instance currentInstance) {
        dataUnLabeled = new Instances("TestInstances", getFvWekaAttributes(), 10);
        dataUnLabeled.add(currentInstance);
        dataUnLabeled.setClassIndex(dataUnLabeled.numAttributes() - 1);
        double[] prediction;
        try {
            prediction = ibk.distributionForInstance(dataUnLabeled.firstInstance());
            //output predictions
            System.out.println("\n Result \n ====================\n");
            for (int i = 0; i < prediction.length; i++) {
                System.out.println("Probability of class " +
                        testData.classAttribute().value(i) +
                        " : " + Double.toString(prediction[i]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
