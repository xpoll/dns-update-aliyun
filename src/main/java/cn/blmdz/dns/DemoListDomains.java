package cn.blmdz.dns;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordInfoRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordInfoResponse;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse.Record;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainsResponse.Domain;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.github.kevinsawicki.http.HttpRequest;

/**
 * dns-update-aliyun
 * 
 * @author xpoll
 * @date 2018年12月18日
 * @url https://xpoll.blmdz.cn
 * @accessKey
 * <pre>
 * accessKey 获取
 * 
 * 控制台 -> 头像下accessKey -> 子用户
 * 或下面链接
 * https://ram.console.aliyun.com/users
 * 
 * 新建 编程访问打勾, 建好就有了, 需要授权
 * 授权, 搜索dns都打兑上, 确定over
 * 
 * 用户 -> 选择新建的用户 下面有用户AccessKey可禁用或新增
 * </pre>
 */
public class DemoListDomains {
    
    private static IAcsClient client = null; // 客户端

    private static String dotype = null; // 必须 -- 定义方法 支持 query update
    private static String accessKeyId = null; // 必须 -- 阿里云key
    private static String accessKeySecret = null; // 必须 -- 阿里云Secret
    private static String IP_URL = null; // 修改必须 -- 能获取外网的地址需要返回json格式
    private static String IP_CODE = null; // 修改必须 -- 返回json格式ip的key
    private static String recordId = null; // 修改必须 -- 修改value值的解析id

    /**
     * <pre>
     * mvn clean install
     * java -jar target/dns-update-aliyun-0.0.1-SNAPSHOT.jar query accessKeyId accessKeySecret
     * java -jar target/dns-update-aliyun-0.0.1-SNAPSHOT.jar update accessKeyId accessKeySecret IP_URL IP_CODE recordId
     * IP_URL 返回参数需要是json 并且包含ip地址, 不支持其他方式
     * 
     * 
     * nginx.conf
     * 
     * location = /ip {
     *   default_type application/json;
     *   return 200 '{"ip":"$remote_addr"}';
     * }
     * 
     * 如: https://blmdz.cn/ip 返回结构为{"ip": "127.0.0.1"}
     * 
     * </pre>
     */
    public static void main(String[] args) throws InterruptedException {
        if (args == null || args.length < 1) {
            System.out.println("参数不正确哦");
            return ;
        }
        
        dotype = args[0];
        
        if ("query".equals(dotype)) {
            if (args.length < 3) {
                System.out.println("参数不正确哦");
                return ;
            }
        } else if ("update".equals(dotype)) {
            if (args.length < 6) {
                System.out.println("参数不正确哦");
                return ;
            }
        } else {
            System.out.println("没有实现该方法");
            return ;
        }
        
        accessKeyId = args[1];
        accessKeySecret = args[2];
        
        client = new DefaultAcsClient(DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret));
        while (true) {
            if ("query".equals(dotype)) {
                show(client);
            } else if ("update".equals(dotype)) {
                
                IP_URL = args[3];
                IP_CODE = args[4];
                recordId = args[5];
                
                DescribeDomainRecordInfoResponse response = describeDomainRecordInfo(client, recordId);
                String rr = response.getRR(), type = response.getType(), value = response.getValue();
                String body = HttpRequest.get(IP_URL).body();
                String ip = JSONObject.parseObject(body).getString(IP_CODE);
                System.out.println(String.format("时间: %s, 原IP: %s, 现IP: %s, %s", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), value, ip, ip.equals(value) ? "不需要更新" : "需要更新"));
                if (!ip.equals(value)) {
                    System.out.println(updateDomainRecord(client, recordId, rr, type, ip) ? "更新成功" : "更新失败");
                }
            }
            
            Thread.sleep(30000L);
        }
    }

    /**
     * 获取domain和详细列表
     */
    public static void show(IAcsClient client) {

        List<Domain> domains = describeDomains(client);
        if (domains != null && !domains.isEmpty()) {
            domains.forEach(domain -> {
                System.out.println(String.format("%s: %s", domain.getDomainId(), domain.getDomainName()));
                List<Record> records = describeDomainRecords(client, domain.getDomainName());
                if (records != null && !records.isEmpty()) {
                    records.forEach(record -> {
                        System.out.println(String.format(
                                "%s %s %s %s", record.getType(), record.getRecordId(), "@".equals(record.getRR())
                                        ? record.getRR() : record.getRR() + "." + domain.getDomainName(),
                                record.getValue()));
                    });
                }
            });
        }
    }

    /**
     * 获取解析记录信息
     * 
     * https://help.aliyun.com/document_detail/29777.html
     */
    public static DescribeDomainRecordInfoResponse describeDomainRecordInfo(IAcsClient client, String recordId) {
        DescribeDomainRecordInfoRequest request = new DescribeDomainRecordInfoRequest();
        request.setActionName("DescribeDomainRecordInfo");
        request.setRecordId(recordId);
        try {
            return client.getAcsResponse(request);
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 修改解析记录
     * 
     * https://help.aliyun.com/document_detail/29774.html
     */
    public static boolean updateDomainRecord(IAcsClient client, String recordId, String rr, String type, String value) {
        UpdateDomainRecordRequest request = new UpdateDomainRecordRequest();
        request.setActionName("UpdateDomainRecord");
        request.setRecordId(recordId);
        request.setRR(rr);
        request.setType(type);
        request.setValue(value);
        try {
            client.getAcsResponse(request);
            return true;
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取解析记录列表
     * 
     * https://help.aliyun.com/document_detail/29776.html
     */
    public static List<Record> describeDomainRecords(IAcsClient client, String domainName) {
        DescribeDomainRecordsRequest request = new DescribeDomainRecordsRequest();
        request.setActionName("DescribeDomainRecords");
        request.setDomainName(domainName);
        request.setPageSize(200L);
        try {
            return client.getAcsResponse(request).getDomainRecords();
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取域名列表
     * 
     * https://help.aliyun.com/document_detail/29751.html
     */
    public static List<Domain> describeDomains(IAcsClient client) {
        DescribeDomainsRequest request = new DescribeDomainsRequest();
        try {
            return client.getAcsResponse(request).getDomains();
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }

}