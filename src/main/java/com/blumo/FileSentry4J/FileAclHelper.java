package com.blumo.FileSentry4J;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.List;
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
    public static void ChangeFileACL(Path targetFilePath, String newPermissions, String... newOwnerUsername) throws Exception {
        // Check the parameters
        if (targetFilePath == null || targetFilePath.getNameCount() == 0) {
            LOGGER.severe("Missing required parameter: targetFilePath");
            throw new IllegalArgumentException("Missing required parameter: targetFilePath");
        }
        if (newPermissions == null || newPermissions.isEmpty()) {
            LOGGER.severe("Missing required parameter: newPermissions");
            throw new IllegalArgumentException("Missing required parameter: newPermissions");
        }

        // Get the new owner and group
        String username = null;
        String group = null;
        if (newOwnerUsername.length > 0) {
            username = (newOwnerUsername[0] != null && !newOwnerUsername[0].isEmpty()) ? newOwnerUsername[0] : null;
        }
        if (newOwnerUsername.length > 1) {
            group = (newOwnerUsername[1] != null && !newOwnerUsername[1].isEmpty()) ? newOwnerUsername[1] : username;
        }

        // Change the owner of the file
        UserPrincipal newOwner = targetFilePath.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(username);
        Files.setOwner(targetFilePath, newOwner);
        LOGGER.info("Changed owner of file " + targetFilePath.toAbsolutePath() + " to " + username);

        // Change the permissions of the file using ACLs
        AclFileAttributeView aclView = Files.getFileAttributeView(targetFilePath, AclFileAttributeView.class);
        AclEntryPermission readPermission = AclEntryPermission.READ_DATA;
        AclEntryPermission writePermission = AclEntryPermission.WRITE_DATA;
        AclEntryPermission readAttributesPermission = AclEntryPermission.READ_ATTRIBUTES;
        AclEntryPermission writeAttributesPermission = AclEntryPermission.WRITE_ATTRIBUTES;
        AclEntryType allow = AclEntryType.ALLOW;
        UserPrincipal owner = aclView.getOwner();

        AclEntry aclEntry = AclEntry.newBuilder()
                .setType(allow)
                .setPrincipal(newOwner)
                .setPermissions(EnumSet.of(readPermission, readAttributesPermission, writeAttributesPermission))
                .build();

        // Add the owner group to the ACL
        GroupPrincipal ownerGroup = targetFilePath.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByGroupName(group);
        AclEntry groupAclEntry = AclEntry.newBuilder()
                .setType(allow)
                .setPrincipal(ownerGroup)
                .setPermissions(EnumSet.of(readPermission, readAttributesPermission, writeAttributesPermission))
                .build();

        // Get the existing ACL and add the new ACL entries
        List<AclEntry> acl = aclView.getAcl();
        acl.add(aclEntry);
        acl.add(groupAclEntry);

        // Set the new ACL
        try {
            aclView.setAcl(acl);
            LOGGER.info("Added new ACL entries to file " + targetFilePath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.severe("Error setting ACL on file: " + targetFilePath.toAbsolutePath() + ". " + e.getMessage());
            throw new Exception("Error setting ACL on file: " + targetFilePath.toAbsolutePath() + ". " + e.getMessage());
        }
    }
}
