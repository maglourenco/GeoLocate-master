package com.example.elevation.adapters;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.elevation.R;

public class CustomList extends ArrayAdapter<String>{

    private final Activity context;
    private final ArrayList<String> attachmentName;
    public CustomList(Activity context,
                      ArrayList<String> attachmentList) {
        super(context, R.layout.estimation_attachment_entry_layout, attachmentList);
        this.context = context;
        attachmentName = attachmentList;
    }
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        CustomList.ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            convertView = inflater.inflate(R.layout.estimation_attachment_entry_layout, null, true);

            holder = new CustomList.ViewHolder();
            holder.textTitle = convertView.findViewById(R.id.AttachmentName);

            convertView.setTag(holder);
        } else {
            holder = (CustomList.ViewHolder) convertView.getTag();
        }

        holder.textTitle.setText(attachmentName.get(position));

        return convertView;
    }

    private static class ViewHolder {
        TextView textTitle;
    }
}
