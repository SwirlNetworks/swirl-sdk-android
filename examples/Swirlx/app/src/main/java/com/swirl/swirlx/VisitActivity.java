/*
 * Swirlx
 * VisitActivity
 *
 * Copyright (C) 2015-2016 Swirl Networks, Inc.
 */

package com.swirl.swirlx;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.swirl.Swirl;
import com.swirl.SwirlListener;
import com.swirl.Visit;
import com.swirl.VisitManager;

import java.util.List;

/**
 * Created by Tom on 8/22/16.
 */
public class VisitActivity extends AppCompatActivity {
    protected ListView      visitList;
    private VisitAdapter    visitAdapter;
    private StatusListener  statusListener  = new StatusListener();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        visitList = new ListView(this);
        visitAdapter = new VisitAdapter(this, VisitManager.getInstance().getActivePlacementVisits());

        visitList.setAdapter(visitAdapter);
        setContentView(visitList);
    }
    @Override public void onPause() {
        Swirl.getInstance().removeListener(statusListener);
        super.onPause();
    }
    @Override public void onResume() {
        super.onResume();
        Swirl.getInstance().addListener(statusListener);
    }

    static class VisitAdapter extends BaseAdapter {
        private List<Visit>   visits;
        private LayoutInflater inflater;

        public VisitAdapter(Activity activity, List<Visit> history) {
            inflater = activity.getLayoutInflater();
            this.visits = history;
        }

        public void setVisits(List<Visit> visits) {
            this.visits = visits;
            notifyDataSetChanged();
        }

        public int getCount() {
            return visits != null ? visits.size() : 0;
        }
        public Object getItem(int position) {
            return visits != null ? visits.get(position) : null;
        }
        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView == null ? inflater.inflate(R.layout.visit_item, null) : convertView;
            TextView text = (TextView)v.findViewById(com.swirl.swirl.R.id.text);
            Visit visit;
            if ((visit = visits.get(position)) != null) {
                text.setText(visit.toString());
            }
            return v;
        }
    }

    class StatusListener extends SwirlListener {
        public StatusListener() {
        }

        @Override protected void onBeginVisit(VisitManager manager, Visit visit) {
            visitAdapter.setVisits(VisitManager.getInstance().getActivePlacementVisits());
        }
        @Override protected void onDwellVisit(VisitManager manager, Visit visit) {
            visitAdapter.setVisits(VisitManager.getInstance().getActivePlacementVisits());
        }
        @Override protected void onEndVisit(VisitManager manager, Visit visit) {
            visitAdapter.setVisits(VisitManager.getInstance().getActivePlacementVisits());
        }

    }
}
