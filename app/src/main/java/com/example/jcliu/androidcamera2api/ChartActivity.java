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

    double[] spectroR, spectroG, spectroB, wavelength;
    private static final String TAG = "AndroidCamera2API";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        LinearLayout chartContainer = (LinearLayout) findViewById(R.id.activity_chart);

        Intent it = getIntent();
        //wavelength = it.getDoubleArrayExtra("wavelength");
        spectroR = it.getDoubleArrayExtra("lightsource1");
        spectroG = it.getDoubleArrayExtra("lightsource2");
        spectroB = it.getDoubleArrayExtra("lightsource3");
        //Log.v("msg", "wavelength:" + Double.toString(wavelength[0]) + ", "  + Double.toString(wavelength[1]));
        Log.d(TAG, "lightsource1:" + Double.toString(spectroR[0]) + ", "  + Double.toString(spectroR[1]));
        Log.d(TAG, "lightsource2:" + Double.toString(spectroG[0]) + ", "  + Double.toString(spectroG[1]));
        Log.d(TAG, "lightsource3:" + Double.toString(spectroB[0]) + ", "  + Double.toString(spectroB[1]));
        //og.d(TAG, "spectroR.length=" + spectroR.length + ", wavelength = " + wavelength.length);
        View v = drawChart();
        chartContainer.addView(v, 0);
    }

    public View drawChart(){
        XYSeries RSeries = new XYSeries(" Center ");
        XYSeries GSeries = new XYSeries(" Right ");
        XYSeries BSeries = new XYSeries(" Left ");
        //XYSeries ASeries = new XYSeries(" all ");
        //Log.v("msg", "spectroB.length=" + spectroR.length + ", wavelength = " + wavelength.length);
        for(int i=0;i<spectroR.length;i++){
            RSeries.add(i, spectroR[i]);
            GSeries.add(i, spectroG[i]);
            BSeries.add(i, spectroB[i]);
            //ASeries.add(wavelength[i], spectroR[i]+spectroG[i]+spectroB[i]);
        }
        Log.d(TAG, "add ok");
        // Creating a dataset to hold each series
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        // Adding Income Series to the dataset
        dataset.addSeries(RSeries);
        dataset.addSeries(GSeries);
        dataset.addSeries(BSeries);
        //dataset.addSeries(ASeries);


        // 線的描述
        XYSeriesRenderer RSeriesRenderer = new XYSeriesRenderer();
        RSeriesRenderer.setColor(Color.RED);
        //xySeriesRenderer.setChartValuesTextSize(40);// Value Text Size
        //RSeriesRenderer.setPointStyle(PointStyle.CIRCLE);
        //RSeriesRenderer.setFillPoints(true);
        RSeriesRenderer.setLineWidth(2);
        //xySeriesRenderer.setDisplayChartValues(true);

        XYSeriesRenderer GSeriesRenderer = new XYSeriesRenderer();
        GSeriesRenderer.setColor(Color.GREEN);
        GSeriesRenderer.setLineWidth(2);

        XYSeriesRenderer BSeriesRenderer = new XYSeriesRenderer();
        BSeriesRenderer.setColor(Color.BLUE);
        BSeriesRenderer.setLineWidth(2);

        //XYSeriesRenderer ASeriesRenderer = new XYSeriesRenderer();
        //ASeriesRenderer.setColor(Color.BLACK);
        //ASeriesRenderer.setLineWidth(2);

        XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
        //multiRenderer.setXLabels(0);
        multiRenderer.setChartTitle("LightSource Raw Spectrum");
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

        multiRenderer.addSeriesRenderer(RSeriesRenderer);
        multiRenderer.addSeriesRenderer(GSeriesRenderer);
        multiRenderer.addSeriesRenderer(BSeriesRenderer);
        //multiRenderer.addSeriesRenderer(ASeriesRenderer);

        View mChart = ChartFactory.getLineChartView(this, dataset, multiRenderer);

        return mChart;
    }
}
