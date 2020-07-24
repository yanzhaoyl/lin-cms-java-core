package io.github.talelin.autoconfigure.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.extern.slf4j.Slf4j;

/**
 * 请求工具类
 *
 * @author pedro@TaleLin
 * @author Juzi@TaleLin
 */

@Slf4j
public class RequestUtil {

	/**
	 * 获得当前请求
	 *
	 * @return Request 对象，如果没有绑定会返回 null
	 */
	public static HttpServletRequest getRequest() {

		/**
		 * RequestContextHolder 持有上下文的Request容器
		 */

		// 从请求上下文中获取请求属性
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		// 从请求上下文中获取当前请求属性
//		RequestAttributes requestAttributes2 = RequestContextHolder.currentRequestAttributes();

		// 当前线程没有绑定 Request
		if (requestAttributes == null) {
			return null;
		}

		if (requestAttributes instanceof ServletRequestAttributes) {
			// 拿到 当前的 Request 请求
			ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
			return servletRequestAttributes.getRequest();
		} else {
			return null;
		}
	}

	/**
	 * 获得请求 url
	 *
	 * @return url
	 */
	public static String getRequestUrl() {
		HttpServletRequest request = RequestUtil.getRequest();
		if (request == null) {
			return null;
		}
		return request.getServletPath();
	}

	/**
	 * 获得请求简略信息
	 *
	 * @param request 请求
	 * @return 简略信息
	 */
	public static String getSimpleRequest(HttpServletRequest request) {

		// log.debug("RequestUtil.getSimpleRequest() 方法，当前线程为 {}，请求方法为 {}，请求路径为：{}", Thread.currentThread().getName(), request.getMethod(),
		// 		request.getRequestURL().toString());

		return request.getMethod() + " " + request.getServletPath();
	}

	/**
	 * 获得请求简略信息
	 *
	 * @return 简略信息
	 */
	public static String getSimpleRequest() {
		HttpServletRequest request = getRequest();
		if (request == null) {
			return null;
		}
		return request.getMethod() + " " + request.getServletPath();
	}
}
