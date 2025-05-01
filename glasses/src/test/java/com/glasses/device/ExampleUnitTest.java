package com.glasses.device;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.cmdc.ai.assist.constraint.AgentServeReq;
import com.cmdc.ai.assist.constraint.ChatModelReq;
import com.cmdc.ai.assist.constraint.ChatMessage;
import com.cmdc.ai.assist.api.SmartHelper;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import kotlin.Unit;

public class ExampleUnitTest {
    private SmartHelper smartHelper = new SmartHelper();

    @Test
    public void testGetGateWay() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);

        // 创建请求参数
        AgentServeReq request = new AgentServeReq(
                "your_secret",      // 密钥
                "your_vendor",      // 厂商标识
                "your_model",       // 设备型号
                "your_sn",         // 设备唯一标识
                "your_mac",        // 设备MAC地址
                "your_ip",         // 设备IP地址
                "your_version"     // 设备版本号
        );

        smartHelper.getGateWay(
                request,
                response -> {
                    System.out.println("Gateway onSuccess callback triggered");
                    System.out.println("Response: " + response);
                    // 验证响应不为空
                    assertNotNull(response);
                    // 验证token不为空
                    assertNotNull(response.getToken());
                    latch.countDown();
                    return Unit.INSTANCE;
                },
                error -> {
                    System.out.println("Gateway onError callback triggered");
                    System.out.println("Error message: " + error);
                    fail("Gateway request failed: " + error);
                    latch.countDown();
                    return Unit.INSTANCE;
                }
        );

        // 等待异步操作完成，最多等待10秒
        System.out.println("Waiting for gateway request to complete...");
        if (!latch.await(30000, TimeUnit.SECONDS)) {
            fail("Gateway request timed out after 10 seconds");
        }

    }

    @Test
    public void testGetModelsAvail() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        smartHelper.getModelsAvail(
                response -> {
                    // 验证响应不为空
                    assertNotNull(response);
                    success[0] = true;
                    latch.countDown();
                    return Unit.INSTANCE;
                },
                error -> {
                    fail("Models available request failed: " + error);
                    latch.countDown();
                    return Unit.INSTANCE;
                }
        );

        latch.await(10, TimeUnit.SECONDS);
        assertTrue("Models available request should succeed", success[0]);
    }

    @Test
    public void testChatModel() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        // 创建聊天消息
        ChatMessage[] messages = new ChatMessage[]{
                new ChatMessage("user", "你好，请问现在几点了？")
        };

        // 创建聊天请求
        ChatModelReq chatRequest = new ChatModelReq(
                "jiutian-lan",    // model
                messages,         // messages
                false,           // stream
                0.75,           // top_p
                10              // top_k
        );

        smartHelper.chatModel(
                new ChatModelReq[]{chatRequest},
                response -> {
                    // 验证响应不为空
                    assertNotNull(response);
                    success[0] = true;
                    latch.countDown();
                    return Unit.INSTANCE;
                },
                error -> {
                    fail("Chat model request failed: " + error);
                    latch.countDown();
                    return Unit.INSTANCE;
                }
        );

        latch.await(10, TimeUnit.SECONDS);
        assertTrue("Chat model request should succeed", success[0]);
    }
}
