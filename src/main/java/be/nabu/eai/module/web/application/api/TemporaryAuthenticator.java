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

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;

public interface TemporaryAuthenticator {
	
	public static final String EXECUTION = "execution";
	public static final String AUTHENTICATION = "authentication";
	
	@WebResult(name = "token")
	public Token authenticate(@NotNull @WebParam(name = "realm") String realm, 
			@WebParam(name = "authentication") @NotNull TemporaryAuthentication authentication, 
			@WebParam(name = "device") Device device, 
			@NotNull @WebParam(name = "type") String type, 
			@WebParam(name = "correlationId") String correlationId);
}
