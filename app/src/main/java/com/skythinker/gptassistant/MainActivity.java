package com.skythinker.gptassistant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import cn.vove7.andro_accessibility_api.AccessibilityApi;
import timber.log.Timber;

import com.agent.intention.api.PoiItem;
import com.agent.intention.api.PoiSearchResultContent;
import com.agent.intention.api.TripCreateRes;
import com.ai.multimodal.MultimodalActivity;
import com.ai.multimodal.business.MultimodalAssistant;
import com.ai.multimodal.utils.ImageUtils;
import com.airbnb.lottie.LottieAnimationView;
import com.cmdc.ai.assist.AIAssistantManager;
import com.cmdc.ai.assist.api.ASRIntelligentDialogue;
import com.cmdc.ai.assist.constraint.DialogueResult;
import com.skythinker.gptassistant.ChatManager.ChatMessage.ChatRole;
import com.skythinker.gptassistant.ChatManager.ChatMessage;
import com.skythinker.gptassistant.ChatManager.MessageList;
import com.skythinker.gptassistant.ChatManager.Conversation;
import com.skythinker.gptassistant.config.PermissionType;
import com.skythinker.gptassistant.helper.AppListHelper;
import com.skythinker.gptassistant.helper.HandlerLineTaskHelper;

import com.agent.intention.api.IntentionApi;
import com.skythinker.gptassistant.utils.Utils;
import com.skythinker.gptassistant.config.MsgType;
import com.skythinker.gptassistant.utils.WebSocketClient;
import com.skythinker.gptassistant.view.CustomImageButton;
import com.skythinker.gptassistant.view.ResizableLinearLayout;
import com.squareup.picasso.Picasso;

@SuppressLint({"UseCompatLoadingForDrawables", "JavascriptInterface", "SetTextI18n"})
public class MainActivity extends Activity {

    private TextView tvGptReply;
    private EditText etUserInput;
    private ImageButton btImage;
    private CustomImageButton btSend;
    private ScrollView svChatArea;
    private LinearLayout llChatList;
    private PopupWindow pwMenu;
    BroadcastReceiver localReceiver = null;

    ChatManager chatManager = null;
    private Conversation currentConversation = null; // 当前会话信息
    private MessageList multiChatList = null; // 指向currentConversation.messages

    AsrClientBase.IAsrCallback asrCallback = null;

    Bitmap selectedImageBitmap = null;

    private ASRIntelligentDialogue realtimeAsr;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION = 1;
    private String curAsrResult = "";
    MainActivity curContext = this;

    private WebSocketClient webSocketClient;
    private static final String WEBSOCKET_URL = "ws://36.213.71.163:11453/api/v1/task/ws/status/";

    LottieAnimationView mLottieAnimationView;
    LottieAnimationView loadingLottieAnimationView;
    private boolean ttsEnabled = true;
    Runnable typingRunnable = null;
    private ResizableLinearLayout mResizableLinearLayout;

    public boolean needCheckOverlayPermission = true;
    private void initTTS() {

        // TTS开关按钮点击事件（切换TTS开关状态）
        (findViewById(R.id.cv_tts_off)).setOnClickListener(view -> {
            ttsEnabled = !ttsEnabled;
            if (ttsEnabled) {
                findViewById(R.id.cv_tts_off).setForeground(getDrawable(R.drawable.tts_off));
                GlobalUtils.showToast(this, R.string.toast_tts_on, false);
            } else {
                findViewById(R.id.cv_tts_off).setForeground(getDrawable(R.drawable.tts_off_enable));
                GlobalUtils.showToast(this, R.string.toast_tts_off, false);
            }
        });

    }

    private void setRecording() {
        //进入就自动收音
        recordingstate();
        Intent broadcastIntent = new Intent("com.skythinker.gptassistant.KEY_SPEECH_START");
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

    }

    private void recordingstate() {
        etUserInput.setHint(R.string.text_listening_hint);
        btSend.setImageResource(R.mipmap.voiceing_ic);
        mLottieAnimationView.setVisibility(View.VISIBLE);
        mLottieAnimationView.playAnimation();
    }

    private void defaultState() {
        etUserInput.setHint(R.string.text_input_hint);
        btSend.setImageResource(R.mipmap.voice_ic);
        mLottieAnimationView.setVisibility(View.GONE);
        mLottieAnimationView.pauseAnimation();

    }

    private void sendState() {
        etUserInput.setHint(R.string.text_input_hint);
        btSend.setImageResource(R.mipmap.voice_ic);
        mLottieAnimationView.setVisibility(View.GONE);
        mLottieAnimationView.pauseAnimation();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentionApi.INSTANCE.handlerRequestPublicIp();
        IntentionApi.INSTANCE.handlerRequestClientToken(() -> {
            AppListHelper.INSTANCE.getAppList();
            return null;
        });

        // 全局异常捕获
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Timber.tag("UncaughtException").e(thread.getClass().getName() + " " + throwable.getMessage());
            throwable.printStackTrace();
            System.exit(-1);
        });

        GlobalDataHolder.init(this); // 初始化全局共享数据

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            initPermission();
            requestOverlayPermission();
        }

        setContentView(R.layout.activity_main); // 设置主界面布局

        initTTS();

        tvGptReply = findViewById(R.id.tv_chat_notice);
        tvGptReply.setTextIsSelectable(true);
        tvGptReply.setMovementMethod(LinkMovementMethod.getInstance());
        etUserInput = findViewById(R.id.et_user_input);
        btSend = findViewById(R.id.bt_send);
        btImage = findViewById(R.id.bt_image);
        svChatArea = findViewById(R.id.sv_chat_list);
        llChatList = findViewById(R.id.ll_chat_list);
        mLottieAnimationView = findViewById(R.id.lottieAnimationView);
        mResizableLinearLayout = findViewById(R.id.chat_content);

        btImage.setOnClickListener(view -> {

            Intent intent = new Intent(MainActivity.this, MultimodalActivity.class);
            this.startActivity(intent);

        });

        handleShareIntent(getIntent()); // 处理分享的文本/图片

        chatManager = new ChatManager(this); // 初始化聊天记录管理器
        ChatMessage.setContext(this); // 设置聊天消息的上下文（用于读写文件）

        btSend.setOnClickListener(view -> {
            int currentImageResourceId = btSend.getCurrentImageResourceId();
            if (currentImageResourceId == R.mipmap.voice_ic) {
                if (!checkAudioPermission()) {
                    Utils.INSTANCE.sendAccessibilityCard(curContext, PermissionType.AUDIO);
                } else {
                    setRecording();
                    view.setTag("recording");
                    etUserInput.clearFocus();
                }
            } else if (currentImageResourceId == R.mipmap.send_btn) {
                sendState();
                String inputStr = etUserInput.getText().toString();
                if (!inputStr.isEmpty()) {
                    curAsrResult = etUserInput.getText().toString();
                    sendQuestion(curAsrResult);

                    Intent broadcastIntent = new Intent("com.skythinker.gptassistant.KEY_SPEECH_STOP");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                }
            } else if (currentImageResourceId == R.mipmap.cancel_btn) {
                defaultState();
            } else if (currentImageResourceId == R.mipmap.voiceing_ic) {
                if (realtimeAsr != null) {
                    realtimeAsr.release();
                }
                defaultState();
            }
        });

        etUserInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 在文本改变之前调用
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 在文本改变时调用
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 在文本改变之后调用
                String text = s.toString();
                if (!TextUtils.isEmpty(text) && btSend.getCurrentImageResourceId() == R.mipmap.voice_ic) {
                    btSend.setImageResource(R.mipmap.send_btn);
                } else if (TextUtils.isEmpty(text) && btSend.getCurrentImageResourceId() == R.mipmap.send_btn) {
                    etUserInput.setHint(R.string.text_input_hint);
                    btSend.setImageResource(R.mipmap.voice_ic);
                }
            }
        });

        // 历史按钮点击事件，跳转到历史记录页面
        (findViewById(R.id.history_btn)).setOnClickListener(view -> {
            pwMenu.dismiss();
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivityForResult(intent, 3);
        });

        // 新建对话按钮点击事件
        (findViewById(R.id.cv_new_chat)).setOnClickListener(view -> {
            HandlerLineTaskHelper.sessionID = "";
            clearChatListView();

            if (currentConversation != null &&
                    ((!multiChatList.isEmpty() && multiChatList.get(0).role != ChatRole.SYSTEM) || (multiChatList.size() > 1 && multiChatList.get(0).role == ChatRole.SYSTEM)) &&
                    GlobalDataHolder.getAutoSaveHistory()) // 包含有效对话则保存当前对话
                chatManager.addConversation(currentConversation);

            currentConversation = new Conversation();
            multiChatList = currentConversation.messages;
        });

        View menuView = LayoutInflater.from(this).inflate(R.layout.main_popup_menu, null);
        pwMenu = new PopupWindow(menuView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        pwMenu.setOutsideTouchable(true);

        (findViewById(R.id.cv_new_chat)).performClick(); // 初始化对话列表

        // 上方空白区域点击事件，退出程序
        (findViewById(R.id.view_bg_empty)).setOnClickListener(view -> {
            finish();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestPermission(); // 申请动态权限
        }

        // 初始化语音识别回调
        asrCallback = new AsrClientBase.IAsrCallback() {
            @Override
            public void onError(String msg) {
                if (tvGptReply != null) {
                    runOnUiThread(() -> tvGptReply.setText(getString(R.string.text_asr_error_prefix) + msg));
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.text_asr_error_prefix) + msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onResult(String result) {
                if (result != null) {
                    runOnUiThread(() -> etUserInput.setText(result));
                }
            }

            @Override
            public void onAutoStop() {
            }
        };

        // 设置本地广播接收器
        localReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case "com.skythinker.gptassistant.KEY_SPEECH_START":
                    etUserInput.setText("");
                    try {
                        toggleAsrRecognition();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    etUserInput.setHint(R.string.text_listening_hint);
                    break;
                case "com.skythinker.gptassistant.KEY_SPEECH_STOP":
                    etUserInput.setText("");
                    etUserInput.setHint(R.string.text_input_hint);

                    if (realtimeAsr != null) {
                        realtimeAsr.release();
                        realtimeAsr = null;
                    }

                    if (!Objects.equals(curAsrResult, "")) {
                        etUserInput.setHint(R.string.text_input_hint);
                        etUserInput.clearFocus();
                        if (curAsrResult.contains("餐厅")) {
                            Utils.INSTANCE.sendTripChatMessage(curContext, "正在理解用户需求，拆分任务", MsgType.LOADING, true);
                            IntentionApi.INSTANCE.createTrip(curAsrResult,
                                (TripCreateRes resp) -> {
                                    if (resp.getCode() == 200) {
                                        runOnUiThread(() -> {
                                            int taskId = resp.getData().getTaskId();
                                            webSocketClient = new WebSocketClient();
                                            webSocketClient.connectWebSocket(curContext, WEBSOCKET_URL + taskId);
                                        });
                                    } else {
                                        Utils.INSTANCE.sendTripChatMessage(curContext, "规划信息生成失败，请稍后重试", MsgType.ERROR, true);
                                    }
                                    return null;
                                },
                                (String error) -> {
                                    Utils.INSTANCE.sendTripChatMessage(curContext, error, MsgType.ERROR, true);
                                    return null;
                                }
                            );
                        } else {
                            Utils.INSTANCE.sendTripChatMessage(curContext, "正在为您加载...", MsgType.LOADING, true);
                            HandlerLineTaskHelper.INSTANCE.start(curContext, curAsrResult);
                        }

                        curAsrResult = "";
                        scrollChatAreaToBottom(curContext);
                    }
                    break;
                case "com.skythinker.gptassistant.KEY_SEND":
                    sendQuestion(null);
                    break;
                case "com.ai.multimodal.imageUriResponse":

                    String imageUriString = intent.getStringExtra("imageUri");
                    if (imageUriString != null) {
                        Uri imageUri = Uri.parse(imageUriString);
                        sendMultimodalQuestion(imageUri);
                        MultimodalAssistant multimodalAssistant = new MultimodalAssistant();
                        multimodalAssistant.conversationDrugInformationService(MainActivity.this, imageUri);
                    }
                    break;
                case "com.ai.multimodal.textResponse":

                    String textResponse = intent.getStringExtra("textResponse");
                    Utils.INSTANCE.sendTripChatMessage(MainActivity.this, Objects.requireNonNull(textResponse));
                    defaultState();
                    break;
                case "com.skythinker.gptassistant.complete":
                    defaultState();
                    break;
            }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.skythinker.gptassistant.KEY_SPEECH_START");
        intentFilter.addAction("com.skythinker.gptassistant.KEY_SPEECH_STOP");
        intentFilter.addAction("com.skythinker.gptassistant.KEY_SEND");
        intentFilter.addAction("com.skythinker.gptassistant.SHOW_KEYBOARD");
        intentFilter.addAction("com.ai.multimodal.imageUriResponse");
        intentFilter.addAction("com.ai.multimodal.textResponse");
        intentFilter.addAction("com.skythinker.gptassistant.complete");
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, intentFilter);

        Intent intentUrl = getIntent();
        Uri uri = intentUrl.getData();
        if (uri != null) {
            String agentStr = "打车去" + uri;
            sendQuestion(agentStr);
            boolean isBaseServiceEnable = AccessibilityApi.Companion.isBaseServiceEnable();
            if (!isBaseServiceEnable) {
                Utils.INSTANCE.sendAccessibilityCard(curContext, PermissionType.ACCESSIBILITY);
            } else {
                HandlerLineTaskHelper.INSTANCE.start(curContext, agentStr);
            }
        }
    }

    // 滚动聊天列表到底部
    private void scrollChatAreaToBottom(MainActivity context) {
        context.svChatArea.post(() -> {
            context.mResizableLinearLayout.restoreAutoHeight();
            int delta = context.svChatArea.getChildAt(0).getBottom()
                    - (context.svChatArea.getHeight() + context.svChatArea.getScrollY());
            if (delta != 0)
                context.svChatArea.smoothScrollBy(0, delta);
        });
    }

    // 添加一条聊天记录到聊天列表布局
    private LinearLayout addChatView(ChatRole role, String content, String imageBase64) {
        ViewGroup.MarginLayoutParams iconParams = new ViewGroup.MarginLayoutParams(dpToPx(30), dpToPx(30)); // 头像布局参数
        iconParams.setMargins(dpToPx(4), dpToPx(12), dpToPx(4), dpToPx(12));

        ViewGroup.MarginLayoutParams contentParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); // 内容布局参数
        contentParams.setMargins(dpToPx(4), dpToPx(15), dpToPx(4), dpToPx(15));

        LinearLayout.LayoutParams popupIconParams = new LinearLayout.LayoutParams(dpToPx(30), dpToPx(30)); // 弹出的操作按钮布局参数
        popupIconParams.setMargins(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(5));

        LinearLayout llOuter = new LinearLayout(this); // 包围整条聊天记录的最外层布局
        llOuter.setOrientation(LinearLayout.HORIZONTAL);

        // 根据角色设置布局参数
        if (role == ChatRole.ASSISTANT) {
            // AI消息在左侧，设置轻微背景色和左对齐
            llOuter.setGravity(Gravity.START);
        } else {
            // 用户消息在右侧，设置右对齐
            llOuter.setGravity(Gravity.END);
        }

        ImageView ivIcon = new ImageView(this); // 设置头像
        if (role == ChatRole.USER)
            ivIcon.setImageResource(R.drawable.chat_user_icon);
        else
            ivIcon.setImageResource(R.drawable.chat_gpt_icon);
        ivIcon.setLayoutParams(iconParams);

        TextView tvContent = new TextView(this); // 设置内容
        SpannableString spannableString = null;
        if (role == ChatRole.USER) {
            if (imageBase64 != null) { // 如有图片则在末尾添加ImageSpan
                spannableString = new SpannableString(content + "\n ");
                Bitmap bitmap = base64ToBitmap(imageBase64);
                int maxSize = dpToPx(120);
                bitmap = resizeBitmap(bitmap, maxSize, maxSize);
                ImageSpan imageSpan = new ImageSpan(this, bitmap);
                spannableString.setSpan(imageSpan, content.length() + 1, content.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                spannableString = new SpannableString(content);
            }
            tvContent.setText(spannableString);

            // 为用户消息添加气泡背景
            tvContent.setBackground(getDrawable(R.drawable.user_msg_bg));
            tvContent.setTextColor(Color.WHITE);
        } else {
            tvContent.setText(content);
            // 为AI消息添加气泡背景
            tvContent.setBackground(getDrawable(R.drawable.assistant_msg_bg));
            tvContent.setTextColor(Color.BLACK);
        }
        tvContent.setTextSize(16);
        tvContent.setLayoutParams(contentParams);
        tvContent.setTextIsSelectable(true);
        tvContent.setMovementMethod(LinkMovementMethod.getInstance());
        // 为消息内容添加内边距，使文本不贴着气泡边缘
        tvContent.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        LinearLayout llPopup = new LinearLayout(this); // 弹出按钮列表布局
        llPopup.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        PaintDrawable popupBackground = new PaintDrawable(Color.TRANSPARENT);
        llPopup.setBackground(popupBackground);
        llPopup.setOrientation(LinearLayout.HORIZONTAL);

        PopupWindow popupWindow = new PopupWindow(llPopup, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true); // 弹出窗口
        popupWindow.setOutsideTouchable(true);
        ivIcon.setTag(popupWindow); // 将弹出窗口绑定到头像上

        CardView cvDelete = new CardView(this); // 删除单条对话按钮
        cvDelete.setForeground(getDrawable(R.drawable.clear_btn));
        cvDelete.setOnClickListener(view -> {
            popupWindow.dismiss();
            ChatMessage chat = (ChatMessage) llOuter.getTag(); // 获取布局上绑定的聊天记录数据
            if (chat != null) {
                int index = multiChatList.indexOf(chat);
                multiChatList.remove(chat);
                while (--index > 0 && (multiChatList.get(index).role == ChatRole.FUNCTION
                        || multiChatList.get(index).functionName != null && multiChatList.get(index).functionName.equals("get_html_text"))) // 将上方联网数据也删除
                    multiChatList.remove(index);
            }
            if (tvContent == tvGptReply) { // 删除的是GPT正在回复的消息框，停止回复和TTS
            }
            llChatList.removeView(llOuter);
            if (llChatList.getChildCount() == 0) // 如果删除后聊天列表为空，则添加占位TextView
                clearChatListView();
        });
        llPopup.addView(cvDelete);

        CardView cvDelBelow = new CardView(this); // 删除下方所有对话按钮
        cvDelBelow.setForeground(getDrawable(R.drawable.del_below_btn));
        cvDelBelow.setOnClickListener(view -> {
            popupWindow.dismiss();
            int index = llChatList.indexOfChild(llOuter);
            while (llChatList.getChildCount() > index && llChatList.getChildAt(0) instanceof LinearLayout) { // 模拟点击各条记录的删除按钮
                PopupWindow pw = (PopupWindow) ((LinearLayout) llChatList.getChildAt(llChatList.getChildCount() - 1)).getChildAt(0).getTag();
                ((LinearLayout) pw.getContentView()).getChildAt(0).performClick();
            }
        });
        llPopup.addView(cvDelBelow);

        if (role == ChatRole.USER) { // USER角色才有的按钮
            CardView cvEdit = new CardView(this); // 编辑按钮
            cvEdit.setForeground(getDrawable(R.drawable.edit_btn));
            cvEdit.setOnClickListener(view -> {
                popupWindow.dismiss();
                ChatMessage chat = (ChatMessage) llOuter.getTag(); // 获取布局上绑定的聊天记录数据
                String text = chat.contentText;
                if (chat.contentImageBase64 != null) { // 若含有图片则设置为选中的图片
                    if (text.endsWith("\n "))
                        text = text.substring(0, text.length() - 2);
                    selectedImageBitmap = base64ToBitmap(chat.contentImageBase64);
                    btImage.setImageResource(R.drawable.image_enabled);
                } else {
                    selectedImageBitmap = null;
                    btImage.setImageResource(R.mipmap.image);
                }
                etUserInput.setText(text); // 添加文本内容到输入框
                cvDelBelow.performClick(); // 删除下方所有对话
            });
            llPopup.addView(cvEdit);
        }

        CardView cvCopy = new CardView(this); // 复制按钮
        cvCopy.setForeground(getDrawable(R.drawable.copy_btn));
        cvCopy.setOnClickListener(view -> { // 复制文本内容到剪贴板
            popupWindow.dismiss();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("chat", tvContent.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.toast_clipboard, Toast.LENGTH_SHORT).show();
        });
        llPopup.addView(cvCopy);

        for (int i = 0; i < llPopup.getChildCount(); i++) { // 设置弹出按钮的样式
            CardView cvBtn = (CardView) llPopup.getChildAt(i);
            cvBtn.setLayoutParams(popupIconParams);
            cvBtn.setCardBackgroundColor(Color.WHITE);
            cvBtn.setRadius(dpToPx(5));
        }

        // 根据角色决定弹出位置和子视图的添加顺序
        if (role == ChatRole.ASSISTANT) {
            // AI消息：头像在左，内容在右
            ivIcon.setOnClickListener(view -> { // 点击头像时弹出操作按钮
                popupWindow.showAsDropDown(view, dpToPx(30), -dpToPx(35));
            });
            // llOuter.addView(ivIcon);
            llOuter.addView(tvContent);
        } else {
            // 用户消息：内容在左，头像在右
            ivIcon.setOnClickListener(view -> { // 点击头像时弹出操作按钮
                popupWindow.showAsDropDown(view, -dpToPx(120), -dpToPx(35));
            });
            llOuter.addView(tvContent);
            // llOuter.addView(ivIcon);
        }

        llChatList.addView(llOuter);

        return llOuter;
    }

    // 等比缩放Bitmap到给定的尺寸范围内
    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = 1;
        if (width > maxWidth || height > maxHeight)
            scale = Math.min((float) maxWidth / width, (float) maxHeight / height);
        return Bitmap.createScaledBitmap(bitmap, (int) (width * scale), (int) (height * scale), true);
    }

    // 将Base64编码转换为Bitmap
    private Bitmap base64ToBitmap(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // 发送一个提问，input为null时则从输入框获取
    private void sendQuestion(String input) {

        boolean isMultiChat = true;

        if (!isMultiChat) {
            findViewById(R.id.cv_new_chat).performClick();
        }

        // 处理提问文本内容
        String userInput = (input == null) ? etUserInput.getText().toString() : input;

        if (llChatList.getChildCount() > 0 && llChatList.getChildAt(0) instanceof TextView) { // 若有占位TextView则删除
            llChatList.removeViewAt(0);
        }

        // 添加对话布局
        LinearLayout llInput = addChatView(ChatRole.USER, userInput, null);
        Utils.INSTANCE.sendTripChatMessage(curContext, getString(R.string.text_waiting_reply), MsgType.LOADING, false);

        if (!multiChatList.isEmpty()) {
            llInput.setTag(multiChatList.get(multiChatList.size() - 1));
        } else {
            ChatMessage chatMessage = new ChatMessage(ChatRole.USER);
            //chatMessage.setText(userInput);
            llInput.setTag(chatMessage);
        }


        btImage.setImageResource(R.mipmap.image);
        selectedImageBitmap = null;
        btSend.setImageResource(R.mipmap.cancel_btn);

    }

    private void sendMultimodalQuestion(Uri uri) {

        boolean isMultiChat = true;

        if (!isMultiChat) {
            findViewById(R.id.cv_new_chat).performClick();
        }
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (bitmap == null) return;

        String imageBase64 = ImageUtils.getBase64FromImageUri(this.getApplicationContext(), uri);

        // 处理提问文本内容
        /*String userInput = (uri == null) ? etUserInput.getText().toString() : uri;*/

        if (llChatList.getChildCount() > 0 && llChatList.getChildAt(0) instanceof TextView) { // 若有占位TextView则删除
            llChatList.removeViewAt(0);
        }

        // 添加对话布局
        LinearLayout llInput = addChatView(ChatRole.USER, "", imageBase64);
        Utils.INSTANCE.sendTripChatMessage(curContext, getString(R.string.text_waiting_reply), MsgType.LOADING, false);

        if (!multiChatList.isEmpty()) {
            llInput.setTag(multiChatList.get(multiChatList.size() - 1));
        } else {
            ChatMessage chatMessage = new ChatMessage(ChatRole.USER);
            //chatMessage.setText(userInput);
            llInput.setTag(chatMessage);
        }

        scrollChatAreaToBottom(curContext);

        btImage.setImageResource(R.mipmap.image);
        selectedImageBitmap = null;
        btSend.setImageResource(R.mipmap.cancel_btn);

    }

    // 清空聊天界面
    private void clearChatListView() {
        llChatList.removeAllViews();

        TextView tv = new TextView(this); // 清空列表后添加一个占位TextView
        tv.setTextColor(Color.parseColor("#000000"));
        tv.setTextSize(15);
        tv.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        tv.setText(R.string.default_greeting);
        //btSend.setImageResource(R.mipmap.send_btn);
        defaultState();
        tvGptReply = tv;
        llChatList.addView(tv);
    }

    // 处理启动Intent
    private void handleShareIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (Intent.ACTION_PROCESS_TEXT.equals(action)) {
                String text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT);
                if (text != null) {
                    etUserInput.setText(text);
                }
            } else if (Intent.ACTION_SEND.equals(action)) { // 分享图片

            }
        }
    }

    private void close() {
    }

    /**
     * 开始识别
     */
    private void start() {
        if (realtimeAsr == null) {
            realtimeAsr = (ASRIntelligentDialogue) AIAssistantManager.Companion.getInstance().asrIntelligentDialogueHelp();
        }

        realtimeAsr.setListener(new ASRIntelligentDialogue.RealtimeAsrListener() {
            String img = null;

            @Override
            public void onAsrMidResult(@NonNull String text) {
                runOnUiThread(() -> {
                    etUserInput.setText(text);
                });
            }

            @Override
            public void onAsrFinalResult(@NonNull String text) {
                runOnUiThread(() -> {
                    if (!text.isEmpty()) {
                        img = null;
                        Log.d("curAsrResult", text);
                        curAsrResult = text;
                        sendQuestion(curAsrResult);

                        etUserInput.setText("");
                        etUserInput.setHint(R.string.text_input_hint);

                        if (realtimeAsr != null) {
                            realtimeAsr.release();
                            realtimeAsr = null;
                        }
                        Intent broadcastIntent = new Intent("com.skythinker.gptassistant.KEY_SPEECH_STOP");
                        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(broadcastIntent);
                        defaultState();
                    }
                });
            }

            @SuppressLint({"CheckResult", "ResourceType"})
            public void onDialogueResult(@NonNull DialogueResult result) {
                Log.d("onDialogueResult", String.valueOf(result));
            }

            @Override
            public void onError(int code, @NonNull String message) {
                Timber.tag("onError").e("onError%s", message);
                close();
            }

            @Override
            public void onComplete() {
                close();
            }
        });
        realtimeAsr.startRecognition(this.getBaseContext());
    }

    // 转换dp为px
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, curContext.getResources().getDisplayMetrics());
    }

    // 申请动态权限
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void requestPermission() {
        String[] permissions = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            requestPermissions(toApplyList.toArray(tmpList), 123);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
    }

    @Override
    public void onStart() {
        super.onStart();
        scrollChatAreaToBottom(curContext);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        etUserInput.clearFocus();
        etUserInput.setText("");
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
        if (((!multiChatList.isEmpty() && multiChatList.get(0).role != ChatRole.SYSTEM) || (multiChatList.size() > 1 && multiChatList.get(0).role == ChatRole.SYSTEM)) &&
                GlobalDataHolder.getAutoSaveHistory()) // 包含有效对话则保存当前对话
            chatManager.addConversation(currentConversation);
        chatManager.removeEmptyConversations();
        chatManager.destroy();
        if(webSocketClient != null) {
            webSocketClient.closeConnection();
        }
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
    }

    /**
     * android 6.0 以上需要动态申请权限
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void initPermission() {

        String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        };
        ArrayList<String> toApplyList = new ArrayList<>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), REQUEST_PERMISSION);
        }
    }


    public void requestOverlayPermission() {
        if (!Settings.canDrawOverlays(curContext)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + curContext.getPackageName()));
            curContext.startActivity(intent);
        }
    }

    private void toggleAsrRecognition() throws IOException {
        if (realtimeAsr != null) {
            realtimeAsr.release();  // 需要添加这个方法
            realtimeAsr = null;
        }
        start();
    }

    /**
     * 麦克风权限是否开启
     */
    public  boolean checkAudioPermission(){
        return ContextCompat.checkSelfPermission(curContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 添加回复信息
     * @param context 上下文
     * @param text 信息文本
     * @param type 信息类型
     * @param isDelPre 是否删除上一次回复内容
     * @param flowTotalTime 流式输出的总时间（为0时不启用流式）
     */
    @SuppressLint("WrongViewCast")
    public void sendTripChatMessage(MainActivity context, String text, MsgType type, Boolean isDelPre, int flowTotalTime) {
        if (context.llChatList.getChildCount() > 0 && context.llChatList.getChildAt(0) instanceof TextView) {
            context.llChatList.removeViewAt(0);
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        View newView = inflater.inflate(R.layout.tab_conf_list_item, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        newView.setLayoutParams(params);
        newView.setId(View.generateViewId());
        newView.setBackground(context.getDrawable(R.drawable.assistant_msg_bg));
        TextView input = newView.findViewById(R.id.tv_list_item_title);

        if (isDelPre) {
            ViewGroup firstChild = (ViewGroup) context.llChatList.getChildAt(context.llChatList.getChildCount() - 1);
            context.llChatList.removeView(firstChild);
        }

        if (flowTotalTime != 0) {
            input.setVisibility(View.GONE);
            int inter = 300;
            TextSwitcher textSwitcher;
            List<String> messages = new ArrayList<>();
            AtomicInteger currentIndex = new AtomicInteger();
            final Handler handler = new Handler();
            int exeNum =  flowTotalTime / inter;
            for(int i = 0; i < exeNum; i++) {
                if(i == exeNum - 1) {
                    messages.add(text);
                } else {
                    messages.add(text.substring(0, (i + 1) * (text.length() / exeNum)));
                }
            }

            textSwitcher = newView.findViewById(R.id.tv_list_item_title_view);
            textSwitcher.setVisibility(View.VISIBLE);
            textSwitcher.setFactory(() -> {
                TextView textView = new TextView(context);
                textView.setTextColor(Color.parseColor("#000000"));
                textView.setTextSize(16);
                return textView;
             });

            typingRunnable = () -> {
                if (currentIndex.get() < messages.size()) {
                    textSwitcher.setText("");
                    textSwitcher.setText(messages.get(currentIndex.getAndIncrement()));
                    handler.postDelayed(typingRunnable, inter);
                } else {
                    currentIndex.set(0);
                }
            };
            handler.postDelayed(typingRunnable, inter);
        } else {
            input.setText(text);
        }

        ImageView sucImg = newView.findViewById(R.id.imageSucView);
        ImageView errImg = newView.findViewById(R.id.imageErrView);
        loadingLottieAnimationView = newView.findViewById(R.id.lottieAnimationLoadingView);

        if (type == MsgType.SUC) {
            sucImg.setVisibility(View.VISIBLE);
        } else if (type == MsgType.ERROR) {
            errImg.setVisibility(View.VISIBLE);
        } else if (type == MsgType.LOADING) {
            loadingLottieAnimationView.setVisibility(View.VISIBLE);
            loadingLottieAnimationView.playAnimation();
        } else if (type == MsgType.COMMON) {
            sucImg.setVisibility(View.GONE);
            errImg.setVisibility(View.GONE);
            loadingLottieAnimationView.setVisibility(View.GONE);
        }

        context.llChatList.addView(newView);
        scrollChatAreaToBottom(context);

        Intent broadcastIntent = new Intent("com.skythinker.gptassistant.complete");
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    /**
     * 生成行程规划case餐厅列表卡片
     * @param context 上下文
     * @param resultInfo 服务端返回的餐厅数据
     * @param resultUrl H5模板地址Url
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void sendTripCard(MainActivity context, PoiSearchResultContent resultInfo, String resultUrl) {
        ArrayList<PoiItem> tripList = resultInfo.getPoi_list();
        LinearLayout tripContent = new LinearLayout(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 30);
        tripContent.setLayoutParams(params);
        tripContent.setOrientation(LinearLayout.VERTICAL);
        tripContent.setBackground(context.getDrawable(R.drawable.assistant_msg_bg));
        tripContent.setPadding(0,40,0,40);
        context.llChatList.addView(tripContent);

        final Handler handler = new Handler();

        for (int i = 0; i < tripList.size(); i++) {
            final int index = i;
            handler.postDelayed(() -> {
                LayoutInflater inflater = LayoutInflater.from(context);
                View newView = inflater.inflate(R.layout.trip_list_item, null);
                newView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                String imgUrl = "";
                PoiItem curItem = tripList.get(index);
                String address = curItem.getAddress();
                String title = curItem.getName();

                int imgNum = curItem.getPoi_photos().size();
                if (imgNum > 0) {
                    imgUrl = curItem.getPoi_photos().get(0);
                }

                String busTime = curItem.getOpentime_week();
                String score = curItem.getRating();
                String tag = curItem.getType();

                newView.setOnClickListener(view -> {
                    Intent intent = new Intent(context, H5TripPlanActivity.class);
                    intent.putExtra("webUrl", resultUrl);
                    context.startActivityForResult(intent, 3);
                });

                ImageView imgView = newView.findViewById(R.id.trip_item_img);
                TextView titleView = newView.findViewById(R.id.trip_item_title);
                TextView busTimeView = newView.findViewById(R.id.trip_item_bus_time);
                TextView addressView = newView.findViewById(R.id.trip_item_address);
                TextView scoreView = newView.findViewById(R.id.trip_item_score);
                TextView recommendReason = newView.findViewById(R.id.trip_recommend_reason);
                recommendReason.setText(curItem.getRecommend_reason());

                if (!Objects.equals(imgUrl, "")) {
                    Picasso.get().load(imgUrl).into(imgView);
                } else {
                    imgView.setImageResource(R.drawable.rest);
                }
                titleView.setText(title);
                busTimeView.setText(busTime.isEmpty() ? "周一至周日 09:00-21:30" : busTime);
                addressView.setText(address);
                scoreView.setText(score + "分");
                String[] tags = tag.split(";");

                TextView tag1 = newView.findViewById(R.id.trip_item_tag1);
                TextView tag2 = newView.findViewById(R.id.trip_item_tag2);
                TextView tag3 = newView.findViewById(R.id.trip_item_tag3);

                for(int j = 0; j < tags.length; j++) {
                    if(j == 0) tag1.setText(tags[0]);
                    if(j == 1) tag2.setText(tags[1]);
                    if(j == 2) tag3.setText(tags[2]);
                }

                View line = newView.findViewById(R.id.trip_item_line);
                newView.setPadding(40,20,30,20);

                if(index == tripList.size() - 1) {
                    line.setVisibility(View.GONE);
                }
                tripContent.addView(newView);
                scrollChatAreaToBottom(context);

            },  i * 300L);
        }
    }

    /**
     * 生成权限开启提示卡片
     * @param context 上下文
     */
    public void sendAccessibilityCard(MainActivity context, PermissionType type, Boolean isDelPre ) {

        if (context.llChatList.getChildCount() > 0 && context.llChatList.getChildAt(0) instanceof TextView) {
            context.llChatList.removeViewAt(0);
        }

        if (isDelPre && context.llChatList.getChildCount() > 0 && context.llChatList.getChildAt(0) instanceof ViewGroup) {
            ViewGroup firstChild = (ViewGroup) context.llChatList.getChildAt(context.llChatList.getChildCount() - 1);
            context.llChatList.removeView(firstChild);
        }

        LinearLayout modelContent = new LinearLayout(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 20, 0, 30);
        modelContent.setLayoutParams(params);
        modelContent.setOrientation(LinearLayout.VERTICAL);
        modelContent.setBackground(context.getDrawable(R.drawable.assistant_msg_bg));
        modelContent.setPadding(0,20,0,20);

        LayoutInflater inflater = LayoutInflater.from(context);
        View newView;

        if (type == PermissionType.ACCESSIBILITY) {
            newView = inflater.inflate(R.layout.accessibility_tip, null);
        } else if(type == PermissionType.AUDIO) {
            newView = inflater.inflate(R.layout.audio_tip, null);
        } else if(type == PermissionType.FLOAT) {
            newView = inflater.inflate(R.layout.float_tip, null);
        } else {
            newView = null;
        }

        assert newView != null;
        newView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        TextView jumpBtn = newView.findViewById(R.id.go_open);
        TextView ignoreBtn = newView.findViewById(R.id.go_ignore);

        jumpBtn.setOnClickListener(view -> {
            Intent intent;
            if (type == PermissionType.ACCESSIBILITY) {
                intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            } else if(type == PermissionType.FLOAT) {
                intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            } else {
                intent = new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS);
            }
            context.startActivity(intent);
        });

        if (ignoreBtn != null) {
            ignoreBtn.setOnClickListener(view -> context.llChatList.removeView(modelContent));
        }

        newView.setPadding(20,20,20,20);

        modelContent.addView(newView);
        context.llChatList.addView(modelContent);
        scrollChatAreaToBottom(context);
    }
}