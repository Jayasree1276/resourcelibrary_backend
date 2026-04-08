package com.example.library.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String subject;
    private String type;
    private String fileUrl;
    private Long uploaderId;
    private String uploaderEmail;
    private String uploaderName;
    
    public Resource() {}

    public Resource(Long id, String title, String subject, String type, String fileUrl, Long uploaderId,
            String uploaderEmail, String uploaderName) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.type = type;
        this.fileUrl = fileUrl;
        this.uploaderId = uploaderId;
        this.uploaderEmail = uploaderEmail;
        this.uploaderName = uploaderName;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	public Long getUploaderId() {
		return uploaderId;
	}

	public void setUploaderId(Long uploaderId) {
		this.uploaderId = uploaderId;
	}

	public String getUploaderEmail() {
		return uploaderEmail;
	}

	public void setUploaderEmail(String uploaderEmail) {
		this.uploaderEmail = uploaderEmail;
	}

	public String getUploaderName() {
		return uploaderName;
	}

	public void setUploaderName(String uploaderName) {
		this.uploaderName = uploaderName;
	}

}
