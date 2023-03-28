import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Similarity {
    protected HashMap<String, HashMap<String, Double>> termFrequencies; // hashmap of target words and words in the sentence with them
    // and the count of words that occur with them
    protected HashMap<String, ArrayList<String>> targetInfo; // hashmap of target words corresponding to an arraylist with
    // the weighting in index 0 and the similarity measure in index 1
    protected HashMap<String, Double> sentenceFrequencies;
    protected HashSet<String> stopList; // hashset containing words in the stopList
    protected HashSet<String> uniqueSet; // hashset containing all unique words in sentences file
    protected ArrayList<String> uniqueList;
    protected ArrayList<Double> idfVector;
    protected Double numSentences; // number of sentences in the sentences file
    protected int wordCount; // number of word occurrences in the sentences file

    /**
     * @param stopListFile is a list of stop words, one per line, that should be ignored from the input
     * @param sentences is a list of sentences/text fragments, one per line, to be used for training the
     *                  distributional similarity method.
     * @param inputFile is a file consisting of lines of the following form:
     *                  <word> <weighting> <sim_measure> (tab separated), where <weighting> is one of:
     *                      – TF: term frequency - use the number of times each word occurs in the word context.
     *                      – TFIDF: term frequency with inverse document frequency weighting – use the term
     *                      frequency times the inverse document frequency. Calculate IDF using the <sentences>
     *                      file treating each line as a separate document.
     *                  and <sim measure> is one of:
     *                      – L1: L1 distance, normalized by the L2 (Euclidean) length of the vectors.
     *                      – EUCLIDEAN: Euclidean distance, normalized by the L2 (Euclidean) length of the vectors.
     *                      – COSINE: Cosine distance, normalized by the L2 (Euclidean) length of the vectors.
     */
    public Similarity(String stopListFile, String sentences, String inputFile) throws IOException {
        // Initialize data structures
        termFrequencies = new HashMap<>();
        sentenceFrequencies = new HashMap<>();
        targetInfo = new HashMap<>();
        stopList = new HashSet<>();
        uniqueSet = new HashSet<>();
        uniqueList = new ArrayList<>();
        wordCount = 0;
        numSentences = 0.0;

        // Read stop words from file and add them to stopList
        File file = new File(stopListFile);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st = br.readLine();

        while (st != null) {
            String stopWord = st.toLowerCase();
            stopList.add(stopWord);

            st = br.readLine();
        }

        // Read target words and their weighting/similarity measures from file and add them to targetInfo
        file = new File(inputFile);
        br = new BufferedReader(new FileReader(file));
        st = br.readLine();

        while (st != null) {
            String[] words = st.split("\\s+");

            if (!targetInfo.containsKey(words[0])) {
                ArrayList<String> infoList = new ArrayList<>();
                infoList.add(words[1]); // add weighting
                infoList.add(words[2]); // add similarity measure
                targetInfo.put(words[0], infoList);
            }

            st = br.readLine();
        }

        // Read sentences from file and process each sentence
        file = new File(sentences);
        br = new BufferedReader(new FileReader(file));
        st = br.readLine();
        ArrayList<String> currentWords;

        while (st != null) {
            currentWords = new ArrayList<>();
            String stLower = st.toLowerCase();

            String[] words = stLower.split("\\s+");
            ArrayList<String> rawWords = new ArrayList<String>();

            // Preprocess sentence
            for (String word : words) {
                // Skip stop words
                if (stopList.contains(word)) {
                    continue;
                }

                // Add all words that are exclusively letters
                if (isAlpha(word)) {
                    rawWords.add(word);
                }
            }

            // Iterate over each word in the sentence and process it
            for (int j = 0; j < rawWords.size(); j++) {
                String jWord = rawWords.get(j);

                // Add word to sentenceFrequencies if it hasn't been seen before
                if (!sentenceFrequencies.containsKey(jWord)) {
                    sentenceFrequencies.put(jWord, 0.0);
                }

                // Add word to currentWords if it hasn't been seen in this sentence before
                if (!currentWords.contains(jWord)) {
                    currentWords.add(jWord);
                }

                // Add word to uniqueSet and uniqueList if it hasn't been seen
                if (!uniqueSet.contains(jWord)) {
                    // adds word if not already in set
                    uniqueSet.add(jWord);
                    uniqueList.add(jWord);
                }

                // Add word to termFrequencies if it hasn't been seen before
                if (!termFrequencies.containsKey(jWord)) {
                    termFrequencies.put(jWord, new HashMap<>());
                }

                int startContext;
                if (j <= 2) {
                    startContext = 0;
                } else {
                    startContext = j - 2;
                }

                int endContext;
                if (j > (rawWords.size() - 3)) {
                    endContext = rawWords.size() - 1;
                } else {
                    endContext = j + 2;
                }

                // Iterate over each other word in the sentence and process it
                for (int k = startContext; k <= endContext; k++) {
                    String kWord = rawWords.get(k);

                    // Don't count a word as co-occurring with itself
                    if (j == k) { continue; }

                    // Add word to termFrequencies and increment its count
                    if (!termFrequencies.get(jWord).containsKey(kWord)) {
                        termFrequencies.get(jWord).put(kWord, 1.0);
                    } else {
                        termFrequencies.get(jWord).put(kWord, termFrequencies.get(jWord).get(kWord) + 1);
                    }
                }

                // Increment word count
                wordCount++;
            }

            numSentences++;

            //update sentence frequencies
            for (String currentWord : currentWords) {
                sentenceFrequencies.put(currentWord, sentenceFrequencies.get(currentWord) + 1.0);
            }

            st = br.readLine();
        }

        //update idf vector
        idfVector = new ArrayList<>(uniqueList.size());
        for (int i = 0; i < uniqueList.size(); i++) {
            Double logCalc = Math.log(numSentences/sentenceFrequencies.get(uniqueList.get(i)));
            idfVector.add(i, logCalc);
        }

        //print the number of unique words, number of words, and number of sentences
        System.out.println(uniqueSet.size());
        System.out.println(wordCount);
        System.out.println(numSentences);

        runStats();
    }


    /**

     This method runs the similarity calculations for each target word in the targetInfo map and prints out the top 10 most similar words
     for each target word.
     It loops through each target word in the targetInfo map, retrieves the weighting and similarity measure strings from the information list
     for the current target word, and calls the runSims method with the current target word, weighting, and similarity measure.
     For each target word, it also prints out the current target word, its weighting, and similarity measure.
     */
    public void runStats() {
        // Loop through each target word in the targetInfo map
        for (String targetWord : targetInfo.keySet()) {
            // Get the information list for the current target word
            ArrayList<String> infoList = targetInfo.get(targetWord);
            // Extract the weighting and similarity measure strings from the information list
            String weighting = infoList.get(0);
            String simMeasure = infoList.get(1);

            // Print out the current target word, its weighting, and similarity measure
            System.out.println("\nSIM: " + targetWord + " " + targetInfo.get(targetWord).get(0)
                    + " " + targetInfo.get(targetWord).get(1));

            // Call the runSims method with the current target word, weighting, and similarity measure
            runSims(targetWord, weighting, simMeasure);
        }
    }


    /**

     Retrieves the term frequency vector for a given word, represented as an ArrayList of Double objects.
     The term frequency vector is created by iterating over each unique word in the corpus and retrieving the
     count of the given word's occurrences. If the count is not present, the term frequency is set to 0.
     @param word the word for which the term frequency vector should be retrieved
     @return an ArrayList of Double objects representing the term frequency vector for the given word
     */
    public ArrayList<Double> getOccVec(String word) {
        // Create a new ArrayList of Double objects to store the term frequencies
        // for a given word
        ArrayList<Double> occVec = new ArrayList<>(uniqueList.size());

        // Declare a variable to store the term frequency for each unique word in the
        // corpus
        double wCount;

        // Iterate over each unique word in the corpus
        for (int i = 0; i < uniqueList.size(); i++) {
            // If the term frequency map for the given word contains a count for the
            // current unique word, retrieve it and store it in wCount
            if (termFrequencies.get(word).get(uniqueList.get(i)) != null) {
                wCount = termFrequencies.get(word).get(uniqueList.get(i));
            } else {
                // Otherwise, set wCount to 0
                wCount = 0;
            }

            // Add the term frequency for the current unique word to the occVec list
            occVec.add(i, wCount);
        }

        // Return the list of term frequencies for the given word
        return occVec;
    }


    /**

     This method takes an ArrayList of doubles and converts each value by multiplying it
     with the corresponding IDF value for the word in the uniqueList of words.
     @param vec an ArrayList of doubles representing a vector
     */
    public void convertToIDF(ArrayList<Double> vec) {
        // iterate through the uniqueList of words
        for (int j = 0; j < uniqueList.size(); j++) {
            // retrieve the current value in the vector
            double current = vec.get(j);
            // multiply the current value by the IDF value for the corresponding word
            vec.set(j, current * idfVector.get(j));
        }
    }


    /**

     Calculates and prints out the top 10 most similar words to a target word using the selected weighting and similarity measure.
     @param targetWord the word to compare against
     @param weighting the weighting method to use, either "RAW" or "IDF"
     @param simMeasure the similarity measure to use, either "L1", "EUCLIDEAN", or "COSINE"
     */
    public void runSims(String targetWord, String weighting, String simMeasure) {
        // create a list of words to compare against
        ArrayList<String> wordsList = new ArrayList<>(uniqueList.size());
        wordsList.addAll(uniqueList);

        // create a list of distances, initialized to 0
        ArrayList<Double> distanceList = new ArrayList<>(Collections.nCopies(wordsList.size(), 0.0));

        // get the occurrence vector for the target word
        ArrayList<Double> vec1 = getOccVec(targetWord);
        ArrayList<Double> vec2 = new ArrayList<>();

        // if using IDF weighting, convert the target word vector to IDF
        if (weighting.equals("IDF")) {
            convertToIDF(vec1);
        }

        // normalize the occurrence vector of the target word
        normVec(vec1);

        // compare the target word to each word in the list using the selected similarity measure
        if (simMeasure.equals("L1")) {
            for (String word : wordsList){
                if(!word.equals(targetWord)) {
                    vec2 = getOccVec(word);
                    if (weighting.equals("IDF")) {
                        convertToIDF(vec2);
                    }

                    // normalize the occurrence vector of the current word
                    normVec(vec2);

                    // calculate the L1 distance between the target word vector and the current word vector
                    distanceList.set(wordsList.indexOf(word), getL1Distance(vec1, vec2));
                }
            }
        }

        if (simMeasure.equals("EUCLIDEAN")) {
            for (String word : wordsList){
                if(!word.equals(targetWord)) {
                    vec2 = getOccVec(word);
                    if (weighting.equals("IDF")) {
                        convertToIDF(vec2);
                    }

                    // normalize the occurrence vector of the current word
                    normVec(vec2);

                    // calculate the Euclidean distance between the target word vector and the current word vector
                    distanceList.set(wordsList.indexOf(word), getEuclideanDistance(vec1, vec2));
                }
            }
        }

        if (simMeasure.equals("COSINE")) {
            for (String word : wordsList){
                if(!word.equals(targetWord)) {
                    vec2 = getOccVec(word);
                    if (weighting.equals("IDF")) {
                        convertToIDF(vec2);
                    }

                    // normalize the occurrence vector of the current word
                    normVec(vec2);

                    // calculate the cosine distance between the target word vector and the current word vector
                    distanceList.set(wordsList.indexOf(word), getCosineDistance(vec1, vec2));
                }
            }
        }

        // sort the distance list
        modQuickSort(distanceList, 0, distanceList.size() - 1, wordsList);

        // reverse the list if using L1 or Euclidean distance measure
        if (simMeasure.equals("L1") || simMeasure.equals("EUCLIDEAN")) {
            Collections.reverse(distanceList);
            Collections.reverse(wordsList);
        }

        // print out the top 10 most similar words
        if (distanceList.size() < 10) {
            // if there are less than 10 words, print out all of them
            Collections.reverse(distanceList);
            Collections.reverse(wordsList);
            for (int k = 0; k < distanceList.size(); k++){
                System.out.println(wordsList.get(k) + "\t" + distanceList.get(k));
            }
        } else {
            // if there are more than 10 words, print out the top 10
            for (int i = distanceList.size() - 1; i > distanceList.size() - 11; i--){
                System.out.println(wordsList.get(i) + "\t" + distanceList.get(i));
            }
        }
    }


    /**

     Calculates the L1 distance between two vectors.
     @param vector1 first vector
     @param vector2 second vector
     @return the L1 distance between the two vectors
     @throws IllegalArgumentException if the vectors are not of equal size
     */
    public double getL1Distance(ArrayList<Double> vector1, ArrayList<Double> vector2) {
        // Check if the vectors are of equal size
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vectors must be of equal size");
        }
        double sum = 0;
        // Calculate the L1 distance by summing the absolute differences of each corresponding element
        for (int i = 0; i < vector1.size(); i++) {
            double diff = vector1.get(i) - vector2.get(i);
            sum += Math.abs(diff);
        }
        return sum;
    }


    /**
     * Calculates the Euclidean distance between two vectors of doubles.
     *
     * @param vector1 the first vector of doubles
     * @param vector2 the second vector of doubles
     * @return the Euclidean distance between the two vectors
     * @throws IllegalArgumentException if the two vectors are not of equal size
     */
    public double getEuclideanDistance(ArrayList<Double> vector1, ArrayList<Double> vector2) {
        // Check that the two vectors have the same size
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vectors must be of equal size");
        }

        // Calculate the sum of squared differences between corresponding elements of the two vectors
        double sum = 0;
        for (int i = 0; i < vector1.size(); i++) {
            double diff = vector1.get(i) - vector2.get(i);
            sum += diff * diff;
        }

        // Take the square root of the sum of squared differences to get the Euclidean distance
        return Math.sqrt(sum);
    }


    /**

     Calculates the cosine distance between two vectors.
     @param vector1 the first vector
     @param vector2 the second vector
     @return the cosine distance between the two vectors
     @throws IllegalArgumentException if the two vectors are not of the same length
     */
    public double getCosineDistance(ArrayList<Double> vector1, ArrayList<Double> vector2) {
        // Throw an exception if the vectors are not of the same length
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vectors must be of the same length.");
        }
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        // Calculate the dot product, and the norm of each vector
        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }
        // Check for divide-by-zero error and return the cosine distance
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0;
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }


    /**

     Calculates the L2 (Euclidean) length of a vector.
     @param vec the vector to calculate the length of
     @return the L2 length of the vector
     */
    public double getL2Length(ArrayList<Double> vec) {
        double sumOfSquares = 0.0;
        for (Double d : vec) {
            sumOfSquares += d * d;
        }
        return Math.sqrt(sumOfSquares);
    }


    /**

     Normalizes the input vector in-place to have a Euclidean L2-norm of 1.
     @param vec the vector to be normalized
     */
    public void normVec(ArrayList<Double> vec) {
        // Calculate the Euclidean L2-norm of the vector
        double l2Length = getL2Length(vec);
        // Check if the L2-norm is zero to avoid division by zero
        if (l2Length != 0) {
        // Divide each element in the vector by the L2-norm
            for (int i = 0; i < vec.size(); i++) {
                vec.set(i, vec.get(i) / l2Length);
            }
        }
    }


    /**
     * Sorts an ArrayList of Doubles in non-decreasing order using the modified quicksort algorithm,
     * and updates a corresponding ArrayList of Strings to maintain their relation.
     *
     * @param arr the ArrayList of Doubles to be sorted
     * @param low the starting index of the sublist to be sorted
     * @param high the ending index of the sublist to be sorted
     * @param wordArr the ArrayList of Strings containing the corresponding words
     */
    public static void modQuickSort(ArrayList<Double> arr, int low, int high, ArrayList<String> wordArr) {
        if (low < high) {
            // Choose a pivot element and partition the list
            int pivotIndex = partition(arr, low, high, wordArr);
            // Recursively sort the left and right sublists
            modQuickSort(arr, low, pivotIndex - 1, wordArr);
            modQuickSort(arr, pivotIndex + 1, high, wordArr);
        }
    }


    /**
     * Partitions an ArrayList of Doubles around a pivot element, and updates a corresponding
     * ArrayList of Strings to maintain their relation.
     *
     * @param arr the ArrayList of Doubles to be partitioned
     * @param low the starting index of the sublist to be partitioned
     * @param high the ending index of the sublist to be partitioned
     * @param wordArr the ArrayList of Strings containing the corresponding words
     * @return the index of the pivot element after partitioning
     */
    public static int partition(ArrayList<Double> arr, int low, int high, ArrayList<String> wordArr) {
        // Choose the pivot element to be the last element in the sublist
        double pivot = arr.get(high);
        // i is the index of the last element in the left sublist
        int i = low - 1;
        for (int j = low; j < high; j++) {
            // If the current element is less than or equal to the pivot, move it to the left sublist
            if (arr.get(j) <= pivot) {
                i++;
                // Swap the current element with the first element in the right sublist
                double temp = arr.get(i);
                arr.set(i, arr.get(j));
                arr.set(j, temp);

                // Update the corresponding word array
                String temp2 = wordArr.get(i);
                wordArr.set(i, wordArr.get(j));
                wordArr.set(j, temp2);
            }
        }
        // Swap the pivot element with the first element in the right sublist
        double temp = arr.get(i + 1);
        arr.set(i + 1, arr.get(high));
        arr.set(high, temp);

        // Update the corresponding word array
        String temp2 = wordArr.get(i + 1);
        wordArr.set(i + 1, wordArr.get(high));
        wordArr.set(high, temp2);

        // Return the index of the pivot element after partitioning
        return i + 1;
    }


    /**

     Determines if a given string consists only of letters.
     @param word the string to be checked
     @return true if the string consists only of letters, false otherwise
     */
    public boolean isAlpha(String word) {
        return word.matches("[a-zA-Z]+");
    }

    public static void main(String[] args) throws IOException {
        String stopListFile = "/Users/ezraford/Desktop/School/CS 159/NLP-Word-Similarity/data/stoplist";
        String sentences = "/Users/ezraford/Desktop/School/CS 159/NLP-Word-Similarity/data/sentences";
        String inputFile = "/Users/ezraford/Desktop/School/CS 159/NLP-Word-Similarity/data/test";

//        String stopListFile = "/Users/talmordoch/Library/Mobile Documents/com~apple~CloudDocs/NLP-Word-Similarity/data/stoplist";
//        String sentences = "/Users/talmordoch/Library/Mobile Documents/com~apple~CloudDocs/NLP-Word-Similarity/data/sentences";
//        String inputFile = "/Users/talmordoch/Library/Mobile Documents/com~apple~CloudDocs/NLP-Word-Similarity/data/test";

        Similarity simRun = new Similarity(stopListFile, sentences, inputFile);
    }
}
