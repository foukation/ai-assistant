package com.cmdc.ai.assist.api

import com.starryos.deviceconnect.DeviceInputInfo
import com.starryos.deviceconnect.DeviceManager
import com.starryos.deviceconnect.DeviceManagerCallback
import com.starryos.deviceconnect.DeviceManagerMsg
import com.starryos.deviceconnect.DeviceOutputInfo
import com.starryos.deviceconnect.DeviceUploadInfo
import com.starryos.deviceconnect.DmActionCallback

/**
 * 该类负责管理与设备的连接状态和数据传输。
 * 主要包括建立连接、断开连接、发送数据和接收数据等功能。
 * */
class DeviceConnect() {

    private val deviceManager = DeviceManager()
    /*private var deviceManager: DeviceManager? = null

    fun createDeviceManager() {
        deviceManager = DeviceManager()
    }*/

    /**
     * 初始化 SDK ⽇志机制，使⽤指定的参数。
     *
     * @param bufSize ⽇志缓冲区的⼤⼩。
     * @param filePath ⽇志⽂件的存储路径。
     * @param maxFileSize ⽇志⽂件的最⼤⼤⼩。
     * @param devUniqueId 设备的唯⼀标识符。
     *
     * 该⽅法调⽤本地⽅法 `native_sdkLog_init` 来设置⽇志记录。
     * 请确保在调⽤此⽅法之前加载了本地库。
     */
    fun sdkLogInit(bufSize: Int, filePath: String, maxFileSize: Int, devUniqueId: String) {
        return deviceManager.sdkLogInit(bufSize, filePath, maxFileSize, devUniqueId)
    }

    /**
     * 设置边缘缓存⽂件的路径。
     *
     * @param path 边缘缓存⽂件的存储路径。
     *
     * 该⽅法调⽤本地⽅法 `native_set_edge_cache_file_path` 来设置缓存⽂件的路径。
     * 请确保在调⽤此⽅法之前，已加载了必要的本地库。
     */
    fun setEdgeCacheFilePath(path: String?) {
        return deviceManager.setEdgeCacheFilePath(path)
    }

    /**
     * 获取设备信息。
     *
     * @param hostAddr 设备的主机地址。
     * @param inputInfo 包含输⼊信息的 DeviceInputInfo 对象。
     * @return 包含设备信息的 DeviceOutputInfo 对象。
     *
     * 该⽅法根据提供的主机地址和输⼊信息获取设备的相关信息。
     * 请确保在调⽤此⽅法之前，已正确设置设备的连接和输⼊参数。
     */
    fun getDeviceInfo(hostAddr: String?, inputInfo: DeviceInputInfo?): DeviceOutputInfo {
        return deviceManager.getDeviceInfo(hostAddr, inputInfo)
    }

    /**
     * 初始化设备管理器。
     *
     * @param productId 产品ID，⽤于标识产品。
     * @param deviceId 设备ID，⽤于标识设备。
     * @param token 认证令牌，⽤于验证设备的身份。
     * @param cloudCaPath 云CA证书路径，⽤于安全连接到云端。
     * @param cloudCertificatePath 云证书路径，⽤于设备身份验证。
     * @param cloudKeyPath 云私钥路径，⽤于安全连接到云端。
     * @param cloudKeyPw 云私钥密码，⽤于解密云私钥。
     * @param listener 设备管理回调接⼝，⽤于处理设备管理相关的回调。
     * @return 初始化结果的状态码。0 表示成功，其他值表示失败。
     *
     * 该⽅法初始化设备管理器，设置必要的认证信息和回调接⼝。
     * 请确保在调⽤此⽅法之前，已正确配置所有参数。
     */
    fun init(
        productId: String?,
        deviceId: String?,
        token: String?,
        cloudCaPath: String?,
        cloudCertificatePath: String?,
        cloudKeyPath: String?,
        cloudKeyPw: String?,
        listener: DeviceManagerCallback?
    ): Int {
        return deviceManager.init(
            productId,
            deviceId,
            token,
            cloudCaPath,
            cloudCertificatePath,
            cloudKeyPath,
            cloudKeyPw,
            listener
        )
    }

    /**
     * 连接所有设备。
     *
     * @param uploadInfo 包含设备上传信息的 DeviceUploadInfo 对象。
     * @param callback 设备管理操作的回调接⼝，⽤于处理连接结果。
     *
     * 该⽅法调⽤本地⽅法 `native_connect_all` 来连接所有设备，传递必要的设备信息和⽹络参数。
     * 请确保在调⽤此⽅法之前，已正确设置所有上传信息参数。
     */
    fun connectAll(uploadInfo: DeviceUploadInfo?, callback: DmActionCallback?) {
        return deviceManager.connectAll(uploadInfo, callback)
    }

    /**
     * 上传设备管理消息。
     *
     * @param provisionType 配置类型，⽤于指定上传的配置类别。
     * @param message 包含设备管理信息的 DeviceManagerMsg 对象。
     * @param callback 设备管理操作的回调接⼝，⽤于处理上传结果。
     *
     * 该⽅法根据指定的配置类型和设备管理信息，调⽤相关逻辑进⾏消息上传。
     * 请确保在调⽤此⽅法之前，已正确配置所有参数。
     */
    fun upload(
        provisionType: Int,
        message: DeviceManagerMsg?,
        callback: DmActionCallback?
    ) {
        return deviceManager.upload(provisionType, message, callback)
    }

    /**
     * 断开所有设备的连接。
     *
     * @param callback 设备管理操作的回调接⼝，⽤于处理断开连接的结果。
     *
     * 该⽅法调⽤本地⽅法 `native_disconnect_all` 来断开所有设备的连接。
     * 请确保在调⽤此⽅法之前，已正确设置回调接⼝，以处理断开连接后的结果。
     */
    fun disconnectAll(callback: DmActionCallback?) {
        return deviceManager.disconnectAll(callback)
    }

    /**
     * 销毁 SDK ⽇志记录机制。
     *
     * 该⽅法调⽤本地⽅法 `native_sdkLog_destroy` 来销毁 SDK 的⽇志记录机制，释放相关资源。
     * 请确保在调⽤此⽅法之前，已经完成了所有⽇志的记录，并且不再需要记录新的⽇志。
     */
    fun sdkLogDestroy() {
        return deviceManager.sdkLogDestroy()
    }
}