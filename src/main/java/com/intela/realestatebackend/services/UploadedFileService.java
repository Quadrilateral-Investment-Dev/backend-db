package com.intela.realestatebackend.services;

import com.intela.realestatebackend.models.UploadedFile;
import com.intela.realestatebackend.models.archetypes.FileType;
import com.intela.realestatebackend.util.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Paths;

@Service
public class UploadedFileService {

    @Value("${application.custom.file-storage.image-directory}")
    private String imageStorageDirectory; // Define your storage directory

    public void removeFile(String filePath) throws IOException {
        File fileToDelete = new File(filePath);

        // Check if the file exists before attempting to delete it
        if (!fileToDelete.exists()) {
            throw new FileNotFoundException("File not found at path: " + filePath);
        }

        // Attempt to delete the file
        if (fileToDelete.delete()) {
            System.out.println("File deleted successfully: " + filePath);
        } else {
            throw new IOException("Failed to delete file at path: " + filePath);
        }
    }

    public void removeFile(UploadedFile image, Integer userId, String additionalPath, FileType fileType) throws IOException {
        String outputPath = Paths.get(imageStorageDirectory, String.valueOf(userId), fileType.toString(), additionalPath, image.getName()).toString();
        File fileToDelete = new File(outputPath);

        // Check if the file exists before attempting to delete it
        if (!fileToDelete.exists()) {
            throw new FileNotFoundException("File not found at path: " + outputPath);
        }

        // Attempt to delete the file
        if (fileToDelete.delete()) {
            System.out.println("File deleted successfully: " + outputPath);
        } else {
            throw new IOException("Failed to delete file at path: " + outputPath);
        }
    }


    public void storeFile(UploadedFile image, Integer userId, String additionalPath, FileType fileType) throws IOException {
        System.out.println("Current working directory: " + new File(".").getAbsolutePath());
        String outputPath = imageStorageDirectory;
        byte[] imageBytes = image.getImage();
        FileOutputStream fileOutputStream = null;
        try {
            outputPath = Paths.get(outputPath, String.valueOf(userId), fileType.toString(), additionalPath, image.getName()).toString();
            if (Util.doesFileExist(outputPath))
                throw new FileAlreadyExistsException("File already exists at path: " + outputPath);
            // Create a new File object for the output path
            File outputFile = new File(outputPath);

            // Create parent directories if they don't exist
            File parentDirectory = outputFile.getParentFile();
            if (parentDirectory != null && !parentDirectory.exists()) {
                if (parentDirectory.mkdirs()) {
                    System.out.println("Created directories for the output path: " + parentDirectory.getAbsolutePath());
                } else {
                    throw new IOException("Failed to create directories for the output path: " + parentDirectory.getAbsolutePath());
                }
            }

            // Create a new file output stream to write the bytes to the specified file path
            fileOutputStream = new FileOutputStream(outputFile);

            // Write the byte array to the file
            fileOutputStream.write(imageBytes);
            fileOutputStream.flush();

            System.out.println("Image saved successfully to: " + outputPath);
            image.setPath(outputPath);
        } finally {
            // Ensure the stream is closed after use
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }
}
