package com.polar.autpolarcompanion;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class HRVActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrv);

        GraphView graph = findViewById(R.id.graph1);

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();

        for(int i = 0; i < UtilityClass.getInstance().getList().size()-1; i++) {
            double y;

            if(UtilityClass.getInstance().getList() != null)
            {
                y = (double) UtilityClass.getInstance().getList().get(i);
            }
            else
            {
                y = 1;
            }
            series.appendData(new DataPoint(i, y), true, 100);
        }

        graph.addSeries(series);

        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScrollableY(true);


    }
}
