package com.astatus.easysocketlan.entity;


/**
 * Created by Administrator on 2017/10/22.
 */

public class ClientDeviceEntity {

    private String name = "";
    private String ip = "";

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private int port = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getId(){
        return getIp() + '_' + getPort();
    }
}
