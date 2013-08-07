package com.stfalcon.unlocker;

import android.app.Application;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.SharedPreferences;

import java.util.ArrayList;

/**
 * Created by anton on 8/5/13.
 */
public class UnlockApp extends Application {

    public static SharedPreferences sPref;
    public static String MY_PREF = "mupref";
    public static KeyguardManager.KeyguardLock keyguardLock;
    public static double OFFSET_KOEF = 1.0;
    public static double OFFSET_KOEF_PITCH = 0.6;
    public static double OFFSET_KOEF_ROLL = 0.4;
    private static double GYROSCOPE_SENSITIVITY = 65.536;
    //private static double GYROSCOPE_SENSITIVITY = 10;
    private static double ACCELEROMETER_SENSITIVITY = 8192.0;
    private static double dt = 0.005;
    private KeyguardManager keyguardManager;

    public static void saveArrayList(ArrayList<double[]> arrayList) {

        SharedPreferences.Editor editor = sPref.edit();
        String[] pitch = new String[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            String s = String.valueOf(arrayList.get(i)[0]);
            pitch[i] = s;
        }
        editor.putInt("pitch_size", pitch.length);
        for (int i = 0; i < pitch.length; i++) {
            editor.putString("pitch_" + i, pitch[i]);
        }

        String[] roll = new String[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            String s = String.valueOf(arrayList.get(i)[1]);
            roll[i] = s;
        }
        editor.putInt("roll_size", roll.length);
        for (int i = 0; i < roll.length; i++) {
            editor.putString("roll_" + i, roll[i]);
        }
        editor.putBoolean("isSave", true);
        editor.commit();
    }

    public static void saveArrays(double[] dpitch, double[] droll) {

        SharedPreferences.Editor editor = sPref.edit();
        String[] pitch = new String[dpitch.length];
        for (int i = 0; i < dpitch.length; i++) {
            String s = String.valueOf(dpitch[i]);
            pitch[i] = s;
        }
        editor.putInt("pitch_size", pitch.length);
        for (int i = 0; i < pitch.length; i++) {
            editor.putString("pitch_" + i, pitch[i]);
        }

        String[] roll = new String[droll.length];
        for (int i = 0; i < droll.length; i++) {
            String s = String.valueOf(droll[i]);
            roll[i] = s;
        }
        editor.putInt("roll_size", roll.length);
        for (int i = 0; i < roll.length; i++) {
            editor.putString("roll_" + i, roll[i]);
        }
        editor.putBoolean("isSave", true);
        editor.commit();
    }

    public static void confArrays(double[] dpitch, double[] droll) {

        SharedPreferences.Editor editor = sPref.edit();
        String[] pitch = new String[dpitch.length];
        for (int i = 0; i < dpitch.length; i++) {
            String s = String.valueOf(dpitch[i]);
            pitch[i] = s;
        }
        editor.putInt("conf_pitch_size", pitch.length);
        for (int i = 0; i < pitch.length; i++) {
            editor.putString("conf_pitch_" + i, pitch[i]);
        }

        String[] roll = new String[droll.length];
        for (int i = 0; i < droll.length; i++) {
            String s = String.valueOf(droll[i]);
            roll[i] = s;
        }
        editor.putInt("conf_roll_size", roll.length);
        for (int i = 0; i < roll.length; i++) {
            editor.putString("conf_roll_" + i, roll[i]);
        }
        editor.putBoolean("isConfirm", true);
        editor.commit();
    }

    public static ArrayList<double[]> loadArrayList() {
        int pitchSize = sPref.getInt("pitch_size", 0);
        double[] pitch = new double[pitchSize];
        for (int i = 0; i < pitchSize; i++) {
            pitch[i] = Double.valueOf(sPref.getString("pitch_" + i, "0"));
        }

        int rollSize = sPref.getInt("roll_size", 0);
        double[] roll = new double[rollSize];
        for (int i = 0; i < rollSize; i++) {
            roll[i] = Double.valueOf(sPref.getString("roll_" + i, "0"));
        }

        ArrayList<double[]> arrayList = new ArrayList<double[]>();
        for (int i = 0; i < rollSize; i++) {
            arrayList.add(new double[]{pitch[i], roll[i]});
        }
        return arrayList;
    }

    public static ArrayList<double[]> loadArrays() {
        int pitchSize = sPref.getInt("pitch_size", 0);
        double[] pitch = new double[pitchSize];
        for (int i = 0; i < pitchSize; i++) {
            pitch[i] = Double.valueOf(sPref.getString("pitch_" + i, "0"));
        }

        int rollSize = sPref.getInt("roll_size", 0);
        double[] roll = new double[rollSize];
        for (int i = 0; i < rollSize; i++) {
            roll[i] = Double.valueOf(sPref.getString("roll_" + i, "0"));
        }

        ArrayList<double[]> arrayList = new ArrayList<double[]>();
        arrayList.add(pitch);
        arrayList.add(roll);

        return arrayList;
    }

    public static ArrayList<double[]> loadConfArrays() {
        int pitchSize = sPref.getInt("conf_pitch_size", 0);
        double[] pitch = new double[pitchSize];
        for (int i = 0; i < pitchSize; i++) {
            pitch[i] = Double.valueOf(sPref.getString("conf_pitch_" + i, "0"));
        }

        int rollSize = sPref.getInt("conf_roll_size", 0);
        double[] roll = new double[rollSize];
        for (int i = 0; i < rollSize; i++) {
            roll[i] = Double.valueOf(sPref.getString("conf_roll_" + i, "0"));
        }

        ArrayList<double[]> arrayList = new ArrayList<double[]>();
        arrayList.add(pitch);
        arrayList.add(roll);

        return arrayList;
    }

    public static double[] complementaryFilter(double accData[], double gyrData[]) {
        double pitchAcc, rollAcc;
        double pitch = 0;
        double roll = 0;
        double[] result = new double[2];

        // Integrate the gyroscope data -> int(angularSpeed) = angle
        pitch += ((float) gyrData[0] / GYROSCOPE_SENSITIVITY) * dt; // Angle around the X-axis
        roll -= ((float) gyrData[1] / GYROSCOPE_SENSITIVITY) * dt;    // Angle around the Y-axis

        // Compensate for drift with accelerometer data if !bullshit
        // Sensitivity = -2 to 2 G at 16Bit -> 2G = 32768 && 0.5G = 8192
        double forceMagnitudeApprox = Math.abs(accData[0]) + Math.abs(accData[1]) + Math.abs(accData[2]);
        if (forceMagnitudeApprox > 8192 && forceMagnitudeApprox < 32768) {
            // Turning around the X axis results in a vector on the Y-axis
            pitchAcc = Math.atan2((float) accData[1], (float) accData[2]) * 180 / Math.PI;
            pitch = pitch * 0.98 + pitchAcc * 0.02;

            // Turning around the Y axis results in a vector on the X-axis
            rollAcc = Math.atan2((float) accData[0], (float) accData[2]) * 180 / Math.PI;
            roll = roll * 0.98 + rollAcc * 0.02;
        }
        result[0] = pitch;
        result[1] = roll;
        return result;
    }

    public static String arrayListToString(ArrayList<double[]> dataList) {
        String s = "";
        for (int i = 0; i < dataList.size(); i++) {
            s += " [ ";
            for (int j = 0; j < dataList.get(i).length; j++) {
                s += " " + dataList.get(i)[j];
            }
            s += " ] " + "\n";
        }
        return s;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sPref = getSharedPreferences(MY_PREF, MODE_PRIVATE);
        keyguardManager = (KeyguardManager) getSystemService(Service.KEYGUARD_SERVICE);
        keyguardLock = keyguardManager.newKeyguardLock("Keyguard_Lock");
    }

}
