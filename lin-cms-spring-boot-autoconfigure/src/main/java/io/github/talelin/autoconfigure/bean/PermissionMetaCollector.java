package io.github.talelin.autoconfigure.bean;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import io.github.talelin.core.annotation.AdminMeta;
import io.github.talelin.core.annotation.GroupMeta;
import io.github.talelin.core.annotation.LoginMeta;
import io.github.talelin.core.annotation.PermissionMeta;
import io.github.talelin.core.annotation.PermissionModule;
import io.github.talelin.core.enumeration.UserLevel;
import io.github.talelin.core.util.AnnotationUtil;

/**
 * 路由信息收集器
 *
 * @author pedro@TaleLin
 * @author Juzi@TaleLin
 * @author colorful@TaleLin
 */
// BeanPostProcessor 是 Spring IOC 容器给我们提供的一个扩展接口
public class PermissionMetaCollector implements BeanPostProcessor {

	private Map<String, MetaInfo> metaMap = new ConcurrentHashMap<>();

	private Map<String, Map<String, Set<String>>> structuralMeta = new ConcurrentHashMap<>();

	public PermissionMetaCollector() {
	}

	/**
	 * bean初始化方法调用前被调用
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	/**
	 * 扫描注解信息，并提取
	 *
	 * @param bean     spring bean
	 * @param beanName 名称
	 * @return spring bean
	 */
	/**
	 * bean初始化方法调用后被调用
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {

		// AnnotationUtils工具类获取类、或类方法的注释对象内容
		Controller controllerAnnotation = AnnotationUtils.findAnnotation(bean.getClass(), Controller.class);
		// 非 Controller 类，无需检查权限信息
		if (controllerAnnotation == null) {
			return bean;
		}

//		System.out.println("bean初始化方法调用后被调用");
//		System.out.println("--------------");
//		System.out.println(beanName + ":" + bean.toString());
//		System.out.println("--------------");

		// Spring针对反射提供的工具类：ReflectionUtils
		Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());

		for (Method method : methods) {

//			System.out.println("-------------");
//			System.out.println(method.getName());
//			System.out.println("-------------");

			AdminMeta adminMeta = AnnotationUtils.findAnnotation(method, AdminMeta.class);

			if (adminMeta != null && adminMeta.mount()) {
//				System.out.println(adminMeta);
				String permission = StringUtils.isEmpty(adminMeta.value()) ? adminMeta.permission() : adminMeta.value();
				putOneMetaInfo(method, permission, adminMeta.module(), UserLevel.ADMIN);
				continue;
			}

			GroupMeta groupMeta = AnnotationUtils.findAnnotation(method, GroupMeta.class);

			if (groupMeta != null && groupMeta.mount()) {
//				System.out.println(groupMeta);
				String permission = StringUtils.isEmpty(groupMeta.value()) ? groupMeta.permission() : groupMeta.value();
				putOneMetaInfo(method, permission, groupMeta.module(), UserLevel.GROUP);
				continue;
			}

			LoginMeta loginMeta = AnnotationUtils.findAnnotation(method, LoginMeta.class);
			if (loginMeta != null && loginMeta.mount()) {
//				System.out.println(loginMeta);
				String permission = StringUtils.isEmpty(loginMeta.value()) ? loginMeta.permission() : loginMeta.value();
				putOneMetaInfo(method, permission, loginMeta.module(), UserLevel.LOGIN);
				continue;
			}
			// 最后寻找 PermissionMeta
			PermissionMeta permissionMeta = AnnotationUtils.findAnnotation(method, PermissionMeta.class);
			if (permissionMeta != null && permissionMeta.mount()) {
				// @io.github.talelin.core.annotation.PermissionMeta(module=图书, mount=true,
				// permission=, value=删除图书)
				String permission = StringUtils.isEmpty(permissionMeta.value()) ? permissionMeta.permission()
						: permissionMeta.value();

				UserLevel level = AnnotationUtil.findRequired(method.getAnnotations());
				putOneMetaInfo(method, permission, permissionMeta.module(), level);
			}
		}
		return bean;
	}

	/**
	 * @param	method		权限方法
	 * @param	permission	权限名称 
	 * @param	module		权限模块
	 * @param	userLevel	权限级别
	 * */
	private void putOneMetaInfo(Method method, String permission, String module, UserLevel userLevel) {
		System.out.println("-----------");
		System.out.println("权限方法:" + method.getName());
		System.out.println("权限方法所在类:" + method.getDeclaringClass());
		System.out.println("权限名称:"+permission);
		System.out.println("权限模块:"+module);
		System.out.println("权限级别:"+userLevel);
		System.out.println("-----------");
		
		if (StringUtils.isEmpty(module)) {
			PermissionModule permissionModule = AnnotationUtils.findAnnotation(method.getDeclaringClass(),
					PermissionModule.class);
			if (permissionModule != null) {
				module = StringUtils.isEmpty(permissionModule.value()) ? method.getDeclaringClass().getName()
						: permissionModule.value();
			}
		}
		String methodName = method.getName();
		String className = method.getDeclaringClass().getName();
		String identity = className + "#" + methodName;
		MetaInfo metaInfo = new MetaInfo(permission, module, identity, userLevel);
		metaMap.put(identity, metaInfo);
		this.putMetaIntoStructuralMeta(identity, metaInfo);
	}

	private void putMetaIntoStructuralMeta(String identity, MetaInfo meta) {
		String module = meta.getModule();
		String permission = meta.getPermission();
		// 如果已经存在了该 module，直接向里面增加
		if (structuralMeta.containsKey(module)) {
			Map<String, Set<String>> moduleMap = structuralMeta.get(module);
			// 如果 permission 已经存在
			this.putIntoModuleMap(moduleMap, identity, permission);
		} else {
			// 不存在 该 module，创建该 module
			Map<String, Set<String>> moduleMap = new HashMap<>();
			// 如果 permission 已经存在
			this.putIntoModuleMap(moduleMap, identity, permission);
			structuralMeta.put(module, moduleMap);
		}
	}

	private void putIntoModuleMap(Map<String, Set<String>> moduleMap, String identity, String auth) {
		if (moduleMap.containsKey(auth)) {
			moduleMap.get(auth).add(identity);
		} else {
			Set<String> eps = new HashSet<>();
			eps.add(identity);
			moduleMap.put(auth, eps);
		}
	}

	/**
	 * 获取路由信息map
	 *
	 * @return 路由信息map
	 */
	public Map<String, MetaInfo> getMetaMap() {
		return metaMap;
	}

	public MetaInfo findMeta(String key) {
		return metaMap.get(key);
	}

	public MetaInfo findMetaByPermission(String permission) {
		Collection<MetaInfo> values = metaMap.values();
		MetaInfo[] objects = values.toArray(new MetaInfo[0]);
		for (MetaInfo object : objects) {
			if (object.getPermission().equals(permission)) {
				return object;
			}
		}
		return null;
	}

	public void setMetaMap(Map<String, MetaInfo> metaMap) {
		this.metaMap = metaMap;
	}

	/**
	 * 获得结构化路由信息
	 *
	 * @return 路由信息
	 */
	public Map<String, Map<String, Set<String>>> getStructuralMeta() {
		return structuralMeta;
	}

	public void setStructrualMeta(Map<String, Map<String, Set<String>>> structuralMeta) {
		this.structuralMeta = structuralMeta;
	}
}
