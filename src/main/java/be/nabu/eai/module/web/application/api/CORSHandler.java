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

import java.net.URI;

import javax.jws.WebParam;
import javax.jws.WebResult;

public interface CORSHandler {
	// the origin is the value of the origin header, the uri is the target uri being accessed
	@WebResult(name = "cors")
	public CORSResult check(@WebParam(name = "applicationId") String applicationId, @WebParam(name = "origin") String origin, @WebParam(name = "uri") URI uri);
}
