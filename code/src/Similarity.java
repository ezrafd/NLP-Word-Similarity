import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Similarity {
    protected HashMap<String, HashMap<String, Double>> termFrequencies; // hashmap of target words and the count of words that occur with them
    protected HashMap<String, Double> sentenceFrequencies;
    protected HashSet<String> stopList; // hashset containing words in the stopList
    protected HashSet<String> uniqueSet; // hashset containing all unique words in sentences file
    protected HashMap<String, ArrayList<Double>> occVecMap; // hashmap storing all the occurrence
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
        for (int i = 0; i < uniqueList.size() - 1; i++) {
            Double logCalc = Math.log(numSentences/sentenceFrequencies.get(uniqueList.get(i)));
            idfVector.add(i, logCalc);
        }

//        if (weighting.equals("IDF")) {
//            ArrayList<Double> TFIDF = new ArrayList<>(uniqueList.size());
//
//            for (int j = 0; j < uniqueList.size()-1; j++) {
//                TFIDF.add(j, occVec.get(j) * idfVector.get(j));
//            }
//
//            System.out.println(TFIDF);
//        }

        System.out.println(idfVector);
        //System.out.println(termFrequencies);
        //System.out.println(sentenceFrequencies);

        System.out.println(uniqueSet.size());
        System.out.println(wordCount);
        System.out.println(numSentences);

    }

    public void runStats(String weighting, String simMeasure) {
        for (String word : uniqueList) {
            occVecMap.put(word, getOccVec(word));
        }

        if (simMeasure.equals("L1")) {

        }
    }

    public Double euclideanLength(ArrayList<Double> vector1, ArrayList<Double> vector2) {
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

    public boolean isAlpha(String word) {
        return word.matches("[a-zA-Z]+");
    }

    public ArrayList<Double> getOccVec(String word) {
        ArrayList<Double> occVec = new ArrayList<>(uniqueList.size());

        for (String w : termFrequencies.get(word).keySet()) {
            int index = uniqueList.indexOf(w);
            Double wCount = termFrequencies.get(word).get(w);

            occVec.add(index, wCount);
        }
        //uniqueList.indexOf(word);

        return occVec;
    }

    public static void main(String[] args) throws IOException {
        String stopListFile = "/Users/ezraford/Desktop/School/CS 159/NLP-Word-Similarity/data/stoplist";
        String sentences = "/Users/ezraford/Desktop/School/CS 159/NLP-Word-Similarity/data/sentences";
        String inputFile = "/Users/ezraford/Desktop/School/CS 159/NLP-Word-Similarity/data/test";
        Similarity simRun = new Similarity(stopListFile, sentences, inputFile);
    }
}
