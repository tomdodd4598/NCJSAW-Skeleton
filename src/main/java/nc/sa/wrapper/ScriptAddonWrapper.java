package nc.sa.wrapper;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mod(modid = ScriptAddonWrapper.MOD_ID, name = ScriptAddonWrapper.MOD_NAME, version = ScriptAddonWrapper.VERSION, dependencies = "required-after:forge@[14.23.5.2847,);required-before:nuclearcraft")
public final class ScriptAddonWrapper {
    
    public static final String MOD_ID = "ncjsaw_skeleton";
    public static final String MOD_NAME = "NCJSAW Skeleton";
    public static final String VERSION = "1.0";
    
    private static final String ADDON_DIRECTORY_NAME = "NCJSAW-Skeleton-1.0";
    
    @Mod.EventHandler
    public void onConstruction(FMLConstructionEvent constructionEvent) throws IOException {
        File addonDirectory = new File("resources/nuclearcraft/addons", ADDON_DIRECTORY_NAME);
        
        CodeSource source = getClass().getProtectionDomain().getCodeSource();
        if (source == null) {
            throw new IOException("Could not determine own jar file - missing code source");
        }
        
        URL location = source.getLocation();
        if (location == null) {
            throw new IOException("Could not determine own jar file - missing code source location");
        }
        
        File ownJar;
        try {
            URI uri = location.toURI();
            ownJar = new File(uri).getCanonicalFile();
        }
        catch (Exception e) {
            throw new IOException("Could not determine own jar file from location " + location, e);
        }
        
        if (!ownJar.isFile()) {
            throw new IOException("Expected own location to be a jar file, got " + ownJar.getAbsolutePath());
        }
        
        if (addonDirectory.exists()) {
            delete(addonDirectory);
        }
        if (!addonDirectory.mkdirs() && !addonDirectory.isDirectory()) {
            throw new IOException("Failed to create addon directory " + addonDirectory.getAbsolutePath());
        }
        
        File canonicalAddonDirectory = addonDirectory.getCanonicalFile();
        boolean foundAddonContent = false;
        
        String contentPrefix = "content/";
        
        try (ZipFile zipFile = new ZipFile(ownJar)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName().replace('\\', '/');
                
                if (!entryName.startsWith(contentPrefix)) {
                    continue;
                }
                
                String relativeName = entryName.substring(contentPrefix.length());
                if (relativeName.isEmpty()) {
                    continue;
                }
                
                foundAddonContent = true;
                
                File outputFile = new File(addonDirectory, relativeName).getCanonicalFile();
                String addonDirectoryPath = canonicalAddonDirectory.getPath();
                String outputFilePath = outputFile.getPath();
                
                if (!outputFilePath.equals(addonDirectoryPath) && !outputFilePath.startsWith(addonDirectoryPath + File.separator)) {
                    throw new IOException("Attempted to extract addon entry outside target directory " + entryName);
                }
                
                if (entry.isDirectory()) {
                    if (!outputFile.mkdirs() && !outputFile.isDirectory()) {
                        throw new IOException("Failed to create addon subdirectory " + outputFile.getAbsolutePath());
                    }
                    continue;
                }
                
                File parent = outputFile.getParentFile();
                if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Failed to create parent directory " + parent.getAbsolutePath());
                }
                
                try (InputStream inputStream = zipFile.getInputStream(entry); FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    
                    while ((read = inputStream.read(buffer)) >= 0) {
                        outputStream.write(buffer, 0, read);
                    }
                }
            }
        }
        
        if (!foundAddonContent) {
            throw new IOException("No script addon content found in jar folder " + contentPrefix);
        }
    }
    
    private static void delete(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        
        File canonical = file.getCanonicalFile();
        
        if (canonical.isDirectory()) {
            File[] children = canonical.listFiles();
            if (children == null) {
                throw new IOException("Failed to list directory while deleting " + canonical.getAbsolutePath());
            }
            
            for (File child : children) {
                delete(child);
            }
        }
        
        if (!canonical.delete() && canonical.exists()) {
            throw new IOException("Failed to delete " + canonical.getAbsolutePath());
        }
    }
}
