package com.example.jcliu.androidcamera2api;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

public class ChartActivity extends AppCompatActivity {

    private static final String TAG = "AndroidCamera2API";
    protected String title;
    protected int numChart;
    String [] signalName = null;
    double[][] spectrum = null;
    int [] defColor ={Color.RED, Color.GREEN, Color.BLUE, Color.BLACK, Color.CYAN, Color.MAGENTA, Color.YELLOW};
    static int leftIdx=0, rightIdx=0;
    boolean is_menu;
    final double [] RIU={1.3325, 1.3453, 1.3523, 1.3639};
    static double [][] peakNM = new double[4][3];
    double [][] peakValue = new double[4][3];
    LinearLayout chartContainer;
    String [] AnalysisSignalName = {"Left", "Center", "Right"};
    private static boolean analysisMenu = false;

    // option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(is_menu)
            getMenuInflater().inflate(R.menu.menu_chart, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        int spectrum_choice=0;
        Intent it = new Intent(this, ChartActivity.class);

        if(id == R.id.peakAnalysis){
            analysisMenu = true;
            // find peak in nm 600 to nm660
            int start=0;
            for(int i=0; i<CalActivity.wavelength.length; i++){
                if(CalActivity.wavelength[i] > 600) {
                    start = i;
                    break;
                }
            }
            Log.d(TAG, "peakAnalysis start(600nm) =" + start);
            for(int sig=2; sig<=numChart; sig++)
                for(int pos=0; pos<3; pos++)
                    peakValue[sig-2][pos]=0;

            for(int i=start; i<CalActivity.wavelength.length; i++){
                if(CalActivity.wavelength[i] < 660){
                    for(int sig=2; sig<=numChart; sig++) {
                        for (int pos = 0; pos < 3; pos++) {
                            if (ComputeActivity.NsignalSource[sig][pos][i] > peakValue[sig - 2][pos]) {
                                peakValue[sig - 2][pos] = ComputeActivity.NsignalSource[sig][pos][i];
                                peakNM[sig - 2][pos] = CalActivity.wavelength[i];
                            }
                        }
                    }
                } else
                    break;
            }
            //
            for(int sig=2; sig<=numChart; sig++)
                for(int pos=0; pos<3; pos++)
                    Log.d(TAG, "peakNM["+sig+"]["+pos+"]="+ peakNM[sig-2][pos]);
            // draw chart
            View v = drawAnalysisChart();
            chartContainer.addView(v, 0);
        } else {
            analysisMenu = false;
            switch (id) {
                case R.id.sample_1:
                    spectrum_choice = 0;
                    it.putExtra("title", "Left Sample Spectrum");
                    break;
                case R.id.sample_2:
                    spectrum_choice = 1;
                    it.putExtra("title", "Central Sample Spectrum");
                    break;
                case R.id.sample_3:
                    spectrum_choice = 2;
                    it.putExtra("title", "Right Sample Spectrum");
                    break;
            }
            // calculate normalized spectrum
            Log.d(TAG, "Normalized");
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            it.putExtra("is_menu", true);
            it.putExtra("numChart", numChart);
            for (int k = 1; k <= numChart; k++) {
                Log.d(TAG, "putExtra: signal " + k);
                it.putExtra("lightsource" + k, ComputeActivity.NsignalSource[k][spectrum_choice]);
                it.putExtra("signal name " + k, signalName[k - 1]);
            }
            Log.d(TAG, "start chart activity");
            startActivity(it);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent it = getIntent();
        is_menu = it.getBooleanExtra("is_menu", false);
        Log.d(TAG, "ChartActivity: is_menu="+is_menu);
        //if(!is_menu)
        //    this.invalidateOptionsMenu();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

       chartContainer = (LinearLayout) findViewById(R.id.activity_chart);

        //wavelength = it.getDoubleArrayExtra("wavelength");
        title = it.getStringExtra("title");
        numChart = it.getIntExtra("numChart", 1);
        Log.d(TAG, "title ="+title+", numChart="+numChart);
        signalName = new String[numChart];
        spectrum = new double[numChart][];

        for(int i=1; i<numChart+1; i++){
            signalName[i-1] = it.getStringExtra("signal name " + i);
            Log.d(TAG, "signal name ="+signalName[i-1]);
            spectrum[i-1] = it.getDoubleArrayExtra("lightsource" + i);
            Log.d(TAG, "signal name ="+signalName[i-1]+", spectrum length="+spectrum[i-1].length);
        }

        if(!analysisMenu) {
            View v = drawChart();
            chartContainer.addView(v, 0);
        } else{
            View v = drawAnalysisChart();
            chartContainer.addView(v, 0);
        }

    }

    public View drawChart(){

        if (CalActivity.wavelength != null && leftIdx==0) {
            for(int i=0; i<CalActivity.wavelength.length; i++){
                if(CalActivity.wavelength[i] > 430){
                    leftIdx = i;
                    break;
                }
            }
            for(int i=CalActivity.wavelength.length-1; i>0; i--){
                if(CalActivity.wavelength[i] < 680){
                    rightIdx = i;
                    break;
                }
            }
        }
        XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
        //multiRenderer.setXLabels(0);
        multiRenderer.setChartTitle(title);
        multiRenderer.setChartTitleTextSize(50);
        multiRenderer.setXTitle("wavelength (nm)");// X Title
        //multiRenderer.setYTitle("Energy");// Y Title
        multiRenderer.setLabelsTextSize(40);// Label Text Size
        multiRenderer.setAxisTitleTextSize(40);// Axis Title Text Size
        multiRenderer.setLegendTextSize(35);
        multiRenderer.setLegendHeight(150);
        multiRenderer.setMargins(new int[]{80, 120, 130, 50}); //top, left, down, right
        multiRenderer.setMarginsColor(Color.WHITE);
        multiRenderer.setXAxisColor(Color.BLACK);
        multiRenderer.setXLabelsColor(Color.BLACK);
        multiRenderer.setYAxisColor(Color.BLACK);
        multiRenderer.setYLabelsColor(0, Color.BLACK);
        multiRenderer.setLabelsColor(Color.BLACK);
        //multiRenderer.setXAxisMin(410);
        //multiRenderer.setXAxisMax(680);
        //multiRenderer.setZoomButtonsVisible(true);// Zoom?
        multiRenderer.setShowGrid(true);// show Grid
        //for(int i=0; i<list_Date.size(); i++){
        //    multiRenderer.addXTextLabel(i+1, "" + list_Date.get(i).toString());
        //}

        // Creating a dataset to hold each series
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        XYSeries [] LSeries = new XYSeries[numChart];
        XYSeriesRenderer [] LSeriesRenderer = new XYSeriesRenderer[numChart];

        for(int i=0; i<numChart; i++) {
            LSeries[i] = new XYSeries(signalName[i]);

            //XYSeries ASeries = new XYSeries(" all ");
            //Log.v("msg", "spectroB.length=" + spectroR.length + ", wavelength = " + wavelength.length);
            if (CalActivity.wavelength == null) {
                for (int j = 0; j < spectrum[i].length; j++) {
                    LSeries[i].add(j, spectrum[i][j]);
                }
            } else {
                for (int j = leftIdx; j < rightIdx; j++) {
                    LSeries[i].add(CalActivity.wavelength[j], spectrum[i][j]);
                }
            }
            Log.d(TAG, "add ok");

            // Adding Income Series to the dataseti
            dataset.addSeries(LSeries[i]);

            // 線的描述
            LSeriesRenderer[i] = new XYSeriesRenderer();
            LSeriesRenderer[i].setLineWidth(2);
            LSeriesRenderer[i].setColor(defColor[i]);

            multiRenderer.addSeriesRenderer(LSeriesRenderer[i]);
        }
        View mChart = ChartFactory.getLineChartView(this, dataset, multiRenderer);

        return mChart;
    }


    public View drawAnalysisChart(){

        XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
        //multiRenderer.setXLabels(0);
        multiRenderer.setChartTitle("Wavelength Sensitivity");
        multiRenderer.setChartTitleTextSize(50);
        multiRenderer.setXTitle("Refractive index value");// X Title
        multiRenderer.setYTitle("Resonance wavelength");
        //multiRenderer.setYTitle("Energy");// Y Title
        multiRenderer.setLabelsTextSize(40);// Label Text Size
        multiRenderer.setAxisTitleTextSize(40);// Axis Title Text Size
        multiRenderer.setLegendTextSize(35);
        multiRenderer.setLegendHeight(150);
        multiRenderer.setMargins(new int[]{80, 120, 130, 50}); //top, left, down, right
        multiRenderer.setMarginsColor(Color.WHITE);
        multiRenderer.setXAxisColor(Color.BLACK);
        multiRenderer.setXLabelsColor(Color.BLACK);
        multiRenderer.setYAxisColor(Color.BLACK);
        multiRenderer.setYLabelsColor(0, Color.BLACK);
        multiRenderer.setLabelsColor(Color.BLACK);
        //multiRenderer.setXAxisMin(410);
        //multiRenderer.setXAxisMax(680);
        //multiRenderer.setZoomButtonsVisible(true);// Zoom?
        multiRenderer.setShowGrid(true);// show Grid

        // Creating a dataset to hold each series
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        XYSeries [] LSeries = new XYSeries[3];
        XYSeriesRenderer [] LSeriesRenderer = new XYSeriesRenderer[3];

        for(int i=0; i<3; i++) {
            LSeries[i] = new XYSeries(AnalysisSignalName[i]);
            for (int j = 0; j < numChart-1; j++) {
                LSeries[i].add(RIU[j], peakNM[j][i]);
            }

            Log.d(TAG, "analysisDraw add ok");

            // Adding Income Series to the dataseti
            dataset.addSeries(LSeries[i]);

            // 線的描述
            LSeriesRenderer[i] = new XYSeriesRenderer();
            LSeriesRenderer[i].setLineWidth(2);
            LSeriesRenderer[i].setColor(defColor[i]);
            LSeriesRenderer[i].setPointStyle(PointStyle.CIRCLE);

            multiRenderer.addSeriesRenderer(LSeriesRenderer[i]);
        }
        View mChart = ChartFactory.getLineChartView(this, dataset, multiRenderer);

        return mChart;
    }

}
