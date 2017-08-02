/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
* 
* Author(s):
* //http://stackoverflow.com/questions/22932821/httpclient-post-with-progress-and-multipartentitybuilder
* Timotheos Constambeys
*  
* Supervisor: Demetrios Zeinalipour-Yazti
*
* URL: http://anyplace.cs.ucy.ac.cy
* Contact: anyplace@cs.ucy.ac.cy
*
* Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
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

package com.dmsl.anyplace.utils;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * HttpEntityWrapper with a progress callback
 *
 * @see <a
 *      href="http://stackoverflow.com/a/7319110/268795">http://stackoverflow.com/a/7319110/268795</a>
 */
public class ProgressHttpEntityWrapper extends HttpEntityWrapper {
	private final ProgressCallback progressCallback;

	public static interface ProgressCallback {
		public void progress(float progress);
	}

	public ProgressHttpEntityWrapper(final HttpEntity entity, final ProgressCallback progressCallback) {
		super(entity);
		this.progressCallback = progressCallback;
	}

	@Override
	public void writeTo(final OutputStream out) throws IOException {
		this.wrappedEntity.writeTo(out instanceof ProgressFilterOutputStream ? out : new ProgressFilterOutputStream(out, this.progressCallback, getContentLength()));
	}

	static class ProgressFilterOutputStream extends FilterOutputStream {
		private final ProgressCallback progressCallback;
		private long transferred;
		private long totalBytes;

		ProgressFilterOutputStream(final OutputStream out, final ProgressCallback progressCallback, final long totalBytes) {
			super(out);
			this.progressCallback = progressCallback;
			this.transferred = 0;
			this.totalBytes = totalBytes;
		}

		@Override
		public void write(final byte[] b, final int off, final int len) throws IOException {
			// super.write(byte b[], int off, int len) calls write(int b)
			out.write(b, off, len);
			this.transferred += len;
			this.progressCallback.progress(getCurrentProgress());
		}

		@Override
		public void write(final int b) throws IOException {
			out.write(b);
			this.transferred++;
			this.progressCallback.progress(getCurrentProgress());
		}

		private float getCurrentProgress() {
			return ((float) this.transferred / this.totalBytes) * 100;
		}
	}
}
