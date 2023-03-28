package com.blumo.FileSentry4J;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This class is used to change the owner and permissions of a file using ACLs.
 * ACLs are used instead of the standard permissions to support cross-platform compatibility.
 * If new group is not defined, owner name will be used for group.
 * - targetFilePath: the path of the file to change
 * - newPermissions: the new permissions to set on the file
 * - newOwnerUsername: the new owner of the file
 * - newOwnerGroup: the new group of the file
 */

public class FileAclHelper {
    private static final Logger LOGGER = Logger.getLogger(FileAclHelper.class.getName());
    public static void ChangeFileACL(Path targetFilePath, String newPermissions, String newOwnerUsername) throws Exception {
        // Check the parameters
        if (targetFilePath == null || targetFilePath.getNameCount() == 0) {
            LOGGER.severe("Missing required parameter: targetFilePath");
            throw new IllegalArgumentException("Missing required parameter: targetFilePath");
        }
        if (newPermissions == null || newPermissions.isEmpty()) {
            LOGGER.severe("Missing required parameter: newPermissions");
            throw new IllegalArgumentException("Missing required parameter: newPermissions");
        }


        // Change the owner of the file
        UserPrincipal newOwner = targetFilePath.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(newOwnerUsername);
        Files.setOwner(targetFilePath, newOwner);
        LOGGER.info("Changed owner of file " + targetFilePath.toAbsolutePath() + " to " + newOwnerUsername);

        // Change the permissions of the file using ACLs
        AclFileAttributeView aclView = Files.getFileAttributeView(targetFilePath, AclFileAttributeView.class);
        AclEntryPermission readPermission = AclEntryPermission.READ_DATA;
        AclEntryPermission writePermission = AclEntryPermission.WRITE_DATA;
        AclEntryPermission readAttributesPermission = AclEntryPermission.READ_ATTRIBUTES;
        AclEntryPermission writeAttributesPermission = AclEntryPermission.WRITE_ATTRIBUTES;
        AclEntryPermission executePermission = AclEntryPermission.EXECUTE;

        AclEntryType allow = AclEntryType.ALLOW;

        LOGGER.info("Prepping new ACL entries for file " + targetFilePath.toAbsolutePath() + " with permissions " + newPermissions);
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

        LOGGER.info("AclEntryBuilder: " + newOwner + " " + actualPermissions);
        AclEntry aclEntry = AclEntry.newBuilder()
                .setType(allow)
                .setPrincipal(newOwner)
                .setPermissions(actualPermissions)
                .build();

        LOGGER.info("add new acl to existing acl");
        // Get the existing ACL and add the new ACL entries
        List<AclEntry> acl = new ArrayList<>();
        acl.add(aclEntry);

        // Set the new ACL
        try {
            LOGGER.info("Setting new ACL entries on file " + targetFilePath.toAbsolutePath());
            aclView.setAcl(acl);
            LOGGER.info("Added new ACL entries to file " + targetFilePath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.severe("IOException setting ACL on file: " + targetFilePath.toAbsolutePath() + ". " + e.getMessage());
            throw new Exception("IOException setting ACL on file: " + targetFilePath.toAbsolutePath() + ". " + e.getMessage());
        } catch (NullPointerException e) {
            try {
                // Change the file permissions of the target file
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
                Files.setPosixFilePermissions(targetFilePath.toAbsolutePath(), permissions);
            } catch (Exception e2) {
                LOGGER.severe("Exception setting permissions on file with ACL & POSIX: " + targetFilePath.toAbsolutePath() + ". " + e2.getMessage());
                throw new Exception("Exception setting permissions on file with ACL & POSIX: " + targetFilePath.toAbsolutePath() + ". " + e2.getMessage());
            }
        } catch (Exception e) {
            LOGGER.severe("Exception setting ACL on file: " + targetFilePath.toAbsolutePath() + ". " + e.getMessage());
            throw new Exception("Exception setting ACL on file: " + targetFilePath.toAbsolutePath() + ". " + e.getMessage());
        }
    }
}
