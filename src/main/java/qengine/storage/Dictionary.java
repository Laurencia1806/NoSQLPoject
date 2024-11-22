package qengine.storage;

import java.util.HashMap;
import java.util.Map;

public class Dictionary {
	
    private final Map<String, Integer> termToId;
    private final Map<Integer, String> idToTerm;
    public static int currentId = 0;

    public Dictionary() {
        this.termToId = new HashMap<>();
        this.idToTerm = new HashMap<>();
    }

    public synchronized int addTerm(String term) {
        if (!termToId.containsKey(term)) {
            termToId.put(term, currentId);
            idToTerm.put(currentId, term);
            currentId++;
        }
        return termToId.get(term);
    }

    // Récupère l'identifiant associé à un terme
    public Integer getId(String term) {
        return termToId.get(term);
    }


    // Récupère le terme associé à un identifiant
    public String getTerm(int id) {
        return idToTerm.get(id);
    }

    public Map<String, Integer> getDictionarySI() {
        return new HashMap<>(termToId);
    }

 
    public Map<Integer, String> getDictionaryIS() {
        return new HashMap<>(idToTerm);
    }


    // Retourne la taille du dictionnaire 
    public int size() {
        return termToId.size();
    }
}
