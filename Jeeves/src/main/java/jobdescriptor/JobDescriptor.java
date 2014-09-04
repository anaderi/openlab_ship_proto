package jobdescriptor;

import com.cedarsoftware.util.io.JsonWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.UnknownHostException;

/**
 *
 * @author Dimitris Sarigiannis
 */
public class JobDescriptor {

    private String jobDescriptorTxt;
    private JSONObject obj;

    public static int Split_equal_arg;
    public static ArrayList<Integer> Split_proportional_list;
    public static ArrayList<Integer> Split_next_args;

    private int nextCounter;
    private int currentJobDescriptor;

    private final String outputFolder = "output";

    public static void initArgs() {
        Split_equal_arg = 3;

        Split_proportional_list = new ArrayList<Integer>();
        Split_proportional_list.add(10);
        Split_proportional_list.add(90);

        Split_next_args = new ArrayList<Integer>();
        //Split_next_args.add(500);
        Split_next_args.add(300);
        //Split_next_args.add(200);
        //Split_next_args.add(1);
    }

    public JobDescriptor(String jobDescriptorName) throws FileNotFoundException, IOException, JSONException {

        File outputDir = new File(outputFolder);
        initDir(outputDir);
        createOutputFolder(outputDir);

        this.nextCounter = 0; //for next sub-jobdescriptor
        this.currentJobDescriptor = 0;

        try {
            BufferedReader br = new BufferedReader(new FileReader(jobDescriptorName + this.currentJobDescriptor+".json"));
            this.jobDescriptorTxt = "";
            do {
                String temp = br.readLine();
                if (temp == null) {
                    break;
                }
                jobDescriptorTxt += temp;
            } while (true);
        } catch (FileNotFoundException fe) {
            System.err.println("File " + jobDescriptorName + this.currentJobDescriptor + ".json not found");
            System.exit(0);
        }

        this.obj = new JSONObject(this.jobDescriptorTxt);
    }

    /*
     Constructor only for testing 
     */
    public JobDescriptor(String jobDescriptorName, int i) throws FileNotFoundException, IOException, JSONException {

        this.nextCounter = 0; //for next sub-jobdescriptor
        this.currentJobDescriptor = 0;

        try {
            System.out.println(jobDescriptorName + i + ".json");
            BufferedReader br = new BufferedReader(new FileReader(jobDescriptorName + i + ".json"));
            this.jobDescriptorTxt = "";
            do {
                String temp = br.readLine();
                if (temp == null) {
                    break;
                }
                jobDescriptorTxt += temp;
            } while (true);
        } catch (FileNotFoundException fe) {
            System.err.println("File " + jobDescriptorName + i + ".json not found");
            System.exit(0);
        }

        this.obj = new JSONObject(this.jobDescriptorTxt);
    }

    private void createOutputFolder(File theDir) {

        if (!theDir.exists()) {
            try {
                theDir.mkdir();
            } catch (SecurityException se) {
                System.exit(1);
            }
        }
    }

    private void initDir(File dir) {
        for (File file : dir.listFiles()) {
            file.delete();
        }
    }

    /*
     Split the JobDescriptor file
     */
    public void Split_equal(int N) throws IOException, JSONException {

        Random RANDOM_SEED = new Random();
        int reduceSCALE = 0;
        
        int scaleBalance = this.getSCALE() % N;

        int rangeBalance = (this.getRANGE().get(1) - this.getRANGE().get(0) + 1) % N;
        int rangeInterval = (this.getRANGE().get(1) - this.getRANGE().get(0) + 1) / N;
        int currentRangeValue = 1;

        for (int i = 1; i <= N; i++) {

            JSONObject subObj = new JSONObject();

            subObj.put("name", obj.get("name").toString());
            subObj.put("environments", obj.get("environments"));
            subObj.put("owner", obj.get("owner").toString());
            subObj.put("app", obj.get("app").toString());
            subObj.put("workdir", obj.get("workdir").toString());
            subObj.put("cmd", obj.get("cmd").toString());

            JSONObject subObj1 = new JSONObject(obj.get("args").toString());
            subObj1.put("default", subObj1.get("default"));

            JSONArray subArr; //e.g. ["nEvents","SCALE",1000]

            ArrayList<JSONArray> sublists = new ArrayList<JSONArray>(); //e.g. ["nEvents","SCALE",500]
            JSONArray sublist = new JSONArray(); //e.g. "nEvents"

            subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(0).toString());
            sublist.put(subArr.get(0));
            sublist.put(subArr.get(1));

            int newScale = (Integer) subArr.get(2) / N + ((scaleBalance--) > 0 ? 1 : 0);
            sublist.put(new Integer((Integer) newScale));
            //update jobdescriptor's SCALE
            reduceSCALE += new Integer((Integer) newScale);

            sublists.add(sublist);
            sublist = new JSONArray();
            subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(1).toString());
            sublist.put(subArr.get(0).toString());
            sublist.put(subArr.get(1).toString());
            sublist.put(subArr.get(2));
            sublists.add(sublist);
            sublist = new JSONArray();
            subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(2).toString());
            JSONArray rangeArray = new JSONArray();
            rangeArray.put(new Integer(currentRangeValue));
            int temp = currentRangeValue + rangeInterval - 1 + ((rangeBalance--) > 0 ? 1 : 0);
            rangeArray.put(new Integer(temp));
            currentRangeValue = temp + 1;
            sublist.put(subArr.get(0).toString());
            sublist.put(subArr.get(1).toString());
            sublist.put(rangeArray);
            sublists.add(sublist);
            sublist = new JSONArray();
            subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(3).toString());
            sublist.put(subArr.get(0));
            sublist.put(RANDOM_SEED.nextInt(1000000000));
            sublists.add(sublist);
            subObj1.put("scaleArg", sublists);

            subObj.put("args", subObj1);

            subObj.put("num_containers", new Integer((Integer) obj.get("num_containers")));
            subObj.put("min_memoryMB", new Integer((Integer) obj.get("min_memoryMB")));
            subObj.put("max_memoryMB", new Integer((Integer) obj.get("max_memoryMB")));
            subObj.put("cpu_per_container", new Integer((Integer) obj.get("cpu_per_container")));

            writeJSONObjectToFile("sub-jobdescriptor" + i, subObj);

        }

        this.setSCALE(this.getSCALE() - reduceSCALE);
    }

    /*
     Split the JobDescriptor file
     */
    //new format 70
    public void Split_equal_70() throws IOException, JSONException {

        Random RANDOM_SEED = new Random();

        JSONObject subObj = new JSONObject();

        for (int i = 1; i <= this.getSETsize(); i++) {

            subObj.put("name", obj.get("name"));
            subObj.put("job_id", i);
            subObj.put("job_parent_id", 0);
            subObj.put("job_super_id", obj.get("job_super_id"));
            subObj.put("env_container", new JSONObject().put("name", this.getEnvironements().get(0)));
            subObj.put("owner", obj.get("owner").toString());
            subObj.put("app_container", obj.get("app_container"));
            subObj.put("email", obj.get("email").toString());
            subObj.put("workdir", obj.get("workdir").toString());
            subObj.put("volumes", obj.get("volumes"));
            subObj.put("cmd", obj.get("cmd").toString());

            //create args
            JSONObject args = new JSONObject();
            JSONArray SET_Array = new JSONArray();
            SET_Array.put(this.getSET(i - 1));
            SET_Array.put(this.getOutput());
            args.put("__POS__", SET_Array);
            subObj.put("args", args);

            subObj.put("num_containers", new Integer((Integer) obj.get("num_containers")));
            subObj.put("min_memoryMB", new Integer((Integer) obj.get("min_memoryMB")));
            subObj.put("max_memoryMB", new Integer((Integer) obj.get("max_memoryMB")));
            subObj.put("cpu_per_container", new Integer((Integer) obj.get("cpu_per_container")));

            writeJSONObjectToFile("sub-jobdescriptor" + i, subObj);

        }

    }

    /*
     Split the JobDescriptor file
     */
    //new format 700
    public void Split_equal_700() throws IOException, JSONException {

        JSONObject subObj = new JSONObject();

        for (int i = 1; i <= this.getSETsize(); i++) {

            subObj.put("name", obj.get("name"));
            subObj.put("job_id", i);
            subObj.put("job_parent_id", 0);
            subObj.put("job_super_id", obj.get("job_super_id"));

            subObj.put("env_container", obj.getJSONObject("env_container"));

            subObj.put("owner", obj.get("owner").toString());
            subObj.put("app_container", obj.get("app_container"));
            subObj.put("email", obj.get("email").toString());
            subObj.put("cmd", obj.get("cmd").toString());

            //create args sub_obj
            JSONObject args = new JSONObject();
            args.put("__POS1__", this.getSET(i - 1));
            args.put("__POS2__", obj.getJSONObject("args").get("__POS2__"));
            subObj.put("args", args);

            subObj.put("num_containers", new Integer((Integer) obj.get("num_containers")));
            subObj.put("min_memoryMB", new Integer((Integer) obj.get("min_memoryMB")));
            subObj.put("max_memoryMB", new Integer((Integer) obj.get("max_memoryMB")));
            subObj.put("cpu_per_container", new Integer((Integer) obj.get("cpu_per_container")));

            writeJSONObjectToFile("sub-jobdescriptor" + i, subObj);

        }

    }

    public void Split_equal_ship() throws JSONException, IOException {

        int start = obj.getJSONObject("args").getJSONArray("scaleArg").getJSONArray(1).getJSONArray(2).getInt(0);
        int stop = obj.getJSONObject("args").getJSONArray("scaleArg").getJSONArray(1).getJSONArray(2).getInt(1);
        int N = stop - start + 1;

        JSONObject subObj = new JSONObject();
        int balance = this.getSCALE() % N;

        for (int i = start; i <= N; i++) {
            subObj.put("name", obj.get("name"));
            subObj.put("job_id", i);
            subObj.put("job_parent_id", 0);
            subObj.put("job_super_id", obj.get("job_id"));
            subObj.put("app_container", obj.get("app_container"));
            subObj.put("email", obj.get("email").toString());
            subObj.put("env_container", obj.getJSONObject("env_container"));
            subObj.put("cmd", obj.get("cmd").toString());

            //create args sub_obj
            JSONObject args = new JSONObject();
            args.put("--nEvents", ((Integer) obj.getJSONObject("args").getJSONArray("scaleArg").getJSONArray(0).get(2) / N) + ((balance--) > 0 ? 1 : 0));
            args.put("--run-number", i);
            args.put("--output", obj.getJSONObject("args").get("--output"));
            subObj.put("args", args);

            subObj.put("num_containers", new Integer((Integer) obj.get("num_containers")));
            subObj.put("min_memoryMB", new Integer((Integer) obj.get("min_memoryMB")));
            subObj.put("max_memoryMB", new Integer((Integer) obj.get("max_memoryMB")));
            subObj.put("cpu_per_container", new Integer((Integer) obj.get("cpu_per_container")));

            writeJSONObjectToFile("sub-jobdescriptor" + i, subObj);
        }
    }

    public void Split_proportional(ArrayList<Integer> l) throws JSONException, IOException {

        double sum = 0;
        for (Integer p : l) {
            sum += p * this.getSCALE() * 0.01;
        }
        if (this.getSCALE() != sum) {
            System.err.println("Split_propotional() is not not applicable.\nPlease change the proportions and try again.");
            return;
        }

        Random RANDOM_SEED = new Random();
        int reduceSCALE = 0;

        int rangeInitInterval = (this.getRANGE().get(1) - this.getRANGE().get(0) + 1);
        int currentRangeValue = 1;

        for (int i = 0; i < l.size(); i++) {

            JSONObject subObj = new JSONObject();

            subObj.put("name", obj.get("name").toString());
            subObj.put("environments", obj.get("environments"));
            subObj.put("owner", obj.get("owner").toString());
            subObj.put("app", obj.get("app").toString());
            subObj.put("workdir", obj.get("workdir").toString());
            subObj.put("cmd", obj.get("cmd").toString());

            JSONObject subObj1 = new JSONObject(obj.get("args").toString());
            subObj1.put("default", subObj1.get("default"));

            JSONArray subArr;

            ArrayList<JSONArray> sublists = new ArrayList<JSONArray>();
            JSONArray sublist = new JSONArray();

            subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(0).toString());
            sublist.put(subArr.get(0));
            sublist.put(subArr.get(1));

            sublist.put(new Integer(((Integer) subArr.get(2) * l.get(i)) / 100)); //we assume that we have exact division always.
            reduceSCALE += (((Integer) subArr.get(2) * l.get(i)) / 100);

            sublists.add(sublist);
            sublist = new JSONArray();
            subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(1).toString());
            sublist.put(subArr.get(0).toString());
            sublist.put(subArr.get(1).toString());
            sublist.put(subArr.get(2));
            sublists.add(sublist);
            sublist = new JSONArray();
            subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(2).toString());
            JSONArray rangeArray = new JSONArray();
            rangeArray.put(new Integer(currentRangeValue));
            int temp = currentRangeValue + (int) (rangeInitInterval * 0.01 * l.get(i)) - 1;
            rangeArray.put(new Integer(temp));
            currentRangeValue = temp + 1;
            sublist.put(subArr.get(0).toString());
            sublist.put(subArr.get(1).toString());
            sublist.put(rangeArray);
            sublists.add(sublist);
            sublist = new JSONArray();
            subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(3).toString());
            sublist.put(subArr.get(0));
            sublist.put(RANDOM_SEED.nextInt(1000000000));
            sublists.add(sublist);
            subObj1.put("scaleArg", sublists);

            subObj.put("args", subObj1);

            subObj.put("num_containers", new Integer((Integer) obj.get("num_containers")));
            subObj.put("min_memoryMB", new Integer((Integer) obj.get("min_memoryMB")));
            subObj.put("max_memoryMB", new Integer((Integer) obj.get("max_memoryMB")));
            subObj.put("cpu_per_container", new Integer((Integer) obj.get("cpu_per_container")));

            writeJSONObjectToFile("sub-jobdescriptor" + (i + 1), subObj);

        }

        this.setSCALE(this.getSCALE() - reduceSCALE);
    }

    public void Split_next(int X) throws JSONException, IOException {

        if (X > this.getSCALE()) {
            System.err.println("Split_next(" + X + ") has been canceled.\nPlease change the argument and try again.");
            return;
        }

        Random RANDOM_SEED = new Random();

        JSONObject subObj = new JSONObject();

        subObj.put("name", obj.get("name").toString());
        subObj.put("environments", obj.get("environments"));
        subObj.put("owner", obj.get("owner").toString());
        subObj.put("app", obj.get("app").toString());
        subObj.put("workdir", obj.get("workdir").toString());
        subObj.put("cmd", obj.get("cmd").toString());

        JSONObject subObj1 = new JSONObject(obj.get("args").toString());
        subObj1.put("default", subObj1.get("default"));

        JSONArray subArr;

        ArrayList<JSONArray> sublists = new ArrayList<JSONArray>();
        JSONArray sublist = new JSONArray();

        subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(0).toString());
        sublist.put(subArr.get(0));
        sublist.put(subArr.get(1));

        //set new sub-jobdescriptor's SCALE
        sublist.put(new Integer(X));
        //update jobdescriptor's SCALE
        this.setSCALE(this.getSCALE() - X);

        sublists.add(sublist);
        sublist = new JSONArray();
        subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(1).toString());
        sublist.put(subArr.get(0).toString());
        sublist.put(subArr.get(1).toString());
        sublist.put(subArr.get(2));
        sublists.add(sublist);
        sublist = new JSONArray();
        subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(2).toString());
        sublist.put(subArr.get(0).toString());
        sublist.put(subArr.get(1).toString());
        sublist.put(subArr.get(2));
        sublists.add(sublist);
        sublist = new JSONArray();
        subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(3).toString());
        sublist.put(subArr.get(0));
        sublist.put(RANDOM_SEED.nextInt(1000000000));
        sublists.add(sublist);
        subObj1.put("scaleArg", sublists);

        subObj.put("args", subObj1);

        subObj.put("num_containers", new Integer((Integer) obj.get("num_containers")));
        subObj.put("min_memoryMB", new Integer((Integer) obj.get("min_memoryMB")));
        subObj.put("max_memoryMB", new Integer((Integer) obj.get("max_memoryMB")));
        subObj.put("cpu_per_container", new Integer((Integer) obj.get("cpu_per_container")));

        writeJSONObjectToFile("sub-jobdescriptor" + (++this.nextCounter), subObj);
        //sub-jobdescriptor has been created successfully
    }

    public void writeJSONObjectToFile(String filename, JSONObject obj) throws IOException, JSONException {

        FileWriter file = new FileWriter(outputFolder + "/" + filename + ".json");

        file.write(JsonWriter.formatJson(obj.toString()));

        file.flush();
        file.close();
    }

    public int getSCALE() throws JSONException {
        JSONObject subObj = new JSONObject(obj.get("args").toString());
        JSONArray arr = new JSONArray(subObj.get("scaleArg").toString());
        JSONArray arr1 = new JSONArray(arr.getJSONArray(0).toString());
        return (Integer) arr1.get(2);
    }

    private void setSCALE(int newSCALE) throws JSONException, IOException {
        JSONObject subObj = new JSONObject();

        subObj.put("name", obj.get("name").toString());
        subObj.put("environments", obj.get("environments"));
        subObj.put("owner", obj.get("owner").toString());
        subObj.put("app", obj.get("app").toString());
        subObj.put("workdir", obj.get("workdir").toString());
        subObj.put("cmd", obj.get("cmd").toString());

        JSONObject subObj1 = new JSONObject(obj.get("args").toString());
        subObj1.put("default", subObj1.get("default"));

        JSONArray subArr;

        ArrayList<JSONArray> sublists = new ArrayList<JSONArray>();
        JSONArray sublist = new JSONArray();

        subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(0).toString());
        sublist.put(subArr.get(0));
        sublist.put(subArr.get(1));
        sublist.put(newSCALE);
        sublists.add(sublist);
        sublist = new JSONArray();
        subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(1).toString());
        sublist.put(subArr.get(0).toString());
        sublist.put(subArr.get(1).toString());
        sublist.put(subArr.get(2));
        sublists.add(sublist);
        sublist = new JSONArray();
        subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(2).toString());
        sublist.put(subArr.get(0).toString());
        sublist.put(subArr.get(1).toString());
        sublist.put(subArr.get(2));
        sublists.add(sublist);
        sublist = new JSONArray();
        subArr = new JSONArray(subObj1.getJSONArray("scaleArg").get(3).toString());
        sublist.put(subArr.get(0));
        sublist.put("RANDOM_SEED");
        sublists.add(sublist);
        subObj1.put("scaleArg", sublists);

        subObj.put("args", subObj1);

        subObj.put("num_containers", new Integer((Integer) obj.get("num_containers")));
        subObj.put("min_memoryMB", new Integer((Integer) obj.get("min_memoryMB")));
        subObj.put("max_memoryMB", new Integer((Integer) obj.get("max_memoryMB")));
        subObj.put("cpu_per_container", new Integer((Integer) obj.get("cpu_per_container")));

        obj = subObj;

        writeJSONObjectToFile("jobdescriptor" + (++this.currentJobDescriptor), subObj); //updates jobdescriptor's file (creates new file to keep history-log)
    }

    public Integer getRANDOM_SEED() throws JSONException {
        JSONObject subObj = new JSONObject(obj.get("args").toString());
        JSONArray arr = new JSONArray(subObj.get("scaleArg").toString());
        JSONArray arr1 = new JSONArray(arr.getJSONArray(3).toString());
        return (Integer) arr1.get(1);
    }

    public ArrayList<String> getEnvironements() throws JSONException {
        ArrayList<String> environments = new ArrayList<String>();
        JSONArray arr = new JSONArray(obj.get("environments").toString());
        for (int i = 0; i < arr.length(); i++) {
            environments.add((String) arr.get(i));
        }
        return environments;
    }

    public JSONObject getEnv_container() throws JSONException {
        return obj.getJSONObject("env_container");
    }

    public ArrayList<Integer> getSET() throws JSONException {
        JSONObject subObj = new JSONObject(obj.get("args").toString());
        JSONArray arr = new JSONArray(subObj.get("scaleArg").toString());
        JSONArray arr1 = new JSONArray(arr.getJSONArray(1).toString()); //set
        JSONArray arr2 = new JSONArray(arr1.getJSONArray(2).toString());
        ArrayList<Integer> SET = new ArrayList<Integer>();
        for (int i = 0; i < arr2.length(); i++) {
            SET.add(arr2.getInt(i));
        }
        return SET;
    }

    //new format70
    public String getSET(int i) throws JSONException {
        return obj.getJSONObject("args").getJSONArray("scaleArg").getJSONArray(1).getJSONArray(2).get(i).toString();
    }

    //new format70
    public String getOutput() throws JSONException {
        return obj.getJSONObject("args").getJSONArray("scaleArg").getJSONArray(2).get(1).toString();
    }

    //new format70
    public int getSETsize() throws JSONException {
        return obj.getJSONObject("args").getJSONArray("scaleArg").getJSONArray(1).getJSONArray(2).length();
    }

    public ArrayList<Integer> getRANGE() throws JSONException {
        JSONObject subObj = new JSONObject(obj.get("args").toString());
        JSONArray arr = new JSONArray(subObj.get("scaleArg").toString());
        JSONArray arr1 = new JSONArray(arr.getJSONArray(2).toString()); //set
        JSONArray arr2 = new JSONArray(arr1.getJSONArray(2).toString());
        ArrayList<Integer> RANGE = new ArrayList<Integer>();
        for (int i = 0; i < arr2.length(); i++) {
            RANGE.add(arr2.getInt(i));
        }
        return RANGE;
    }

    public String getOwner() throws JSONException {
        return obj.get("owner").toString();
    }

    public String getApp() throws JSONException {
        return obj.get("app").toString();
    }

    public String getWorkDir() throws JSONException {
        return obj.get("workdir").toString();
    }

    public String getCmd() throws JSONException {
        return obj.get("cmd").toString();
    }

    public String getName() throws JSONException {
        return obj.get("name").toString();
    }

    public int getNumberOfContainers() throws JSONException {
        return (Integer) obj.get("num_containers");
    }

    public int getMinMemoryMB() throws JSONException {
        return (Integer) obj.get("min_memoryMB");
    }

    public int getMaxMemoryMB() throws JSONException {
        return (Integer) obj.get("max_memoryMB");
    }

    public int getCPUperContainer() throws JSONException {
        return (Integer) obj.get("cpu_per_container");
    }

    public int getCurrentJobDescriptor() {
        return currentJobDescriptor;
    }

   /* 
    public static void main(String[] args) throws IOException, JSONException, InterruptedException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, UnknownHostException {
        //TODO
        
        initArgs();
        
        JobDescriptor jd = new JobDescriptor("jobdescriptor");
        
        jd.Split_equal(Split_equal_arg);
        //jd.Split_equal_ship();

        //       jd.Split_proportional(Split_proportional_list);
//        
//       
//        
//        jd.Split_next(Split_next_args.get(0));
//        jd.Split_next(Split_next_args.get(1));
//        jd.Split_next(Split_next_args.get(2));
//        jd.Split_next(Split_next_args.get(3));
    }
*/
}
