import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CollectionUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextPreProcessor {
    public static int corpus_word_total = 0;

    public static String clean_text(String text) {
        text = text.replaceAll("\n", " ")
                .replaceAll("\r", " ")
                .replaceAll("-"," ")
                .replaceAll(" +", " ")
                .replaceAll("%","")
                .replaceAll("\"","")
                .replaceAll("'","")
                .replaceAll("/","")
                .replaceAll("”","")
                .replaceAll("“","")
                .replaceAll("’","")
                .replaceAll(":","")
                .replaceAll(",","")
                .replaceAll("–","")
                .replaceAll("-","")
                .replaceAll(" +"," ")
                .replaceAll("\\(","")
                .replaceAll("\\)","")
                .replaceAll("\\[","")
                .replaceAll("\\$","")
                .replaceAll("]","")
                .replaceAll("\\d","");

        return text;
    }

    public static String remove_stopwords(String text) {
        List<String> stopwords = new ArrayList<>();
        try {
            stopwords = Files.readAllLines(Paths.get("stopwords"));
        } catch (IOException e){
            System.out.println("Can't find stopwords file: "+e.getMessage());
        }

        ArrayList<String> allWords =
                Stream.of(text.split(" "))
                        .collect(Collectors.toCollection(ArrayList<String>::new));

        ArrayList<String> temp = new ArrayList<>();
        for (String allWord : allWords) {
            if (!stopwords.contains(allWord) && !stopwords.contains(allWord.toLowerCase()))
                temp.add(allWord);
        }
        text = String.join(" ", temp);
        return text;
    }

    private List<String> get_domain_specific_stopwords(){
        List<String> domain_stopwords = new ArrayList<>();
            try {
                domain_stopwords = Files.readAllLines(Paths.get("domain_stopwords"));
            } catch (IOException e){
                System.out.println("Can't find domain stopwords file: "+e.getMessage());
            }
        return domain_stopwords;
    }

    public static String get_filename(String file_location){
        Path path;
        path = Paths.get(file_location);
        return TextPreProcessor.create_filename(path);
    }

    public ArrayList<String> get_filenames(List<String> file_locations){
        ArrayList<String> filenames = new ArrayList<>();
        for (String location: file_locations) {
            filenames.add(get_filename(location));
        }
        return filenames;
    }

    public String get_corpus_as_str(LinkedHashMap<String, String> corpus_data){
        StringBuilder cleaned_full_text = new StringBuilder();
        for (Map.Entry<String, String> file : corpus_data.entrySet())
            cleaned_full_text.append(file.getValue()).append(" ");

        String corpus_string = cleaned_full_text.toString();
        corpus_word_total = corpus_string.split(" ").length;

        return corpus_string;
    }

    public LinkedHashMap<String, String> get_file_data(String file_location, String filename){
        Path path;
        String text;

        ArrayList<String> file_data = new ArrayList<>();
        try {
            path = Paths.get(file_location);
            text = Files.readString(path);
            text = TextPreProcessor.remove_stopwords(text);
            text = TextPreProcessor.clean_text(text);
            file_data.add(text);
        } catch (IOException e){
            System.out.println(e.getMessage());
        }

        LinkedHashMap<String, String> corpus_data = new LinkedHashMap<>();
        corpus_data.put(filename,file_data.get(0));

        return corpus_data;
    }

    public LinkedHashMap<String, String> get_corpus_data(List<String> file_locations, ArrayList<String> filenames){
        Path path;
        String text;

        ArrayList<String> file_data = new ArrayList<>();

        for (String location: file_locations)
            try {
                path = Paths.get(location);
                text = Files.readString(path);

                text = TextPreProcessor.remove_stopwords(text);
                text = TextPreProcessor.clean_text(text);

                file_data.add(text);
            } catch (IOException e){
                System.out.println(e.getMessage());
            }

        LinkedHashMap<String, String> corpus_data = new LinkedHashMap<>();
        for (int i = 0; i < filenames.size(); i++)
            corpus_data.put(filenames.get(i),file_data.get(i));

        return corpus_data;
    }

    public static String create_filename(Path path){
        String filename, dir;
        dir = path.getParent().getFileName().toString();
        filename = path.getFileName().toString();
        filename = dir+"__"+filename;
        return filename.substring(0, filename.lastIndexOf('.'));
    }

    public CoreDocument get_pipeline_results(String full_text_str){
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        // set up pipeline
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // make an example document
        CoreDocument doc = new CoreDocument(full_text_str);
        // annotate the document
        pipeline.annotate(doc);

        return doc;
    }

    public ArrayList<String> get_entities_list(CoreDocument doc){
        ArrayList<String> entities = new ArrayList<>();
        for (CoreEntityMention em : doc.entityMentions()) {
            entities.add(em.text());
        }
        return entities;
    }

    public ArrayList<String> remove_sub_strings(ArrayList<String> list){
        list.sort((a, b) -> -Integer.compare(a.length(), b.length()));
        ArrayList<String> list_substrings_removed = new ArrayList<>();
        for (int i = list.size()-1; i >= 0; i--) {
            boolean is_sub_string = false;
            for (int j = i-1; j >= 0; j--)
                if (list.get(j).contains(list.get(i))) {
                    is_sub_string = true;
                    break;
                }
            if (!is_sub_string)
                list_substrings_removed.add(list.get(i));
        }
        return list_substrings_removed;
    }

    public ArrayList<String> clean_add_underscores(ArrayList<String> entities){
        ArrayList<String> entities_substrings_removed_cleaned = new ArrayList<>();
        for (String entity: entities) {
            entity = entity.replaceAll(" +"," ").replaceAll(" ", "_");
            entities_substrings_removed_cleaned.add(entity);
        }
        return entities_substrings_removed_cleaned;
    }

    public String get_list_as_string(ArrayList<String> list){
        StringBuilder full_str = new StringBuilder();
        for (String lem: list) {
            full_str.append(lem);
            full_str.append(" ");
        }
        return full_str.toString();
    }

    public ArrayList<String> cross_ref_lists(ArrayList<String> list_one, ArrayList<String> list_two){
        ArrayList<String> final_list = new ArrayList<>();
        for (String item1: list_one){
            boolean appears = false;
            for (String item2: list_two){
                String[] temp = item2.toLowerCase().split("_");
                if (Arrays.asList(temp).contains(item1.toLowerCase()))
                    appears = true;
            }
            if (!appears)
                final_list.add(item1);
        }
        return final_list;
    }

    public ArrayList<String> check_for_dupes_and_add(ArrayList<String> orig_list, ArrayList<String> list_to_add){
        for (String new_item: list_to_add)
            if (!orig_list.contains(new_item.toLowerCase()))
                orig_list.add(new_item.toLowerCase());
        return orig_list;
    }

    public ArrayList<String> refine_final_terms_list(ArrayList<String> curr_terms, LinkedHashMap<String, String> corpus_data, int min_appearances){
        DocTermMatrixCreator doc_term_matrix_creator = new DocTermMatrixCreator();
        List<String> domain_stopwords = get_domain_specific_stopwords();
        int[] appearances = doc_term_matrix_creator.get_term_frequencies_across_corpus(curr_terms, corpus_data);
        ArrayList<String> final_terms_list = new ArrayList<>();
        for (int i = 0; i < curr_terms.size(); i++) {
            String term = curr_terms.get(i);
            if (appearances[i] >= min_appearances)
                if (!domain_stopwords.contains(term))
                    final_terms_list.add(term);
        }
        return final_terms_list;
    }

    public ArrayList<String> get_lemmas_list(CoreDocument doc){
        ArrayList<String> lemmas = new ArrayList<>();
        for (CoreSentence sent : doc.sentences())
            lemmas.addAll(sent.lemmas());
        return lemmas;
    }

    public static ArrayList<String> get_n_grams_map(ArrayList<String> words, String full_text){
        int MIN_N_GRAM = 2;
        int MAX_N_GRAM = 3;
        double MIN_N_GRAM_FREQ = 0.02;

        List<List<String>> n_grams = CollectionUtils.getNGrams(words, MIN_N_GRAM, MAX_N_GRAM);
        ArrayList<String> grams_to_use = new ArrayList<>();;
        double str_freq;
        for (List<String> gram : n_grams){
            String str_to_check = String.join(" ", gram);

            // Call method to count frequency of gram in file, returning value.
            str_freq = TextPreProcessor.get_gram_freq(str_to_check, full_text);
            str_to_check = str_to_check.trim().replaceAll(" ","_").toLowerCase();

            if (str_freq >= MIN_N_GRAM_FREQ & !grams_to_use.contains(str_to_check)) {
                grams_to_use.add(str_to_check);
            }
        }

        return grams_to_use;
    }

    public static Double get_gram_freq(String str_to_check, String full_text) {
        int full_text_len = full_text.length();
        int num_occurences = (full_text_len - full_text
                    .replace(str_to_check, "")
                    .length()) / str_to_check.length();
        return 100*((double) num_occurences / corpus_word_total);
    }
}
