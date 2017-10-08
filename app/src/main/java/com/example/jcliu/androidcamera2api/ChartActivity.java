package com.example.jcliu.androidcamera2api;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        LinearLayout chartContainer = (LinearLayout) findViewById(R.id.activity_chart);
        Intent it = getIntent();
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
                if(CalActivity.wavelength[i] > 400){
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
