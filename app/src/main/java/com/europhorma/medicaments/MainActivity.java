package com.europhorma.medicaments;

// Importation des classes nécessaires
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.europhorma.medicaments.DatabaseHelper;
import com.europhorma.medicaments.R;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    // Déclaration des variables
    DatabaseHelper dbm;
    ListView listview;
    Spinner spinnerVoiesAdmin;
    Button btnSearch, btnLogout;
    EditText editTextDenominationSubstance, editTextTitulaires, editTextFormePharmaceutique,
            editTextDenomination, editTextDateAutorisation;
    ListView listViewResults;

    // Constantes pour la gestion de la session utilisateur
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_USER_NAME = "username";
    private static final String KEY_USER_STATUS = "userStatus";
    private static final String USER_STATUS_OK = "Authentifié";

    // Méthode pour supprimer les données de connexion
    private void resetUserStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_STATUS);
        editor.remove(KEY_USER_NAME);
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Si l'utilisateur n'est pas connecté, on le redirige vers l'écran de connexion
        if (!isUserAuthenticated()) {
            Intent intent = new Intent(this, Authentification.class);
            startActivity(intent);
            finish(); // ferme l'activité actuelle
            return;
        }

        setContentView(R.layout.activity_main);

        // Configuration du bouton de déconnexion
        Button btnLogout  = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmationDialog();
            }
        });

        // Initialisation de la base de données
        dbm = new DatabaseHelper(this);

        // Initialisation des éléments de l'interface
        spinnerVoiesAdmin = findViewById(R.id.spinnerVoiesAdmin);
        btnSearch = findViewById(R.id.btnSearch);
        editTextDenominationSubstance = findViewById(R.id.editTextDenominationSubstance);
        editTextTitulaires = findViewById(R.id.editTextTitulaires);
        editTextDenomination = findViewById(R.id.editTextDenomination);
        editTextFormePharmaceutique = findViewById(R.id.editTextFormePharmaceutique);
        editTextDateAutorisation = findViewById(R.id.editTextDateAutorisation);
        listViewResults = findViewById(R.id.listViewResults);

        // Remplissage du spinner avec les voies d'administration
        setupVoiesAdminSpinner();

        // Action au clic du bouton "Rechercher"
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch();
            }
        });

        // Action au clic sur un médicament : affichage de la composition
        listViewResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Medicament selectedMedicament = (Medicament) adapterView.getItemAtPosition(position);
                afficherCompositionMedicament(selectedMedicament);
            }
        });
    }

    // Remplit le spinner avec les voies d'administration distinctes
    private void setupVoiesAdminSpinner() {
        List<String> voiesAdminList = dbm.getDistinctVoiesAdmin();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, voiesAdminList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVoiesAdmin.setAdapter(spinnerAdapter);
    }

    // Lance la recherche des médicaments selon les critères
    private void performSearch() {
        String denomination = editTextDenomination.getText().toString().trim();
        String formePharmaceutique = editTextFormePharmaceutique.getText().toString().trim();
        String titulaires = editTextTitulaires.getText().toString().trim();
        String denominationSubstance = removeAccents(editTextDenominationSubstance.getText().toString().trim());
        String dateAutorisation = editTextDateAutorisation.getText().toString().trim();
        String voiesAdmin = spinnerVoiesAdmin.getSelectedItem().toString();

        cacherClavier(); // Ferme le clavier après clic

        // Récupère les résultats depuis la BDD
        List<Medicament> searchResults = dbm.searchMedicaments(denomination, formePharmaceutique, titulaires, denominationSubstance, voiesAdmin, dateAutorisation);

        // Affiche les résultats dans la ListView via un adapter personnalisé
        MedicamentAdapter medicamentAdapter = new MedicamentAdapter(this, searchResults);
        listViewResults.setAdapter(medicamentAdapter);

        // Gère les clics sur le bouton Composition
        medicamentAdapter.setOnButtonCClickListener(new MedicamentAdapter.OnButtonCClickListener() {
            @Override
            public void onButtonCClick(Medicament medicament) {
                afficherCompositionMedicament(medicament);
            }
        });

        // Gère les clics sur le bouton Présentation
        medicamentAdapter.setOnButtonPClickListener(new MedicamentAdapter.OnButtonPClickListener() {
            @Override
            public void onButtonPClick(Medicament medicament) {
                afficherPresentationMedicament(medicament);
            }
        });
    }

    // Cache le clavier virtuel
    private void cacherClavier() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View vueCourante = getCurrentFocus();
        if (vueCourante != null) {
            imm.hideSoftInputFromWindow(vueCourante.getWindowToken(), 0);
        }
    }

    // Supprime les accents d’une chaîne de caractères (pour éviter les erreurs de recherche)
    private String removeAccents(String input) {
        if (input == null) return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
    }

    // Vérifie si l'utilisateur est connecté
    private boolean isUserAuthenticated() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String userStatus = preferences.getString(KEY_USER_STATUS, "");
        return USER_STATUS_OK.equals(userStatus);
    }

    // Affiche une boîte de confirmation pour la déconnexion
    void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
                .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logout(); // Déconnecte
                    }
                })
                .setNegativeButton("Non", null)
                .show();
    }

    // Supprime les infos de session et redirige vers l'authentification
    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_STATUS);
        editor.remove(KEY_USER_NAME);
        editor.apply();

        Intent LogoutIntent = new Intent(MainActivity.this, Authentification.class);
        startActivity(LogoutIntent);
        finish();
    }

    // Affiche une boîte avec la composition d’un médicament
    private void afficherCompositionMedicament(Medicament medicament) {
        List<String> composition = dbm.getCompositionMedicament(medicament.getCodeCIS());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Composition de " + medicament.getCodeCIS());

        StringBuilder compositionText = new StringBuilder();
        if (composition.isEmpty()) {
            compositionText.append("aucune composition disponible pour ce médicament.\n");
        } else {
            for (String item : composition) {
                compositionText.append(item).append("\n");
            }
        }

        builder.setMessage(compositionText.toString());
        builder.setPositiveButton("OK", null);
        builder.create().show();
    }

    // Affiche une boîte avec les présentations d’un médicament
    private void afficherPresentationMedicament(Medicament medicament) {
        List<String> presentation = dbm.getPresentationMedicament(medicament.getCodeCIS());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Présentations de " + medicament.getCodeCIS());

        StringBuilder presentationText = new StringBuilder();
        if (presentation.isEmpty()) {
            presentationText.append("aucune présentation disponible pour ce médicament.\n");
        } else {
            for (String item : presentation) {
                presentationText.append(item).append("\n");
            }
        }

        builder.setMessage(presentationText.toString());
        builder.setPositiveButton("OK", null);
        builder.create().show();
    }
}
