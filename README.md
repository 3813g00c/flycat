# flycat
### （本项目仅供学习交流使用，严禁用于任何其他用途！）
使用Java基于Netty+Socks5+TLS实现的代理服务

做这个项目主要是为了学习Netty的相关知识，目前暂不支持与原始Trojan协议兼容，故无法再手机上使用，后续版本会兼容Trojan。
由于刚开始，目前支持的功能较少，后续根据情况更新

## 速度测试

测试选择了一台韩国的vps，可以看twitch直播，YouTube2k视频也没什么压力，但是实际影响速度的主要还是服务器的位置和vps提供商，后续会先增加CDN流量中转功能提升速度

## 当前版本支持
* TCP代理
* TLS/SSL隧道传输
* 防范GFW被动/主动监测机制

## 后续计划
* CDN流量中转
* UDP代理
* 兼容原始Trojan协议，以使用iOS/Android代理客户端
* 流量统计
* 用户管理

## 使用方法
1. clone项目，本地编译，使用Maven打包客户端和服务端
2. 生成客户端和服务端的SSL证书
3. 购买vps，部署服务端，推荐使用Docker部署，方便快捷，部署完成之后安装BBR加速（安装方法自行百度）
4. 在本地运行客户端（jar包）

## 配置文件

服务端：
```yaml
server:
  port: 8081

netty:
  run-type: server
  local-addr: 0.0.0.0  # 代理服务器地址
  local-port: 443  # 代理服务器端口（推荐443）
  remote-addr: 127.0.0.1  
  remote-port: 8081  # 本地的web服务器端口
  password: 123456
```

客户端：
```yaml
server:
  port: 8080

netty:
  run-type: client
  local-addr: 127.0.0.1
  local-port: 1080
  remote-addr: # 你自己的服务器地址
  remote-port: 443
  password: test132989

```

## GFW主动监测防御原理

当一个连接试图连接服务器监听的端口时：

* 如果TLS握手成功，服务器会验证密码，如果未携带密码或者密码验证不通过，服务器会将连接代理到本地的web服务器端口上，此时远端看来，服务器就是一个正常的HTTPS网站

* 如果TLS握手失败，会直接断开连接，和其他的HTTPS服务器一样

* 如果密码验证通过，则返回代理成功的响应，后续会将客户端的所有请求直接发送给真正的服务器

## 为什么集成了SpringBoot

刚开始设计的时候准备做一个管理界面、提供用户管理、服务器监测、速度监测等功能，服务端的SpringBoot服务被当做墙主动监测时提供的真实web服务，由于代理使用了TLS，你也可以直接将服务端当作你的HTTPS服务器，用来给你的网站提供HTTPS服务，访客可以正常地通过代理服务浏览你的网站，而和代理流量互不影响。
