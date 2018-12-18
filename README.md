# dns-update-aliyun
始: 家里光猫是提供外网的，但需要光猫映射，光猫映射已添加OK，无奈外网IP经常变动，时常需要我去百度下我的IP才能在外网操作，我本来就是有域名的，所以这个步骤就变成了:
>百度IP->登陆阿里云域名解析->修改解析

烦躁，就找了下域名解析的api，然后就有了这个jar

### 首先，利用外部能访问的地址解决查看自己外网IP的问题，内部无法获取外网IP的〒▽〒

1. 找个可以用的网址利用代码获取解析一下
2. 自己有服务器返回一下
 

		// 如nginx直接匹配返回json格式的IP
	    location = /ip {
	    	default_type application/json;
	    	return 200 '{"ip":"$remote_addr"}';
	    }
	[https://blmdz.cn/ip](https://blmdz.cn/ip "https://blmdz.cn/ip") 返回格式为: {"ip":"127.0.0.1"}
    
### 获取阿里云accessKey

1. 控制台 -> 头像下accessKey -> 子用户【或[链接](https://ram.console.aliyun.com/users "链接")】
2. 新建 编程访问打勾, 建好就有了, 需要授权
3. 授权, 搜索dns都打兑上, 确定over

### install
其他就不多说了，代码可以自己看，逻辑非常简单

    git clone https://github.com/xpoll/dns-update-aliyun.git
	cd dns-update-aliyun
    mvn clean install
    // 查询domain列表
    java -jar target/dns-update-aliyun-0.0.1-SNAPSHOT.jar query accessKeyId accessKeySecret
    // 修改映射
    java -jar target/dns-update-aliyun-0.0.1-SNAPSHOT.jar update accessKeyId accessKeySecret IP_URL IP_CODE recordId

在这里 query 和 update 是固定参数 accessKeyId、accessKeySecret就不用说了必须参数，

IP_URL 修改必须参数，能获取外网的地址需要返回json格式，例如上文: https://blmdz.cn/ip

IP_CODE 修改必须参数，返回json格式ip的key，例如上文: ip

recordId 修改必须参数，修改域名映射的id，通过query可以查到

样例结果如下
    
    $ java -jar target/dns-update-aliyun-0.0.1-SNAPSHOT.jar query accessKeyId accessKeySecret
	...
    A 123456 abc.aabbcc.com 121.121.121.121
	...
    $ java -jar target/dns-update-aliyun-0.0.1-SNAPSHOT.jar update accessKeyId accessKeySecret https://blmdz.cn/ip ip 123456
    时间: 2018-12-12 12:12:12, 原IP: 121.121.121.121, 现IP: 122.122.122.122, 需要更新
    更新成功
    $ java -jar target/dns-update-aliyun-0.0.1-SNAPSHOT.jar update accessKeyId accessKeySecret https://blmdz.cn/ip ip 123456
    时间: 2018-12-12 12:12:12, 原IP: 122.122.122.122, 现IP: 122.122.122.122, 不需要更新


可以看代码修改成自己想要的样子，
while(true) 也好、运行一次也好