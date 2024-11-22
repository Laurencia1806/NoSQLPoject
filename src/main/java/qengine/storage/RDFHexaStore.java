package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import fr.boreal.model.logicalElements.impl.ConstantImpl;

import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFAtom;
import qengine.model.StarQuery;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import qengine.storage.Dictionary;


/**
 * Implémentation d'un HexaStore pour stocker des RDFAtom.
 * Cette classe utilise six index pour optimiser les recherches.
 * Les index sont basés sur les combinaisons (Sujet, Prédicat, Objet), (Sujet, Objet, Prédicat),
 * (Prédicat, Sujet, Objet), (Prédicat, Objet, Sujet), (Objet, Sujet, Prédicat) et (Objet, Prédicat, Sujet).
 */

public class RDFHexaStore implements RDFStorage {
	
	
	public Dictionary dictionary = new Dictionary();
	
    private Map<Integer, Map<Integer, Set<Integer>>> indexSPO = new HashMap<>();
    private Map<Integer, Map<Integer, Set<Integer>>> indexSOP = new HashMap<>();
    private Map<Integer, Map<Integer, Set<Integer>>> indexPSO = new HashMap<>();
    private Map<Integer, Map<Integer, Set<Integer>>> indexPOS = new HashMap<>();
    private Map<Integer, Map<Integer, Set<Integer>>> indexOSP = new HashMap<>();
    private Map<Integer, Map<Integer, Set<Integer>>> indexOPS = new HashMap<>();
	
    @Override
    public boolean add(RDFAtom atom) {
        Term subject = atom.getTripleSubject();
        Term predicate = atom.getTriplePredicate();
        Term object = atom.getTripleObject();

        // Ajouter les termes au dictionnaire 
        int subjectId = this.dictionary.addTerm(subject.toString());
        int predicateId = this.dictionary.addTerm(predicate.toString());
        int objectId = this.dictionary.addTerm(object.toString());

        // Ajouter l'atome aux différents index
        addToIndexes(subjectId, predicateId, objectId);
        return true;
    }

    private void addToIndexes(int subject, int predicate, int object) {
        // Ajouter l'atome à chaque index spécifique
        addToSingleIndex(indexSPO, subject, predicate, object);
        addToSingleIndex(indexSOP, subject, object, predicate);
        addToSingleIndex(indexPSO, predicate, subject, object);
        addToSingleIndex(indexPOS, predicate, object, subject);
        addToSingleIndex(indexOSP, object, subject, predicate);
        addToSingleIndex(indexOPS, object, predicate, subject);
    }

    private void addToSingleIndex(Map<Integer, Map<Integer, Set<Integer>>> index, int key1, int key2, int value) {
        // Ajouter la valeur à l'index en utilisant les clés 
        index.computeIfAbsent(key1, k -> new HashMap<>())
             .computeIfAbsent(key2, k -> new HashSet<>())
             .add(value);
    }

    @Override
    public long size() {
        long count = 0;
        // Parcourir l'index SPO pour compter le nombre total d'atomes
        for (Map<Integer, Set<Integer>> predicateMap : indexSPO.values()) {
            for (Set<Integer> objectSet : predicateMap.values()) {
                count += objectSet.size();
            }
        }
        return count;
    }


    @Override
    public Iterator<Substitution> match(StarQuery query) {
        throw new NotImplementedException();
    }

    @Override
    public Collection<Atom> getAtoms() {
        Set<Atom> atoms = new HashSet<>();

        // Parcourir chaque sujet dans l'index SPO
        for (Map.Entry<Integer, Map<Integer, Set<Integer>>> subjectEntry : indexSPO.entrySet()) {
            Integer subjectId = subjectEntry.getKey();
            String subjectValue = dictionary.getTerm(subjectId);
            Term subjectTerm = SameObjectTermFactory.instance().createOrGetLiteral(subjectValue);

            // Parcourir chaque prédicat associé au sujet
            Map<Integer, Set<Integer>> predicateMap = subjectEntry.getValue();
            for (Map.Entry<Integer, Set<Integer>> predicateEntry : predicateMap.entrySet()) {
                Integer predicateId = predicateEntry.getKey();
                String predicateValue = dictionary.getTerm(predicateId);
                Term predicateTerm = SameObjectTermFactory.instance().createOrGetLiteral(predicateValue);

                // Parcourir chaque objet associé au prédicat
                Set<Integer> objectIds = predicateEntry.getValue();
                for (Integer objectId : objectIds) {
                    String objectValue = dictionary.getTerm(objectId);
                    Term objectTerm = SameObjectTermFactory.instance().createOrGetLiteral(objectValue);

                    // Créer un RDFAtom et l'ajouter à l'ensemble des atomes
                    RDFAtom atom = new RDFAtom(subjectTerm, predicateTerm, objectTerm);
                    atoms.add(atom);
                }
            }
        }
        return atoms;
    }

    
    @Override
    public Iterator<Substitution> match(RDFAtom atom) {
        Term subjectTerm = atom.getTripleSubject();
        Term predicateTerm = atom.getTriplePredicate();
        Term objectTerm = atom.getTripleObject();

        boolean subjectIsVar = subjectTerm instanceof Variable;
        boolean predicateIsVar = predicateTerm instanceof Variable;
        boolean objectIsVar = objectTerm instanceof Variable;

        List<Substitution> substitutions = new ArrayList<>();

        if (!subjectIsVar && !predicateIsVar && !objectIsVar) {
            // Tous les termes sont constants. Vérifier si le triplet existe
            Integer subjectId = dictionary.getId(subjectTerm.toString());
            Integer predicateId = dictionary.getId(predicateTerm.toString());
            Integer objectId = dictionary.getId(objectTerm.toString());

            if (subjectId == null || predicateId == null || objectId == null) {
                // Au moins un terme est inconnu, aucun résultat
                return Collections.emptyIterator();
            }

            boolean exists = indexSPO.containsKey(subjectId) &&
                             indexSPO.get(subjectId).containsKey(predicateId) &&
                             indexSPO.get(subjectId).get(predicateId).contains(objectId);

            if (exists) {
                SubstitutionImpl subst = new SubstitutionImpl();
                substitutions.add(subst);
            }
            return substitutions.iterator(); 
        }
        else if (subjectIsVar && !predicateIsVar && !objectIsVar) {
            // Le sujet est une variable
            Integer predicateId = dictionary.getId(predicateTerm.toString());
            Integer objectId = dictionary.getId(objectTerm.toString());

            if (predicateId == null || objectId == null) {
                // Prédicat ou objet inconnu, aucun résultat
                return Collections.emptyIterator();
            }

            Set<Integer> subjects = indexPOS.getOrDefault(predicateId, Collections.emptyMap())
                                            .getOrDefault(objectId, Collections.emptySet());

            Variable subjectVar = (Variable) subjectTerm;
            for (Integer subjId : subjects) {
                SubstitutionImpl subst = new SubstitutionImpl();
                String subjectValue = dictionary.getTerm(subjId);
                subst.add(subjectVar, SameObjectTermFactory.instance().createOrGetLiteral(subjectValue));
                substitutions.add(subst);
            }
            return substitutions.iterator();
        } 
        else if (!subjectIsVar && predicateIsVar && !objectIsVar) {
            // Le prédicat est une variable
            Integer subjectId = dictionary.getId(subjectTerm.toString());
            Integer objectId = dictionary.getId(objectTerm.toString());

            if (subjectId == null || objectId == null) {
                return Collections.emptyIterator();
            }

            Set<Integer> predicates = indexSOP.getOrDefault(subjectId, Collections.emptyMap())
                                            .getOrDefault(objectId, Collections.emptySet());

            Variable predicateVar = (Variable) predicateTerm;
            for (Integer predicateId : predicates) {
                SubstitutionImpl subst = new SubstitutionImpl();
                String predicateValue = dictionary.getTerm(predicateId);
                subst.add(predicateVar, SameObjectTermFactory.instance().createOrGetLiteral(predicateValue));
                substitutions.add(subst);
            }
            return substitutions.iterator();
        }
        else if (!subjectIsVar && !predicateIsVar && objectIsVar) {
            // L'objet est une variable
            Integer subjectId = dictionary.getId(subjectTerm.toString());
            Integer predicateId = dictionary.getId(predicateTerm.toString());

            if (subjectId == null || predicateId == null) {
                return Collections.emptyIterator();
            }

            Set<Integer> objects = indexSPO.getOrDefault(subjectId, Collections.emptyMap())
                                            .getOrDefault(predicateId, Collections.emptySet());

            Variable objectVar = (Variable) objectTerm;
            for (Integer objId : objects) {
                SubstitutionImpl subst = new SubstitutionImpl();
                String objectValue = dictionary.getTerm(objId);
                subst.add(objectVar, SameObjectTermFactory.instance().createOrGetLiteral(objectValue));
                substitutions.add(subst);
            }
            return substitutions.iterator();
        }
        else if (subjectIsVar && predicateIsVar && !objectIsVar) {
            // Sujet et prédicat sont des variables
            Integer objectId = dictionary.getId(objectTerm.toString());

            if (objectId == null) {
                return Collections.emptyIterator();
            }

            Map<Integer, Set<Integer>> subjectMap = indexOSP.getOrDefault(objectId, Collections.emptyMap());
            for (Map.Entry<Integer, Set<Integer>> entry : subjectMap.entrySet()) {
                Integer subjectId = entry.getKey();
                Set<Integer> predicates = entry.getValue();
                String subjectValue = dictionary.getTerm(subjectId);
                for (Integer predicateId : predicates) {
                    SubstitutionImpl subst = new SubstitutionImpl();
                    String predicateValue = dictionary.getTerm(predicateId);
                    subst.add((Variable) subjectTerm, SameObjectTermFactory.instance().createOrGetLiteral(subjectValue));
                    subst.add((Variable) predicateTerm, SameObjectTermFactory.instance().createOrGetLiteral(predicateValue));
                    substitutions.add(subst);
                }
            }
            return substitutions.iterator();
        }
        else if (subjectIsVar && !predicateIsVar && objectIsVar) {
            // Sujet et objet sont des variables
            Integer predicateId = dictionary.getId(predicateTerm.toString());

            if (predicateId == null) {
                return Collections.emptyIterator();
            }

            Map<Integer, Set<Integer>> subjectMap = indexPSO.getOrDefault(predicateId, Collections.emptyMap());
            for (Map.Entry<Integer, Set<Integer>> entry : subjectMap.entrySet()) {
                Integer subjectId = entry.getKey();
                Set<Integer> objects = entry.getValue();
                String subjectValue = dictionary.getTerm(subjectId);
                for (Integer objectId : objects) {
                    SubstitutionImpl subst = new SubstitutionImpl();
                    String objectValue = dictionary.getTerm(objectId);
                    subst.add((Variable) subjectTerm, SameObjectTermFactory.instance().createOrGetLiteral(subjectValue));
                    subst.add((Variable) objectTerm, SameObjectTermFactory.instance().createOrGetLiteral(objectValue));
                    substitutions.add(subst);
                }
            }
            return substitutions.iterator();
        }
        else if (!subjectIsVar && predicateIsVar && objectIsVar) {
            // Prédicat et objet sont des variables
            Integer subjectId = dictionary.getId(subjectTerm.toString());

            if (subjectId == null) {
                return Collections.emptyIterator();
            }

            Map<Integer, Set<Integer>> predicateMap = indexSPO.getOrDefault(subjectId, Collections.emptyMap());
            for (Map.Entry<Integer, Set<Integer>> entry : predicateMap.entrySet()) {
                Integer predicateId = entry.getKey();
                Set<Integer> objects = entry.getValue();
                String predicateValue = dictionary.getTerm(predicateId);
                for (Integer objectId : objects) {
                    SubstitutionImpl subst = new SubstitutionImpl();
                    String objectValue = dictionary.getTerm(objectId);
                    subst.add((Variable) predicateTerm, SameObjectTermFactory.instance().createOrGetLiteral(predicateValue));
                    subst.add((Variable) objectTerm, SameObjectTermFactory.instance().createOrGetLiteral(objectValue));
                    substitutions.add(subst);
                }
            }
            return substitutions.iterator();
        }
        else if (subjectIsVar && predicateIsVar && objectIsVar) {
            // Tous les termes sont des variables
            for (Map.Entry<Integer, Map<Integer, Set<Integer>>> sEntry : indexSPO.entrySet()) {
                Integer subjectId = sEntry.getKey();
                String subjectValue = dictionary.getTerm(subjectId);
                for (Map.Entry<Integer, Set<Integer>> pEntry : sEntry.getValue().entrySet()) {
                    Integer predicateId = pEntry.getKey();
                    String predicateValue = dictionary.getTerm(predicateId);
                    for (Integer objectId : pEntry.getValue()) {
                        String objectValue = dictionary.getTerm(objectId);
                        SubstitutionImpl subst = new SubstitutionImpl();
                        subst.add((Variable) subjectTerm, SameObjectTermFactory.instance().createOrGetLiteral(subjectValue));
                        subst.add((Variable) predicateTerm, SameObjectTermFactory.instance().createOrGetLiteral(predicateValue));
                        subst.add((Variable) objectTerm, SameObjectTermFactory.instance().createOrGetLiteral(objectValue));
                        substitutions.add(subst);
                    }
                }
            }
            return substitutions.iterator();
        }

        // Aucun cas correspondant, retourner un itérateur vide
        return Collections.emptyIterator();
    }

    
    
    // Getters pour les indexes
    public Map<Integer, Map<Integer, Set<Integer>>> getIndexSPO() {
        return indexSPO;
    }

    public Map<Integer, Map<Integer, Set<Integer>>> getIndexSOP() {
        return indexSOP;
    }

    public Map<Integer, Map<Integer, Set<Integer>>> getIndexPSO() {
        return indexPSO;
    }

    public Map<Integer, Map<Integer, Set<Integer>>> getIndexPOS() {
        return indexPOS;
    }

    public Map<Integer, Map<Integer, Set<Integer>>> getIndexOSP() {
        return indexOSP;
    }

    public Map<Integer, Map<Integer, Set<Integer>>> getIndexOPS() {
        return indexOPS;
    }

}
