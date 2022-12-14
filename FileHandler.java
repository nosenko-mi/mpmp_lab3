package com.ltl.mpmp_lab3;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileHandler {
    private static FileHandler instance;
    private final String fileName;
    private final Context context;

    private FileHandler(String fileName, Context context) {
        this.fileName = fileName;
        this.context = context;
    }

    public static FileHandler getInstance(String fileName, Context context){
        if (instance == null){
            instance = new FileHandler(fileName, context);
        }
        return instance;
    }

    public void saveRecord(Integer points){
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fileOutputStream.write(points.toString().getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Integer loadRecord(){
        FileInputStream fileInputStream = null;
        String record = "";
        try {
            fileInputStream = context.openFileInput(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            record = bufferedReader.readLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null){
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (record.equals("")){
            return 0;
        }
        return Integer.parseInt(record);
    }

    public String getFileName() {
        return fileName;
    }

}
