package dev.filechampion.filechampion4j;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

/**
 * Unit tests for FileAclHelper
 */
public class FileAclHelperTest {
    private Path tempFilePath;
    private String sharedMessages = "Expected error message to start with '";

    // Create a temporary file
    @BeforeEach
    void setUp() throws Exception {
        tempFilePath = Files.createTempFile("test-file", ".txt");

    }

    // Test change permissions
    @ParameterizedTest
    @ValueSource(strings = {"r", "w", "x", "rw", "rwx"})
    void testChangeFileAclWrite(String newPermissions) throws Exception {
        String newOwnerUsername = System.getProperty("user.name");
        FileAclHelper aclHelper = new FileAclHelper(tempFilePath, newOwnerUsername, newPermissions);

        // Change the ACL of the temporary file
        String result = aclHelper.changeFileAcl();

        // Check that the result is success
        assertTrue(result.startsWith("Success"), "Expected result to start with 'Success' but got: " + result);

        // Check that the new owner is correct
        assertTrue(checkOwner(tempFilePath, newOwnerUsername), "Expected owner to be " + newOwnerUsername + " but was " + Files.getOwner(tempFilePath));

        // Check that the new permissions are correct
        String actualPermissions = "";
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            actualPermissions = getAclPermissions(tempFilePath);
        } else {
            actualPermissions = getPosixFilePermissions(tempFilePath);
        }
        assertEquals(newPermissions, actualPermissions, "Expected permissions to be " + newPermissions + " but was " + actualPermissions);
    }

    // Test non existing permissions
    @Test
    void testChangeFileAclNonExistingPermissions() throws Exception {
        String newPermissions = "invalid-permissions";
        String newOwnerUsername = System.getProperty("user.name");
        assertThrows(IllegalArgumentException.class, () -> {
            new FileAclHelper(tempFilePath, newOwnerUsername, newPermissions);
        }, "Expected IllegalArgumentException to be thrown");
    }

    // Check getUserPrinciple failure
    @Test
    void testGetUserPrincipleFailure() throws Exception {
        String newPermissions = "rwx";
        FileAclHelper aclHelper = new FileAclHelper(tempFilePath, "invalid-user-394f84398fn3948dfn239048d023d", newPermissions);

        // Change the ACL of the temporary file
        String result = aclHelper.changeFileAcl();

        // Check that the result is success
        String expectedErrMsg = "Error: Could not get user principal";
        assertTrue(result.startsWith(expectedErrMsg), String.format("%s %s' but got: %s", sharedMessages, expectedErrMsg, result));
    }

    // Check access denied scenario in windows
    @Test
    void testAccessDeniedFailure() throws Exception {
        if (System.getProperty("os.name").startsWith("Windows")) {
            String newPermissions = "r";
            FileAclHelper aclHelper = new FileAclHelper(tempFilePath, System.getProperty("user.name"), newPermissions);
            aclHelper.changeFileAcl();
            FileAclHelper newAclHelper = new FileAclHelper(tempFilePath, System.getProperty("user.name"), "rwx");
            String result = newAclHelper.changeFileAcl();
            String expectedErrMsg = "Error: Access denied";
            assertTrue(result.startsWith(expectedErrMsg), String.format("%s %s' but got: %s", sharedMessages, expectedErrMsg, result));
        } else { assertTrue(true, "not reproducable on non-windows");}
    }

    // Check file system failure
    @Test
    void testFileSystemFailure() throws Exception {
        String newPermissions = "r";
        String newOwnerUsername = System.getProperty("user.name");
        FileAclHelper aclHelper = new FileAclHelper(tempFilePath, newOwnerUsername, newPermissions);
        // Change the ACL of the temporary file
        aclHelper.changeFileAcl();

        FileAclHelper newAclHelper = new FileAclHelper(Paths.get("C:\\invalid\\path\\to\\file.txt"), newOwnerUsername, "rwx");
        String result = newAclHelper.changeFileAcl();

        // Check that the result is success
        String expectedErrMsg = "Error: File system error";
        assertTrue(result.startsWith(expectedErrMsg), String.format("%s %s' but got: %s", sharedMessages, expectedErrMsg, result));
    }


    /////////////////////////
    // Helper methods

    private String getPosixFilePermissions (Path path) throws Exception {
        // Get the Posix file permissions for the file
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);

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
                    break;
            }
        }

        return rwxPermissions.toString();
    }

    private String getAclPermissions (Path path) throws Exception {
        // Get the ACL entries for the tmp test file
        AclFileAttributeView view = Files.getFileAttributeView(
            Paths.get(path.toUri()), AclFileAttributeView.class);
        
        // Get the ACL entries for the file
        String permissions = "";
        for (AclEntry entry : view.getAcl()) {
            // Check each permission and add the corresponding symbol to the output string
            if (entry.permissions().contains(AclEntryPermission.READ_DATA)) {
                permissions += "r";
            }
            if (entry.permissions().contains(AclEntryPermission.WRITE_DATA)) {
                permissions += "w";
            }
            if (entry.permissions().contains(AclEntryPermission.EXECUTE)) {
                permissions += "x";
            }
        }
        return permissions.replace(String.valueOf("-"), "" );
    }

    private boolean checkOwner (Path path, String owner) throws Exception {
        UserPrincipalLookupService lookupService = path.getFileSystem().getUserPrincipalLookupService();
        UserPrincipal expectedOwner = lookupService.lookupPrincipalByName(owner);
        UserPrincipal actualOwner = Files.getOwner(path);
        return expectedOwner.equals(actualOwner);
    }
}