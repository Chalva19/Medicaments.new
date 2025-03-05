package com.europhorma.medicaments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.security.SecureRandom;

public class Authentification extends AppCompatActivity {
    // Déclaration des constantes pour les clés SharedPreferences
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_USER_NAME = "username";
    private static final String KEY_USER_STATUS = "userStatus";
    private static final String USER_STATUS_OK = "Authentifié";

    private EditText editTextCodeVisiteur, editTextCleTemporaire, editTextUsername;
    private Button btnEnvoyerCode, btnValiderCle;
    private TextView textViewInfo;
    private String SecureKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_authentification);

        // Initialisation des vues
        editTextCodeVisiteur = findViewById(R.id.codeVisiteur);
        editTextCleTemporaire = findViewById(R.id.codeTemporaire);
        btnEnvoyerCode = findViewById(R.id.ok);
        btnValiderCle = findViewById(R.id.envoye);
        editTextUsername = findViewById(R.id.nomVisiteur);


        // Masquer la deuxième partie au démarrage
        editTextCleTemporaire.setVisibility(View.GONE);
        btnValiderCle.setVisibility(View.GONE);

        btnEnvoyerCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                afficherDeuxiemePartie();

            }

        });
// Bouton pour valider la clé temporaire et authentifier l'utilisateur
        btnValiderCle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickOk(v);
            }
        });


    }    private String generateRandomCode() {
        // Caractères possibles dans le code
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        // Longueur du code souhaitée
        int codeLength = 12;

        // Utilisation de SecureRandom pour une génération sécurisée
        SecureRandom random = new SecureRandom();

        // StringBuilder pour construire le code
        StringBuilder codeBuilder = new StringBuilder(codeLength);

        // Boucle pour construire le code caractère par caractère
        for (int i = 0; i < codeLength; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            codeBuilder.append(randomChar);
        }


        // Retourne le code généré
        return codeBuilder.toString();
    }
    private void setUserStatus(String status) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_STATUS, status);
        // editor.putString("NOM",)
        editor.apply();
    }
    private void setUserName(String lenom) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_NAME, lenom);
        // editor.putString("NOM",)
        editor.apply();
    }

    /**
     * Afficher la deuxième partie après saisie du code visiteur.
     */
    private void afficherDeuxiemePartie() {
        String codeVisiteur = editTextCodeVisiteur.getText().toString().trim();

        if (codeVisiteur.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer votre code visiteur", Toast.LENGTH_SHORT).show();
            return;
        }
        SecureKey = generateRandomCode();
        // Affichage de la deuxième partie
        //editTextCleTemporaire.setVisibility(View.VISIBLE);
        //editTextUsername.setVisibility(View.VISIBLE);
        LinearLayout temporaryCodeLayout = findViewById(R.id.temporaryCodeLayout);
        temporaryCodeLayout.setVisibility(View.VISIBLE);
        editTextCleTemporaire.setVisibility(View.VISIBLE);  // Rend le champ non modifiable
        editTextCleTemporaire.setEnabled(false);    // Désactive l'édition
        editTextCleTemporaire.setText(SecureKey);

        btnValiderCle.setVisibility(View.VISIBLE);

    }
    public void clickOk(View v) {
        Log.d("Authentification", "Méthode clickOk appelée");
        // String str1 = secureKey;
        String str2 =  editTextCleTemporaire.getText().toString();

        Log.d("Authentification", "Clé Sécurisée : " + SecureKey);
        Log.d("Authentification", "Clé Saisie : " + str2);
        if (SecureKey.equals(str2)) {
            String status1 = USER_STATUS_OK;
            setUserName(editTextUsername.getText().toString());
            setUserStatus(status1);
            // Log.d("COMPARE", "OK");
            Log.d("Authentification", "Authentification réussie");
            Toast toast = Toast.makeText(this, "Authentification réussie", Toast.LENGTH_LONG);
            toast.show();

            Intent authIntent = new Intent(this, MainActivity.class);
            startActivity(authIntent);
            finish();
        } else {
            Log.d("Authentification", "Échec de l'authentification");
            Toast toast = Toast.makeText(this, "Identifiant ou code incorrect", Toast.LENGTH_LONG);
            toast.show();
        }
    }


}
