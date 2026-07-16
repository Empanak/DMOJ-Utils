package com.empanak.dmojutils;

import com.empanak.dmojutils.view.MainView;

import java.io.InputStream;
import java.util.logging.LogManager;

public class Main {

    public static void main(String[] args) {

        try(InputStream in = Main.class.getResourceAsStream("/logging.properties")){
            LogManager.getLogManager().readConfiguration(in);
        }catch (Exception e){
            throw new RuntimeException(e);
        }

        MainView mainView = new MainView();
        mainView.setVisible(true);
    }
}