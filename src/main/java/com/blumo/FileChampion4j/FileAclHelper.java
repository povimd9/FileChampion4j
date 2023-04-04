package com.blumo.FileChampion4j;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * This class is used to change the owner and permissions of a file.
 * Set Owner uses java.nio.file.Files.setOwner for cross-platform support.
 * Set Permissions attempts to identify the Operating System, using java.nio.file.attribute.AclFileAttributeView for Windows, 
 * and java.nio.file.attribute.PosixFileAttributeView for any other.
 */

public class FileAclHelper {
    private Path targetFilePath;
    private String newPermissions;
    private String newOwnerUsername;
    private String changeAclResult;
    private String errMsg;

    private static final Logger LOGGER = Logger.getLogger(FileAclHelper.class.getName());

    /**
     * changeFileAcl is the main method of this class. It attempts to change the owner and permissions of a file.
    * @param targetFilePath (Path) the path of the file to change
    * @param newOwnerUsername (String) the new owner of the file
    * @param newPermissions (String the new permissions of the file (e.g. "rwx")
    * @return (String) a String containing the result of the operation
    */
    public String changeFileAcl(Path targetFilePath, String newOwnerUsername, String newPermissions) {
        this.targetFilePath = targetFilePath;
        this.newOwnerUsername = newOwnerUsername;
        this.newPermissions = newPermissions;
        
        UserPrincipal newOwner = getUserPrinciple(targetFilePath, newOwnerUsername);
        if (newOwner == null) {
            errMsg = String.format("Error: Could not get user principal for %s", newOwnerUsername);
            return errMsg;
        }

        // Try to change the owner of the file
        String newOwnerChangeResults = setNewOwner(newOwner);
        if (newOwnerChangeResults.contains("Error")) {
            return newOwnerChangeResults;
        }

        // Try to change the permissions of the file
        String newPermissionsChangeResults = setNewPermissions(newOwner);
        if (newPermissionsChangeResults.contains("Error")) {
            return newPermissionsChangeResults;
        }
        
        // If we get here, both the owner and permissions were changed successfully
        String successMessage = String.format("Success: Changed owner and permissions of file %s", targetFilePath.toAbsolutePath());
        LOGGER.info(successMessage);
        return successMessage;
    }

    public String getChangeAclResult() {
        return changeAclResult;
    }

    // Get the user principal for the new owner
    private UserPrincipal getUserPrinciple (Path targetFilePath, String newOwnerUsername) {
        try {
            return targetFilePath.getFileSystem()
            .getUserPrincipalLookupService()
            .lookupPrincipalByName(newOwnerUsername);
        } catch (Exception e) {
            errMsg = String.format("Error: Exception getting user principal: %s", e.getMessage());
            LOGGER.severe(errMsg);
            return null;
        }
    }

    // Change the owner of the file
    private String setNewOwner(UserPrincipal newOwner) {
        try {
            // Change the owner of the file
            Files.setOwner(targetFilePath, newOwner);
            String logMessage = String.format("Success: Changed owner of file %s to %s", targetFilePath.toAbsolutePath(), newOwnerUsername);
            LOGGER.info(logMessage);
            return logMessage;
        } catch (AccessDeniedException e) {
            errMsg = String.format("Error: Access denied while changing file owner: %s", e.getMessage());
            LOGGER.severe(errMsg);
            return errMsg;
        } catch (FileSystemException e) {
            errMsg = String.format("Error: File system error while changing file owner: %s", e.getMessage());
            LOGGER.severe(errMsg);
            return errMsg;
        }
        catch (Exception e) {
            errMsg = String.format("Error: while changing file owner: %s", e.getMessage());
            LOGGER.severe(errMsg);
            return errMsg;
        }
    }

    // Check the OS and call the appropriate method to change the permissions
    private String setNewPermissions(UserPrincipal newOwner) {
        String os = System.getProperty("os.name");

        if (os.startsWith("Windows")) {
            return setNewPermissionsWindows(newOwner);
        } else {
            return setNewPermissionsUnix();
        }
    }

    // Change the permissions of the file using ACL on Windows OS
    private String setNewPermissionsWindows(UserPrincipal newOwner) {
        try {
            String statusMessage;
            // Change the permissions of the file using ACLs
            AclFileAttributeView aclView = Files.getFileAttributeView(targetFilePath, AclFileAttributeView.class);
            AclEntryPermission readPermission = AclEntryPermission.READ_DATA;
            AclEntryPermission writePermission = AclEntryPermission.WRITE_DATA;
            AclEntryPermission readAttributesPermission = AclEntryPermission.READ_ATTRIBUTES;
            AclEntryPermission writeAttributesPermission = AclEntryPermission.WRITE_ATTRIBUTES;
            AclEntryPermission executePermission = AclEntryPermission.EXECUTE;

            AclEntryType allow = AclEntryType.ALLOW;

            EnumSet<AclEntryPermission> actualPermissions = EnumSet.noneOf(AclEntryPermission.class);
            if (newPermissions.contains("r")) {
                actualPermissions.add(readPermission);
            }
            if (newPermissions.contains("w")) {
                actualPermissions.add(writePermission);
            }
            if (newPermissions.contains("a")) {
                actualPermissions.add(readAttributesPermission);
                actualPermissions.add(writeAttributesPermission);
            }
            if (newPermissions.contains("x")) {
                actualPermissions.add(executePermission);
            }

            statusMessage = String.format("Attempting to change permissions to %s", actualPermissions);
            LOGGER.info(statusMessage);

            // Create the new ACL entry
            AclEntry aclEntry = AclEntry.newBuilder()
                    .setType(allow)
                    .setPrincipal(newOwner)
                    .setPermissions(actualPermissions)
                    .build();

            // Get the existing ACL and add the new ACL entries
            List<AclEntry> acl = new ArrayList<>();
            acl.add(aclEntry);

            // Set the new ACL
            aclView.setAcl(acl);
            statusMessage = String.format("Success: Changed permissions of file %s to %s", targetFilePath.toAbsolutePath(), actualPermissions);
            LOGGER.info(statusMessage);
            return statusMessage;
        } catch (Exception e) {
            errMsg = String.format("Error: Exception setting permissions on file with ACL: %s. %s", targetFilePath.toAbsolutePath(), e.getMessage());
            LOGGER.severe(errMsg);
            return errMsg;
        }
    }

    // Change the permissions of the file using POSIX on Unix OS
    private String setNewPermissionsUnix() {
        String statusMessage;

        // Build the new permissions
        Set<PosixFilePermission> permissions = new HashSet<>();
        if (newPermissions.contains("r")) {
            permissions.add(PosixFilePermission.OWNER_READ);
        }
        if (newPermissions.contains("w")) {
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }
        if (newPermissions.contains("a")) {
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }
        if (newPermissions.contains("x")) {
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }

        // Change the permissions of the file using POSIX
        try {
            statusMessage = String.format("Attempting to change permissions to %s", permissions);
            LOGGER.info(statusMessage);
            Files.setPosixFilePermissions(targetFilePath.toAbsolutePath(), permissions);
            statusMessage = String.format("Success: Changed permissions of file %s to %s", targetFilePath.toAbsolutePath(), permissions);
            LOGGER.info(statusMessage);
            return statusMessage;
        } catch (Exception e) {
            errMsg = String.format("Error: Exception setting permissions on file with POSIX: %s. %s", targetFilePath.toAbsolutePath(), e.getMessage());
            LOGGER.severe(errMsg);
            return errMsg;
        }
    }
}
