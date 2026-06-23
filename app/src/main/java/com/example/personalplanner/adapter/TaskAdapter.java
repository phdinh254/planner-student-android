package com.example.personalplanner.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalplanner.R;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.utils.PlanBusinessRules;
import com.google.android.material.card.MaterialCardView;

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
        holder.txtCategoryName.setText(plan.getCategoryName().trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.uncategorized_course)
                : plan.getCategoryName());
        String[] typeValues = holder.itemView.getResources().getStringArray(R.array.plan_type_values);
        String[] typeNames = holder.itemView.getResources().getStringArray(R.array.plan_type_names);
        String typeName = plan.getPlanType();
        for (int i = 0; i < typeValues.length; i++) {
            if (typeValues[i].equals(plan.getPlanType())) {
                typeName = typeNames[i];
                break;
            }
        }
        holder.txtStudyMeta.setText(holder.itemView.getContext().getString(
                R.string.study_meta,
                typeName + " / " + holder.itemView.getResources().getStringArray(R.array.priority_names)[plan.getPriority()],
                plan.getDurationMinutes()
        ));

        holder.chkStatus.setOnCheckedChangeListener(null);
        holder.chkStatus.setChecked(plan.getStatus() == StudyPlan.STATUS_COMPLETED);
        int flags = holder.txtTaskTitle.getPaintFlags();
        holder.txtTaskTitle.setPaintFlags(plan.getStatus() == StudyPlan.STATUS_COMPLETED
                ? flags | Paint.STRIKE_THRU_TEXT_FLAG
                : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        boolean overdue = PlanBusinessRules.isOverdue(plan, System.currentTimeMillis());
        holder.txtOverdueBadge.setVisibility(overdue ? View.VISIBLE : View.GONE);
        holder.txtTaskDateTime.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                overdue ? R.color.error : R.color.primary
        ));
        holder.cardTask.setStrokeColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                overdue ? R.color.error : R.color.outline
        ));
        holder.cardTask.setStrokeWidth(overdue ? 2 : 1);
        holder.itemView.setOnClickListener(v -> listener.onTaskClick(plan));
        holder.chkStatus.setOnCheckedChangeListener((button, checked) ->
                listener.onStatusChanged(plan, checked));
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardTask;
        final TextView txtTaskTitle;
        final TextView txtTaskDescription;
        final TextView txtTaskDateTime;
        final TextView txtCategoryName;
        final TextView txtStudyMeta;
        final TextView txtOverdueBadge;
        final CheckBox chkStatus;

        PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTask = itemView.findViewById(R.id.cardTask);
            txtTaskTitle = itemView.findViewById(R.id.txtTaskTitle);
            txtTaskDescription = itemView.findViewById(R.id.txtTaskDescription);
            txtTaskDateTime = itemView.findViewById(R.id.txtTaskDateTime);
            txtCategoryName = itemView.findViewById(R.id.txtCategoryName);
            txtStudyMeta = itemView.findViewById(R.id.txtStudyMeta);
            txtOverdueBadge = itemView.findViewById(R.id.txtOverdueBadge);
            chkStatus = itemView.findViewById(R.id.chkStatus);
        }
    }
}
