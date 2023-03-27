import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Similarity {
    protected HashMap<String, HashMap<String, Double>> termFrequencies; // hashmap of target words and the count of words that occur with them
    protected HashMap<String, ArrayList<String>> targetInfo; // hashmap of target words corresponding to an arraylist with
    // the weighting in index 0 and the similarity measure in index 1
    protected HashMap<String, Double> sentenceFrequencies;
    protected HashSet<String> stopList; // hashset containing words in the stopList
    protected HashSet<String> uniqueSet; // hashset containing all unique words in sentences file
    protected HashMap<String, ArrayList<Double>> occVecMap; // hashmap storing all the occurrence vectors
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
    public Similarity (String stopListFile, String sentences, String inputFile) throws IOException {
        termFrequencies = new HashMap<>();
        sentenceFrequencies = new HashMap<>();
        targetInfo = new HashMap<>();
        occVecMap = new HashMap<>();
        stopList = new HashSet<>();
        uniqueSet = new HashSet<>();
        uniqueList = new ArrayList<>();
        wordCount = 0;
        numSentences = 0.0;

        File file = new File(stopListFile);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st = br.readLine();

        while (st != null){
            String stopWord = st.toLowerCase();
            stopList.add(stopWord);

            st = br.readLine();
        }

        file = new File(inputFile);
        br = new BufferedReader(new FileReader(file));
        st = br.readLine();

        while (st != null){
            String stLower = st.toLowerCase();

            String[] words = stLower.split("\\s+");
            if (!termFrequencies.containsKey(words[0])) {
                termFrequencies.put(words[0], new HashMap<>());
            }

            if (!targetInfo.containsKey(words[0])) {
                ArrayList<String> infoList = new ArrayList<>();
                infoList.add(words[1]);
                infoList.add(words[2]);
                targetInfo.put(words[0], infoList);
            }

            st = br.readLine();
        }

        file = new File(sentences);
        br = new BufferedReader(new FileReader(file));
        st = br.readLine();
        ArrayList<String> currentWords;

        while (st != null){
            currentWords = new ArrayList<>();
            String stLower = st.toLowerCase();

            String[] words = stLower.split("\\s+");
            for (String word : words) {
                if (!isAlpha(word)) {
                    continue;
                }

                if (stopList.contains(word)) {
                    continue;
                }

                if (!sentenceFrequencies.containsKey(word)) {
                    sentenceFrequencies.put(word, 0.0);
                }
                currentWords.add(word);

                if (termFrequencies.containsKey(word)) {
                    for (String w : words) {
                        if (!isAlpha(w)) {
                            continue;
                        }

                        if (stopList != null && stopList.contains(w)) {
                            continue;
                        }

                        if (!termFrequencies.containsKey(w)) {
                            termFrequencies.put(w, new HashMap<>());
                            for (String w2 : words) {
                                if (!isAlpha(w2)) {
                                    continue;
                                }

                                if (stopList != null && stopList.contains(w2)) {
                                    continue;
                                }

                                if (!termFrequencies.get(w).containsKey(w2)) {
                                    termFrequencies.get(w).put(w2, 1.0);
                                } else {
                                    termFrequencies.get(w).put(w2, termFrequencies.get(w).get(w2) + 1);
                                }
                            }
                        }

                        if (!termFrequencies.get(word).containsKey(w)) {
                            termFrequencies.get(word).put(w, 1.0);
                        } else {
                            termFrequencies.get(word).put(w, termFrequencies.get(word).get(w) + 1);
                        }


                    }
                }

                wordCount++;

                if (!uniqueSet.contains(word)) {
                    // adds word if not already in set
                    uniqueSet.add(word);
                    uniqueList.add(word);
                }
            }

            numSentences++;

            for (String currentWord : currentWords) {
                sentenceFrequencies.put(currentWord, sentenceFrequencies.get(currentWord) + 1.0);
            }

            st = br.readLine();
        }

        idfVector = new ArrayList<>(uniqueList.size());
        for (int i = 0; i < uniqueList.size(); i++) {
            Double logCalc = Math.log(numSentences/sentenceFrequencies.get(uniqueList.get(i)));
            idfVector.add(i, logCalc);
        }

//        if (weighting.equals("IDF")) {
//            ArrayList<Double> TFIDF = new ArrayList<>(uniqueList.size());
//
//            for (int j = 0; j < uniqueList.size(); j++) {
//                TFIDF.add(j, occVec.get(j) * idfVector.get(j));
//            }
//
//            System.out.println(TFIDF);
//        }

        //System.out.println(idfVector);
        //System.out.println(termFrequencies);
        //System.out.println(sentenceFrequencies);

        System.out.println(uniqueSet.size());
        System.out.println(wordCount);
        System.out.println(numSentences);

        runStats();

    }

    public void runStats() {

        // normalizes all vectors
        for (String word : uniqueList) {
            occVecMap.put(word, getOccVec(word));
        }

        for (String targetWord : targetInfo.keySet()) {
            ArrayList<String> infoList = targetInfo.get(targetWord);
            String weighting = infoList.get(0);
            String simMeasure = infoList.get(1);
            
            runSims(targetWord, weighting, simMeasure);
        }

    }

    public ArrayList<Double> getOccVec(String word) {
        ArrayList<Double> occVec = new ArrayList<>(uniqueList.size());

        // initialize variable to store the term frequency
        double wCount;

        for (int i = 0; i < uniqueList.size(); i++) {
            if (termFrequencies.containsKey(word) && termFrequencies.get(word).get(uniqueList.get(i)) != null) {
                wCount = termFrequencies.get(word).get(uniqueList.get(i));
            } else {
                wCount = 0;
            }

            occVec.add(i, wCount);
        }
        //uniqueList.indexOf(word);

        return occVec;
    }

    public void runSims(String targetWord, String weighting, String simMeasure) {
        ArrayList<String> wordsList = new ArrayList<>(termFrequencies.get(targetWord).keySet().size());
        wordsList.addAll(termFrequencies.get(targetWord).keySet());

        ArrayList<Double> distanceList = new ArrayList<>(wordsList.size());
        ArrayList<Double> vec1 = occVecMap.get(targetWord);
        ArrayList<Double> normVec1 = normVec(vec1);
        ArrayList<Double> vec2 = new ArrayList<>();
        ArrayList<Double> normVec2 = new ArrayList<>();

        if (simMeasure.equals("l1")) {
            for (String word : wordsList){
                vec2 = occVecMap.get(word);
                normVec2 = normVec(vec2);
                distanceList.add(wordsList.indexOf(word), getL1Distance(normVec1, normVec2));
            }
        }

        if (simMeasure.equals("euclidean")) {
            for (String word : wordsList){
                vec2 = occVecMap.get(word);
                normVec2 = normVec(vec2);
                distanceList.add(wordsList.indexOf(word), getEuclideanDistance(normVec1, normVec2));
            }
        }

        if (simMeasure.equals("cosine")) {
            for (String word : wordsList){
                vec2 = occVecMap.get(word);
                normVec2 = normVec(vec2);
                distanceList.add(wordsList.indexOf(word), getCosineDistance(normVec1, normVec2));
            }
        }

        modQuickSort(distanceList, 0, distanceList.size() - 1, wordsList);
        for (int i = distanceList.size() - 1; i > distanceList.size() - 11; i--){
            System.out.println(wordsList.get(i) + ": " + distanceList.get(i));
        }
    }

    public double getL1Distance(ArrayList<Double> vector1, ArrayList<Double> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vectors must be of equal size");
        }

        double sum = 0;
        for (int i = 0; i < vector1.size(); i++) {
//            System.out.println(vector1.get(i));
//            System.out.println(vector2.get(i));
            double diff = vector1.get(i) - vector2.get(i);
            sum += Math.abs(diff);
        }

        return sum;
    }

    public double getEuclideanDistance(ArrayList<Double> vector1, ArrayList<Double> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vectors must be of equal size");
        }

        double sum = 0;
        for (int i = 0; i < vector1.size(); i++) {
            double diff = vector1.get(i) - vector2.get(i);
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    public double getCosineDistance(ArrayList<Double> vector1, ArrayList<Double> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vectors must be of the same length.");
        }
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }
        if (norm1 == 0.0 || norm2 == 0.0) {
            throw new IllegalArgumentException("One or both vectors are zero vectors.");
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public double getL2Length(ArrayList<Double> vec) {
        double sumOfSquares = 0.0;
        for (Double d : vec) {
            sumOfSquares += d * d;
        }
        return Math.sqrt(sumOfSquares);
    }

    public ArrayList<Double> normVec (ArrayList<Double> vec){
        double l2Length = getL2Length(vec);

        if (l2Length == 0) {
            return vec;
        }

        ArrayList<Double> normVec = new ArrayList<Double>(vec.size());
        for (int i = 0; i < vec.size(); i++){
            normVec.add(i, vec.get(i)/l2Length);
        }
        return normVec;
    }

    public static void modQuickSort(ArrayList<Double> arr, int low, int high, ArrayList<String> wordArr) {
        if (low < high) {
            int pivotIndex = partition(arr, low, high, wordArr);
            modQuickSort(arr, low, pivotIndex - 1, wordArr);
            modQuickSort(arr, pivotIndex + 1, high, wordArr);
        }
    }

    public static int partition(ArrayList<Double> arr, int low, int high, ArrayList<String> wordArr) {
        double pivot = arr.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr.get(j) <= pivot) {
                i++;
                double temp = arr.get(i);
                arr.set(i, arr.get(j));
                arr.set(j, temp);

                String temp2 = wordArr.get(i);
                wordArr.set(i, wordArr.get(j));
                wordArr.set(j, temp2);
            }
        }
        double temp = arr.get(i + 1);
        arr.set(i + 1, arr.get(high));
        arr.set(high, temp);

        String temp2 = wordArr.get(i + 1);
        wordArr.set(i + 1, wordArr.get(high));
        wordArr.set(high, temp2);

        return i + 1;
    }

    public boolean isAlpha(String word) {
        return word.matches("[a-zA-Z]+");
    }

    public static void main(String[] args) throws IOException {
        String stopListFile = "/Users/ezraford/Desktop/School/CS 159/NLP-Word-Similarity/data/stoplist";
        String sentences = "/Users/ezraford/Desktop/School/CS 159/NLP-Word-Similarity/data/sentences2";
        String inputFile = "/Users/ezraford/Desktop/School/CS 159/NLP-Word-Similarity/data/test";

        String stopListFile = "/Users/talmordoch/Desktop/NLP/assignment 5/data/stoplist";
        String sentences = "/Users/talmordoch/Desktop/NLP/assignment 5/data/sentences";
        String inputFile = "/Users/talmordoch/Desktop/NLP/assignment 5/data/test";

        Similarity simRun = new Similarity(stopListFile, sentences, inputFile);
    }
}
