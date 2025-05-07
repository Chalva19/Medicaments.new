package com.europhorma.medicaments;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Valeur par défaut pour la sélection de la voie d'administration
    private static final String PREMIERE_VOIE = "selectionnez une voie d'administration";
    private final Context mycontext;
    private static final String DATABASE_NAME = "medicaments.db"; // Nom de la base de données
    private static final int DATABASE_VERSION = 1; // Version de la base de données
    private String DATABASE_PATH; // Chemin d'accès à la base
    private SQLiteDatabase database; // Instance de la base de données SQLite

    // Constructeur de la classe
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mycontext = context;

        // Détermine le chemin de la base de données dans le dossier de l'application
        String filesDir = context.getFilesDir().getPath();
        DATABASE_PATH = filesDir.substring(0, filesDir.lastIndexOf("/")) + "/databases/";

        // Vérifie si la base existe déjà, sinon la copie depuis les assets
        if (!checkdatabase()) {
            Log.d("APP", "BDD a copier");
            copydatabase();
        }

        // Ouvre la base de données
        openDatabase();
    }

    // Vérifie si la base de données a déjà été copiée
    private boolean checkdatabase() {
        File dbfile = new File(DATABASE_PATH + DATABASE_NAME);
        return dbfile.exists(); // Renvoie true si la base de données existe, sinon false
    }

    // Copie la base de données depuis le dossier 'assets' vers le dossier de l'application
    private void copydatabase() {
        final String outFileName = DATABASE_PATH + DATABASE_NAME;

        InputStream myInput;

        try {
            // Ouvre la base de données dans les assets
            myInput = mycontext.getAssets().open(DATABASE_NAME);

            // Crée le dossier s'il n'existe pas
            File pathFile = new File(DATABASE_PATH);
            if (!pathFile.exists()) {
                if (!pathFile.mkdirs()) {
                    Toast.makeText(mycontext, "Erreur : copydatabase(), pathFile.mkdirs()", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Crée la base de destination
            OutputStream myOutput = new FileOutputStream(outFileName);

            // Transfère les données de l'input vers l'output
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }

            // Ferme les flux
            myOutput.flush();
            myOutput.close();
            myInput.close();

            Log.d("APP", "BDD copiée");

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("APP", "erreur copie de la base");
            Toast.makeText(mycontext, "Erreur : copydatabase()", Toast.LENGTH_SHORT).show();
        }

        // Définit la version de la base copiée
        try {
            SQLiteDatabase checkdb = SQLiteDatabase.openDatabase(DATABASE_PATH + DATABASE_NAME, null, SQLiteDatabase.OPEN_READWRITE);
            checkdb.setVersion(DATABASE_VERSION);
        } catch (SQLiteException e) {
            // La base n'existe pas encore
        }
    }

    // Ouvre la base de données
    public void openDatabase() {
        String dbPath = DATABASE_PATH + DATABASE_NAME;
        database = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    // Vérifie si une table existe dans la base
    public boolean checkTableExists(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{tableName});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    // Récupère toutes les voies d'administration distinctes sans point-virgule
    public List<String> getDistinctVoiesAdmin() {
        List<String> voiesAdminList = new ArrayList<>();
        voiesAdminList.add(PREMIERE_VOIE); // Ajoute une option par défaut dans la liste

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT DISTINCT Voie_dadministration FROM CIS_bdpm WHERE Voie_dadministration NOT LIKE '%;%' ORDER BY Voie_dadministration", null);
            while (cursor.moveToNext()) {
                voiesAdminList.add(cursor.getString(0));
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Erreur lors de la récupération des voies d'administration", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return voiesAdminList;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Pas utilisé car la base est copiée depuis les assets
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // À implémenter si tu veux gérer une migration plus tard
    }

    // Recherche de médicaments selon plusieurs critères
    public List<Medicament> searchMedicaments(String denomination, String formePharmaceutique, String titulaires, String denominationSubstance, String voiesAdmin, String dateAutorisation) {
        List<Medicament> medicamentList = new ArrayList<>();
        ArrayList<String> selectionArgs = new ArrayList<>();

        // Paramètres de filtre pour la requête SQL
        selectionArgs.add("%" + denomination + "%");
        selectionArgs.add("%" + formePharmaceutique + "%");
        selectionArgs.add("%" + titulaires + "%");
        selectionArgs.add("%" + denominationSubstance + "%");
        selectionArgs.add(dateAutorisation);

        String finSQL = "";
        if (!voiesAdmin.equals(PREMIERE_VOIE)) {
            finSQL = "AND Voie_dadministration LIKE ?";
            selectionArgs.add("%" + voiesAdmin + "%");
        }

        // Recherche du Code_CIS dans la table des composants avec accents normalisés
        String SQLSubstance = "SELECT CODE_CIS FROM CIS_COMPO_bdpm WHERE replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(upper(Denomination_substance), 'Â','A'),'Ä','A'),'À','A'),'É','E'),'Á','A'),'Ï','I'), 'Ê','E'),'È','E'),'Ô','O'),'Ü','U'), 'Ç','C' ) LIKE ?";

        // Requête principale
        String query = "SELECT *,(select count(*) from CIS_COMPO_bdpm c where c.Code_CIS=m.Code_CIS) as nb_molecule, (select count(*) from CIS_GENER_bdpm g where g.Code_CIS=m.Code_CIS) as Generique FROM CIS_bdpm m WHERE " +
                "Denomination LIKE ? AND " +
                "Forme_pharmaceutique LIKE ? AND " +
                "Titulaire LIKE ? AND " +
                "Code_CIS IN (" + SQLSubstance + ") AND " +
                "Date_dAMM_2 >= ? " +
                finSQL;

        Log.d("SQL", query);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, selectionArgs.toArray(new String[0]));

        // Traitement des résultats
        if (cursor.moveToFirst()) {
            do {
                int codeCIS = cursor.getInt(cursor.getColumnIndex("Code_CIS"));
                String denominationMedicament = cursor.getString(cursor.getColumnIndex("Denomination"));
                String formePharmaceutiqueMedicament = cursor.getString(cursor.getColumnIndex("Forme_pharmaceutique"));
                String voiesAdminMedicament = cursor.getString(cursor.getColumnIndex("Voie_dadministration"));
                String titulairesMedicament = cursor.getString(cursor.getColumnIndex("Titulaire"));
                String statutAdministratif = cursor.getString(cursor.getColumnIndex("Statut_administratif_AMM"));
                dateAutorisation = cursor.getString(cursor.getColumnIndex("Date_dAMM_2"));
                String CountMolecule = cursor.getString(cursor.getColumnIndex("nb_molecule"));


                // Création de l'objet médicament
                Medicament medicament = new Medicament();
                medicament.setCodeCIS(codeCIS);
                medicament.setDenomination(denominationMedicament);
                medicament.setFormePharmaceutique(formePharmaceutiqueMedicament);
                medicament.setVoiesAdmin(voiesAdminMedicament);
                medicament.setTitulaires(titulairesMedicament);
                medicament.setStatutAdministratif(statutAdministratif);
                medicament.setDateAutorisation(dateAutorisation);
                medicament.setNb_molecule(String.valueOf(getNombreMolecules(codeCIS))); // ou CountMolecule directement
                if (cursor.getInt(cursor.getColumnIndex("Type_generique"))>0){
                    medicament.setGeneric("GENERIQUE");
                }

                medicamentList.add(medicament);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return medicamentList;
    }

    // Récupère le nombre de molécules d'un médicament (via Code_CIS)
    public int getNombreMolecules(int codeCIS) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT count(*) FROM CIS_COMPO_bdpm WHERE Code_CIS=?", new String[]{String.valueOf(codeCIS)});
        cursor.moveToFirst();
        int nb = cursor.getInt(0);
        cursor.close();
        db.close();
        return nb;
    }

    // Récupère la composition d'un médicament
    public List<String> getCompositionMedicament(int codeCIS) {
        List<String> compositionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM CIS_compo_bdpm WHERE Code_CIS = ?", new String[]{String.valueOf(codeCIS)});

        int i = 0;
        if (cursor.moveToFirst()) {
            do {
                i++;
                String substance = cursor.getString(cursor.getColumnIndex("Denomination_substance"));
                String dosage = cursor.getString(cursor.getColumnIndex("Dosage_substance"));
                compositionList.add(i + ":" + substance + "(" + dosage + ")");
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return compositionList;
    }

    // Récupère la présentation d’un médicament
    public List<String> getPresentationMedicament(int codeCIS) {
        List<String> presentationList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM CIS_CIP_bdpm WHERE Code_CIS = ?", new String[]{String.valueOf(codeCIS)});

        int i = 0;
        if (cursor.moveToFirst()) {
            do {
                i++;
                String libellePresentation = cursor.getString(cursor.getColumnIndex("Libelle_presentation"));
                presentationList.add(i + ":" + libellePresentation);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return presentationList;
    }
}
