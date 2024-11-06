/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
