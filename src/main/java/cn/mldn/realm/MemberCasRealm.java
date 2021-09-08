package cn.mldn.realm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cas.CasRealm;
import org.apache.shiro.cas.CasToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.util.CollectionUtils;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidationException;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.util.StringUtils;

import cn.mldn.service.IMemberService;

@SuppressWarnings("deprecation")
public class MemberCasRealm extends CasRealm { 
	// 所有的认证的处理都应该交由CAS负责完成，
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		// 此时接受的AuthenticationToken对象实际上是CasToken类型
		CasToken casToken = (CasToken) token;
		if (casToken == null) {
			return null;
		}
		// CAS一定要返回给用户一个票根，所以需要取得这个票根的内容
		String ticket = (String) casToken.getCredentials();
		// 需要对票根的有效性进行验证
		if (!StringUtils.hasText(ticket)) {
			return null;
		}
		// 如果现在票根验证的格式正确，那么需要进行票根有效性的验证
		TicketValidator ticketValidator = super.ensureTicketValidator(); // 验证票根的有效性
		// 首先需要针对于票根的CAS做一个验证处理
		Assertion validate;
		try {
			validate = ticketValidator.validate(ticket, super.getCasService());
			// 当验证处理完成之后，应该通过CAS取得用户信息
			AttributePrincipal principal = validate.getPrincipal();
			// 取得当前登录的用户名
			String mid = principal.getName();
			// 取出用户名之后需要将所有的相关信息（包括CAS相关信息）一起进行一个列表的创建
			List principals = CollectionUtils.asList(mid, principal.getAttributes());
			PrincipalCollection principalCollection = new SimplePrincipalCollection(principals, super.getName());
			return new SimpleAuthenticationInfo(principalCollection, ticket);
		} catch (TicketValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return super.doGetAuthenticationInfo(null);

	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		System.out.println("********** 2、用户角色与权限：doGetAuthorizationInfo **********");
		String username = (String) principals.getPrimaryPrincipal(); // 取得用户登录名
		SimpleAuthorizationInfo auth = new SimpleAuthorizationInfo(); // 定义授权信息的返回数据
		try {
			Map<String, Object> map = this.memberService.listAuthByMember(username);
			Set<String> allRoles = (Set<String>) map.get("allRoles");
			Set<String> allActions = (Set<String>) map.get("allActions");
			auth.setRoles(allRoles);// 所有的角色必须以Set集合的形式出现
			auth.setStringPermissions(allActions); // 所有的权限必须以Set集合的形式出现
		} catch (Exception e) {
			e.printStackTrace();
		}
		return auth;
	}

	@Resource
	private IMemberService memberService ;

}
