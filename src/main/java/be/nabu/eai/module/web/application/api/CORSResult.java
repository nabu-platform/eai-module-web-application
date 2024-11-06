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

public interface CORSResult {
	// the methods that are allowed
	public List<String> getMethods();
	// whether this origin is allowed
	public Boolean getAllowed();
	// credentials are cookies, authorization headers, or TLS client certificates
	public Boolean getAllowedCredentials();
	// if the above is true, are all origins allowed or just this one?
	public Boolean getAllowedAll();
}
