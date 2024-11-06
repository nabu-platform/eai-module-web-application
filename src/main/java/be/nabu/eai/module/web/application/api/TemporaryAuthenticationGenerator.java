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

import java.util.Date;

import javax.jws.WebParam;
import javax.validation.constraints.NotNull;

import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.types.base.Duration;

public interface TemporaryAuthenticationGenerator {
	public TemporaryAuthentication generate(
		// the web application this is for
		@NotNull @WebParam(name = "webApplicationId") String webApplicationId,
		// the realm (although this can be deduced from the web application?)
		@NotNull @WebParam(name = "realm") String realm, 
		// the alias of the user you want to authenticate as
		@NotNull @WebParam(name = "alias") String alias,
		// the authentication id is a unique reference to the realm and alias combination
		@WebParam(name = "authenticationId") String authenticationId,
		// how many times it can be used
		@WebParam(name = "maxUses") Integer maxUses, 
		// until when it can be used
		@WebParam(name = "until") Date until, 
		// you may set an until for example 1 day in the future, this is a hard cutoff
		// timeout can be added at say 30minutes meaning if you don't use it for 30 minutes, it expires
		@WebParam(name = "timeout") Duration timeout,
		// the type of authentication, for example a temporary authentication for file downloads should not be usable for fully authenticating
		// so you define the usecase for this temporary authentication
		@WebParam(name = "type") String type,
		// you can pre-generate the secret according to your own rules
		// if not, one will be generated for you
		@WebParam(name = "secret") String secret, 
		// this allows you to correlate it to something else
		@WebParam(name = "correlationId") String correlationId,
		// the device this is requested on
		@WebParam(name = "device") Device device,
		@WebParam(name = "impersonator") String impersonator,
		@WebParam(name = "impersonatorRealm") String impersonatorRealm,
		// when you are impersonating someone else, store the id of the original impersonator!
		@WebParam(name = "impersonatorId") String impersonatorId,
		// when the temporary authentication is resolved into a token, you can give it a particular token id
		@WebParam(name = "tokenId") String tokenId,
		// when a temporary authentication is resolved into a token, you can set the authenticator
		@WebParam(name = "authenticator") String authenticator);
}
