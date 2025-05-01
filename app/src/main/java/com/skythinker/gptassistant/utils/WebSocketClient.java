package com.skythinker.gptassistant.utils;

import android.util.Log;
import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import com.agent.intention.api.OkHttpManager;
import com.agent.intention.api.TripAllResultContent;
import com.agent.intention.api.TripItemContent;
import com.google.gson.Gson;
import com.skythinker.gptassistant.MainActivity;
import com.skythinker.gptassistant.config.MsgType;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient {
    private final OkHttpClient client = OkHttpManager.INSTANCE.getDefaultClient();
    private WebSocket ws;

    public void connectWebSocket(MainActivity context, String url) {
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer "+ OkHttpManager.INSTANCE.getClientToken())
            .addHeader("X-Client-Ip", OkHttpManager.INSTANCE.getPublicIp())
            .build();

        ws = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                super.onOpen(webSocket, response);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                super.onMessage(webSocket, text);
                Log.d("WebSocketTrip", text);

                Gson gson = new Gson();
                String resultUrl = "";
                TripItemContent tripItemContent = gson.fromJson(text, TripItemContent.class);
                int status = tripItemContent.getStatus();
                TripAllResultContent allResult = tripItemContent.getAll_result();

                if(allResult != null) {
                    if(status == 1) {
                        Utils.INSTANCE.sendTripChatMessage(context, allResult.getStatus_summary_info(), MsgType.LOADING,true);
                    }
                    if(status == 2) {
                        try {
                            if(tripItemContent.getResult() != null) {
                                resultUrl = tripItemContent.getResult().getFile_url();
                            }
                            String reason = allResult.getPoi_search_result().getTop1_poi_recommend_reason();
                            Utils.INSTANCE.sendTripChatMessage(context, allResult.getStatus_summary_info(), MsgType.SUC,true);
                            int index = reason.indexOf('。');
                            if (index != -1) {
                                Utils.INSTANCE.sendTripChatMessage(context, reason.substring(0, index) + "。", MsgType.COMMON,false,1300);
                                Thread.sleep(1700);
                            }

                            Utils.INSTANCE.sendTripCard(context, allResult.getPoi_search_result(), resultUrl);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if(status == 3) {
                        String errMsg;
                        int subStatus1 = tripItemContent.getSub_status1();
                        int subStatus2 = tripItemContent.getSub_status2();
                        if(subStatus1 == 3 && subStatus2 == 0) {
                            errMsg = "获取目标地址在地图上的经纬度信息失败，请检查目标地址是否有误";
                        } else {
                            errMsg = "规划信息生成失败，请稍后重试";
                        }
                        Utils.INSTANCE.sendTripChatMessage(context,"任务失败："+ errMsg, MsgType.ERROR,true);
                    }
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                super.onMessage(webSocket, bytes);
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosing(webSocket, code, reason);
                Log.d("WebSocketTrip","WebSocket is onClosing.");
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
                Log.d("WebSocketTrip","WebSocket is Closed.");
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                Log.d("WebSocketTrip","WebSocket is onFailure.");
                Utils.INSTANCE.sendTripChatMessage(context,"任务执行失败", MsgType.ERROR,true);
            }
        });
    }

    public void sendMessage(String message) {
        if (ws != null) {
            ws.send(message);
        } else {
            Log.d("WebSocketTrip","WebSocket is not connected.");
        }
    }

    public void closeConnection() {
        if (ws != null) {
            ws.close(1000, "Goodbye!");
        } else {
            Log.d("WebSocketTrip","WebSocket is not connected.");
        }
    }
}
