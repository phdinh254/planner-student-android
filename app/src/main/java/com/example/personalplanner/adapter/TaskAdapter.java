package com.example.personalplanner.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalplanner.R;
import com.example.personalplanner.model.Task;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final Context context;
    private ArrayList<Task> taskList;
    private final OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onTaskClick(Task task);
        void onStatusChanged(Task task, boolean isChecked);
    }

    public TaskAdapter(Context context, ArrayList<Task> taskList, OnTaskActionListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    public void setData(ArrayList<Task> newTaskList) {
        ArrayList<Task> safeList = newTaskList == null ? new ArrayList<>() : newTaskList;
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new TaskDiffCallback(taskList, safeList));
        this.taskList = safeList;
        result.dispatchUpdatesTo(this);
    }

    public int getPosition(Task task) {
        return taskList.indexOf(task);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.txtTaskTitle.setText(task.getTitle());
        String description = task.getDescription();
        holder.txtTaskDescription.setText(description);
        holder.txtTaskDescription.setVisibility(
                description == null || description.trim().isEmpty() ? View.GONE : View.VISIBLE
        );
        holder.txtTaskDateTime.setText(
                context.getString(R.string.task_date_time, task.getDate(), task.getTime())
        );

        holder.chkStatus.setOnCheckedChangeListener(null);
        holder.chkStatus.setChecked(task.getStatus() == 1);
        holder.chkStatus.setContentDescription(context.getString(
                task.getStatus() == 1 ? R.string.mark_pending : R.string.mark_completed,
                task.getTitle()
        ));

        if (task.getStatus() == 1) {
            holder.txtTaskTitle.setPaintFlags(
                    holder.txtTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        } else {
            holder.txtTaskTitle.setPaintFlags(
                    holder.txtTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });

        holder.chkStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onStatusChanged(task, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList == null ? 0 : taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {

        TextView txtTaskTitle, txtTaskDescription, txtTaskDateTime;
        CheckBox chkStatus;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            txtTaskTitle = itemView.findViewById(R.id.txtTaskTitle);
            txtTaskDescription = itemView.findViewById(R.id.txtTaskDescription);
            txtTaskDateTime = itemView.findViewById(R.id.txtTaskDateTime);
            chkStatus = itemView.findViewById(R.id.chkStatus);
        }
    }

    private static class TaskDiffCallback extends DiffUtil.Callback {
        private final ArrayList<Task> oldList;
        private final ArrayList<Task> newList;

        TaskDiffCallback(ArrayList<Task> oldList, ArrayList<Task> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getTaskId()
                    == newList.get(newItemPosition).getTaskId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Task oldTask = oldList.get(oldItemPosition);
            Task newTask = newList.get(newItemPosition);
            return oldTask.getStatus() == newTask.getStatus()
                    && safeEquals(oldTask.getTitle(), newTask.getTitle())
                    && safeEquals(oldTask.getDescription(), newTask.getDescription())
                    && safeEquals(oldTask.getDate(), newTask.getDate())
                    && safeEquals(oldTask.getTime(), newTask.getTime());
        }

        private boolean safeEquals(String first, String second) {
            return first == null ? second == null : first.equals(second);
        }
    }
}
