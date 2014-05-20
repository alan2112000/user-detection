package com.AlanYu.Filter;

import android.util.Log;

import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class J48ClassiferForAC extends AbstractFilter {

    public J48ClassiferForAC() {
        this.setOption();
        this.setFeature();
        trainingData = new Instances("Rel", this.getFvWekaAttributes(), 3000);
        trainingData.setClassIndex(CLASS_INDEX_AC);
    }

    @Override
    protected void setOption() {
        Log.d("set Option", "in seting option in classifier");
        String[] options = new String[1];
        options[0] = "-U";
        tree = new J48();
        try {
            tree.setOptions(options);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void testData() {
    }

    @Override
    public void trainingData() {
        Log.d("TrainingData", "in traininData phase.....");
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

    @Override
    protected void setFeature() {
        Log.d("Setting Feature", "setting Feature in AC filter ");

        // add numeric attribute
        Attribute attribute1 = new Attribute("x");
        Attribute attribute2 = new Attribute("y");
        Attribute attribute3 = new Attribute("z");

        // declare class attribute
        FastVector fvClassVal = new FastVector(2);
        fvClassVal.addElement("owner");
        fvClassVal.addElement("other");
        Attribute classAttribute = new Attribute("the class", fvClassVal);

        // Declare feature vector
        fvWekaAttributes = new FastVector(4);
        fvWekaAttributes.addElement(attribute1);
        fvWekaAttributes.addElement(attribute2);
        fvWekaAttributes.addElement(attribute3);
        fvWekaAttributes.addElement(classAttribute);
//		super.setFeature();
    }
}
