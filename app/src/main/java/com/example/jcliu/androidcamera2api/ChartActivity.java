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
    int [] defColor ={Color.RED, Color.GREEN, Color.BLUE, Color.BLACK, Color.CYAN};
    static int leftIdx=0, rightIdx=0;
    boolean is_menu;

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

        switch (id){
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
        // initialized normalized spectrum array
        /*
        int length = ComputeActivity.signalSource[0][0].length;
        Log.d(TAG, "length =" + length);
        if (ComputeActivity.signalSource[3][0] == null) { // 初始化正規光譜陣列
            for (int j = 0; j < 3; j++) {
                ComputeActivity.signalSource[3][j] = new double[length];
                ComputeActivity.signalSource[4][j] = new double[length];
                Log.d(TAG, "initialize source array[3,4][" + j + "]");
            }
        }
        // compute normalized spectrum
        for (int j = 0; j < length; j++) {
            //Log.d(TAG, "i="+i+", j="+j);
            if (ComputeActivity.signalSource[0][spectrum_choice][j] != 0) {
                ComputeActivity.signalSource[3][spectrum_choice][j] = ComputeActivity.signalSource[1][spectrum_choice][j] / ComputeActivity.signalSource[0][spectrum_choice][j];
                ComputeActivity.signalSource[4][spectrum_choice][j] = ComputeActivity.signalSource[2][spectrum_choice][j] / ComputeActivity.signalSource[0][spectrum_choice][j];
            } else {
                ComputeActivity.signalSource[3][spectrum_choice][j] = 0;
                ComputeActivity.signalSource[4][spectrum_choice][j] = 0;
            }
            //Log.d(TAG, "source[3][0][" + j + "]=" + ComputeActivity.signalSource[3][0][j] + ", source[1][0][" + j + "]=" + ComputeActivity.signalSource[1][0][j]);
        }
        Log.d(TAG, "Normalized ok");
*/
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        it.putExtra("is_menu", true);
        it.putExtra("numChart", 2);
        it.putExtra("lightsource1", ComputeActivity.signalSource[3][spectrum_choice]);
        it.putExtra("signal name 1", " In Air ");
        it.putExtra("lightsource2", ComputeActivity.signalSource[4][spectrum_choice]);
        it.putExtra("signal name 2", " In Water ");
        Log.d(TAG, "start chart activity");
        startActivity(it);
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

        LinearLayout chartContainer = (LinearLayout) findViewById(R.id.activity_chart);

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

        View v = drawChart();
        chartContainer.addView(v, 0);
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
}
