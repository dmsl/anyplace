/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Christakis Achilleos, Constandinos Demetriou, Marcos Antonios Charalambous
 *
 * Co-supervisor: Paschalis Mpeis
 * 
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: http://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2019, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */

package cy.ac.ucy.anyplace;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.GzipSource;
import okio.Okio;

public class UnzippingInterceptor implements Interceptor {
	@Override
	public Response intercept(Chain chain) throws IOException {
		Response response = chain.proceed(chain.request());
		return unzip(response);

	}

	// copied from okhttp3.internal.http.HttpEngine (because is private)
	private Response unzip(final Response response) throws IOException {
		if (response.body() == null) {
			return response;
		}

		// check if we have gzip response
		String contentEncoding = response.headers().get("Content-Encoding");

		// this is used to decompress gzipped responses
		if (contentEncoding != null && contentEncoding.equals("gzip")) {
			Long contentLength = response.body().contentLength();
			GzipSource responseBody = new GzipSource(response.body().source());
			Headers strippedHeaders = response.headers().newBuilder().build();
			return response.newBuilder().headers(strippedHeaders)
					.body(new RealResponseBody(response.body().contentType().toString(), contentLength,
							Okio.buffer(responseBody)))
					.build();
		} else {
			return response;
		}
	}
}
