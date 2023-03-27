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
    public Similarity (String stopListFile, String sentences, String inputFile) throws IOException {
        termFrequencies = new HashMap<>();
        sentenceFrequencies = new HashMap<>();
        targetInfo = new HashMap<>();
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
            String[] words = st.split("\\s+");

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
            for (int j = 0; j < words.length; j++) {
                if (!isAlpha(words[j])) { continue; }

                if (stopList.contains(words[j])) { continue; }


                if (!sentenceFrequencies.containsKey(words[j])) {
                    sentenceFrequencies.put(words[j], 0.0);
                }
                currentWords.add(words[j]);

                if (!termFrequencies.containsKey(words[j])) {
                    termFrequencies.put(words[j], new HashMap<>());
                }

                for (int k = 0; k < words.length; k++) {
                    if (!isAlpha(words[k])) { continue; }
                    if (stopList != null && stopList.contains(words[k])) { continue; }

                    //don't count itself as a word it occurs with
                    if (j == k) { continue; }

                    if (!termFrequencies.get(words[j]).containsKey(words[k])) {
                        termFrequencies.get(words[j]).put(words[k], 1.0);
                    } else {
                        termFrequencies.get(words[j]).put(words[k], termFrequencies.get(words[j]).get(words[k]) + 1);
                    }
                }

                wordCount++;

                if (!uniqueSet.contains(words[j])) {
                    // adds word if not already in set
                    uniqueSet.add(words[j]);
                    uniqueList.add(words[j]);
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

        //System.out.println(idfVector);
        System.out.println(termFrequencies);
        //System.out.println(sentenceFrequencies);

        System.out.println(uniqueSet.size());
        System.out.println(wordCount);
        System.out.println(numSentences);

        runStats();
    }

    public void runStats() {
        for (String targetWord : targetInfo.keySet()) {
            ArrayList<String> infoList = targetInfo.get(targetWord);
            String weighting = infoList.get(0);
            String simMeasure = infoList.get(1);

            System.out.println("\nSIM: " + targetWord + " " + targetInfo.get(targetWord).get(0)
                    + " " + targetInfo.get(targetWord).get(1));
            runSims(targetWord, weighting, simMeasure);
        }
    }

    public ArrayList<Double> getOccVec(String word) {
        ArrayList<Double> occVec = new ArrayList<>(uniqueList.size());

        // initialize variable to store the term frequency
        double wCount;

        for (int i = 0; i < uniqueList.size(); i++) {
            if (termFrequencies.get(word).get(uniqueList.get(i)) != null) {
                wCount = termFrequencies.get(word).get(uniqueList.get(i));
            } else {
                wCount = 0;
            }

            occVec.add(i, wCount);
        }

        return occVec;
    }

    public void runSims(String targetWord, String weighting, String simMeasure) {
        ArrayList<String> wordsList = new ArrayList<>(uniqueList.size());
        wordsList.addAll(uniqueList);

        ArrayList<Double> distanceList = new ArrayList<>(wordsList.size());
        ArrayList<Double> vec1 = getOccVec(targetWord);
        ArrayList<Double> normVec1;
        System.out.println(uniqueList);
        System.out.println(vec1);
        ArrayList<Double> vec2 = new ArrayList<>();
        ArrayList<Double> normVec2 = new ArrayList<>();

        if (weighting.equals("IDF")) {
            for (int j = 0; j < uniqueList.size(); j++) {
                double current = vec1.get(j);
                vec1.set(j, current * idfVector.get(j));
            }
        }

        normVec1 = normVec(vec1);

        if (simMeasure.equals("L1")) {
            for (String word : wordsList){
                vec2 = getOccVec(word);
                normVec2 = normVec(vec2);
                distanceList.add(wordsList.indexOf(word), getL1Distance(normVec1, normVec2));
            }
        }

        if (simMeasure.equals("EUCLIDEAN")) {
            for (String word : wordsList){
                vec2 = getOccVec(word);
                normVec2 = normVec(vec2);
                distanceList.add(wordsList.indexOf(word), getEuclideanDistance(normVec1, normVec2));
            }
        }

        if (simMeasure.equals("COSINE")) {
            for (String word : wordsList){
                vec2 = getOccVec(word);
                normVec2 = normVec(vec2);
                distanceList.add(wordsList.indexOf(word), getCosineDistance(normVec1, normVec2));
            }
        }

        modQuickSort(distanceList, 0, distanceList.size() - 1, wordsList);

        if (distanceList.size() < 10) {
            Collections.reverse(distanceList);
            Collections.reverse(wordsList);
            for (int k = 0; k < distanceList.size(); k++){
                System.out.println(wordsList.get(k) + "\t" + distanceList.get(k));
            }
        } else {
            for (int i = distanceList.size() - 1; i > distanceList.size() - 11; i--){
                System.out.println(wordsList.get(i) + "\t" + distanceList.get(i));
            }
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
        String sentences = "/Users/ezraford/Desktop/School/CS 159/NLP-Word-Similarity/data/5asentences";
        String inputFile = "/Users/ezraford/Desktop/School/CS 159/NLP-Word-Similarity/data/5atest";

//        String stopListFile = "/Users/talmordoch/Library/Mobile Documents/com~apple~CloudDocs/NLP-Word-Similarity/data/stoplist";
//        String sentences = "/Users/talmordoch/Library/Mobile Documents/com~apple~CloudDocs/NLP-Word-Similarity/data/sentences";
//        String inputFile = "/Users/talmordoch/Library/Mobile Documents/com~apple~CloudDocs/NLP-Word-Similarity/data/test";

        Similarity simRun = new Similarity(stopListFile, sentences, inputFile);
    }
}
