package io.github.talelin.core.annotation;

import java.lang.annotation.*;

/**
 * 路由信息，记录路由权限、模块等信息
 *
 * @author pedro@TaleLin
 * @author colorful@TaleLin
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PermissionMeta {

	/** 权限名称 */
	String value();

	@Deprecated
	/** 权限名称(已弃用) */
	String permission() default "";

	/** 所属模块 */
	String module() default "";

	/** 是否有效 */
	boolean mount() default true;

}
