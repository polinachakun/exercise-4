package solid;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

/**
 * A CArtAgO artifact that agent can use to interact with LDP containers in a Solid pod.
 */
public class Pod extends Artifact {

    private String podURL; // the location of the Solid pod 

  /**
   * Method called by CArtAgO to initialize the artifact. 
   *
   * @param podURL The location of a Solid pod
   */
    public void init(String podURL) {
        this.podURL = podURL;
        log("Pod artifact initialized for: " + this.podURL);
    }

  /**
   * CArtAgO operation for creating a Linked Data Platform container in the Solid pod
   *
   * @param containerName The name of the container to be created
   * 
   */
@OPERATION
public void createContainer(String containerName) {
    try {
        String containerURL = this.podURL + "/" + containerName + "/";
        
        HttpURLConnection checkConn = (HttpURLConnection) new URL(containerURL).openConnection();
        checkConn.setRequestMethod("HEAD");
        
        if (checkConn.getResponseCode() == 200) {
            log("Container already exists: " + containerURL);
            return;
        }
        
        HttpURLConnection conn = (HttpURLConnection) new URL(this.podURL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/turtle");
        conn.setRequestProperty("Link", "<http://www.w3.org/ns/ldp#Container>; rel=\"type\"");
        conn.setRequestProperty("Slug", containerName);
        conn.setRequestProperty("Authorization", "Bearer " + getAuthToken());
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write("".getBytes());
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            log("Container created successfully: " + containerURL);
        } else {
            log("Failed to create container. Response code: " + responseCode);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    log(line);
                }
            }
        }
    } catch (Exception e) {
        log("Error creating container: " + e.getMessage());
        e.printStackTrace();
    }
}

  /**
   * CArtAgO operation for publishing data within a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource will be created
   * @param fileName The name of the .txt file resource to be created in the container
   * @param data An array of Object data that will be stored in the .txt file
   */
    @OPERATION
    public void publishData(String containerName, String fileName, Object[] data) {
            try {
                String resourceURL = this.podURL + "/" + containerName + "/" + fileName;
                
                String content = createStringFromArray(data);
                
                HttpURLConnection conn = (HttpURLConnection) new URL(resourceURL).openConnection();
                conn.setRequestMethod("PUT"); 
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "text/plain"); 
                conn.setRequestProperty("Authorization", "Bearer " + getAuthToken());
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(content.getBytes());
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    log("Data published successfully to: " + resourceURL);
                } else {
                    log("Failed to publish data. Response code: " + responseCode);
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            log(line);
                        }
                    }
                }
            } catch (Exception e) {
                log("Error publishing data: " + e.getMessage());
                e.printStackTrace();
            }
    }

  /**
   * CArtAgO operation for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @param data An array whose elements are the data read from the .txt file
   */
    @OPERATION
    public void readData(String containerName, String fileName, OpFeedbackParam<Object[]> data) {
        data.set(readData(containerName, fileName));
    }

  /**
   * Method for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @return An array whose elements are the data read from the .txt file
   */
    public Object[] readData(String containerName, String fileName) {
        try {
            String resourceURL = this.podURL + "/" + containerName + "/" + fileName;
            
            HttpURLConnection conn = (HttpURLConnection) new URL(resourceURL).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "text/plain");
            conn.setRequestProperty("Authorization", "Bearer " + getAuthToken());
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                StringBuilder content = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                
                log("Data read successfully from: " + resourceURL);
               
                return createArrayFromString(content.toString());
            } else {
                log("Failed to read data. Response code: " + responseCode);
                return new Object[0];
                }
        } catch (Exception e) {
            log("Error reading data: " + e.getMessage());
            e.printStackTrace();
            
            return new Object[0];
        }
    }

  /**
   * Method that converts an array of Object instances to a string, 
   * e.g. the array ["one", 2, true] is converted to the string "one\n2\ntrue\n"
   *
   * @param array The array to be converted to a string
   * @return A string consisting of the string values of the array elements separated by "\n"
   */
    public static String createStringFromArray(Object[] array) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : array) {
            sb.append(obj.toString()).append("\n");
        }
        return sb.toString();
    }

  /**
   * Method that converts a string to an array of Object instances computed by splitting the given string with delimiter "\n"
   * e.g. the string "one\n2\ntrue\n" is converted to the array ["one", "2", "true"]
   *
   * @param str The string to be converted to an array
   * @return An array consisting of string values that occur by splitting the string around "\n"
   */
    public static Object[] createArrayFromString(String str) {
        return str.split("\n");
    }


  /**
   * CArtAgO operation for updating data of a .txt file in a Linked Data Platform container of the Solid pod
   * The method reads the data currently stored in the .txt file and publishes in the file the old data along with new data 
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be updated
   * @param data An array whose elements are the new data to be added in the .txt file
   */
    @OPERATION
    public void updateData(String containerName, String fileName, Object[] data) {
        Object[] oldData = readData(containerName, fileName);
        Object[] allData = new Object[oldData.length + data.length];
        System.arraycopy(oldData, 0, allData, 0, oldData.length);
        System.arraycopy(data, 0, allData, oldData.length, data.length);
        publishData(containerName, fileName, allData);
    }

    private String getAuthToken() {
        return "afd7d4aac5c0bf42e33a95cfb9330683395d26cae42c57febc8222258645dbf133a663b119aba53c25fc3494f441f544aae0c21a43d7efb002f41635f66d2290"; 
    }
}
