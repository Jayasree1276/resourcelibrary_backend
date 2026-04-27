package com.example.library.controller;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.example.library.model.Resource;
import com.example.library.service.ResourceService;
@RestController
@RequestMapping("/api/resources")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ResourceController {
	 @Autowired
	    private ResourceService service;

	    // GET all resources
	    @GetMapping
	    public List<Resource> getAllResources() {
	        return service.getAllResources();
	    }
	    @GetMapping("/{id}")
	    public Resource getById(@PathVariable Long id) {
	        return service.getResourceById(id);
	    }
	    // POST add resource
	    @PostMapping
	    public Resource addResource(@RequestBody Resource resource) {
	        return service.saveResource(resource);
	    }

	    // DELETE resource
	    @DeleteMapping("/{id}")
	    public void deleteResource(@PathVariable Long id) {
	        service.deleteResource(id);
	    }
	    @PostMapping("/upload")
	    public Resource uploadResource(
	            @RequestParam("title") String title,
	            @RequestParam("subject") String subject,
	            @RequestParam("type") String type,
	            @RequestParam("file") MultipartFile file,
	            @RequestParam(value = "uploaderId", required = false) Long uploaderId,
	            @RequestParam(value = "uploaderEmail", required = false) String uploaderEmail,
	            @RequestParam(value = "uploaderName", required = false) String uploaderName
	    ) {
	        try {
	            Resource resource = new Resource();
	            resource.setTitle(title);
	            resource.setSubject(subject);
	            resource.setType(type);
	            resource.setFileUrl(storeFile(file));
	            resource.setUploaderId(uploaderId);
	            resource.setUploaderEmail(uploaderEmail);
	            resource.setUploaderName(uploaderName);

	            return service.saveResource(resource);

	        } catch (Exception e) {
	            e.printStackTrace();
	            return null;
	        }
	    }

	    @PutMapping("/{id}")
	    public Resource updateResource(
	            @PathVariable Long id,
	            @RequestParam("title") String title,
	            @RequestParam("subject") String subject,
	            @RequestParam("type") String type,
	            @RequestParam(value = "file", required = false) MultipartFile file,
	            @RequestParam(value = "uploaderId", required = false) Long uploaderId,
	            @RequestParam(value = "uploaderEmail", required = false) String uploaderEmail,
	            @RequestParam(value = "uploaderName", required = false) String uploaderName
	    ) {
	        Resource existing = service.findById(id)
	                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

	        existing.setTitle(title);
	        existing.setSubject(subject);
	        existing.setType(type);

	        if (uploaderId != null) {
	            existing.setUploaderId(uploaderId);
	        }
	        if (uploaderEmail != null && !uploaderEmail.isBlank()) {
	            existing.setUploaderEmail(uploaderEmail);
	        }
	        if (uploaderName != null && !uploaderName.isBlank()) {
	            existing.setUploaderName(uploaderName);
	        }

	        try {
	            if (file != null && !file.isEmpty()) {
	                existing.setFileUrl(storeFile(file));
	            }
	        } catch (Exception e) {
	            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
	        }

	        return service.saveResource(existing);
	    }

	    @GetMapping("/download/{id}")
	    public ResponseEntity<org.springframework.core.io.Resource> downloadResource(@PathVariable Long id) {
	        Resource resource = service.getResourceById(id);
	        if (resource == null) {
	            return ResponseEntity.notFound().build();
	        }
	        try {
	            String fileUrl = resource.getFileUrl();
	            if (fileUrl == null || fileUrl.isBlank()) {
	                return ResponseEntity.notFound().build();
	            }

	            String fileName;
	            if (fileUrl.contains("/uploads/")) {
	                fileName = fileUrl.substring(fileUrl.lastIndexOf("/uploads/") + "/uploads/".length());
	            } else {
	                fileName = Paths.get(fileUrl).getFileName().toString();
	            }

	            Path filePath = Paths.get("uploads", fileName);
	            org.springframework.core.io.Resource fileResource = new UrlResource(filePath.toUri());

	            if (fileResource.exists() && fileResource.isReadable()) {
	                String contentType = Files.probeContentType(filePath);
	                return ResponseEntity.ok()
	                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
	                        .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
	                        .contentLength(Files.size(filePath))
	                        .body(fileResource);
	            } else {
	                return ResponseEntity.notFound().build();
	            }
	        } catch (Exception e) {
	            return ResponseEntity.internalServerError().build();
	        }
	    }

	    private String storeFile(MultipartFile file) throws Exception {
	        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
	        String uploadDir = "uploads/";

	        File uploadPath = new File(uploadDir);
	        if (!uploadPath.exists()) {
	            uploadPath.mkdir();
	        }

	        Path filePath = Paths.get(uploadDir + fileName);
	        Files.write(filePath, file.getBytes());
	        return "http://localhost:8080/uploads/" + fileName;
	    }
}
