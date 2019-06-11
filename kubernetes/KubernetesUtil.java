package com.dtsz.baseclass.web.util.kubernetes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.AppsV1beta2Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.util.ClientBuilder;

/**
 * @ClassName: KubernetesUtil
 * @Description: TODO
 * @see:
 * @date: 2019年1月24日 下午2:51:51
 * @version :1.0
 */
public class KubernetesUtil {

	protected static Logger logger = Logger.getLogger(KubernetesUtil.class.getName());

	public static SSLSocketFactory getSocketFactory(String password, String keyStorePath) throws Exception {
		KeyStore keyStore = getKeyStore(password, keyStorePath);

		// 自定义信任管理器,信任任何证书
		X509TrustManager tm = new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		KeyManagerFactory kf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		char[] pwdChars = password.toCharArray();
		kf.init(keyStore, pwdChars);

		// https站点强制通信协议TLSv1.2
		SSLContext sc = SSLContext.getInstance("TLSv1.2");
		sc.init(kf.getKeyManagers(), new TrustManager[] { tm }, null);
		return sc.getSocketFactory();
	}

	/**
	 * 获得KeyStore.
	 * 
	 * @param keyStorePath
	 *            密钥库路径
	 * @param password
	 *            密码
	 * @return 密钥库
	 * @throws Exception
	 */
	public static KeyStore getKeyStore(String password, String keyStorePath) throws Exception {
		// 实例化密钥库 KeyStore用于存放证书，创建对象时 指定交换数字证书的加密标准
		// 指定交换数字证书的加密标准
		KeyStore ks = KeyStore.getInstance("PKCS12");
		// 获得密钥库文件流
		FileInputStream is = new FileInputStream(keyStorePath);
		// 加载密钥库
		ks.load(is, password.toCharArray());
		// 关闭密钥库文件流
		is.close();
		return ks;
	}

	/**
	 * 带上证书获取k8s的CoreV1Api
	 * 
	 * @param keyStorePath
	 *            秘钥路径(证书路径),例:D:\\test\\client.pfx
	 * @param keyStorePassword
	 *            密码(证书密码),例:1
	 * @param k8sUrl
	 *            kubernetes地址,例:https://192.168.148.130:6443
	 * @return
	 * @throws Exception
	 */
	public static CoreV1Api getCoreV1Api(String keyStorePath, String keyStorePassword, String k8sUrl) throws Exception {
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.setBasePath(k8sUrl);
		ApiClient client = clientBuilder.build();
		SSLSocketFactory sslSocketFactory = KubernetesUtil.getSocketFactory(keyStorePassword, keyStorePath);
		client.getHttpClient().setSslSocketFactory(sslSocketFactory);
		Configuration.setDefaultApiClient(client);
		CoreV1Api api = new CoreV1Api();
		return api;
	}

	/**
	 * 带上证书获取k8s的ExtensionsV1beta1Api
	 * 
	 * @param keyStorePath
	 *            秘钥路径(证书路径),例:D:\\test\\client.pfx
	 * @param keyStorePassword
	 *            密码(证书密码),例:1
	 * @param k8sUrl
	 *            kubernetes地址,例:https://192.168.148.130:6443
	 * @return
	 * @throws Exception
	 */
	public static ExtensionsV1beta1Api getExtensionsV1beta1Api(String keyStorePath, String keyStorePassword,
			String k8sUrl) throws Exception {
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.setBasePath(k8sUrl);

		ApiClient client = clientBuilder.build();

		SSLSocketFactory sslSocketFactory = KubernetesUtil.getSocketFactory(keyStorePassword, keyStorePath);
		client.getHttpClient().setSslSocketFactory(sslSocketFactory);
		Configuration.setDefaultApiClient(client);
		ExtensionsV1beta1Api extensionsV1beta1Api = new ExtensionsV1beta1Api();
		return extensionsV1beta1Api;
	}

	/**
	 * 带上证书获取k8s的AppsV1beta2Api
	 * 
	 * @param keyStorePath
	 *            秘钥路径(证书路径),例:D:\\test\\client.pfx
	 * @param keyStorePassword
	 *            密码(证书密码),例:1
	 * @param k8sUrl
	 *            kubernetes地址,例:https://192.168.148.130:6443
	 * @return
	 * @throws Exception
	 */
	public static AppsV1beta2Api getAppsV1beta2Api(String keyStorePath, String keyStorePassword, String k8sUrl)
			throws Exception {
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.setBasePath(k8sUrl);
		ApiClient client = clientBuilder.build();
		SSLSocketFactory sslSocketFactory = KubernetesUtil.getSocketFactory(keyStorePassword, keyStorePath);
		client.getHttpClient().setSslSocketFactory(sslSocketFactory);
		Configuration.setDefaultApiClient(client);
		AppsV1beta2Api appsV1beta2Api = new AppsV1beta2Api();
		return appsV1beta2Api;
	}

	/**
	 * 替换指定文件中的参数
	 * 
	 * @param filePath
	 *            文件路径,例:D:\\test\\ingress.yaml
	 * @param args
	 *            替换参数,例:Map<"${port}","8080">
	 * @return
	 * @throws Exception
	 */
	public static String replaceArgs(String path, Map<String, String> args) throws Exception {
		//读取文件
		File file = new File(path);
		return replaceArgs(file, args);
	}
	
	/**
	 * 替换指定文件中的参数
	 * 
	 * @param file
	 *            要替换的文件
	 * @param args
	 *            替换参数,例:Map<"${port}","8080">
	 * @return
	 * @throws Exception
	 */
	public static String replaceArgs(File file, Map<String, String> args) throws Exception {
		//获取输入流
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		StringBuffer out = new StringBuffer();
		// 替换
		String line = null;
		Set<String> keySet = args.keySet();
		// 以行为单位进行遍历
		while ((line = br.readLine()) != null) {
			// 替换每一行中符合被替换字符条件的字符串
			for (String replaceKey : keySet) {
				line = line.replace(replaceKey, args.get(replaceKey));
			}
			out.append(line);
			// 添加换行符，并进入下次循环
			out.append(System.getProperty("line.separator"));
		}
		// 关闭输入流
		br.close();
		return out.toString();
	}
}
