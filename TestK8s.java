/**   
 * @版权：本文件版权归属于北京大唐神州科技有限公司,JDK1.7
 *
 * @message: TODO 
 * @Package: com.dtsz.baseclass.web.util 
 * @author: Gsy   
 * @date: 2019年1月24日 上午10:30:07 
 * @version: 1.0
 */
package com.dtsz.baseclass.web.util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.dtsz.baseclass.web.util.kubernetes.Kubernetes;
import com.dtsz.baseclass.web.util.kubernetes.KubernetesFactory;
import com.dtsz.baseclass.web.util.kubernetes.KubernetesUtil;

import io.kubernetes.client.apis.AppsV1beta2Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapList;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1Namespace;
import io.kubernetes.client.models.V1NamespaceList;
import io.kubernetes.client.models.V1Node;
import io.kubernetes.client.models.V1NodeList;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServiceList;
import io.kubernetes.client.models.V1beta1Ingress;
import io.kubernetes.client.models.V1beta2StatefulSet;
import io.kubernetes.client.util.Yaml;

/** 
 * @ClassName: TestK8s 
 * @Description: TODO
 * @see: 
 * @author: Gsy
 * @date: 2019年1月24日 上午10:30:07 
 * @version :1.0 
 */
public class TestK8s {

	//@Test
	public void testCreateApp() throws Exception {
		String keyStorePath = "F:\\k8sAndDocker\\k8sTestFile\\gsy-client.pfx";
		String password = "123456";
		String basePath = "https://192.168.3.181:6443";
		KubernetesFactory kubernetesFactory = new KubernetesFactory(keyStorePath, password, basePath);
		kubernetesFactory.createApp("REPORT","www.dtsz.com",1,"login.jsp","192.168.3.181:5000/report:1.0.5", "192.168.3.181:5000/tomcat:8-jre8",null,null);
		//kubernetesFactory.updateApp("REPORT", "a");
		//kubernetesFactory.deleteApp("REPORT");
	}
	//@Test
	public void testReplaceArgs() throws Exception {
		Map<String,String> args = new HashMap<String,String>();
		args.put("${name}", "gsy");
		String yaml = KubernetesUtil.replaceArgs("F:\\k8sAndDocker\\k8sTestFile\\ingress1.yaml", args);
		V1beta1Ingress updateIngress = Yaml.loadAs(yaml,V1beta1Ingress.class);
		System.out.println(updateIngress.getMetadata().getName());
	}
	
	
	//@Test
	public void testConfigMap() throws Exception {
		String keyStorePath = "F:\\k8sAndDocker\\k8sTestFile\\gsy-client.pfx";
		String password = "123456";
		String basePath = "https://192.168.3.181:6443";
		
		CoreV1Api  api = KubernetesUtil.getCoreV1Api(keyStorePath, password, basePath);
		//查询
//		V1ConfigMapList listNamespacedConfigMap = api.listNamespacedConfigMap("default", null, null, null, null, null, null, null, null, null);
//		List<V1ConfigMap> items = listNamespacedConfigMap.getItems();
//		for (V1ConfigMap v1ConfigMap : items) {
//			System.out.println(v1ConfigMap.getMetadata().getName());
//		}
		//新建
		File file = new File("F:\\k8sAndDocker\\k8sTestFile\\configmap.yaml");
		V1ConfigMap configMap = Yaml.loadAs(file,V1ConfigMap.class);
		Map<String,String> annontion = new HashMap<String,String>();
		annontion.put("port","40000");
		configMap.getMetadata().setAnnotations(annontion);
		api.createNamespacedConfigMap("default", configMap, null);
		//删除
		//api.deleteNamespacedConfigMap("tomcat-report-configmap", "default", new V1DeleteOptions(), null, null, true, null);
		//api.replaceNamespacedConfigMap("report", "default", configMap, null);
		
		
	}
	
	@Test
	public void testIngress() throws Exception {
		String keyStorePath = "F:\\k8sAndDocker\\k8sTestFile\\gsy-client.pfx";
		String password = "123456";
		String basePath = "https://192.168.3.181:6443";
		ExtensionsV1beta1Api  extensionsV1beta1Api = KubernetesUtil.getExtensionsV1beta1Api(keyStorePath, password, basePath);
		//删除
		//extensionsV1beta1Api.deleteNamespacedIngress("usrp-ingress", "default", new V1DeleteOptions(), null, null, null, null);
		//新建
//		File createIngressFile = new File("F:\\k8sAndDocker\\k8sTestFile\\ingress1.yaml");
//		V1beta1Ingress createIngress = Yaml.loadAs(createIngressFile,V1beta1Ingress.class);
//		extensionsV1beta1Api.createNamespacedIngress("default", createIngress, null);

		//修改
		//File updateIngressFile = new File("F:\\k8sAndDocker\\k8sTestFile\\ingress1.yaml");
		//V1beta1Ingress updateIngress = Yaml.loadAs(updateIngressFile,V1beta1Ingress.class);
		//extensionsV1beta1Api.replaceNamespacedIngress("usrp-ingress", "default", updateIngress, null);
		new Kubernetes().updateIngress(extensionsV1beta1Api, "aaa", "bbb", null);
	}
	
	//@Test
	public void testStatefulSet() throws Exception {
		String keyStorePath = "F:\\k8sAndDocker\\k8sTestFile\\gsy-client.pfx";
		String password = "123456";
		String basePath = "https://192.168.3.181:6443";
		
		AppsV1beta2Api api = KubernetesUtil.getAppsV1beta2Api(keyStorePath, password, basePath);
		//查询
//		V1beta2StatefulSetList listNamespacedStatefulSet = api.listNamespacedStatefulSet("default", null, null, null, null, null, null, null, null, null);
//		List<V1beta2StatefulSet> items = listNamespacedStatefulSet.getItems();
//		for (V1beta2StatefulSet v1beta2StatefulSet : items) {
//			System.out.println(v1beta2StatefulSet.getMetadata().getName());
//		}
		
		//新建
		File file = new File("F:\\k8sAndDocker\\k8sTestFile\\report-state.yaml");
		V1beta2StatefulSet statefulSet = Yaml.loadAs(file,V1beta2StatefulSet.class);
		//api.createNamespacedStatefulSet("default", statefulSet, null);
		//删除
//		CoreV1Api k8sApi = KubernetesUtil.getCoreV1Api(keyStorePath, password, basePath);
//		V1PodList podList = k8sApi.listNamespacedPod("default", null, null, null, null, null, null, null, null, null);
//		List<V1Pod> deletePod = new ArrayList<V1Pod>();
//		for (V1Pod v1Pod : podList.getItems()) {
//			String name = v1Pod.getMetadata().getOwnerReferences().get(0).getName();
//			if("tomcat-report1".equals(name)) {
//				deletePod.add(v1Pod);
//			}
//		}
//		try {
//			api.deleteNamespacedStatefulSet("tomcat-report1", "default", new V1DeleteOptions(), null, null, null, null);
//		}catch(Exception e ) {
//			System.out.println("删除safefulset"+"tomcat-report1");
//		}
//		for (V1Pod v1Pod : deletePod) {
//			try {
//				k8sApi.deleteNamespacedPod(v1Pod.getMetadata().getName(), "default", new V1DeleteOptions(), null, null, null, null);
//			}catch(Exception e ) {
//				System.out.println("删除pod"+v1Pod.getMetadata().getName());
//			}
//		}
		//修改
		api.replaceNamespacedStatefulSet("tomcat-report1", "default", statefulSet, null);
		
		
	}
	
	
	
	//@Test
	public void testCreateService() throws Exception {
		String keyStorePath = "F:\\k8sAndDocker\\k8sTestFile\\gsy-client.pfx";
		String password = "123456";
		String basePath = "https://192.168.3.181:6443";
		
		CoreV1Api api = KubernetesUtil.getCoreV1Api(keyStorePath, password, basePath);
		File reportSvc = new File("F:\\k8sAndDocker\\k8sTestFile\\report-service.yaml");
	    V1Service reportService = Yaml.loadAs(reportSvc,V1Service.class);
		api.createNamespacedService("default", reportService, null);
	}
	//@Test
	public void testDeleteService() throws Exception {
		String keyStorePath = "F:\\k8sAndDocker\\k8sTestFile\\gsy-client.pfx";
		String password = "123456";
		String basePath = "https://192.168.3.181:6443";
		
		CoreV1Api api = KubernetesUtil.getCoreV1Api(keyStorePath, password, basePath);
		api.deleteNamespacedService("report-svc1", "default", new V1DeleteOptions(), null, null, null, null);
	}
	//@Test
	public void test() throws Exception {
		String keyStorePath = "F:\\k8sAndDocker\\k8sTestFile\\gsy-client.pfx";
		String password = "123456";
		String basePath = "https://192.168.3.181:6443";
		
		CoreV1Api api = KubernetesUtil.getCoreV1Api(keyStorePath, password, basePath);
		//查看所有工作空间
		V1NamespaceList v1NamespaceList = api.listNamespace(null, null, null, null, null, null, null, null, null);
		for(V1Namespace item: v1NamespaceList.getItems()) {
			System.out.println("工作空间++++++++++++++++++==================" + item.getMetadata().getName());
		}
		System.out.println();
		//查看所有pod
		V1PodList list = api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);
		for (V1Pod item : list.getItems()) {
			System.out.println("pod++++++++++++++++++>>>>>>>>>>>>>>>>>>>>" + item.getMetadata().getName());
		}
		System.out.println();
		//查看所有node
		V1NodeList listNode = api.listNode(null, null, null, null, null, null, null, null, null);
		for (V1Node v1Node : listNode.getItems()) {
			System.out.println("node中的pod数量++++++++++++++++++>>>>>>>>>>>>>>>>>>>>" +v1Node.getStatus().getAllocatable().get("pods").getNumber());
			System.out.println("node++++++++++++++++++>>>>>>>>>>>>>>>>>>>>" +v1Node.getMetadata().getName());
		}
		
		//查看default名称空间下所有服务并创建一个服务
		System.out.println();
		V1ServiceList listNamespacedService = api.listNamespacedService("default", null, null, null, null, null, null, null, null, null);
		for (V1Service v1Service : listNamespacedService.getItems()) {
			System.out.println("1.Service++++++++++++++++++>>>>>>>>>>>>>>>>>>>>" + v1Service.getMetadata().getName());
		}
		System.out.println();
		
		//查看default名称空间下所有服务并删除一个服务
		V1ServiceList listNamespacedService2 = api.listNamespacedService("default", null, null, null, null, null, null, null, null, null);
		for (V1Service v1Service : listNamespacedService2.getItems()) {
			System.out.println("2.Service++++++++++++++++++>>>>>>>>>>>>>>>>>>>>" + v1Service.getMetadata().getName());
		}
		System.out.println();
		
		
		
		
		
	}
	
	
}
