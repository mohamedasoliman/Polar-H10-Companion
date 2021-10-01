package com.polar.autpolarcompanion;

import java.util.ArrayList;

public class UtilityClass {

    private static UtilityClass instance;

    private ArrayList list;

    public ArrayList getList() {
        return list;
    }

    public void setList(ArrayList list) {
        this.list = list;
    }

    private UtilityClass(){}

    public static UtilityClass getInstance(){
        if(instance == null){
            instance = new UtilityClass();
        }
        return instance;
    }
}