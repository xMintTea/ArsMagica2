package am2.spell;

import am2.AMCore;
import am2.LogHelper;
import am2.texture.ResourceManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Items;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SideOnly(Side.CLIENT)
public class SpellTextureHelper {
    public static final SpellTextureHelper instance = new SpellTextureHelper();

    private IIcon[] icons;

    private static final String ICON_ASSETS_PATH   = "assets/arsmagica2/textures/items/spells/icons";
    private static final String ICON_ASSETS_PREFIX = ICON_ASSETS_PATH + "/";
    private static final String ICON_REGISTER_PREFIX = "spells/icons/";

    private SpellTextureHelper() {}

    public void loadAllIcons(IIconRegister register) {
        List<String> resources;
        try {
            resources = getResourceListing();
            if (resources.isEmpty()) {
                LogHelper.error("No spell icons found! Put PNGs into assets/arsmagica2/textures/items/spells/icons/ (mod jar or active resource pack).");
            } else {
                LogHelper.info("Located %d spell icons", resources.size());
            }
            icons = new IIcon[resources.size()];
            int count = 0;
            for (String s : resources) {
                icons[count++] = ResourceManager.RegisterTexture(s, register);
            }
        } catch (Throwable e) {
            if (icons == null) icons = new IIcon[0];
            e.printStackTrace();
        }
    }

    public static List<String> getResourceListing() throws IOException, URISyntaxException {
        List<String> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Enumeration<URL> urls = SpellTextureHelper.class.getClassLoader().getResources(ICON_ASSETS_PATH);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            LogHelper.debug("SpellTextureHelper: scanning classpath entry %s", url);
            try {
                String proto = url.getProtocol();
                if ("file".equals(proto)) {
                    File dir = new File(url.toURI());
                    if (dir.isDirectory()) {
                        File[] pngs = dir.listFiles();
                        if (pngs != null) for (File png : pngs) {
                            if (png.isFile() && png.getName().endsWith(".png")) {
                                addIcon(found, seen, stripPng(png.getName()));
                            }
                        }
                    }
                } else if ("jar".equals(proto)) {
                    JarURLConnection conn = (JarURLConnection) url.openConnection();
                    scanJar(conn.getJarFile(), found, seen);
                }
            } catch (Throwable t) {
                LogHelper.error("SpellTextureHelper: failed scanning %s", url);
                t.printStackTrace();
            }
        }


        if (found.isEmpty()) {
            URL loc = AMCore.class.getProtectionDomain().getCodeSource().getLocation();
            LogHelper.debug("SpellTextureHelper: fallback CodeSource = %s", loc);
            if (loc != null) {
                try {
                    if ("file".equals(loc.getProtocol())) {
                        File f = new File(loc.toURI());
                        if (f.isFile()) {
                            JarFile jf = new JarFile(f);
                            try { scanJar(jf, found, seen); } finally { jf.close(); }
                        } else if (f.isDirectory()) {
                            for (File d = f; d != null; d = d.getParentFile()) {
                                File iconsDir = new File(d, ICON_ASSETS_PATH);
                                if (iconsDir.isDirectory()) {
                                    File[] pngs = iconsDir.listFiles();
                                    if (pngs != null) for (File png : pngs) {
                                        if (png.isFile() && png.getName().endsWith(".png")) {
                                            addIcon(found, seen, stripPng(png.getName()));
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } else if ("jar".equals(loc.getProtocol())) {
                        scanJar(((JarURLConnection) loc.openConnection()).getJarFile(), found, seen);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        scanResourcePacks(found, seen);

        Collections.sort(found, String.CASE_INSENSITIVE_ORDER);
        return found;
    }

    private static String stripPng(String n) {
        return n.substring(0, n.length() - 4);
    }

    private static void addIcon(List<String> out, Set<String> seen, String rawName) {
        if (rawName == null || rawName.isEmpty()) return;
        if (rawName.indexOf('/') >= 0 || rawName.indexOf('\\') >= 0) return;
        String entry = ICON_REGISTER_PREFIX + rawName;
        if (seen.add(entry.toLowerCase(Locale.ROOT))) out.add(entry);
    }

    private static void scanJar(JarFile jarFile, List<String> out, Set<String> seen) {
        Enumeration<JarEntry> en = jarFile.entries();
        while (en.hasMoreElements()) {
            String n = en.nextElement().getName();
            if (n.startsWith(ICON_ASSETS_PREFIX) && n.endsWith(".png")) {
                addIcon(out, seen, n.substring(ICON_ASSETS_PREFIX.length(), n.length() - 4));
            }
        }
    }

    private static void scanResourcePacks(List<String> out, Set<String> seen) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.mcDataDir == null || mc.getResourceManager() == null) return;
            File[] files = new File(mc.mcDataDir, "resourcepacks").listFiles();
            if (files == null) return;
            for (File f : files) {
                if (f.getName().endsWith(".zip")) {
                    ZipFile zf = null;
                    try {
                        zf = new ZipFile(f);
                        Enumeration<? extends ZipEntry> en = zf.entries();
                        while (en.hasMoreElements()) {
                            String n = en.nextElement().getName();
                            if (n.startsWith(ICON_ASSETS_PREFIX) && n.endsWith(".png")) {
                                String name = n.substring(ICON_ASSETS_PREFIX.length(), n.length() - 4);
                                if (isResourceActive(mc, name)) addIcon(out, seen, name);
                            }
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        if (zf != null) try { zf.close(); } catch (IOException ignored) {}
                    }
                } else if (f.isDirectory()) {
                    File[] pngs = new File(f, ICON_ASSETS_PATH).listFiles();
                    if (pngs != null) for (File png : pngs) {
                        if (png.isFile() && png.getName().endsWith(".png")) {
                            String name = stripPng(png.getName());
                            if (isResourceActive(mc, name)) addIcon(out, seen, name);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static boolean isResourceActive(Minecraft mc, String iconName) {
        try {
            ResourceLocation rl = new ResourceLocation("arsmagica2", "textures/items/spells/icons/" + iconName + ".png");
            return mc.getResourceManager().getResource(rl) != null;
        } catch (Throwable e) {
            return false;
        }
    }

    public IIcon getIcon(int index) {
        if (icons == null || icons.length == 0) return Items.diamond_sword.getIconFromDamage(0);
        if (index < 0 || index >= icons.length) index = 0;
        return icons[index];
    }

    public IIcon[] getAllIcons() {
        return icons == null ? new IIcon[0] : icons;
    }
}
