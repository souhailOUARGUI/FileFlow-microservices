package org.example.folderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FolderNotFoundException extends RuntimeException {
    
    public FolderNotFoundException(String message) {
        super(message);
    }
    
    public FolderNotFoundException(Long folderId) {
        super("Folder not found with id: " + folderId);
    }
}
