package com.tutoring.filter;

import com.tutoring.dao.UserDao;
import com.tutoring.entity.User;
import com.tutoring.enumeration.ErrorCode;
import com.tutoring.exception.CustomException;
import com.tutoring.util.JwtUtils;
import com.tutoring.util.UserRoleUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 每次请求前执行:
 * 1) 从Header中取Token
 * 2) 验证Token
 * 3) 将用户信息放入SecurityContext
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDao userDao;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 如果拿到了 Token，则验证它
        if (token != null && jwtUtils.validateToken(token)) {
            Claims claims = jwtUtils.getClaims(token);
            String userIdStr = claims.getSubject(); // subject里放的是 userId
            String roleStr = (String) claims.get("role");

            try {
                Long userId = Long.valueOf(userIdStr);
                // 从 DB 获取用户
                User user = userDao.selectById(userId);
                if (user == null) {
                    throw new CustomException(ErrorCode.UNAUTHORIZED, "User not found.");
                }

                // 判断用户角色是否一致
                if (user.getRole().name().equals(roleStr)) {
                    // 构建 Security 认证对象
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    user, // Principal
                                    null, // Credentials
                                    UserRoleUtil.buildAuthorities(user.getRole())
                            );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置到上下文
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (NumberFormatException e) {
                log.warn("Token subject不是数字类型: {}", userIdStr);
            }
        }

        // 放行
        filterChain.doFilter(request, response);
    }
}


//这里的逻辑是将 JWT 过滤器作为整个安全过滤链的一部分，它不会主动拒绝请求，而是负责从请求中解析出 JWT 并设置安全上下文。如果没有有效的 JWT，
// 它不会阻止请求继续向下传递，而是让后续的 Spring Security 组件来决定是否允许访问受保护的资源。下面详细说明：
//
//所有请求都会进入过滤器
//
//无论用户访问的是注册页面、登录页面还是其他受保护的接口，JwtAuthenticationFilter 的 doFilterInternal 都会被调用。
// 但这并不意味着每个请求都需要携带 JWT。
//对于登录、注册等公开接口，Spring Security 的配置（见 SecurityConfig）使用了 permitAll()，允许这些接口不经过身份验证即可访问。
//公开接口与 JWT 过滤器
//
//当用户访问注册或登录页面时，虽然 JwtAuthenticationFilter 依然执行，但一般来说：
//请求头中通常不会包含 Authorization 字段（没有 JWT）。
//过滤器在判断到没有 Token 或者 Token 无效（jwtUtils.validateToken(token) 返回 false）时，不会在 SecurityContext 中设置认证对象。
//后续 Spring Security 的配置中，对于 /user/signup、/user/verify-code、/user/login 这些路径是 permitAll()，因此即使没有认证信息，
// 访问依然被允许。
//Token 失效或无效的情况
//
//如果请求中携带的 JWT 无效或已过期，那么 jwtUtils.validateToken(token) 就会返回 false。
//这时过滤器不会进一步解析 Token，也不会设置 SecurityContext。
//关键点： 过滤器本身并不拒绝请求，而是让后续的安全配置做判断。如果是需要认证的接口（未在 permitAll 列表中），
// 那么因为 SecurityContext 中没有认证信息，后续的 Spring Security 处理会拒绝请求（通常返回 401 或 403）。
//对于公开接口（如登录或注册），即使 Token 无效或缺失，由于这些接口不需要认证，所以请求最终会被放行。
//代码的设计思路
//
//职责单一：JWT 过滤器只负责解析和校验 Token，并将认证信息填入 SecurityContext。如果 Token 不存在或不合法，它不会主动阻断请求，
// 而是让安全框架根据配置决定是否允许访问。
//安全配置负责拦截：在 SecurityConfig 中，你通过 antMatchers(...).permitAll() 明确规定了哪些路径可以不认证访问，
// 其余路径则要求请求必须有合法认证信息。这种设计将 JWT 的解析与请求授权分离，使逻辑更加清晰。
//总结
//
//当用户访问公开接口（如注册、登录）时，即使 JWT 过滤器执行了，但由于没有 Token或 Token无效，SecurityContext 中没有认证信息，
// 而这些接口本身被配置为不需要认证，所以请求会顺利通过。
//当用户访问受保护的接口时，如果请求中没有合法 JWT，过滤器不会设置认证信息，后续 Spring Security 检查到没有认证信息，就会拒绝该请求。
