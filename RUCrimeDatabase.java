package crime;
import edu.rutgers.cs112.LL.LLNode;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
/**
 * Students will be analyzing cybercrime incident data using hash tables (with separate chaining) 
 * and linked lists to organize and query crime records efficiently. The theme revolves around 
 * parsing, storing, and analyzing real-world crime logs from Rutgers University’s public 
 * safety database: Daily Crime & Fire Safety Log.
 * 
 * @author Anna Lu
 * @author Krish Lenka
 */
public class RUCrimeDatabase {
    private LLNode<Incident>[] incidentTable; // Array of LLNodes
    private int totalIncidents;
    private static final double LOAD_FACTOR_THRESHOLD = 4.0;

    /**
     * Default constructor initializes the hash table with a size of 10.
     * The total number of incidents is set to zero.
     */
    public RUCrimeDatabase() {
        incidentTable = new LLNode[10];
        totalIncidents = 0;
    }

    /**
     * Adds a new incident to the hash table
     * @param incident An incident object which we will use to add to the hash table:
     */
    public void addIncident(Incident incident) {
        //compute hash index/case # is index
        int index = hashFunction(incident.getIncidentNumber());

        //insert at head of linked list
        LLNode<Incident> newNode = new LLNode <> (incident);
        newNode.setNext(incidentTable[index]);
        incidentTable[index]= newNode;

        totalIncidents++;

        //check load factor
        double loadFactor = (double)totalIncidents/incidentTable.length;
        if (loadFactor >= LOAD_FACTOR_THRESHOLD){ //exceed/equals 4.0
            rehash();
        }
    }

    /**
     * Reads the csv file, creates an Incident object for each line, and calls addIncident().
     * @param filename Path to file containing incident data
     */
    public void buildIncidentTable(String inputfile) {
        StdIn.setFile(inputfile);

        while (!StdIn.isEmpty()){
            String line = StdIn.readLine();
            String[] parts = line.split(",");

            String incidentNumber = parts[0];
            String nature = parts[1];//I made an edit
            String reportDate = parts[2];
            String occurenceDate = parts[3];
            String location = parts[4];
            String disposition = parts[5];
            String genLocation = parts[6];
            Category category = Category.fromString(nature.trim().toLowerCase());

            //constructor
            Incident incident = new Incident (
                incidentNumber,
                nature,
                reportDate,
                occurenceDate,
                location,
                disposition,
                genLocation,
                category
            );
            addIncident(incident); //add to hash table
        }
    }
 

    /**
     * Rehashes the incident groups in the hash table.
     * This is called when the load factor exceeds a certain threshold.
     */
    public void rehash() { //rebuild hash table from scratch
        //save old table
        LLNode<Incident> [] oldTable = incidentTable;

        //new table with double the size
        incidentTable = (LLNode<Incident>[]) new LLNode[oldTable.length * 2];

        //reset count
        totalIncidents = 0;

        for (LLNode<Incident> head : oldTable){
            LLNode<Incident> current = head;
            while (current != null){
                addIncident(current.getData());
                current = current.getNext();
            }
        }   
    } 

    /**
     * Deletes an incident based on its incident number.
     * @param incidentNumber The incident number of the incident to delete 
     */
    public void deleteIncident(String incidentNumber) {
        int index = hashFunction(incidentNumber);

        LLNode<Incident> current = incidentTable[index];
        LLNode<Incident> previous = null;

        while (current != null){
            if (current.getData().getIncidentNumber().equals(incidentNumber)){
                //found delete front
                if (previous == null){
                    incidentTable[index] = current.getNext();
                } else{
                    previous.setNext(current.getNext());
                }
                totalIncidents--;
                return;
            }
            previous = current;
            current = current.getNext();
        }
    }

    /**
     * Iterates over another RUCrimeDatabase's incident table, and adds its incidents
     * to this table IF they do not already exist.
     * @param other RUCrimeDatabase to copy new incidents from
     */
    public void join(RUCrimeDatabase other) {
        LLNode<Incident>[] otherTable = other.getIncidentTable();

        for(LLNode<Incident> head : otherTable){
            LLNode<Incident> current = head;

            while (current != null){
                Incident inc = current.getData();

                //check existence first
                if (!containsIncident(inc.getIncidentNumber())){
                    addIncident(inc);
                }
                current = current.getNext();
            }
        }
    } 
    private boolean containsIncident(String incidentNumber){
        int index = hashFunction(incidentNumber);

        LLNode<Incident> current = incidentTable[index];

        while (current != null){
            if (current.getData().getIncidentNumber().equals(incidentNumber)){
                return true;
            }
            current = current.getNext();
        }
        return false;
    }
    
    /**
     * Returns a list of the top K locations with the most incidents 
     * If K > numLocations, return all locations
     * @return ArrayList<String> containing the top K locations
     */
    public ArrayList<String> topKLocations(int K) {
        String [] locations = {"ACADEMIC", "CAMPUS SERVICES", "OTHER", "PARKING LOT",
            "RECREATION", "RESIDENTIAL", "STREET/ROADWAY"};
        
        int [] counts = new int [locations.length];

        for (LLNode<Incident> head: incidentTable){
            LLNode<Incident> current = head;

            while (current != null){
                String loc = current.getData().getGeneralLocation();
                for (int i = 0; i < locations.length; i++){
                    if (locations[i].equals(loc)){
                        counts[i]++;
                        //return;
                    }
                }
                current = current.getNext();
            }
        }

        ArrayList<String> result = new ArrayList<>();

        for (int t=0; t<K && t < locations.length; t++){
            int maxIndex = 0;
            for (int i = 1; i < locations.length; i++){
                if (counts[i] > counts[maxIndex]){
                    maxIndex = i;
                }
            }
            if (counts[maxIndex] == 0){
                return null; //no more locations with incidents
            }
            result.add(locations[maxIndex]);
            counts[maxIndex] = -1; //mark as used
        }
        return result; // Replace this line, it is provided so the code compiles
    }  

    /**
     * Returns the percentage of incidents for every category.
     * Categories: Property, Violent,
     *             Mischief, Trespass, or Other
     * @return A HashMap<Category, Double> with percentage of incidents of each category
     */
    public HashMap<Category, Double> natureBreakdown() { 
        HashMap<Category, Double> map = new HashMap<>();
        //intialize all categories to 0
        for (Category c: Category.values()){
            map.put(c, 0.0); 
            //categories already saved, adds all 5
        }

        if (totalIncidents == 0){
            return map;
        }

        //count category occurrences/scan hash table
        for (LLNode<Incident> head: incidentTable){
            LLNode<Incident> current = head;

            while (current != null){
                Category c = current.getData().getCategory();
                map.put(c, map.get(c)+1); //increment count
                current = current.getNext();
            }
        }
        //converts counts to percentages
        for (java.util.Map.Entry<Category, Double> entry : map.entrySet()){
            double count = entry.getValue();
            double percentage = 100*(count/totalIncidents);
            entry.setValue(percentage);
        }
        return map; 
        //hashmaps don't guarantee order
        //as long as categories/values are correct
    }

    //Given methods
    /**
     * DO NOT MODIFY THIS METHOD.
     * Returns the hash table array for inspection/testing
     * @return The array of LLNode<IncidentGroup> representing the hash table
     */
    public LLNode<Incident>[] getIncidentTable() {
        return incidentTable;
    }

    public void setIncidentTable(LLNode<Incident>[] incidentTable) {
        this.incidentTable = incidentTable;
    }

    public int numberOfIncidents() {
        return totalIncidents;
    }

    /**
     * DO NOT MODIFY THIS METHOD.
     * Returns the index in the hash table for a given incident number.
     * @return The index in the hash table for the incident number
     * @param incidentNumber The incident number to hash
     */
    private int hashFunction(String incidentNumber) {
        String last5Digits = incidentNumber.substring(Math.max(0, incidentNumber.length() - 5));
        int val = Integer.parseInt(last5Digits) % incidentTable.length;
        //System.out.println("Hashing incident number: " + last5Digits + " val: " + val);
        return val;
    }

}