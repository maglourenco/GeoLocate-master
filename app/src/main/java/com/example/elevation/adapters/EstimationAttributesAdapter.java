package com.example.elevation.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.elevation.EstimationAttributes;
import com.example.elevation.R;

import java.util.ArrayList;

public class EstimationAttributesAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<EstimationAttributes> mEstimationAttributes;

    public EstimationAttributesAdapter(Context context, ArrayList<EstimationAttributes> estimationAttributes) {
        this.context = context;
        this.mEstimationAttributes = estimationAttributes;
    }

    @Override
    public int getCount() {
        return mEstimationAttributes.size();
    }

    @Override
    public EstimationAttributes getItem(int position) {
        return mEstimationAttributes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.estimation_attributes_entry_layout, parent, false);
        }

        // Get the current item from the data list
        EstimationAttributes currentItem = getItem(position);

        // Set the title and subtitle in the row layout
        TextView keyAttributeTextView = convertView.findViewById(R.id.keyAttribute);
        keyAttributeTextView.setText(currentItem.getAttributeKey());

        TextView valueAttributeTextView = convertView.findViewById(R.id.valueAttribute);
        valueAttributeTextView.setText(currentItem.getAttributeValue());

        return convertView;
    }
}

