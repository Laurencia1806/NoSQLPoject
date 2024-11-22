package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFAtom;
import qengine.model.StarQuery;
import qengine.storage.RDFHexaStore;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe {@link RDFHexaStore}.
 */
public class RDFHexaStoreTest {
    private static final Literal<String> SUBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("subject1");
    private static final Literal<String> PREDICATE_1 = SameObjectTermFactory.instance().createOrGetLiteral("predicate1");
    private static final Literal<String> OBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("object1");
    private static final Literal<String> SUBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("subject2");
    private static final Literal<String> PREDICATE_2 = SameObjectTermFactory.instance().createOrGetLiteral("predicate2");
    private static final Literal<String> OBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("object2");
    private static final Literal<String> OBJECT_3 = SameObjectTermFactory.instance().createOrGetLiteral("object3");
    private static final Variable VAR_X = SameObjectTermFactory.instance().createOrGetVariable("?x");
    private static final Variable VAR_Y = SameObjectTermFactory.instance().createOrGetVariable("?y");
    private static final Variable VAR_Z = SameObjectTermFactory.instance().createOrGetVariable("?z");



    @Test
    public void testAddAllRDFAtoms() {
        RDFHexaStore store = new RDFHexaStore();

        // Ajouter plusieurs RDFAtom
        RDFAtom rdfAtom1 = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFAtom rdfAtom2 = new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2);

        Set<RDFAtom> rdfAtoms = Set.of(rdfAtom1, rdfAtom2);

        assertTrue(store.addAll(rdfAtoms.stream()), "Les RDFAtoms devraient Ãªtre ajoutÃ©s avec succÃ¨s.");

        // VÃ©rifier que tous les atomes sont prÃ©sents
        Collection<Atom> atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajoutÃ©.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajoutÃ©.");

        store = new RDFHexaStore();
        assertTrue(store.addAll(rdfAtoms), "Les RDFAtoms devraient Ãªtre ajoutÃ©s avec succÃ¨s.");

        // VÃ©rifier que tous les atomes sont prÃ©sents
        atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajoutÃ©.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajoutÃ©.");
    }

    @Test
    public void testAddRDFAtom() {
        RDFHexaStore store = new RDFHexaStore();

        // CrÃ©er un RDFAtom
        RDFAtom rdfAtom = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);

        // Ajouter l'atome au store
        boolean added = store.add(rdfAtom);

        // VÃ©rifier que l'ajout a rÃ©ussi
        assertTrue(added, "L'ajout du RDFAtom devrait rÃ©ussir.");

        Collection<Atom> atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom), "Le store devrait contenir le RDFAtom ajoutÃ©.");

        // VÃ©rifier que les index ont Ã©tÃ© mis Ã  jour correctement
        int subjectId = store.dictionary.getId(SUBJECT_1.toString());
        int predicateId = store.dictionary.getId(PREDICATE_1.toString());
        int objectId = store.dictionary.getId(OBJECT_1.toString());

        assertTrue(store.getIndexSPO().getOrDefault(subjectId, Collections.emptyMap())
                .getOrDefault(predicateId, Collections.emptySet())
                .contains(objectId), "L'index SPO devrait contenir l'atome.");

        assertTrue(store.getIndexPOS().getOrDefault(predicateId, Collections.emptyMap())
                .getOrDefault(objectId, Collections.emptySet())
                .contains(subjectId), "L'index POS devrait contenir l'atome.");

        assertTrue(store.getIndexOSP().getOrDefault(objectId, Collections.emptyMap())
                .getOrDefault(subjectId, Collections.emptySet())
                .contains(predicateId), "L'index OSP devrait contenir l'atome.");
    }

	@Test
	public void testAddDuplicateAtom() {
	    RDFHexaStore store = new RDFHexaStore();
	    RDFAtom rdfAtom = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);
	    store.add(rdfAtom);
	    store.add(rdfAtom);

	    Collection<Atom> atoms = store.getAtoms();
	    assertEquals(1, atoms.size(), "Le store ne devrait contenir qu'un seul RDFAtom.");
	    assertTrue(atoms.contains(rdfAtom), "Le store devrait contenir le RDFAtom ajoutÃ©.");
	}

	@Test
	public void testSize() {
	    RDFHexaStore store = new RDFHexaStore();
	    assertEquals(0, store.size(), "La taille initiale du store devrait Ãªtre 0.");
	    store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1));
	    assertEquals(1, store.size(), "La taille du store devrait Ãªtre 1 aprÃ¨s l'ajout d'un atome.");
	    store.add(new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2));
	    assertEquals(2, store.size(), "La taille du store devrait Ãªtre 2 aprÃ¨s l'ajout d'un second atome.");
	    store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1));
	    assertEquals(2, store.size(), "La taille du store ne devrait pas augmenter en ajoutant un atome en double.");
	}


	@Test
	public void testMatchAtom() {
	    RDFHexaStore store = new RDFHexaStore();
	    
	    store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1)); // RDFAtom(subject1, predicate1, object1)
	    store.add(new RDFAtom(SUBJECT_2, PREDICATE_1, OBJECT_2)); // RDFAtom(subject2, predicate1, object2)
	    store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_3)); // RDFAtom(subject1, predicate1, object3)

	    RDFAtom matchingAtom1 = new RDFAtom(SUBJECT_1, PREDICATE_1, VAR_X); 
	    RDFAtom matchingAtom2 = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1); 
	    RDFAtom matchingAtom3 = new RDFAtom(SUBJECT_1, VAR_X, OBJECT_1);
	    RDFAtom matchingAtom4 = new RDFAtom(SUBJECT_1, VAR_X, VAR_Y);
	    RDFAtom matchingAtom5 = new RDFAtom(VAR_X, PREDICATE_1, OBJECT_1);   
	    RDFAtom matchingAtom6 = new RDFAtom(VAR_X, VAR_Y, OBJECT_1);
	    RDFAtom matchingAtom7 = new RDFAtom(VAR_X, PREDICATE_1, VAR_Y);        

	    
	    // Cas 1: (?x = object1)
	    Iterator<Substitution> matchedAtoms1 = store.match(matchingAtom1);
	    List<Substitution> matchedList1 = new ArrayList<>();
	    matchedAtoms1.forEachRemaining(matchedList1::add);

	    Substitution firstResult = new SubstitutionImpl();
	    firstResult.add(VAR_X, OBJECT_1);
	    assertTrue(matchedList1.contains(firstResult), "Missing substitution: " + firstResult);

	    // Cas 2: (subject1, predicate1, object1) existe
	    Iterator<Substitution> matchedAtoms2 = store.match(matchingAtom2);
	    List<Substitution> matchedList2 = new ArrayList<>();
	    matchedAtoms2.forEachRemaining(matchedList2::add);

	    Substitution secondResult = new SubstitutionImpl();
	    assertTrue(matchedList2.contains(secondResult), "Missing substitution: " + secondResult);

	    // Cas 3: (?x = predicate1)
	    Iterator<Substitution> matchedAtoms3 = store.match(matchingAtom3);
	    List<Substitution> matchedList3 = new ArrayList<>();
	    matchedAtoms3.forEachRemaining(matchedList3::add);

	    Substitution thirdResult = new SubstitutionImpl();
	    thirdResult.add(VAR_X, PREDICATE_1);
	    assertTrue(matchedList3.contains(thirdResult), "Missing substitution: " + thirdResult);

	    // Cas 4: (?x = predicate1, ?y = object1)
	    Iterator<Substitution> matchedAtoms4 = store.match(matchingAtom4);
	    List<Substitution> matchedList4 = new ArrayList<>();
	    matchedAtoms4.forEachRemaining(matchedList4::add);

	    Substitution fourthResult = new SubstitutionImpl();
	    fourthResult.add(VAR_X, PREDICATE_1);
	    fourthResult.add(VAR_Y, OBJECT_1);
	    assertTrue(matchedList4.contains(fourthResult), "Missing substitution: " + fourthResult);

	    // Cas 5: (?x = subject1)
	    Iterator<Substitution> matchedAtoms5 = store.match(matchingAtom5);
	    List<Substitution> matchedList5 = new ArrayList<>();
	    matchedAtoms5.forEachRemaining(matchedList5::add);

	    Substitution fifthResult = new SubstitutionImpl();
	    fifthResult.add(VAR_X, SUBJECT_1);
	    assertTrue(matchedList5.contains(fifthResult), "Missing substitution: " + fifthResult);

	    // Cas 6: (?x = subject1, ?y = predicate1)
	    Iterator<Substitution> matchedAtoms6 = store.match(matchingAtom6);
	    List<Substitution> matchedList6 = new ArrayList<>();
	    matchedAtoms6.forEachRemaining(matchedList6::add);

	    Substitution sixthResult = new SubstitutionImpl();
	    sixthResult.add(VAR_X, SUBJECT_1);
	    sixthResult.add(VAR_Y, PREDICATE_1);
	    assertTrue(matchedList6.contains(sixthResult), "Missing substitution: " + sixthResult);

	    // Cas 7: (?x = subject1, ?y = object3)
	    Iterator<Substitution> matchedAtoms7 = store.match(matchingAtom7);
	    List<Substitution> matchedList7 = new ArrayList<>();
	    matchedAtoms7.forEachRemaining(matchedList7::add);

	    Substitution seventhResult = new SubstitutionImpl();
	    Substitution eightResult = new SubstitutionImpl();
	    seventhResult.add(VAR_X, SUBJECT_1);
	    seventhResult.add(VAR_Y, OBJECT_3);
	    eightResult.add(VAR_X, SUBJECT_1);
	    eightResult.add(VAR_Y, OBJECT_1);
	    assertTrue(matchedList7.contains(seventhResult), "Missing substitution: " + seventhResult);
	    assertTrue(matchedList7.contains(eightResult), "Missing substitution: " + eightResult);
	    
	    //Cas 8:le sujet est null
	    
	    RDFAtom matchingAtomSubjectUnknown = new RDFAtom(
	            SameObjectTermFactory.instance().createOrGetLiteral("unknown_subject"), // Sujet inconnu
	            PREDICATE_1,
	            OBJECT_1
	    );
	    Iterator<Substitution> matchedAtomsSubject = store.match(matchingAtomSubjectUnknown);
	    List<Substitution> matchedListSubject = new ArrayList<>();
	    matchedAtomsSubject.forEachRemaining(matchedListSubject::add);
	    assertTrue(matchedListSubject.isEmpty(), "Aucune correspondance trouvée pour un sujet inconnu.");

	    //Cas 9:Le prédicat est null
	    RDFAtom matchingAtomPredicateUnknown = new RDFAtom(
	            SUBJECT_1,
	            SameObjectTermFactory.instance().createOrGetLiteral("unknown_predicate"), // Prédicat inconnu
	            OBJECT_1
	    );
	    Iterator<Substitution> matchedAtomsPredicate = store.match(matchingAtomPredicateUnknown);
	    List<Substitution> matchedListPredicate = new ArrayList<>();
	    matchedAtomsPredicate.forEachRemaining(matchedListPredicate::add);
	    assertTrue(matchedListPredicate.isEmpty(), "Aucune correspondance trouvée pour un prédicat inconnu.");

	    //Cas 10: où l'objet est null
	    RDFAtom matchingAtomObjectUnknown = new RDFAtom(
	            SUBJECT_1,
	            VAR_X,
	            SameObjectTermFactory.instance().createOrGetLiteral("unknown_object") // Objet inconnu
	    );
	    Iterator<Substitution> matchedAtomsObject = store.match(matchingAtomObjectUnknown);
	    List<Substitution> matchedListObject = new ArrayList<>();
	    matchedAtomsObject.forEachRemaining(matchedListObject::add);
	    assertTrue(matchedListObject.isEmpty(), "Aucune correspondance trouvée pour un objet inconnu.");

	    //Cas 11: Sujet varivale et predicat unknow

	    RDFAtom matchingAtomPredicateUnknown2 = new RDFAtom(
	            SUBJECT_1,
	            SameObjectTermFactory.instance().createOrGetLiteral("unknown_object"),
	            VAR_X // Objet inconnu
	    );
	    Iterator<Substitution> matchedAtomsPredicate2 = store.match(matchingAtomPredicateUnknown2);
	    List<Substitution> matchedListPredicate2 = new ArrayList<>();
	    matchedAtomsPredicate2.forEachRemaining(matchedListPredicate2::add);
	    assertTrue(matchedListPredicate2.isEmpty(), "Aucune correspondance trouvée pour un objet inconnu.");

	 
	    //Cas 12: Sujet varivale et object unknow
	    RDFAtom matchingAtomObjectUnknown2 = new RDFAtom(
	            VAR_X,
	            PREDICATE_1,
	            SameObjectTermFactory.instance().createOrGetLiteral("unknown_object") // Objet inconnu
	    );
	    Iterator<Substitution> matchedAtomsObject2 = store.match(matchingAtomObjectUnknown2);
	    List<Substitution> matchedListObject2 = new ArrayList<>();
	    matchedAtomsObject2.forEachRemaining(matchedListObject2::add);
	    assertTrue(matchedListObject2.isEmpty(), "Aucune correspondance trouvée pour un objet inconnu.");

	    
	    //Cas 13: Var constnull Var
	    RDFAtom matchingAtomPredicateUnknown3 = new RDFAtom(
	            VAR_X,
	            SameObjectTermFactory.instance().createOrGetLiteral("unknown_object") ,// Objet iconnu
	            VAR_Y
	    );
	    
	    Iterator<Substitution> matchedAtomsPredicate3 = store.match(matchingAtomPredicateUnknown3);
	    List<Substitution> matchedListPredicate3 = new ArrayList<>();
	    matchedAtomsPredicate3.forEachRemaining(matchedListPredicate3::add);
	    assertTrue(matchedListPredicate3.isEmpty(), "Aucune correspondance trouvée pour un objet inconnu.");
	    

	    //Cas 13: Var var object unknow
	    RDFAtom matchingAtomObjectUnknown3 = new RDFAtom(
	            VAR_X,
	            VAR_Y,
	            SameObjectTermFactory.instance().createOrGetLiteral("unknown_object") // Objet inconnu
	    );
	    Iterator<Substitution> matchedAtomsObject3 = store.match(matchingAtomObjectUnknown3);
	    List<Substitution> matchedListObject3 = new ArrayList<>();
	    matchedAtomsObject3.forEachRemaining(matchedListObject3::add);
	    assertTrue(matchedListObject3.isEmpty(), "Aucune correspondance trouvée pour un objet inconnu.");

	    
      //Cas 14:const var var
	    
	    RDFAtom matchingAtomSubjectUnknown2 = new RDFAtom(
	            SameObjectTermFactory.instance().createOrGetLiteral("unknown_subject"), // Sujet inconnu
	            VAR_X,
	            VAR_Y
	    );
	    Iterator<Substitution> matchedAtomsSubject2 = store.match(matchingAtomSubjectUnknown2);
	    List<Substitution> matchedListSubject2 = new ArrayList<>();
	    matchedAtomsSubject2.forEachRemaining(matchedListSubject2::add);
	    assertTrue(matchedListSubject2.isEmpty(), "Aucune correspondance trouvée pour un sujet inconnu.");

	  //Cas 15
	    RDFAtom queryAtom = new RDFAtom(VAR_X, VAR_Y, VAR_Z); // (?x, ?y, ?z)

	    // Appeler la méthode match()
	    Iterator<Substitution> matchedAtoms = store.match(queryAtom);
	    List<Substitution> matchedList = new ArrayList<>();
	    matchedAtoms.forEachRemaining(matchedList::add);

	    // Vérifier le nombre de correspondances
	    assertEquals(3, matchedList.size(), "Le nombre de correspondances devrait être égal au nombre de triplets dans le store.");

	    // Définir les substitutions attendues
	    SubstitutionImpl expectedSubstitution1 = new SubstitutionImpl();
	    expectedSubstitution1.add(VAR_X, SUBJECT_1);
	    expectedSubstitution1.add(VAR_Y, PREDICATE_1);
	    expectedSubstitution1.add(VAR_Z, OBJECT_1);


	    // Vérifier que les substitutions attendues sont présentes
	    assertTrue(matchedList.contains(expectedSubstitution1), "La substitution pour (subject1, predicate1, object1) est manquante.");
	  
	}


    @Test
    public void testMatchStarQuery() {
    	RDFHexaStore store = new RDFHexaStore();
        
        RDFAtom rdfAtom1 = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);
        store.add(rdfAtom1);
        
        RDFAtom rdfAtom2 = new RDFAtom(SUBJECT_1, PREDICATE_2, OBJECT_2);
        store.add(rdfAtom2);
        
        RDFAtom rdfAtom3 = new RDFAtom(SUBJECT_2, PREDICATE_1, OBJECT_1);
        store.add(rdfAtom3);
        
        RDFAtom rdfAtom4 = new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2);
        store.add(rdfAtom4);
        
        

        RDFAtom queryAtom1 = new RDFAtom(VAR_X, PREDICATE_1, OBJECT_1);
        RDFAtom queryAtom2 = new RDFAtom(VAR_X, PREDICATE_2, OBJECT_2);
        List<RDFAtom> queryAtoms = Arrays.asList(queryAtom1, queryAtom2);
        StarQuery starQuery = new StarQuery("Q1", queryAtoms, Collections.singleton(VAR_X));
        
        // Exécuter la requête
        Iterator<Substitution> results = store.match(starQuery);
        
        // Collecter les résultats dans une liste
        List<Substitution> resultList = new ArrayList<>();
        results.forEachRemaining(resultList::add);
        
        // Créer les substitutions attendues
        SubstitutionImpl expectedSubst1 = new SubstitutionImpl();
        expectedSubst1.add(VAR_X, SUBJECT_1); 
        
        SubstitutionImpl expectedSubst2 = new SubstitutionImpl();
        expectedSubst2.add(VAR_X, SUBJECT_2); 
        
        // Vérifier que les résultats contiennent les substitutions attendues
        assertTrue(resultList.contains(expectedSubst1), "La substitution {?x -> subject1} est attendue.");
        assertTrue(resultList.contains(expectedSubst2), "La substitution {?x -> subject2} est attendue.");
        
        assertEquals(2, resultList.size(), "Le nombre de résultats devrait être 2.");
       
        Literal<String> NON_EXISTENT_PERSON = SameObjectTermFactory.instance().createOrGetLiteral("NonExistentPerson");
        RDFAtom noResultQueryAtom1 = new RDFAtom(VAR_X, PREDICATE_1, NON_EXISTENT_PERSON);
        RDFAtom noResultQueryAtom2 = new RDFAtom(VAR_X, PREDICATE_2, OBJECT_2);
        List<RDFAtom> noResultQueryAtoms = Arrays.asList(noResultQueryAtom1, noResultQueryAtom2);
        StarQuery noResultQuery = new StarQuery("NoResultQuery", noResultQueryAtoms, Collections.singleton(VAR_X));
        
        Iterator<Substitution> noResultIterator = store.match(noResultQuery);
        assertFalse(noResultIterator.hasNext(), "La requête sans résultats devrait retourner un itérateur vide.");
    }

    // Vos autres tests d'HexaStore ici
    
    @Test
    public void testGetIndexSOP() {
        RDFHexaStore store = new RDFHexaStore();

      
        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1)); 
        store.add(new RDFAtom(SUBJECT_2, PREDICATE_1, OBJECT_2)); 
        Map<Integer, Map<Integer, Set<Integer>>> indexSOP = store.getIndexSOP();

        Integer subject1Id = store.dictionary.getId(SUBJECT_1.toString());
        Integer subject2Id = store.dictionary.getId(SUBJECT_2.toString());
        Integer object1Id = store.dictionary.getId(OBJECT_1.toString());
        Integer object2Id = store.dictionary.getId(OBJECT_2.toString());
        Integer predicate1Id = store.dictionary.getId(PREDICATE_1.toString());

        assertTrue(indexSOP.containsKey(subject1Id), "L'index SOP devrait contenir le sujet 1.");
        assertTrue(indexSOP.containsKey(subject2Id), "L'index SOP devrait contenir le sujet 2.");

        Map<Integer, Set<Integer>> subject1Map = indexSOP.get(subject1Id);
        assertTrue(subject1Map.containsKey(object1Id), "L'index SOP pour subject1 devrait contenir object1.");
        assertTrue(subject1Map.get(object1Id).contains(predicate1Id), "L'index SOP pour subject1 et object1 devrait contenir predicate1.");

        Map<Integer, Set<Integer>> subject2Map = indexSOP.get(subject2Id);
        assertTrue(subject2Map.containsKey(object2Id), "L'index SOP pour subject2 devrait contenir object2.");
        assertTrue(subject2Map.get(object2Id).contains(predicate1Id), "L'index SOP pour subject2 et object2 devrait contenir predicate1.");
    }

    @Test
    public void testGetIndexPSO() {
        RDFHexaStore store = new RDFHexaStore();

        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1)); // (subject1, predicate1, object1)
        store.add(new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2)); // (subject2, predicate2, object2)

        Map<Integer, Map<Integer, Set<Integer>>> indexPSO = store.getIndexPSO();

        Integer predicate1Id = store.dictionary.getId(PREDICATE_1.toString());
        Integer predicate2Id = store.dictionary.getId(PREDICATE_2.toString());
        Integer subject1Id = store.dictionary.getId(SUBJECT_1.toString());
        Integer subject2Id = store.dictionary.getId(SUBJECT_2.toString());
        Integer object1Id = store.dictionary.getId(OBJECT_1.toString());
        Integer object2Id = store.dictionary.getId(OBJECT_2.toString());

        assertTrue(indexPSO.containsKey(predicate1Id), "L'index PSO doit contenir predicate1.");
        assertTrue(indexPSO.containsKey(predicate2Id), "L'index PSO doit contenir predicate2.");

        Map<Integer, Set<Integer>> predicate1Map = indexPSO.get(predicate1Id);
        assertTrue(predicate1Map.containsKey(subject1Id), "L'index PSO pour predicate1 doit contenir subject1.");
        assertTrue(predicate1Map.get(subject1Id).contains(object1Id), "L'index PSO pour predicate1 et subject1 doit contenir object1.");

        Map<Integer, Set<Integer>> predicate2Map = indexPSO.get(predicate2Id);
        assertTrue(predicate2Map.containsKey(subject2Id), "L'index PSO pour predicate2 doit contenir subject2.");
        assertTrue(predicate2Map.get(subject2Id).contains(object2Id), "L'index PSO pour predicate2 et subject2 doitt contenir object2.");
    }
    
    @Test
    public void testGetIndexOPS() {
        RDFHexaStore store = new RDFHexaStore();

        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1)); 
        store.add(new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_1)); 
        store.add(new RDFAtom(SUBJECT_2, PREDICATE_1, OBJECT_2)); 

        Map<Integer, Map<Integer, Set<Integer>>> indexOPS = store.getIndexOPS();

        Integer subject1Id = store.dictionary.getId(SUBJECT_1.toString());
        Integer subject2Id = store.dictionary.getId(SUBJECT_2.toString());
        Integer predicate1Id = store.dictionary.getId(PREDICATE_1.toString());
        Integer predicate2Id = store.dictionary.getId(PREDICATE_2.toString());
        Integer object1Id = store.dictionary.getId(OBJECT_1.toString());
        Integer object2Id = store.dictionary.getId(OBJECT_2.toString());

        assertTrue(indexOPS.containsKey(object1Id), "L'index OPS devrait contenir l'objet 1.");

        Map<Integer, Set<Integer>> object1Map = indexOPS.get(object1Id);
        assertTrue(object1Map.containsKey(predicate1Id), "L'index OPS pour object1 devrait contenir predicate1.");
        assertTrue(object1Map.get(predicate1Id).contains(subject1Id), "L'index OPS pour (object1, predicate1) devrait contenir subject1.");
        assertTrue(object1Map.containsKey(predicate2Id), "L'index OPS pour object1 devrait contenir predicate2.");
        assertTrue(object1Map.get(predicate2Id).contains(subject2Id), "L'index OPS pour (object1, predicate2) devrait contenir subject2.");

        assertTrue(indexOPS.containsKey(object2Id), "L'index OPS devrait contenir l'objet 2.");

        Map<Integer, Set<Integer>> object2Map = indexOPS.get(object2Id);
        assertTrue(object2Map.containsKey(predicate1Id), "L'index OPS pour object2 devrait contenir predicate1.");
        assertTrue(object2Map.get(predicate1Id).contains(subject2Id), "L'index OPS pour (object2, predicate1) devrait contenir subject2.");
    }


}
