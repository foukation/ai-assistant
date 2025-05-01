package com.skythinker.gptassistant;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.agent.intention.api.IntentionApi;
import com.agent.intention.api.TripContentRes;
import com.agent.intention.api.TripDelRes;
import com.agent.intention.api.TripItemContent;
import com.skythinker.gptassistant.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends Activity {

    private class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.ViewHolder> {
        HistoryActivity historyActivity;
        private final List<TripItemContent> tripItemContents = new ArrayList<>();

        public HistoryListAdapter(HistoryActivity historyActivity) {
            this.historyActivity = historyActivity;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_list_item, parent, false);
            return new HistoryListAdapter.ViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TripItemContent currentItem = tripItemContents.get(position);
            holder.tvTitle.setText(currentItem.getDescription());
            holder.tvDetail.setText(currentItem.getCommand());
            holder.tvId.setText(currentItem.getId() + "");

            int length = currentItem.getCreatedAt().length();
            String result = currentItem.getCreatedAt().substring(length - 8, length);
            holder.tvTime.setText(result);
            if(currentItem.getResult() != null) {
                holder.tvUrl.setText(currentItem.getResult().getFile_url());
            }

            int status = currentItem.getStatus();
            if(status == 0) {
                holder.tvStatus.setText("待处理");
                holder.tvStatus.setTextColor(Color.parseColor("#909399"));
            }
            if(status == 1) {
                holder.tvStatus.setText("执行中");
                holder.tvStatus.setTextColor(Color.parseColor("#909399"));
            }
            if(status == 2) {
                holder.tvStatus.setText("已完成");
                holder.tvStatus.setTextColor(Color.parseColor("#409eff"));
            }
            if(status == 3) {
                holder.tvStatus.setText("失败");
                holder.tvStatus.setTextColor(Color.parseColor("#f56c6c"));
            }
            if(status == 4) {
                holder.tvStatus.setText("已停止");
                holder.tvStatus.setTextColor(Color.parseColor("#909399"));
            }
        }

        @Override
        public int getItemCount() {
            return tripItemContents.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvTitle;
            private final TextView tvDetail;
            private final TextView tvTime;
            private final TextView tvStatus;
            private final TextView tvUrl;
            private final TextView tvId;

            public ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_history_item_title);
                tvDetail = itemView.findViewById(R.id.tv_history_item_detail);
                tvTime = itemView.findViewById(R.id.tv_history_item_time);
                tvStatus = itemView.findViewById(R.id.tv_history_item_status);
                tvUrl = itemView.findViewById(R.id.tv_history_item_url);
                tvId = itemView.findViewById(R.id.tv_history_item_id);

                LinearLayout llOuter = itemView.findViewById(R.id.ll_history_item_outer);

                llOuter.setOnClickListener((view) -> {
                    if(tvStatus.getText() == "已完成" && tvUrl.getText() != "") {
                        Intent intent = new Intent(historyActivity, H5TripPlanActivity.class);
                        intent.putExtra("webUrl",tvUrl.getText());
                        startActivityForResult(intent, 3);
                    } else {
                        ToastUtils.shortCall("当前任务还未生成成功或已失败");
                    }
                });

                llOuter.setOnLongClickListener((view) -> {
                    new ConfirmDialog(historyActivity)
                        .setContent("是否删除此项数据？")
                        .setOnConfirmListener(() -> {

                            IntentionApi.INSTANCE.delTrip((String) tvId.getText(),
                                (TripDelRes resp) -> {
                                    if(resp.getCode() == 200) {
                                        runOnUiThread(()-> {
                                            ToastUtils.shortCall("删除成功");
                                            historyActivity.getTripList(HistoryListAdapter.this);
                                        });
                                    }
                                    return null;
                                },
                                (String error) -> null
                            );
                        })
                        .setOnCancelListener(() -> {
                        })
                        .show();
                    return false;
                });

            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        RecyclerView rvHistoryList = findViewById(R.id.rv_history_list);
        rvHistoryList.setLayoutManager(new LinearLayoutManager(this));
        HistoryListAdapter historyListAdapter = new HistoryListAdapter(this);
        rvHistoryList.setAdapter(historyListAdapter);

        (findViewById(R.id.bt_history_back)).setOnClickListener((view) -> {
            finish();
        });

        getTripList(historyListAdapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    protected void getTripList(HistoryListAdapter adapter) {
        IntentionApi.INSTANCE.getTripList(
            (TripContentRes resp) -> {
                runOnUiThread(()-> {
                    adapter.tripItemContents.clear();
                    if(resp.getData().getList() != null) {
                        adapter.tripItemContents.addAll(resp.getData().getList());
                    }
                    adapter.notifyDataSetChanged();
                });
                return null;
            },
            (String error) -> null
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
    }
}