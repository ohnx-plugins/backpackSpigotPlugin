package ca.masonx.backpack;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class InventoryIO {
	public static void write(String file, String b64) throws Exception{
		ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
		out.writeObject(b64);
		out.close();
	}

	public static String read(String location) throws Exception{
		String e = null;
        ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(location)));
        e = (String) in.readObject();
        in.close();
        return e;
    }
	
	public static List<String> list(String parent) {
		List<String> results = new ArrayList<String>();
		File[] files = new File(parent).listFiles();

		for (File file : files)
		    if (file.isFile())
		        results.add(file.getName());
		return results;
	}
	
	public static void delete(String path) {
		File delFile = new File(path);
		delFile.delete();
	}
	
	public static boolean nouveau(String path) throws Exception {
		File f = new File(path);
		if(f.exists()){
			return false;
		} else {
			f.createNewFile();
			return true;
		}
	}
}
