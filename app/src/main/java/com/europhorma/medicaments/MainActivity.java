package com.europhorma.medicaments;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.europhorma.medicaments.DatabaseHelper;
import com.europhorma.medicaments.R;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {


    /*MedicamentAdapter adapter;*/
    DatabaseHelper dbm;
    ListView listview;
    Spinner spinnerVoiesAdmin;
    Button btnSearch;
    Button btnLogout;
    EditText editTextDenominationSubstance;
    EditText editTextTitulaires;
    EditText editTextFormePharmaceutique;
    EditText editTextDenomination;
    EditText editTextDateAutorisation;
    ListView listViewResults;
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_USER_NAME = "username";
    private static final String KEY_USER_STATUS = "userStatus";
    private static final String USER_STATUS_OK = "Authentifié";

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
        if (!isUserAuthenticated()) {
            // Si l'utilisateur n'est pas authentifié, redirigez vers l'écran de connexion
            Intent intent = new Intent(this, Authentification.class);
            startActivity(intent);
            finish(); // Ferme l'activité courante
            return;
        }
        setContentView(R.layout.activity_main);
        Button btnLogout  = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmationDialog();
            }
        });

        dbm = new DatabaseHelper(this);

        // Initialisation du Spinner
        spinnerVoiesAdmin = findViewById(R.id.spinnerVoiesAdmin);
        btnSearch = findViewById(R.id.btnSearch);
        editTextDenominationSubstance = findViewById(R.id.editTextDenominationSubstance);
        editTextTitulaires = findViewById(R.id.editTextTitulaires);
        editTextDenomination = findViewById(R.id.editTextDenomination);
        editTextFormePharmaceutique = findViewById(R.id.editTextFormePharmaceutique);
        editTextDateAutorisation = findViewById(R.id.editTextDateAutorisation);
        listViewResults =  findViewById(R.id.listViewResults);
        setupVoiesAdminSpinner();
        //performSearch();
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch();
            }
        });
        listViewResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Get the selected item
                Medicament selectedMedicament = (Medicament) adapterView.getItemAtPosition(position);
                // Show composition of the selected medicament
                afficherCompositionMedicament(selectedMedicament);
            }
        });
    }

    private void setupVoiesAdminSpinner() {
        List<String> voiesAdminList = dbm.getDistinctVoiesAdmin();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, voiesAdminList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVoiesAdmin.setAdapter(spinnerAdapter);
    }
    private void performSearch() {
        // TODO: Implement the search logic using the entered criteria and update the ListView
        String denomination = editTextDenomination.getText().toString().trim();
        String formePharmaceutique = editTextFormePharmaceutique.getText().toString().trim();
        String titulaires = editTextTitulaires.getText().toString().trim();
        String denominationSubstance = removeAccents(editTextDenominationSubstance.getText().toString().trim());
        String dateAutorisation = editTextDateAutorisation.getText().toString().trim();
        String voiesAdmin = spinnerVoiesAdmin.getSelectedItem().toString();
        MedicamentAdapter medicamentAdapter;
        cacherClavier();

        // TODO: Use dbHelper to fetch search results and update the ListView
        List<Medicament> searchResults = dbm.searchMedicaments(denomination, formePharmaceutique, titulaires, denominationSubstance, voiesAdmin, dateAutorisation);
//dbHelper.writeToLogFile("test");
        // TODO: Create and set an adapter for the ListView to display search results
        medicamentAdapter = new MedicamentAdapter(this, searchResults);
        listViewResults.setAdapter(medicamentAdapter);
        medicamentAdapter.setOnButtonCClickListener(new MedicamentAdapter.OnButtonCClickListener() {
            @Override
            public void onButtonCClick(Medicament medicament) {
                // Votre logique ici
                afficherCompositionMedicament(medicament);
            }
        });
        medicamentAdapter.setOnButtonPClickListener(new MedicamentAdapter.OnButtonPClickListener() {
            @Override
            public void onButtonPClick(Medicament medicament) {
                // Votre logique ici
                afficherPresentationMedicament(medicament);
            }
        });
    }

    private void cacherClavier() {
        // Obtenez le gestionnaire de fenêtre
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Obtenez la vue actuellement focalisée, qui devrait être la vue avec le clavier
        View vueCourante = getCurrentFocus();

        // Vérifiez si la vue est non nulle pour éviter les erreurs
        if (vueCourante != null) {
            // Masquez le clavier
            imm.hideSoftInputFromWindow(vueCourante.getWindowToken(), 0);
        }
    }
    private String getEditTextValue(int editTextId) {
        EditText editText = findViewById(editTextId);
        return (editText.getText() != null) ? editText.getText().toString().trim() : "";
    }
    private String removeAccents(String input) {
        if (input == null) {
            return null;
        }

        // Normalisation en forme de décomposition (NFD)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // Remplacement des caractères diacritiques
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
    }
    private boolean isUserAuthenticated() {

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String userStatus = preferences.getString(KEY_USER_STATUS, "");

        // Vérifiez si la chaîne d'état de l'utilisateur est "authentification=OK"
        return USER_STATUS_OK.equals(userStatus);
    }
    void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
                .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Logique de déconnexion
                        logout();
                    }
                })
                .setNegativeButton("Non", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Annuler la déconnexion
                        dialog.dismiss();
                    }
                })
                .show();
    }
    private void logout() {
        // Effacer le statut de connexion
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_STATUS);
        editor.remove(KEY_USER_NAME);
        editor.apply();

        // Rediriger vers l'écran d'authentification
        Intent LogoutIntent = new Intent(MainActivity.this,Authentification.class);
        startActivity(LogoutIntent);
        finish(); // Ferme l'activité actuelle
    }

    private void afficherCompositionMedicament(Medicament medicament) {
        List<String> composition = dbm.getCompositionMedicament(medicament.getCodeCIS());
        //List<String> presentation = dbm.getPresentationMedicament(medicament.getCodeCIS());

        // Afficher la composition du médicament dans une boîte de dialogue ou autre méthode d'affichage
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Composition de " + medicament.getCodeCIS());
        StringBuilder compositionText = new StringBuilder();

        if (composition.isEmpty()) {
            compositionText.append("aucune composition disponible pour ce médicament.").append("\n");
        } else {

            for (String item : composition) {
                compositionText.append(item).append("\n");
            }


        }



        builder.setMessage(compositionText.toString());
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void afficherPresentationMedicament(Medicament medicament) {

        List<String> presentation = dbm.getPresentationMedicament(medicament.getCodeCIS());

        // Afficher la composition du médicament dans une boîte de dialogue ou autre méthode d'affichage
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Présentations de " + medicament.getCodeCIS());
        StringBuilder presentationText = new StringBuilder();


        if (presentation.isEmpty()) {
            presentationText.append("aucune presentation disponible pour ce médicament.").append("\n");
        } else {

            for (String item : presentation) {
                presentationText.append(item).append("\n");
            }


        }
        builder.setMessage(presentationText.toString());
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}

