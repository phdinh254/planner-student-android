package com.example.personalplanner.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalplanner.R;
import com.example.personalplanner.data.model.StudyPlan;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.PlanViewHolder> {
    public interface OnTaskActionListener {
        void onTaskClick(StudyPlan plan);
        void onStatusChanged(StudyPlan plan, boolean checked);
    }

    private final ArrayList<StudyPlan> plans = new ArrayList<>();
    private final OnTaskActionListener listener;

    public TaskAdapter(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void setData(ArrayList<StudyPlan> newPlans) {
        int oldSize = plans.size();
        plans.clear();
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
        if (newPlans != null) {
            plans.addAll(newPlans);
            if (!newPlans.isEmpty()) {
                notifyItemRangeInserted(0, newPlans.size());
            }
        }
    }

    public int getPosition(StudyPlan plan) {
        return plans.indexOf(plan);
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PlanViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        StudyPlan plan = plans.get(position);
        holder.txtTaskTitle.setText(plan.getTitle());
        holder.txtTaskDescription.setText(plan.getDescription());
        holder.txtTaskDescription.setVisibility(plan.getDescription().trim().isEmpty()
                ? View.GONE : View.VISIBLE);
        holder.txtTaskDateTime.setText(holder.itemView.getContext().getString(
                R.string.task_date_time, plan.getDate(), plan.getTime()));
        holder.txtCourseName.setText(plan.getCourseName().trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.uncategorized_course)
                : plan.getCourseName());
        holder.txtStudyMeta.setText(holder.itemView.getContext().getString(
                R.string.study_meta,
                holder.itemView.getResources().getStringArray(R.array.priority_names)[plan.getPriority()],
                plan.getDurationMinutes()
        ));

        holder.chkStatus.setOnCheckedChangeListener(null);
        holder.chkStatus.setChecked(plan.getStatus() == 1);
        int flags = holder.txtTaskTitle.getPaintFlags();
        holder.txtTaskTitle.setPaintFlags(plan.getStatus() == 1
                ? flags | Paint.STRIKE_THRU_TEXT_FLAG
                : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        holder.itemView.setOnClickListener(v -> listener.onTaskClick(plan));
        holder.chkStatus.setOnCheckedChangeListener((button, checked) ->
                listener.onStatusChanged(plan, checked));
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {
        final TextView txtTaskTitle;
        final TextView txtTaskDescription;
        final TextView txtTaskDateTime;
        final TextView txtCourseName;
        final TextView txtStudyMeta;
        final CheckBox chkStatus;

        PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTaskTitle = itemView.findViewById(R.id.txtTaskTitle);
            txtTaskDescription = itemView.findViewById(R.id.txtTaskDescription);
            txtTaskDateTime = itemView.findViewById(R.id.txtTaskDateTime);
            txtCourseName = itemView.findViewById(R.id.txtCourseName);
            txtStudyMeta = itemView.findViewById(R.id.txtStudyMeta);
            chkStatus = itemView.findViewById(R.id.chkStatus);
        }
    }
}
