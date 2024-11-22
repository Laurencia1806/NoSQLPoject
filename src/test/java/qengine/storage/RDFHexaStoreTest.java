package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFAtom;
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


    @Test
    public void testAddAllRDFAtoms() {
        RDFHexaStore store = new RDFHexaStore();

        // Ajouter plusieurs RDFAtom
        RDFAtom rdfAtom1 = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFAtom rdfAtom2 = new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2);

        Set<RDFAtom> rdfAtoms = Set.of(rdfAtom1, rdfAtom2);

        assertTrue(store.addAll(rdfAtoms.stream()), "Les RDFAtoms devraient être ajoutés avec succès.");

        // Vérifier que tous les atomes sont présents
        Collection<Atom> atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajouté.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajouté.");

        store = new RDFHexaStore();
        assertTrue(store.addAll(rdfAtoms), "Les RDFAtoms devraient être ajoutés avec succès.");

        // Vérifier que tous les atomes sont présents
        atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajouté.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajouté.");
    }

    @Test
    public void testAddRDFAtom() {
        RDFHexaStore store = new RDFHexaStore();

        // Créer un RDFAtom
        RDFAtom rdfAtom = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);

        // Ajouter l'atome au store
        boolean added = store.add(rdfAtom);

        // Vérifier que l'ajout a réussi
        assertTrue(added, "L'ajout du RDFAtom devrait réussir.");

        Collection<Atom> atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom), "Le store devrait contenir le RDFAtom ajouté.");

        // Vérifier que les index ont été mis à jour correctement
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
	    assertTrue(atoms.contains(rdfAtom), "Le store devrait contenir le RDFAtom ajouté.");
	}

	@Test
	public void testSize() {
	    RDFHexaStore store = new RDFHexaStore();
	    assertEquals(0, store.size(), "La taille initiale du store devrait être 0.");
	    store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1));
	    assertEquals(1, store.size(), "La taille du store devrait être 1 après l'ajout d'un atome.");
	    store.add(new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2));
	    assertEquals(2, store.size(), "La taille du store devrait être 2 après l'ajout d'un second atome.");
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
	}


    @Test
    public void testMatchStarQuery() {
        throw new NotImplementedException();
    }

    // Vos autres tests d'HexaStore ici
}
