package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.exceptions.TechnicienException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_DATE = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[012])/((19|20)[0-9]{2})$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception {
        String fileName = "employes.csv";
        readFile(fileName);
        this.employes.forEach(employe -> logger.info(employe.toString()));
        this.employeRepository.save(this.employes);
    }

    

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public void readFile(String fileName) throws Exception {

        Stream<String> stream;

        try {
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        }
        catch (FileNotFoundException e){
            logger.error(String.format(errorMessage("no_file"), fileName));
            return;
        }

        List<String> lignes = stream.collect(Collectors.toList());

        // parcours des lignes trouvées
        for (int i=0; i<lignes.size(); i++){
            try {
                processLine(lignes.get(i));
            }
            catch (BatchException e){
                logger.error(String.format(errorMessage("error_ligne"), (i+1), e.getMessage()));
            }
        }
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {

        switch (ligne.substring(0,1)) {
            case "T":
                processTechnicien(ligne);
                break;
            case "M":
                processManager(ligne);
                break;
            case "C":
                processCommercial(ligne);
                break;
            default:
                throw new BatchException(String.format(errorMessage("error_type_employe"), ligne.substring(0,1), ligne));
        }

    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        String[] arrayCommercial = this.splitLigne(ligneCommercial);

        this.check("Commercial", ligneCommercial, NB_CHAMPS_COMMERCIAL, arrayCommercial);
        // teste sur le cA
        try{
            if (Double.parseDouble(arrayCommercial[5])>=10000000.00){
                throw new NumberFormatException();
            }
        }catch (NumberFormatException e){
            throw new BatchException(String.format(errorMessage("error_CA"), arrayCommercial[5], ligneCommercial));
        }
        // teste sur la performance
        try{
            if(Integer.parseInt(arrayCommercial[6]) >= 2147483647 || Integer.parseInt(arrayCommercial[6]) < 0) {
                throw new NumberFormatException();
            }
        }
        catch (NumberFormatException e){
            throw new BatchException(String.format(errorMessage("error_perf"), arrayCommercial[6], ligneCommercial));
        }

        //ajoute l'employe maintenant que tout les champs ont été testé
        if(this.employeRepository.findByMatricule( arrayCommercial[0])==null){
            this.employes.add(new Commercial(
                    arrayCommercial[1],
                    arrayCommercial[2],
                    arrayCommercial[0],
                    DateTimeFormat.forPattern("dd/mm/yyyy").parseLocalDate(arrayCommercial[3]),
                    Double.parseDouble(arrayCommercial[4]),
                    Double.parseDouble(arrayCommercial[5]),
                    Integer.parseInt(arrayCommercial[6])
            ));
        }
        else{
            logger.info(String.format(errorMessage("error_matricule_exist"), arrayCommercial[0], ligneCommercial));
        }
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        String[] arrayManager = this.splitLigne(ligneManager);

        this.check("Manager", ligneManager, NB_CHAMPS_MANAGER, arrayManager);

        //ajoute l'employe maintenant que tout les champs ont été testé
        if(this.employeRepository.findByMatricule( arrayManager[0])==null){
            this.employes.add(new Manager(
                    arrayManager[1],
                    arrayManager[2],
                    arrayManager[0],
                    DateTimeFormat.forPattern("dd/mm/yyyy").parseLocalDate(arrayManager[3]),
                    Double.parseDouble(arrayManager[4]),
                    null
            ));
        }
        else{
            logger.info(String.format(errorMessage("error_matricule_exist"), arrayManager[0], ligneManager));
        }
    }


    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        String[] arrayTechnicien = this.splitLigne(ligneTechnicien);
        
        this.check("Technicien", ligneTechnicien, NB_CHAMPS_TECHNICIEN, arrayTechnicien);
        // teste sur le grade
        {
            try{
                Integer grade = Integer.parseInt(arrayTechnicien[5]);
                if (grade < 1 || grade > 5){
                    throw new BatchException(String.format(errorMessage("error_grade_level"), grade.toString(), ligneTechnicien));
                }
            }
            catch (NumberFormatException e){
                throw new BatchException(String.format(errorMessage("error_grade_type"), arrayTechnicien[5], ligneTechnicien));
            }
        }
        //tests sur le matricule du manager
        {
            if(!arrayTechnicien[6].matches(REGEX_MATRICULE_MANAGER)){
                throw new BatchException(String.format(errorMessage("error_regex"), arrayTechnicien[6], REGEX_MATRICULE_MANAGER, ligneTechnicien));
            }

            boolean ifInBdd = this.managerRepository.findByMatricule(managerMatricule) != null;
            boolean ifInFile = this.employes.stream().filter(employe -> employe.getMatricule().matches(managerMatricule)).count() == 1;


            boolean ifexist = (ifInBdd || ifInFile);
            if (ifexist == false){
                throw new BatchException(String.format(errorMessage("wrong_manager_id"), arrayTechnicien[6], ligneTechnicien));
            }
        }

        // Ajoute de l'employé
        if(this.employeRepository.findByMatricule( arrayTechnicien[0])==null)
        {
            // Instancie le manager de l'employé 
            Manager manage;
            if(this.managerRepository.findByMatricule(arrayTechnicien[6])!=null)
            {
                manage = this.managerRepository.findByMatricule(arrayTechnicien[6]);
            }
            else 
            {
                manage = (Manager) this.employes.stream()
                        .filter(employe -> employe.getMatricule().equals(arrayTechnicien[6]))
                        .collect(Collectors.toList())
                        .get(0);
            }

            
            try 
            {
                
                this.employes.add(new Technicien(
                        arrayTechnicien[1],
                        arrayTechnicien[2],
                        arrayTechnicien[0],
                        DateTimeFormat.forPattern("dd/mm/yyyy").parseLocalDate(arrayTechnicien[3]),
                        Double.parseDouble(arrayTechnicien[4]),
                        Integer.parseInt(arrayTechnicien[5]),
                        manage
                        
                ));
            }
            catch (TechnicienException e)
            {
                throw new BatchException(String.format(errorMessage("error_technicien"), e.getMessage(), ligneTechnicien));
            }
        }
        else
        {
            logger.info(String.format(errorMessage("error_matricule_exist"), arrayTechnicien[0], ligneTechnicien));
        }
    }


    /**
     * Méthode qui sépare une ligne
     * @param ligne la ligne a parser
     */
    private String[] splitLigne(String ligne) {
        ligne = ligne.split(",");
        return ligne;
    }





    /**
     * Methode qui teste les champs commun (champs typique d'un Employe
     * @param type la role de l'employé
     * @param ligne le tableau contenant les infos parsées
     * @param nbChamps le nombre de champs que doit avoir parsedLigne
     * @param parsedLigne Les champs composant l'employe
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void check(String type, String ligne, int nbChamps, String[] parsedLigne) throws BatchException{

        // Check NB Champs
        if(this.splitLigne(ligne).length!=nbChamps){
            throw new BatchException(String.format(errorMessage("error_ligne_numb"), type, nbChamps , parsedLigne.length, ligne));
        }
        
        // Check Matricule
        if (!(parsedLigne[0].matches(REGEX_MATRICULE))){
            throw new BatchException(String.format(errorMessage("error_regex"), parsedLigne[0] , REGEX_MATRICULE, ligne));
        }

        // Check Nom
        if(!parsedLigne[1].matches(REGEX_NOM)){
            throw new BatchException(String.format(errorMessage("error_name_format"), parsedLigne[1], ligne));
        }

        // Check Prenom
        if(!parsedLigne[2].matches(REGEX_PRENOM)){
            throw new BatchException(String.format(errorMessage("error_surname_format"), parsedLigne[2] , ligne));
        }

        //Check Date
        if (!parsedLigne[3].matches(REGEX_DATE)){
            throw new BatchException(String.format(errorMessage("error_date_format"), parsedLigne[3], ligne));
        }
        
        //Check Salaire
        try{
            if (Double.parseDouble(parsedLigne[4])>=10000000.00){
                throw new NumberFormatException();
            }
        }
        catch (NumberFormatException e){
            throw new BatchException(String.format(errorMessage("error_salary_format"), parsedLigne[4], ligne));
        }

    }


    private String errorMessage(String value){        

        switch (value) {
            
            case "no_file"              :  value = "Le fichier %s n'existe pas ! ";
                    break;
            case "error_ligne"          :  value = "Ligne %d : %s";
                    break;
            case "error_type_employe"   :  value = "Type d'employé inconnu : %s => %s ";
                    break;
            case "error_CA"             :  value = "Le chiffre d'affaire du commercial est incorrect : %f => %s";
                    break;
            case "error_perf"           :  value = "La performance du commercial est incorrecte : %d => %s ";
                    break;
            case "error_matricule_exist":  value = "Matricule %s déjà présent en base => %s";
                    break;                    
            case "error_grade_level"    :  value = "Le grade doit être compris entre 1 et 5 : %s = > %s ";
                    break;
            case "error_grade_type"     :  value = "Le grade du technicien est incorrect : %f  =>  %s ";
                    break;
            case "error_regex"          :  value = "La chaîne %s ne respecte pas l'expression régulière %s => %s ";
                    break;
            case "wrong_manager_id"     :  value = "Le manager de matricule %s n'a pas été trouvé dans le fichier ou en base de données => %s ";
                    break;
            case "error_technicien"     :  value = " %s => %s ";
                    break;
            case "error_matricule_exist":  value = "Matricule %s déjà présent en base => %s";
                    break;
            case "error_ligne_numb"     :  value = "La ligne %s ne contient pas %d éléments mais %d => %s";
                    break;
            case "error_salary_format"  :  value = "%f n'est pas un nombre valide pour un salaire => %s ";
                    break;
            case "error_name_format"    :  value = "%s n'est pas un nom correct => %s";
                    break;
            case "error_surname_format" :  value = "%s n'est pas un prenom correct => %s";
                    break;
            case "error_date_format"    :  value = "%s ne respecte pas le format de date dd/MM/yyyy => %s";
                    break;
            default                     :  value = "Invalid error";
                    break;
        }
        return value;
    }
}
