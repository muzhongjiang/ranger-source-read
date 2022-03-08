
apache ranger-release-ranger-2.2.0


#Build Process  


1. Check out the code from GIT repository

2. 执行命令（如果出错，多试几次）:
```shell
mvn clean compile package install  -Drat.skip=true -DskipTests  -X
#mvn clean compile package install assembly:assembly  -Drat.skip=true -DskipTests 

```
   (Ranger Admin UI tests depend on PhantomJS. If the build fails with npm or Karma errors you can either
      i. install PhantomJS dependencies for your platform (bzip2 and fontconfig)
     ii. skip JavaScript test execution: mvn -DskipJSTests ...)

3. target目录下:

   ranger-<version>-admin.tar.gz
   ranger-<version>-atlas-plugin.tar.gz
   ranger-<version>-hbase-plugin.tar.gz
   ranger-<version>-hdfs-plugin.tar.gz
   ranger-<version>-hive-plugin.tar.gz
   ranger-<version>-kafka-plugin.tar.gz
   ranger-<version>-kms.tar.gz
   ranger-<version>-knox-plugin.tar.gz
   ranger-<version>-migration-util.tar.gz
   ranger-<version>-ranger-tools.tar.gz
   ranger-<version>-solr-plugin.tar.gz
   ranger-<version>-sqoop-plugin.tar.gz
   ranger-<version>-src.tar.gz
   ranger-<version>-storm-plugin.tar.gz
   ranger-<version>-tagsync.tar.gz
   ranger-<version>-usersync.tar.gz
   ranger-<version>-yarn-plugin.tar.gz
   ranger-<version>-kylin-plugin.tar.gz
   ranger-<version>-elasticsearch-plugin.tar.gz
   ranger-<version>-ozone-plugin.tar.gz
   ranger-<version>-presto-plugin.tar.gz
   ranger-<version>-schema-registry-plugin.tar.gz



#Deployment Process  

##Installation Host Information
1.  Ranger Admin Tool Component  (ranger-<version-number>-admin.tar.gz) should be installed on a host where Policy Admin Tool web application runs on port 6080 (default).
2.  Ranger User Synchronization Component (ranger-<version-number>-usersync.tar.gz) should be installed on a host to synchronize the external user/group information into Ranger database via Ranger Admin Tool.
3.  Ranger Component plugin should be installed on the component boxes:
    (a)  HDFS Plugin needs to be installed on Name Node hosts
    (b)  Hive Plugin needs to be installed on HiveServer2 hosts
    (c)  HBase Plugin needs to be installed on both Master and Regional Server nodes.
    (d)  Knox Plugin needs to be installed on Knox gateway host.
    (e)  Storm Plugin needs to be installed on Storm hosts.
    (f)  Kafka/Solr Plugin needs to be installed on their respective component hosts.
    (g)  YARN plugin needs to be installed on YARN Resource Manager hosts
    (h)  Sqoop plugin needs to be installed on Sqoop2 hosts
    (i)  Kylin plugin needs to be installed on Kylin hosts
    (j)  Elasticsearch plugin needs to be installed on Elasticsearch hosts
    (k)  Ozone plugin needs to be installed on Ozone hosts
    (l)  Presto plugin needs to be installed on Presto hosts
    (m)  Schema Registry plugin needs to be installed on Schema Registry hosts

##Installation Process  

1. Download the tar.gz file into a temporary folder in the box where it needs to be installed.
2. Expand the tar.gz file into /usr/lib/ranger/ folder
3. Go to the component name under the expanded folder (e.g. /usr/lib/ranger/ranger-<version-number>-admin/)
4. Modify the install.properties file with appropriate variables
5. If the module has setup.sh, 
       Execute ./setup.sh

   If the install.sh file does not exists, 
       Execute ./enable-<component>-plugin.sh


#本地运行    
1、修改ranger-admin-site.xml：
security-admin/src/main/resources/conf.dist/ranger-admin-site.xml
```xml




```

2、修改security-admin/src/main/webapp/WEB-INF/web.xml
```xml




```

3、修改security-admin/src/main/webapp/META-INF/applicationContext.xml
```xml




```



4、数据库配置
（1）、创建database：
```mysql
create database ranger character set utf8mb4;
```
（2）、执行sql脚本：
security-admin/db/mysql/optimized/current/ranger_core_db_mysql.sql
security-admin/db/mysql/xa_audit_db.sql

