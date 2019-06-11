package com.dtsz.baseclass.web.util.kubernetes;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AppsV1beta2Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;

/**
 * @ClassName: KubernetesFactory
 * @Description: TODO
 * @see:
 * @author: Gsy
 * @date: 2019年2月20日 上午11:27:46
 * @version :1.0
 */
public class KubernetesFactory {
	
	private static Kubernetes kubernetes = new Kubernetes();
	
	private ExtensionsV1beta1Api extensionsV1beta1Api ;
	
	private AppsV1beta2Api appsV1beta2Api;
	
	private CoreV1Api coreV1Api;
	
	public KubernetesFactory(String keyStorePath, String keyStorePassword, String k8sUrl) throws Exception {
		
		coreV1Api = KubernetesUtil.getCoreV1Api(keyStorePath, keyStorePassword, k8sUrl);
		
		extensionsV1beta1Api = KubernetesUtil.getExtensionsV1beta1Api(keyStorePath, keyStorePassword, k8sUrl);
		
		appsV1beta2Api = KubernetesUtil.getAppsV1beta2Api(keyStorePath, keyStorePassword, k8sUrl);
	}
	
	public void createApp(String appName, String host, Integer replicas,String livePath, String initImage, String image, Integer memory, Integer cpu) throws Exception{
		kubernetes.createIngress(extensionsV1beta1Api, appName, host);
		kubernetes.createService(coreV1Api, appName);
		kubernetes.createConfigMap(coreV1Api, appName, replicas);
		kubernetes.createStatefulSet(appsV1beta2Api, appName, replicas, livePath, initImage, image,memory,cpu);
	}
	
	public void deleteApp(String appName) throws ApiException  {
			kubernetes.deleteIngress(extensionsV1beta1Api, appName);
			kubernetes.deleteService(coreV1Api, appName);
			kubernetes.deleteConfigMap(coreV1Api, appName);
			kubernetes.deleteStatefulSet(appsV1beta2Api, coreV1Api, appName);
	}
	
	public void updateApp(String appName, String newAppName, String host, Integer replicas, String livePath, String initImage,
			String image, Integer memory, Integer cpu) throws Exception {
		kubernetes.updateIngress(extensionsV1beta1Api, appName, newAppName, host);
		kubernetes.updateService(coreV1Api, appName, newAppName);
		kubernetes.updateConfigMap(coreV1Api, appName, newAppName, replicas);
		kubernetes.updateStatefulSet(appsV1beta2Api, coreV1Api, appName, newAppName, replicas, livePath, initImage,
				image, memory, cpu);
	}

}
