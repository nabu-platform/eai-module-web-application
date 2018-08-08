package be.nabu.eai.module.web.application.api;

import java.util.List;

import be.nabu.libs.authentication.api.Permission;

// the link between permissions and roles should be configurable, not hardcoded anywhere
// however, because the web system allows you to set either a role handler and/or a permission handler
// we often fill them both in allowing you to choose how fine grained your permission should be
// for things like management services, admin services, page editing etc, it can be interesting to get this relation
// however it is currently explicitly not in the authentication API as it is a descriptive relation, not a functional one
public interface PermissionWithRole extends Permission {
	public List<String> getRoles();
}
