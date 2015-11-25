/* SldShw's FileSystem
 * Handles culling of files of certian types from directories for ad-hoc execution
 */
import java.io.File;
import java.io.FileFilter;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;
import java.util.List;


class SSFileSystem{

	public static final ExtensionFilter imageFilter;
	public static final ExtensionFilter musicFilter;
	public static final ExtensionFilter sldshwFilter;
	public static final ExtensionFilter omniFilter;

	static{
		//define extensions
		String[] image_exts = ImageIO.getReaderFileSuffixes();
		for(int i=image_exts.length; i--!=0;)	//ensure everything is lower case
			image_exts[i] = image_exts[i].toLowerCase();
		String[] music_exts = {"mp3"};//TODO figure out how to define this with the API
		String[] sldshw_exts = {"txt"};
		//build filters
		imageFilter = new ExtensionFilter(image_exts);
		musicFilter = new ExtensionFilter(music_exts);
		sldshwFilter = new ExtensionFilter(sldshw_exts);

		//build omni filter
		String[] omni_exts = new String[image_exts.length+music_exts.length+sldshw_exts.length];
		int i=0;
		for(int j=image_exts.length; j--!=0;)
			omni_exts[i++]=image_exts[j];
		for(int j=music_exts.length; j--!=0;)
			omni_exts[i++]=music_exts[j];
		for(int j=sldshw_exts.length; j--!=0;)
			omni_exts[i++]=sldshw_exts[j];
		//should accept directories also for recursive stuff
		omniFilter = new ExtensionFilter(omni_exts){
			@Override
			public boolean accept(File f){
				if(f.isDirectory()) return true;
				return super.accept(f);
			}
		};
	}

	File dir; //local root dir

	public SSFileSystem(){
		dir = new File(".");
	}
	public SSFileSystem(File dir){
		this.dir = dir;
	}

	public void listFiles(List<File> images, List<File> songs, boolean recursive){
		File[] files = dir.listFiles(omniFilter);
		for(int i=0; i<files.length; i++){
			if(imageFilter.accept(files[i])) images.add(files[i]);
			else if(musicFilter.accept(files[i])) songs.add(files[i]);
			else if(recursive && files[i].isDirectory()){
				new SSFileSystem(files[i]).listFiles(images,songs,recursive);
			}
		}
	}


}

//check to see if a file matchs a set of extensions
class ExtensionFilter implements FileFilter {

	String[] exts;
	ExtensionFilter(String[] exts){
		this.exts=exts;
	}

	public boolean accept(File f){
		//does not accept directories or nonexistant files
		if(!f.isFile())
			return false;
		//find the extension of the file
		String[] parts = f.getName().split("[.]");
		String fileExt = parts[parts.length-1].toLowerCase();
		//check the extension against those accepted
		for(int i=exts.length; i--!=0;)
			if(fileExt.equals(exts[i]))
				return true;
		//if not found return false
		return false;
	}
}