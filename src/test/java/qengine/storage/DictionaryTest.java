package qengine.storage;

import qengine.storage.Dictionary;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DictionaryTest {



	    @Test
	    public void testGetTermById() {
	        Dictionary dictionary = new Dictionary();

	      
	        int id1 = dictionary.addTerm("term1");
	        int id2 = dictionary.addTerm("term2");

	        String term1 = dictionary.getTerm(id1);
	        String term2 = dictionary.getTerm(id2);
	        assertEquals("term1", term1, "Le terme associ� � l'ID 0 devrait �tre 'term1'.");
	        assertEquals("term2", term2, "Le terme associ� � l'ID 1 devrait �tre 'term2'.");
	    }

	    @Test
	    public void testGetIdByTerm() {
	        Dictionary dictionary = new Dictionary();

	       
	        dictionary.addTerm("term1");
	        dictionary.addTerm("term2");

	        Integer id1 = dictionary.getId("term1");
	        Integer id2 = dictionary.getId("term2");
	        assertNotNull(id1, "L'ID pour 'term1' ne  peut pas �tre null.");
	        assertNotNull(id2, "L'ID pour 'term2' ne peut pas �tre null.");
	       
	    }

	    @Test
	    public void testDictionariesConsistency() {
	        Dictionary dictionary = new Dictionary();

	       
	        dictionary.addTerm("term1");
	        dictionary.addTerm("term2");
	        Map<String, Integer> termToId = dictionary.getDictionarySI();
	        Map<Integer, String> idToTerm = dictionary.getDictionaryIS();

	        assertEquals(termToId.size(), idToTerm.size(), "Les deux dictionnaires doivent avoir la m�me taille.");
	        for (Map.Entry<String, Integer> entry : termToId.entrySet()) {
	            String term = entry.getKey();
	            Integer id = entry.getValue();
	            assertEquals(term, idToTerm.get(id), "Le terme et l'ID doivent �tre identiques les deux dictionnaires.");
	        }
	    }

	    @Test
	    public void testDictionarySize() {
	        Dictionary dictionary = new Dictionary();
	        assertEquals(0, dictionary.size(), "La taille initiale du dictionnaire devrait �tre 0.");
	        dictionary.addTerm("term1");
	        assertEquals(1, dictionary.size(), "La taille du dictionnaire devrait �tre 1 apr�s l'ajout d'un terme.");
	        dictionary.addTerm("term2");
	        assertEquals(2, dictionary.size(), "La taille du dictionnaire devrait �tre 2 apr�s l'ajout d'un deuxi�me terme.");
	        dictionary.addTerm("term1");
	        assertEquals(2, dictionary.size(), "La taille du dictionnaire ne devrait pas changer apr�s l'ajout d'un terme existant.");
	    }
}
