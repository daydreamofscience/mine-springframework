/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.yankaizhang.springframework.webmvc.multipart.commons;

import com.yankaizhang.springframework.webmvc.multipart.MultipartFile;
import org.apache.commons.fileupload.DefaultFileItem;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 使用了apache-fileupload包
 * MultipartFile具体实现
 * @author dzzhyk
 */
public class CommonsMultipartFile implements MultipartFile {

	protected final Logger logger = LoggerFactory.getLogger(CommonsMultipartFile.class);

	/**
	 * 真正的文件对象
	 */
	private final FileItem fileItem;

	/**
	 * 根据获取的apache fileItem对象创建本类
	 */
	protected CommonsMultipartFile(FileItem fileItem) {
		this.fileItem = fileItem;
	}

	/**
	 * 返回真正的文件对象（几乎用不到）
	 */
	public FileItem getFileItem() {
		return fileItem;
	}

	@Override
	public String getName() {
		return this.fileItem.getFieldName();
	}

	@Override
	public boolean isEmpty() {
		return (this.fileItem.getName() == null || this.fileItem.getName().length() == 0);
	}

	@Override
	public String getOriginalFilename() {
		return (!isEmpty() ? new File(this.fileItem.getName()).getName() : null);
	}

	@Override
	public String getContentType() {
		return (!isEmpty() ? this.fileItem.getContentType() : null);
	}

	@Override
	public long getSize() {
		return this.fileItem.getSize();
	}

	@Override
	public byte[] getBytes() {
		return this.fileItem.get();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.fileItem.getInputStream();
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		if (dest.exists() && !dest.delete()) {
			throw new IOException("Destination file [" + dest.getAbsolutePath() +
			                      "] already exists and could not be deleted");
		}
		try {
			this.fileItem.write(dest);
			if (logger.isDebugEnabled()) {
				String action = "transferred";
				if (this.fileItem instanceof DefaultFileItem) {
					action = ((DefaultFileItem) this.fileItem).getStoreLocation().exists() ? "copied" : "moved";
				}
				logger.debug("Multipart file [" + getName() + "] with original file name [" +
										 getOriginalFilename() + "], stored " + getStorageDescription() + ": " +
				             action + " to [" + dest.getAbsolutePath() + "]");
			}
		}
		catch (FileUploadException ex) {
			throw new IllegalStateException(ex.getMessage());
		}
		catch (IOException ex) {
			throw ex;
		}
		catch (Exception ex) {
			logger.error("Could not transfer to file", ex);
			throw new IOException("文件转写失败: " + ex.getMessage());
		}
	}

	protected String getStorageDescription() {
		if (this.fileItem.isInMemory()) {
			return "in memory";
		}
		else if (this.fileItem instanceof DefaultFileItem) {
			return "at [" + ((DefaultFileItem) this.fileItem).getStoreLocation().getAbsolutePath() + "]";
		}
		else {
			return "at disk";
		}
	}

}
