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

import java.security.MessageDigest;
import java.security.SecureRandom;

public class Authentification extends AppCompatActivity {

    // Constantes pour stocker les informations utilisateur dans les préférences
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_USER_NAME = "username";
    private static final String KEY_USER_STATUS = "userStatus";
    private static final String USER_STATUS_OK = "Authentifié";
    private static final String SECURETOKEN = "euroforma@5785";

    // Composants de l'interface
    private EditText editTextCodeVisiteur, editTextCleTemporaire, editTextUsername;
    private Button btnEnvoyerCode, btnValiderCle;
    private TextView textViewInfo;
    private String SecureKey;  // Clé temporaire générée
    private WebServiceCaller webServiceCaller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_authentification);

        // Récupération des composants depuis la vue
        editTextCodeVisiteur = findViewById(R.id.codeVisiteur);
        editTextCleTemporaire = findViewById(R.id.codeTemporaire);
        btnEnvoyerCode = findViewById(R.id.ok);
        btnValiderCle = findViewById(R.id.envoye);
        editTextUsername = findViewById(R.id.nomVisiteur);

        // Cacher les champs de la deuxième étape par défaut
        editTextCleTemporaire.setVisibility(View.GONE);
        btnValiderCle.setVisibility(View.GONE);

        // Listener pour le bouton "Envoyer Code"
        btnEnvoyerCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                afficherDeuxiemePartie();  // Affiche la deuxième partie
            }
        });

        // Listener pour le bouton "Valider Clé"
        btnValiderCle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickOk(v);  // Valide la clé saisie
            }
        });
    }

    // Méthode pour générer une clé aléatoire de 12 caractères
    private String generateRandomCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int codeLength = 12;
        SecureRandom random = new SecureRandom();
        StringBuilder codeBuilder = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            int randomIndex = random.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

    // Sauvegarde le statut de l'utilisateur (ex: "Authentifié")
    private void setUserStatus(String status) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_STATUS, status);
        editor.apply();
    }

    // Sauvegarde le nom d'utilisateur dans les préférences
    private void setUserName(String lenom) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_NAME, lenom);
        editor.apply();
    }

    // Affiche la deuxième partie de l'authentification (clé temporaire)
    private void afficherDeuxiemePartie() {
        String codeVisiteur = editTextCodeVisiteur.getText().toString().trim();

        // Vérifie que le code visiteur a bien été saisi
        if (codeVisiteur.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer votre code visiteur", Toast.LENGTH_SHORT).show();
            return;
        }

        // Génère la clé sécurisée
        SecureKey = generateRandomCode();

        // Affiche les champs de la deuxième partie
        LinearLayout temporaryCodeLayout = findViewById(R.id.temporaryCodeLayout);
        temporaryCodeLayout.setVisibility(View.VISIBLE);
        editTextCleTemporaire.setEnabled(true);
        editTextCleTemporaire.setVisibility(View.VISIBLE);
        btnValiderCle.setVisibility(View.VISIBLE);

        // Affiche la clé dans les logs pour débogage
        Log.d("CODE", SecureKey);

        // Appel au webservice pour envoyer la clé par email
        webServiceCaller = new WebServiceCaller(this);
        appellerWebService(codeVisiteur, SecureKey);
    }

    // Méthode appelée lors du clic sur "Valider"
    public void clickOk(View v) {
        Log.d("Authentification", "Méthode clickOk appelée");

        // Récupère la clé saisie par l'utilisateur
        String str2 = editTextCleTemporaire.getText().toString();

        Log.d("Authentification", "Clé Sécurisée : " + SecureKey);
        Log.d("Authentification", "Clé Saisie : " + str2);

        // Vérifie que la clé saisie correspond à la clé générée
        if (SecureKey.equals(str2)) {
            setUserName(editTextUsername.getText().toString());
            setUserStatus(USER_STATUS_OK);
            Log.d("Authentification", "Authentification réussie");
            Toast.makeText(this, "Authentification réussie", Toast.LENGTH_LONG).show();

            // Redirige vers l’activité principale
            Intent authIntent = new Intent(this, MainActivity.class);
            startActivity(authIntent);
            finish();
        } else {
            Log.d("Authentification", "Échec de l'authentification");
            Toast.makeText(this, "Identifiant ou code incorrect", Toast.LENGTH_LONG).show();
        }
    }

    // Calcule un hash SHA-256 à partir d'une chaîne donnée
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Affiche un message dans un Toast
    private void affiche(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // Appelle le WebService avec les paramètres nécessaires
    private void appellerWebService(String CV, String SK) {
        String url = "https://auth.euroforma.site/authent2.php";
        String key = sha256(SK + CV + SECURETOKEN);

        webServiceCaller.appelWebService(
                url,
                key,
                CV,
                SK,
                new WebServiceCaller.WebServiceCallback() {
                    @Override
                    public void onSuccess(String jsonResponse) {
                        affiche(jsonResponse);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        affiche("Erreur: " + errorMessage);
                    }
                }
        );
    }
}
