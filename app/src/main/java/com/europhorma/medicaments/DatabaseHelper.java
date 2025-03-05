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

    private static final String PREMIERE_VOIE = "selectionnez une voie d'administration";
    private final Context mycontext;
    private static final String DATABASE_NAME = "medicaments.db";
    private static final int DATABASE_VERSION = 1;
    private String DATABASE_PATH ;
    private SQLiteDatabase database;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mycontext = context;
        String filesDir = context.getFilesDir().getPath();
        DATABASE_PATH = filesDir.substring(0,filesDir.lastIndexOf("/"))+"/databases/";

        if (!checkdatabase()){
            Log.d("APP","BDD a copier");
            copydatabase();
        }
        openDatabase();
    }

    private boolean checkdatabase() {
        // retourne true/false si la bdd existe dans le dossier de l'app
        File dbfile = new File(DATABASE_PATH + DATABASE_NAME);

        return dbfile.exists();
    }
    private void copydatabase() {

        final String outFileName = DATABASE_PATH + DATABASE_NAME;

        //AssetManager assetManager = mycontext.getAssets();
        InputStream myInput;

        try {
            // Ouvre le fichier de la  bdd de 'assets' en lecture
            myInput = mycontext.getAssets().open(DATABASE_NAME);

            // dossier de destination
            File pathFile = new File(DATABASE_PATH);
            if (!pathFile.exists()) {
                if (!pathFile.mkdirs()) {
                    Toast.makeText(mycontext, "Erreur : copydatabase(), pathFile.mkdirs()", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Ouverture en écriture du fichier bdd de destination
            OutputStream myOutput = new FileOutputStream(outFileName);

            // transfert de inputfile vers outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }

            // Fermeture
            Log.d("APP", "BDD copiée");
            myOutput.flush();
            myOutput.close();
            myInput.close();

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("APP", "erreur copie de la base");
            Toast.makeText(mycontext, "Erreur : copydatabase()", Toast.LENGTH_SHORT).show();
        }

        // on greffe le numéro de version
        try {
            SQLiteDatabase checkdb = SQLiteDatabase.openDatabase(DATABASE_PATH + DATABASE_NAME, null, SQLiteDatabase.OPEN_READWRITE);
            checkdb.setVersion(DATABASE_VERSION);
        } catch (SQLiteException e) {
            // bdd n'existe pas
        }

    }
    public void openDatabase() {
        String dbPath = DATABASE_PATH + DATABASE_NAME;
        database = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
    }


    public boolean checkTableExists(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{tableName});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }


    public List<String> getDistinctVoiesAdmin() {
        List<String> voiesAdminList = new ArrayList<>();
        voiesAdminList.add(PREMIERE_VOIE); // Ajoute la première option

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

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public List<Medicament> searchMedicaments(String denomination, String formePharmaceutique, String titulaires, String denominationSubstance, String voiesAdmin, String dateAutorisation) {
        List<Medicament> medicamentList = new ArrayList<>();
        ArrayList<String> selectionArgs = new ArrayList<>();
        selectionArgs.add("%" + denomination + "%");
        selectionArgs.add("%" + formePharmaceutique + "%");
        selectionArgs.add("%" + titulaires + "%");
        selectionArgs.add("%" + denominationSubstance + "%");
        selectionArgs.add(dateAutorisation);
        SQLiteDatabase db = this.getReadableDatabase();
        String finSQL = "";
        // String Sql_nbmolecule ="" ;

        if (!voiesAdmin.equals(PREMIERE_VOIE)) {
            finSQL = "AND  Voie_dadministration LIKE ?";
            selectionArgs.add("%" + voiesAdmin + "%");
        }
        String SQLSubstance = "SELECT CODE_CIS FROM CIS_COMPO_bdpm WHERE replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(upper(Denomination_substance), 'Â','A'),'Ä','A'),'À','A'),'É','E'),'Á','A'),'Ï','I'), 'Ê','E'),'È','E'),'Ô','O'),'Ü','U'), 'Ç','C' ) LIKE ?";
//String SQLSubstance = "SELECT CODE_CIS FROM CIS_COMPO_bdpm WHERE Denomination_substance COLLATE latin1_general_cs_ai LIKE ?" ;

        // La requête SQL de recherche
        String query = "SELECT *,(select count(*) from CIS_COMPO_bdpm c where c.Code_CIS=m.Code_CIS) as nb_molecule FROM CIS_bdpm m  WHERE " +
                "Denomination LIKE ? AND " +
                "Forme_pharmaceutique LIKE ? AND " +
                "Titulaire LIKE ? AND " +
                "Code_CIS IN (" + SQLSubstance + ") " +
               // " AND strftime('%Y',Date_dAMM_2 ) > ? " + // recherche par annee
                 " AND Date_dAMM_2  >= ?  " + // recherche par date exact
                finSQL;

        // Les valeurs à remplacer dans la requête

        Log.d("SQL",query);
        Cursor cursor = db.rawQuery(query, selectionArgs.toArray(new String[0]));


        if (cursor.moveToFirst()) {
            do {
                // Récupérer les valeurs de la ligne actuelle
                int codeCIS = cursor.getInt(cursor.getColumnIndex("Code_CIS"));
                String denominationMedicament = cursor.getString(cursor.getColumnIndex("Denomination"));
                String formePharmaceutiqueMedicament = cursor.getString(cursor.getColumnIndex("Forme_pharmaceutique"));
                String voiesAdminMedicament = cursor.getString(cursor.getColumnIndex("Voie_dadministration"));
                String titulairesMedicament = cursor.getString(cursor.getColumnIndex("Titulaire"));
                String statutAdministratif = cursor.getString(cursor.getColumnIndex("Statut_administratif_AMM"));
                dateAutorisation = cursor.getString(cursor.getColumnIndex("Date_dAMM_2"));
                String CountMolecule = cursor.getString(cursor.getColumnIndex("nb_molecule"));


                // Créer un objet Medicament avec les valeurs récupérées
                Medicament medicament = new Medicament();
                medicament.setCodeCIS(codeCIS);
                medicament.setDenomination(denominationMedicament);
                medicament.setFormePharmaceutique(formePharmaceutiqueMedicament);
                medicament.setVoiesAdmin(voiesAdminMedicament);
                medicament.setTitulaires(titulairesMedicament);
                medicament.setStatutAdministratif(statutAdministratif);
                medicament.setDateAutorisation(dateAutorisation);
                // medicament.setNb_molecule(CountMolecule.toString());
                medicament.setNb_molecule(String.valueOf(getNombreMolecules(codeCIS)));
                // Ajouter l'objet Medicament à la liste
                medicamentList.add(medicament);
            } while (cursor.moveToNext());

        }

        cursor.close();
        db.close();

        return medicamentList;
    }
    public int getNombreMolecules(int codeCIS) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(*) from CIS_COMPO_bdpm  where Code_CIS=?", new String[]{String.valueOf(codeCIS)});
        cursor.moveToFirst();
        int nb = cursor.getInt(0);
        return (nb);
    }
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