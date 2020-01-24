package id.michale.volcanomonitoring;

import androidx.lifecycle.ViewModel;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

public class MainActivityModel extends ViewModel {

    private String tempStatus;
    private String tempValue;
    private String humiStatus;
    private String humiValue;

    private  String vibratStatus;
    private  String vibratValue;

    private int eruptSymbol;
    private  String eruptStatus;

    private String lastSync;

    private   ArrayList<String> chartXVal = new ArrayList<>(6);
    private ArrayList<Entry> chartYValSuhu = new ArrayList<>(6);
    private  ArrayList<Entry> chartYValKelembapan = new ArrayList<>(6);

    public MainActivityModel(){

    }

    public String getTempStatus() {
        return tempStatus;
    }

    public void setTempStatus(String tempStatus) {
        this.tempStatus = tempStatus;
    }

    public String getTempValue() {
        return tempValue;
    }

    public void setTempValue(String tempValue) {
        this.tempValue = tempValue;
    }

    public String getHumiStatus() {
        return humiStatus;
    }

    public void setHumiStatus(String humiStatus) {
        this.humiStatus = humiStatus;
    }

    public String getHumiValue() {
        return humiValue;
    }

    public void setHumiValue(String humiValue) {
        this.humiValue = humiValue;
    }

    public String getVibratStatus() {
        return vibratStatus;
    }

    public void setVibratStatus(String vibratStatus) {
        this.vibratStatus = vibratStatus;
    }

    public String getVibratValue() {
        return vibratValue;
    }

    public void setVibratValue(String vibratValue) {
        this.vibratValue = vibratValue;
    }

    public String getEruptStatus() {
        return eruptStatus;
    }

    public void setEruptStatus(String eruptStatus) {
        this.eruptStatus = eruptStatus;
    }

    public int getEruptSymbol() {
        return eruptSymbol;
    }

    public void setEruptSymbol(int eruptSymbol) {
        this.eruptSymbol = eruptSymbol;
    }

    public ArrayList<String> getChartXVal() {
        return new ArrayList<>(this.chartXVal);
    }

    public void setChartXVal(ArrayList<String> chartXVal) {

        this.chartXVal=new ArrayList<>(chartXVal);
    }

    public ArrayList<Entry> getChartYValSuhu() {
        return new ArrayList<>(this.chartYValSuhu);
    }

    public void setChartYValSuhu(ArrayList<Entry> chartYValSuhu) {
        this.chartYValSuhu=new ArrayList<>(chartYValSuhu);
    }

    public ArrayList<Entry> getChartYValKelembapan() {
        return new ArrayList<>(this.chartYValKelembapan);
    }

    public void setChartYValKelembapan(ArrayList<Entry> chartYValKelembapan) {
        this.chartYValKelembapan=new ArrayList<>(chartYValKelembapan);
    }

    public String getLastSync() {
        return lastSync;
    }

    public void setLastSync(String lastSync) {
        this.lastSync = lastSync;
    }
}
