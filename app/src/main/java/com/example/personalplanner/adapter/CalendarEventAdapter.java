package com.example.personalplanner.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalplanner.R;
import com.example.personalplanner.data.model.StudyPlan;

import java.util.ArrayList;

public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder> {
    public interface OnEventClickListener {
        void onEventClick(StudyPlan plan);
    }

    private final ArrayList<StudyPlan> plans = new ArrayList<>();
    private final OnEventClickListener listener;

    public CalendarEventAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    public void setData(ArrayList<StudyPlan> newPlans) {
        plans.clear();
        if (newPlans != null) {
            plans.addAll(newPlans);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EventViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_event, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        StudyPlan plan = plans.get(position);
        holder.txtEventTitle.setText(plan.getTitle());
        holder.txtEventMeta.setText(plan.getTime() + " - " + labelForType(plan.getPlanType()));
        int colorRes = R.color.primary;
        if (plan.getPriority() == StudyPlan.PRIORITY_HIGH) {
            colorRes = R.color.danger;
        } else if (plan.getPriority() == StudyPlan.PRIORITY_MEDIUM) {
            colorRes = R.color.warning;
        } else if (StudyPlan.TYPE_CLASS.equals(plan.getPlanType())) {
            colorRes = R.color.primary;
        } else if (StudyPlan.TYPE_PART_TIME.equals(plan.getPlanType())) {
            colorRes = R.color.teal;
        }
        holder.viewPriorityBar.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), colorRes));
        holder.itemView.setOnClickListener(v -> listener.onEventClick(plan));
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    private String labelForType(String type) {
        if (StudyPlan.TYPE_ASSIGNMENT.equals(type)) return "B\u00e0i t\u1eadp";
        if (StudyPlan.TYPE_CLASS.equals(type)) return "\u0110i h\u1ecdc";
        if (StudyPlan.TYPE_PART_TIME.equals(type)) return "L\u00e0m th\u00eam";
        if (StudyPlan.TYPE_EXAM.equals(type)) return "Thi";
        if (StudyPlan.TYPE_PROJECT.equals(type)) return "D\u1ef1 \u00e1n";
        return "C\u00e1 nh\u00e2n";
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final View viewPriorityBar;
        final TextView txtEventTitle;
        final TextView txtEventMeta;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            viewPriorityBar = itemView.findViewById(R.id.viewPriorityBar);
            txtEventTitle = itemView.findViewById(R.id.txtEventTitle);
            txtEventMeta = itemView.findViewById(R.id.txtEventMeta);
        }
    }
}
