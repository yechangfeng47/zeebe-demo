
安装包安装
Es 下载安装
1、下载7.16.2版本，选择服务器对应版本，下载安装。链接地址：
https://www.elastic.co/cn/downloads/past-releases/elasticsearch-7-16-2
 
2、找到解压安装包位置，修改elasticsearch.yml配置。
（1）cd /usr/local/elasticsearch-7.16.2/config 
（2）vim elasticsearch.yml





注意：es 启动需要非root 账户
（1）授权新用户,并切换到该用户启动。参考博客：             https://blog.csdn.net/qq_36666651/article/details/81285391
（2）若启动报：max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]
需设大内核，参考博客：https://yc-ma.blog.csdn.net/article/details/102720396


  Zeebe 下载安装
1、检查java 版本java -version,需要jdk11 以上版本
https://www.oracle.com/java/technologies/downloads/#java11
Linux 下JDK卸载与安装
https://blog.csdn.net/jx_lihuifu/article/details/80761038      

2、在https://github.com/camunda-cloud/zeebe 地址下,点击releases 

选择安装包点击下: camunda-cloud-zeebe-1.3.1.tar.gz

2、下载hazelcast，下载地址：
https://github.com/camunda-community-hub/zeebe-hazelcast-exporter/releases
在zeebe安装包下，新建文件夹“exporters”  将安装包放入该文件夹下。


3、修改配置
（1）cd /usr/local/zeebe/config
（2）vim application.yaml
（3） exporters:
          elasticsearch:
           className: io.camunda.zeebe.exporter.ElasticsearchExporter
                args:
 url: http://127.0.0.1:9200
      hazelcast:
        className: io.zeebe.hazelcast.exporter.HazelcastExporter
        jarPath: exporters/zeebe-hazelcast-exporter-1.1.0-jar-with-dependencies.jar


 若启动报 Could not create the Java Virtual Machine 错误：
  cd /usr/local/zeebe/bin  vim broker
  删除：--illegal-access=deny




   


  operate下载安装
   1、下载地址：https://github.com/camunda-cloud/zeebe/releases
 2、修改配置：
    （1）cd /usr/local/operate/config
    （2）vim application.yml 
 

zeebe-simple-monitor下载，运行：
https://github.com/camunda-community-hub/zeebe-simple-monitor/releases


Github 下载项目，测试连接：https://github.com/yechangfeng47/zeebe-demo.git




参考博客：https://blog.csdn.net/qq_22606825/article/details/104608522
官方文档1：https://docs.camunda.io/docs/self-managed/overview/
官方文档2：https://docs.camunda.io/docs/components/zeebe/zeebe-overview/   
参考启动项目：https://gitee.com/tanwubo/zeebe-quickstart-demo
参考启动项目2:  https://github.com/camunda-community-hub/zeebe-docker-compose
 




Docker 安装：
Docker 重启命令
systemctl restart docker.service

Docker运行Zeebe：
docker run --name zeebe -p 26500:26500 camunda/zeebe:latest

进入 zeebe 容器：
docker exec -it zeebe /bin/bash

Docker 复制文件：
docker cp /root/order-process.bpmn 2a4778847091:/usr/local/zeebe/order-process.bpmn


docker run --name operate -p 8080:8080 camunda/operate:latest
