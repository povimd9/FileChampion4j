package com.blumo.FileChampion4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.EnumSet;
import java.util.Set;


public class FileAclHelperTest {

    private Path tempFilePath;
    private String newPermissions;
    private String newOwnerUsername;

    @BeforeEach
    void setUp() throws Exception {
        // Create a temporary file
        tempFilePath = Files.createTempFile("test-file", ".txt");

        // Set up new owner and permissions
        newPermissions = "rwx";
        newOwnerUsername = System.getProperty("user.name");
    }

    @Test
    void testChangeFileAcl() throws Exception {
        FileAclHelper aclHelper = new FileAclHelper();

        // Change the ACL of the temporary file
        String result = aclHelper.changeFileAcl(tempFilePath, newOwnerUsername, newPermissions);

        // Check that the result is success
        Assertions.assertTrue(result.startsWith("Success"));

        // Check that the new owner is correct
        UserPrincipalLookupService lookupService = tempFilePath.getFileSystem().getUserPrincipalLookupService();
        UserPrincipal owner = lookupService.lookupPrincipalByName(newOwnerUsername);
        UserPrincipal actualOwner = Files.getOwner(tempFilePath);
        Assertions.assertEquals(owner, actualOwner);

        // Check that the new permissions are correct
        String actualPermissions = "";
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            // Get the ACL entries for the tmp test file
            AclFileAttributeView view = Files.getFileAttributeView(
                Paths.get(tempFilePath.toUri()), AclFileAttributeView.class);
            
            // Get the ACL entries for the file
            String permissions = "";
            for (AclEntry entry : view.getAcl()) {
                // Check each permission and add the corresponding symbol to the output string
                if (entry.permissions().contains(AclEntryPermission.READ_DATA)) {
                permissions += "r";
                } else {
                permissions += "-";
                }
                if (entry.permissions().contains(AclEntryPermission.WRITE_DATA)) {
                permissions += "w";
                } else {
                permissions += "-";
                }
                if (entry.permissions().contains(AclEntryPermission.APPEND_DATA)) {
                permissions += "a";
                } else {
                permissions += "-";
                }
                if (entry.permissions().contains(AclEntryPermission.EXECUTE)) {
                permissions += "x";
                } else {
                permissions += "-";
                }
                if (entry.permissions().contains(AclEntryPermission.DELETE)) {
                permissions += "d";
                } else {
                permissions += "-";
                }
                if (entry.permissions().contains(AclEntryPermission.READ_ATTRIBUTES)) {
                permissions += "r";
                } else {
                permissions += "-";
                }
                if (entry.permissions().contains(AclEntryPermission.WRITE_ATTRIBUTES)) {
                permissions += "w";
                } else {
                permissions += "-";
                }
            }
            actualPermissions = permissions.replace(String.valueOf("-"), "" );
        } else {
            // Get the Posix file permissions for the file
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(tempFilePath);

            // Convert the user permissions to rwx format
            StringBuilder rwxPermissions = new StringBuilder();
            for (PosixFilePermission permission : EnumSet.copyOf(permissions)) {
                switch (permission) {
                    case OWNER_READ:
                        rwxPermissions.append("r");
                        break;
                    case OWNER_WRITE:
                        rwxPermissions.append("w");
                        break;
                    case OWNER_EXECUTE:
                        rwxPermissions.append("x");
                        break;
                    default:
                        rwxPermissions.append("-");
                        break;
                }
            }
            actualPermissions = rwxPermissions.toString();
        }
        Assertions.assertEquals(newPermissions, actualPermissions);
    }
}