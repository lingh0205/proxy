### HTTP代理服务器
    支持HTTP、HTTPS、WebSocket,HTTPS采用动态签发SSL证书,可以拦截http、https的报文并进行处理。
    例如：http(s)协议抓包,http(s)动态替换请求内容或响应内容等等。
#### HTTPS支持
    需要导入项目中的CA证书(src/resources/ca.crt)至受信任的根证书颁发机构。
    可以使用CertDownIntercept拦截器，开启网页下载证书功能，访问http://serverIP:serverPort即可进入。
    注：安卓手机上安装证书若弹出键入凭据存储的密码，输入锁屏密码即可。
#### 二级代理
    可设置二级代理服务器,支持http,socks4,socks5。
#### 启动

#### 流程
SSL握手

![SSL握手](https://sfault-image.b0.upaiyun.com/751/727/751727588-59ccbe3293bef_articlex)

HTTP通讯

![HTTP通讯](https://sfault-image.b0.upaiyun.com/114/487/1144878844-59ccbe42037b6_articlex)

### 配置方式
#### 默认参数方式
默认方式启动，代理将工作在1080端口，并且由于mongodb相关配置未修改，请求数据无法入库

#### 自定义参数
通过修改config.yml中的record属性为true，可以启用请求数据入库功能，所有的流量数据将存储在config.yml中配置的mongodb中，请正确配置mongodb的连接信息，各参数定义如下：
- record : 
    - true -> 流量入库
    - false -> 流量不入库
- host : mongodb 服务器ip
- port : mongodb 服务开放端口
- user : mongodb 登录用户
- passwd : mongodb 登录密码
- database : 流量库库名
- isAuth : 是否启用mongodb认证，若配置为false，则 user/passwd 配置不生效

config.yml 中 ignore 部分可以配置后缀名和content-type过滤，过滤后的请求数据不会入库 