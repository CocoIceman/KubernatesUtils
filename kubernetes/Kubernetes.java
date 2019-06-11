package com.dtsz.baseclass.web.util.kubernetes;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonSyntaxException;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.AppsV1beta2Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapList;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1OwnerReference;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServiceList;
import io.kubernetes.client.models.V1ServicePort;
import io.kubernetes.client.models.V1beta1Ingress;
import io.kubernetes.client.models.V1beta1IngressList;
import io.kubernetes.client.models.V1beta2StatefulSet;
import io.kubernetes.client.util.Yaml;

/**
 * @ClassName: Kubernetes
 * @Description: TODO
 * @see:
 * @author: Gsy
 * @date: 2019年2月20日 下午2:02:20
 * @version :1.0
 */
public class Kubernetes {

	protected static Logger logger = Logger.getLogger(Kubernetes.class.getName());
	private static String currentPath = Kubernetes.class.getResource("").getPath();
	private static String namespace = "default";
	/**
	 * 创建Ingress
	 * 
	 * @param extensionsV1beta1Api
	 *            操作Ingress的API
	 * @param contextName
	 *            contextName的名称
	 * @param host
	 *            url访问地址 例: www.dtsz.com
	 * @throws Exception
	 */
	public void createIngress(ExtensionsV1beta1Api extensionsV1beta1Api, String contextName, String host)
			throws Exception {
		//判断Ingress是否存在
		V1beta1Ingress ingress = this.getIngress(extensionsV1beta1Api, contextName);
		if(ingress != null) {
			logger.info("[" + contextName + "]" + "Ingress已经存在!");
			throw new Exception("[" + contextName + "]" + "Ingress已经存在!");
		}
		
		//kubernetes 的ingress、service、safefulset 等他们的名称全部需要小写字母
		String lowerContextName = contextName.toLowerCase();
		
		//替换backend中的参数,追加到yaml文件中
		String backend = "      - path: /${backendName}\r\n" + 
						 "        backend:\r\n" + 
						 "          serviceName: ${serviceName}\r\n" + 
						 "          servicePort: 8080";
		backend = backend.replace("${backendName}", contextName);
		backend = backend.replace("${serviceName}", lowerContextName);
		
		//获取yaml文件路径
		String ingressFileName = "ingress.yaml";
		String path = currentPath + ingressFileName;
		path = URLDecoder.decode(path, "utf-8");
		
		//读取预定文件并修改其中的参数
		Map<String, String> args = new HashMap<String, String>();
		args.put("${ingressName}", lowerContextName);
		args.put("${ingress}", backend);
		args.put("${host}", host);
		String content = KubernetesUtil.replaceArgs(path, args);
		
		//根据文件中的内容构造Ingress对象
		V1beta1Ingress v1beta1Ingress = Yaml.loadAs(content, V1beta1Ingress.class);
		
		//kubernetes中创建Ingress
		extensionsV1beta1Api.createNamespacedIngress(namespace, v1beta1Ingress, null);
	}
	
	/**
	 * 删除Ingress
	 * @param extensionsV1beta1Api
	 * 						 操作Ingress的API
	 * @param contextName
	 * 				要删除的上下文根
	 * @throws ApiException
	 */
	public void deleteIngress(ExtensionsV1beta1Api extensionsV1beta1Api, String contextName) throws ApiException {
			extensionsV1beta1Api.deleteNamespacedIngress(contextName.toLowerCase(), namespace, new V1DeleteOptions(), null, null, null, null);
	}
	
	/**
	 * 更新一个Ingress
	 * @param extensionsV1beta1Api
	 * 						 操作Ingress的API
	 * @param oldContextName
	 * 					要修改的上下文根
	 * @param newContextName
	 * 					修改后的上下文根
	 * @param host
	 * 					修改的主机名
	 * @throws Exception
	 */
	public void updateIngress(ExtensionsV1beta1Api extensionsV1beta1Api, String oldContextName, String newContextName, String host) throws Exception {
		boolean rename = false;
		if(StringUtils.isNotBlank(newContextName) && (!oldContextName.equals(newContextName))) {
			//判读Ingress是否存在
			V1beta1Ingress ingress = this.getIngress(extensionsV1beta1Api, newContextName);
			if(ingress != null) {
				logger.info("[" + newContextName + "]" + "Ingress已经存在!");
				throw new Exception("[" + newContextName + "]" + "Ingress已经存在!");
			}
			rename = true;
		}
		V1beta1Ingress readNamespacedIngress = extensionsV1beta1Api.readNamespacedIngress(oldContextName, namespace, null, null, null);
		if(StringUtils.isBlank(host)) {
			host = readNamespacedIngress.getSpec().getRules().get(0).getHost();
		}
		this.deleteIngress(extensionsV1beta1Api, oldContextName);
		if(rename) {
			this.createIngress(extensionsV1beta1Api, newContextName, host);
		}else {
			this.createIngress(extensionsV1beta1Api, oldContextName, host);
		}
	}
	/**
	 * 根据上下文根查找Ingress
	 * @param extensionsV1beta1Api
	 * 				 操作Ingress的API
	 * @param contextName
	 * 				上下文根
	 * @return
	 * @throws ApiException
	 */
	public V1beta1Ingress getIngress(ExtensionsV1beta1Api extensionsV1beta1Api, String contextName)
			throws ApiException {
		V1beta1IngressList listNamespacedIngress = extensionsV1beta1Api.listNamespacedIngress(namespace, null, null, null, null, null, null, null, null, null);
		List<V1beta1Ingress> items = listNamespacedIngress.getItems();
		for (V1beta1Ingress v1beta1Ingress : items) {
			if (v1beta1Ingress.getMetadata().getName().equals(contextName)) {
				return v1beta1Ingress;
			}
		}
		return null;
	}
	
	/**
	 * 创建一个Service
	 * 
	 * @param coreV1Api
	 *            操作Service的API
	 * @param serviceName
	 *            要创建的服务的名称
	 * @throws Exception
	 */
	public void createService(CoreV1Api coreV1Api, String serviceName) throws Exception {
		//判断Service是否存在
		V1Service service = this.getService(coreV1Api, serviceName);
		if (service != null) {
			logger.info("[" + serviceName + "]" + "Service已经存在!");
			throw new Exception("[" + serviceName + "]" + "Service已经存在!");
		}
		
		//获得一个Service未使用的端口
		Integer serviceLastPort = this.getServiceLastPort(coreV1Api);
		
		//替换文件中的参数
		Map<String, String> args = new HashMap<String, String>();
		args.put("${serviceName}", serviceName.toLowerCase());
		args.put("${port}", serviceLastPort + "");
		String path = currentPath + "service.yaml";
		path = URLDecoder.decode(path, "utf-8");
		String content = KubernetesUtil.replaceArgs(path, args);
		
		//根据文件中的内容生成Service对象
		V1Service v1Service = Yaml.loadAs(content, V1Service.class);
		
		//kubernetes中创建Service
		coreV1Api.createNamespacedService(namespace, v1Service, null);
	}

	/**
	 * 删除服务
	 * 
	 * @param coreV1Api
	 *            操作Service的API
	 * @param serviceName
	 *            要删除的Service的名称
	 * @throws ApiException
	 */
	public void deleteService(CoreV1Api coreV1Api, String serviceName) throws ApiException {
			coreV1Api.deleteNamespacedService(serviceName.toLowerCase(), namespace, new V1DeleteOptions(), null, null, null, null);
	}

	/**
	 * 更新Service
	 * 
	 * @param coreV1Api
	 *            操作Service的API
	 * @param oldServiceName
	 *            要修改的服务的名称
	 * @param newServiceName
	 *            修改后的服务的名称
	 * @throws Exception
	 */
	public void updateService(CoreV1Api coreV1Api, String oldServiceName, String newServiceName) throws Exception {
		if(StringUtils.isBlank(newServiceName)) {
			return ;
		}
		if(oldServiceName.equals(newServiceName)) {
			return ;
		}
		V1Service service = this.getService(coreV1Api, newServiceName);
		if (service != null) {
			logger.info("[" + newServiceName + "]" + "Service已经存在!");
			throw new Exception("[" + newServiceName + "]" + "Service已经存在!");
		}
		this.deleteService(coreV1Api, oldServiceName);
		this.createService(coreV1Api, newServiceName);
	}

	/**
	 * 根据service名称获取一个Service
	 * 
	 * @param coreV1Api
	 * 				操作Service的API
	 * @param serviceName
	 * 				服务的名称
	 * @return
	 * @throws ApiException
	 */
	public V1Service getService(CoreV1Api coreV1Api, String serviceName) throws ApiException {
		serviceName = serviceName.toLowerCase();
		List<V1Service> findService = this.findService(coreV1Api);
		for (V1Service v1Service : findService) {
			if (serviceName.equals(v1Service.getMetadata().getName())) {
				return v1Service;
			}
		}
		return null;
	}

	/**
	 * 获取Service列表
	 * 
	 * @param coreV1Api
	 * 				操作Service的API
	 * @return
	 * @throws ApiException
	 */
	public List<V1Service> findService(CoreV1Api coreV1Api) throws ApiException {
		V1ServiceList listNamespacedService = coreV1Api.listNamespacedService(namespace, null, null, null, null, null,
				null, null, null, null);
		return listNamespacedService.getItems();
	}

	/**
	 * 获得一个Service的最新的未使用的端口
	 * 
	 * @param coreV1Api
	 * 				操作Service的API
	 * @return
	 * @throws ApiException
	 */
	public Integer getServiceLastPort(CoreV1Api coreV1Api) throws ApiException {
		Integer servicePort = -1;
		List<V1Service> findService = this.findService(coreV1Api);
		for (V1Service v1Service : findService) {
			List<V1ServicePort> ports = v1Service.getSpec().getPorts();
			if (ports != null) {
				for (V1ServicePort v1ServicePort : ports) {
					Integer port = v1ServicePort.getPort();
					if (port > servicePort) {
						servicePort = port;
					}
				}
			}
		}
		if (servicePort == -1) {
			return 80;
		}
		return servicePort + 1;
	}

	/**
	 * 创建一个ConfigMap
	 * 
	 * @param coreV1Api
	 * 				操作ConfigMap的API
	 * @param configMapName
	 * 				要创建的ConfigMap名称
	 * @param rmiUrlNum
	 * 				rmiUrlNum数量
	 * @throws Exception
	 */
	public void createConfigMap(CoreV1Api coreV1Api, String configMapName, Integer rmiUrlNum) throws Exception {
		// 判断该ConfigMap是否存在
		V1ConfigMap configMap = this.getConfigMap(coreV1Api, configMapName);
		if(configMap != null) {
			logger.info("[" + configMapName + "]" + "ConfigMap已经存在!");
			throw new Exception("[" + configMapName + "]" + "ConfigMap已经存在!");
		}
		configMapName = configMapName.toLowerCase();
		// 获得一个configMap可用的端口号
		String configMapLastPort = getConfigMapLastPort(coreV1Api);

		// 获取rmiUrl
		String rmiUrl = this.getRmiUrl(configMapName, configMapLastPort, rmiUrlNum);
		
		// 替换文件中的参数
		Map<String, String> args = new HashMap<String, String>();
		args.put("${rmiUrls}", rmiUrl);
		args.put("${port}", configMapLastPort);
		args.put("${configMapName}", configMapName);
		args.put("${rmiUrlsNum}", (rmiUrlNum == null ? 1 : rmiUrlNum) + "");
		String path = currentPath + "configmap.yaml";
		path = URLDecoder.decode(path, "utf-8");
		String content = KubernetesUtil.replaceArgs(path, args);

		// 根据文件中的内容生成ConfigMap对象
		V1ConfigMap v1ConfigMap = Yaml.loadAs(content, V1ConfigMap.class);

		// 在kubernetes中创建ConfigMap
		coreV1Api.createNamespacedConfigMap(namespace, v1ConfigMap, null);
	}
	
	/**
	 * 删除configMap
	 * 
	 * @param coreV1Api
	 * 				操作ConfigMap的API
	 * @param configMapName
	 * 				要删除的ConfigMap的名称
	 * @throws ApiException
	 */
	public void deleteConfigMap(CoreV1Api coreV1Api, String configMapName) throws ApiException {
			coreV1Api.deleteNamespacedConfigMap(configMapName.toLowerCase(), namespace, new V1DeleteOptions(), null, null, null, null);
	}
	
	/**
	 * 修改一个ConfigMap
	 * @param coreV1Api
	 * 			操作ConfigMap的API
	 * @param oldConfigMapName
	 * 			要操作的ConfigMap的名称
	 * @param newConfigMapName
	 * 			修改后的ConfigMap名称
	 * @param rmiUrlNum
	 * 			rmiUrl的数量
	 * @throws Exception
	 */
	public void updateConfigMap(CoreV1Api coreV1Api, String oldConfigMapName, String newConfigMapName, Integer rmiUrlNum) throws Exception {
		//更新前判断ConfigMap的名称时候存在
		if (StringUtils.isBlank(newConfigMapName) && rmiUrlNum == null) {
			return ;
		}
		if ((StringUtils.isBlank(newConfigMapName) && rmiUrlNum != null) || (oldConfigMapName.equals(newConfigMapName) && rmiUrlNum != null)) {
			this.updateConfigMapRmiUrlNum(coreV1Api, oldConfigMapName, rmiUrlNum);
			return ;
		}
		if (oldConfigMapName.equals(newConfigMapName) && rmiUrlNum == null) {
			return ;
		}
		if(!oldConfigMapName.equals(newConfigMapName)) {
			V1ConfigMap configMap = this.getConfigMap(coreV1Api, newConfigMapName);
			if(configMap != null) {
				logger.info("[" + newConfigMapName + "]" + "ConfigMapName已经存在!");
				throw new Exception("[" + newConfigMapName + "]" + "ConfigMapName已经存在!");
			}
		}
		
		// 判断rmiUrlNum是否为空
		if(rmiUrlNum == null) {
			logger.info("[" + newConfigMapName + "]" + "rmiUrlNum是必需的参数!");
			throw new Exception("[" + newConfigMapName + "]" + "rmiUrlNum是必需的参数!");
		}
		
		//修改ConfigMap
		this.deleteConfigMap(coreV1Api, oldConfigMapName);
		this.createConfigMap(coreV1Api, newConfigMapName, rmiUrlNum);
	}
	
	/**
	 * 修改ConfigMap的RmiUrl数量(该数量与Pod的个数对应)
	 * 	
	 * @param coreV1Api
	 * 				操作ConfigMap的API
	 * @param configMapName
	 * 				要操作的ConfigMap的名称
	 * @param rmiUrlNum
	 * 				RmiUrl数量(该数量与Pod的个数对应)
	 * @throws Exception
	 */
	public void updateConfigMapRmiUrlNum(CoreV1Api coreV1Api, String configMapName, Integer rmiUrlNum) throws Exception {
		//修改前判断rmiUrlNum是否为空
		if(rmiUrlNum == null) {
			logger.info("[" + configMapName + "]" + "rmiUrlNum是必需的参数!");
			throw new Exception("[" + configMapName + "]" + "rmiUrlNum是必需的参数!");
		}
		//获取该ConfigMap的相关信息
		V1ConfigMap configMap = this.getConfigMap(coreV1Api, configMapName);
		Map<String, String> annotations = configMap.getMetadata().getAnnotations();
		
		if(annotations != null) {
			String port = annotations.get("port");
			if (StringUtils.isNotBlank(port)) {
				String rmiUrl = this.getRmiUrl(configMapName, port, rmiUrlNum);
				
				// 替换文件中的参数
				Map<String, String> args = new HashMap<String, String>();
				args.put("${rmiUrls}", rmiUrl);
				args.put("${port}", port);
				args.put("${configMapName}", configMapName);
				args.put("${rmiUrlsNum}", (rmiUrlNum == null ? 1 : rmiUrlNum) + "");
				String path = currentPath + "configmap.yaml";
				path = URLDecoder.decode(path, "utf-8");
				String content = KubernetesUtil.replaceArgs(path, args);

				// 根据文件中的内容生成ConfigMap对象
				V1ConfigMap v1ConfigMap = Yaml.loadAs(content, V1ConfigMap.class);

				// 修改kubernetes中的ConfigMap
				coreV1Api.replaceNamespacedConfigMap(configMapName, namespace, v1ConfigMap, null);
			}
		}
	}
	
	/**
	 * 修改ConfigMapName的名称
	 * 
	 * @param coreV1Api
	 * 				操作ConfigMap的API
	 * @param oldConfigMapName
	 * 				要修改的ConfigMap的名称
	 * @param newConfigMapName
	 * 				修改后的ConfigMap的名称
	 * @throws Exception
	 */
	public void updateConfigMapName(CoreV1Api coreV1Api, String oldConfigMapName, String newConfigMapName) throws Exception {
		//修改前先判断该ConfigMap是否已经存在
		V1ConfigMap isExitconfigMap = this.getConfigMap(coreV1Api, newConfigMapName);
		if(isExitconfigMap != null) {
			logger.info("[" + newConfigMapName + "]" + "ConfigMap已经存在!");
			throw new Exception("[" + newConfigMapName + "]" + "ConfigMap已经存在!");
		}
		
		//获取原来ConfigMap的信息
		V1ConfigMap configMap = this.getConfigMap(coreV1Api, oldConfigMapName);
		Map<String, String> annotations = configMap.getMetadata().getAnnotations();
		
		//修改kubernetes中的ConfigMap
		if(annotations != null) {
			String rmiUrlsNum = annotations.get("rmiUrlsNum");
			if (StringUtils.isNotBlank(rmiUrlsNum)) {
				this.deleteConfigMap(coreV1Api, oldConfigMapName);
				this.createConfigMap(coreV1Api, newConfigMapName, Integer.parseInt(rmiUrlsNum));
			}
		}
	}
	
	/**
	 * 获取指定的ConfigMap
	 * 
	 * @param coreV1Api
	 * 				操作ConfigMap的API
	 * @param configMapName
	 * 				要获取的ConfigMap的名称
	 * @return
	 * @throws ApiException
	 */
	public V1ConfigMap getConfigMap(CoreV1Api coreV1Api, String configMapName) throws ApiException {
		configMapName = configMapName.toLowerCase();
		List<V1ConfigMap> findConfigMap = this.findConfigMap(coreV1Api);
		for (V1ConfigMap v1ConfigMap : findConfigMap) {
			if(configMapName.equals(v1ConfigMap.getMetadata().getName())) {
				return v1ConfigMap;
			}
		}
		return null;
	}
	
	/**
	 * 获取ConfigMap列表
	 * 
	 * @param coreV1Api
	 * 				操作ConfigMap的API
	 * @return
	 * @throws ApiException
	 */
	public List<V1ConfigMap> findConfigMap(CoreV1Api coreV1Api) throws ApiException {
		V1ConfigMapList listNamespacedConfigMap = coreV1Api.listNamespacedConfigMap(namespace, null, null, null, null, null, null, null, null, null);
		return listNamespacedConfigMap.getItems();
	}
	
	/**
	 * 获取ConfigMap的一个可用的端口号
	 * 
	 * @param coreV1Api
	 * 				操作ConfigMap的API
	 * @return
	 * @throws ApiException
	 */
	public String getConfigMapLastPort(CoreV1Api coreV1Api) throws ApiException {
		int port = -1;
		List<V1ConfigMap> findConfigMap = this.findConfigMap(coreV1Api);
		for (V1ConfigMap v1ConfigMap : findConfigMap) {
			Map<String, String> annotations = v1ConfigMap.getMetadata().getAnnotations();
			if(annotations != null ) {
				String portString = annotations.get("port");
				if(StringUtils.isNotBlank(portString)) {
					try {
						int portInt = Integer.parseInt(portString);
						if(portInt > port) {
							port = portInt;
						}
					}catch(NumberFormatException e) {
						logger.info("获取到的configMap的端口不能转换", e);
					}
				}
			}
		}
		if (port == -1) {
			return "40000";
		}
		
		return port + 1 + "";
	}
	
	/**
	 * 获取一个RmiUrl
	 * 
	 * @param serviceName
	 * 				要监听的服务名称
	 * @param port
	 * 			要监听的服务端口号
	 * @param rmiUrlNum
	 * @return
	 */
	private String getRmiUrl(String serviceName, String port, Integer rmiUrlNum) {
		// 构造rmiUrls并替换其中的参数
		String rmiUrl = "//${serviceName}:${port}/modelCache|//${serviceName}:${port}/systemCache|//${serviceName}:${port}/synchronousCache|//${serviceName}:${port}/messageQueueCache|";
		StringBuffer rmiUrls = new StringBuffer();
		if (rmiUrlNum != null) {
			for (int i = 0; i < rmiUrlNum; i++) {
				rmiUrls.append(rmiUrl.replace("${serviceName}", serviceName + "-" + i));
			}
		} else {
			rmiUrlNum = 1;
			rmiUrls.append(rmiUrl.replace("${serviceName}", serviceName + "-0"));
		}
		rmiUrl = rmiUrls.toString().replace("${port}", port);
		return rmiUrl;
	}
	
	public void createStatefulSet(AppsV1beta2Api appsV1beta2Api, String safefulSetName, Integer replicas, String livePath, String initImage, String image, Integer memory, Integer cpu) throws Exception {
		String safefulSetNameLower = safefulSetName.toLowerCase();
		// 替换文件中的参数
		String command = "[\"cp\", \"-r\", \"/${project}/.\", \"/app/${contextName}\"]";
		// project 如果这个注释还在,project就还没改,这个要改
		command = command.replace("${project}", "APP");
		command = command.replace("${contextName}", safefulSetName);
		Map<String, String> args = new HashMap<String, String>();
		args.put("${statefulSetName}", safefulSetNameLower);
		args.put("${serviceName}", safefulSetNameLower);
		args.put("${configMapName}", safefulSetNameLower);
		args.put("${replicas}", replicas + "");
		args.put("${command}", command);
		args.put("${livePath}", "/"+safefulSetName+"/" + livePath);
		args.put("${initImage}", initImage);
		args.put("${image}", image);
		args.put("${cpu}", cpu + "");
		args.put("${memory}", memory + "");
		String path = currentPath + "statefulset.yaml";
		path = URLDecoder.decode(path, "utf-8");
		String content = KubernetesUtil.replaceArgs(path, args);
		V1beta2StatefulSet v1beta2StatefulSet = Yaml.loadAs(content, V1beta2StatefulSet.class);
		appsV1beta2Api.createNamespacedStatefulSet(namespace, v1beta2StatefulSet, null);
	}
	
	public void deleteStatefulSet(AppsV1beta2Api appsV1beta2Api,CoreV1Api coreV1Api, String safefulSetName) {
		try {
			safefulSetName = safefulSetName.toLowerCase();
			V1PodList podList = coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null);
			List<V1Pod> deletePod = new ArrayList<V1Pod>();
			for (V1Pod v1Pod : podList.getItems()) {
				List<V1OwnerReference> ownerReferences = v1Pod.getMetadata().getOwnerReferences();
				if(ownerReferences != null) {
					String name = ownerReferences.get(0).getName();
					if(safefulSetName.equals(name)) {
						deletePod.add(v1Pod);
					}
				}
			}
			try {
				appsV1beta2Api.deleteNamespacedStatefulSet(safefulSetName, namespace, new V1DeleteOptions(), null, null, null, null);
			}catch(JsonSyntaxException e ) {
				if (e.getCause() instanceof IllegalStateException) {
	                IllegalStateException ise = (IllegalStateException) e.getCause();
	                if (ise.getMessage() != null && ise.getMessage().contains("Expected a string but was BEGIN_OBJECT")) {
	                	logger.debug("Catching exception because of issue https://github.com/kubernetes-client/java/issues/86", e);
	                }else { 
	                	throw e;
	                }
	            }
			}
			for (V1Pod v1Pod : deletePod) {
				try {
					coreV1Api.deleteNamespacedPod(v1Pod.getMetadata().getName(), "default", new V1DeleteOptions(), null, null, null, null);
				}catch(JsonSyntaxException e ) {
					if (e.getCause() instanceof IllegalStateException) {
		                IllegalStateException ise = (IllegalStateException) e.getCause();
		                if (ise.getMessage() != null && ise.getMessage().contains("Expected a string but was BEGIN_OBJECT")) {
		                	logger.debug("Catching exception because of issue https://github.com/kubernetes-client/java/issues/86", e);
		                }else { 
		                	throw e;
		                }
		            }
				}
			}
		}catch(Exception e) {
		}
	}
	
	
	public void updateStatefulSet(AppsV1beta2Api appsV1beta2Api, CoreV1Api coreV1Api, String oldSafefulSetName, String newSafefulSetName, Integer replicas, String livePath, String initImage, String image, Integer memory, Integer cpu) throws Exception {
		boolean rename = false;
		String nativeSafefulSetName = oldSafefulSetName;
		oldSafefulSetName = oldSafefulSetName.toLowerCase();
		newSafefulSetName = newSafefulSetName.toLowerCase();
		if(StringUtils.isNotBlank(newSafefulSetName) && (!oldSafefulSetName.equals(newSafefulSetName))) {
			V1beta2StatefulSet getStatefulSet = null;
			try {
				getStatefulSet = appsV1beta2Api.readNamespacedStatefulSet(newSafefulSetName, namespace, null, null, null);
			}catch(ApiException e) {}
			if(getStatefulSet != null) {
				logger.info("[" + newSafefulSetName + "]" + "StatefulSet已经存在!");
				throw new Exception("[" + newSafefulSetName + "]" + "StatefulSet已经存在!");
			}
			rename = true;
		}
		
		V1beta2StatefulSet readNamespacedStatefulSet = appsV1beta2Api.readNamespacedStatefulSet(oldSafefulSetName, namespace, null, null, null);
		
		if(replicas == null) {
			replicas = readNamespacedStatefulSet.getSpec().getReplicas();
		}
		if(StringUtils.isBlank(livePath)) {
			livePath = readNamespacedStatefulSet.getSpec().getTemplate().getSpec().getContainers().get(0).getLivenessProbe().getHttpGet().getPath();
		}
		if(StringUtils.isBlank(initImage)) {
			initImage = readNamespacedStatefulSet.getSpec().getTemplate().getSpec().getInitContainers().get(0).getImage();
		}
		if(StringUtils.isBlank(image)) {
			image = readNamespacedStatefulSet.getSpec().getTemplate().getSpec().getContainers().get(0).getImage();
		}
		
		if(rename) {
			this.deleteStatefulSet(appsV1beta2Api, coreV1Api, oldSafefulSetName);
			this.createStatefulSet(appsV1beta2Api, newSafefulSetName, replicas, livePath, initImage, image, memory, cpu);
		}else {
			readNamespacedStatefulSet.getSpec().setReplicas(replicas);
			readNamespacedStatefulSet.getSpec().getTemplate().getSpec().getContainers().get(0).getLivenessProbe().getHttpGet().setPath(nativeSafefulSetName +"/"+livePath);
			readNamespacedStatefulSet.getSpec().getTemplate().getSpec().getInitContainers().get(0).setImage(initImage);
			readNamespacedStatefulSet.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(image);
			appsV1beta2Api.replaceNamespacedStatefulSet(oldSafefulSetName, namespace, readNamespacedStatefulSet, null);
			//this.createStatefulSet(appsV1beta2Api, oldSafefulSetName, replicas, livePath, initImage, image, memory, cpu);
		}
	}
}
