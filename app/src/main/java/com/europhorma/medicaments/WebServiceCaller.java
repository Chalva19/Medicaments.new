package com.europhorma.medicaments;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;

import javax.net.ssl.HttpsURLConnection;

public class WebServiceCaller {

    private final Context context;

    // Constructeur
    public WebServiceCaller(Context context) {
        this.context = context;
    }

    // Interface pour le callback
    public interface WebServiceCallback {
        void onSuccess(String jsonResponse);
        void onError(String errorMessage);
    }

    /**
     * Méthode pour appeler un webservice PHP en HTTPS avec paramètres GET
     * @param url URL de base du webservice
     * @param key clé d'API
     * @param codeVisiteur code du visiteur
     * @param cleAuthent clé d'authentification
     * @param callback callback pour gérer la réponse
     */
    public void appelWebService(String url, String key, String codeVisiteur, String cleAuthent, WebServiceCallback callback) {
        // Ajout des paramètres à l'URL
        String urlComplete = url + "?key=" + Uri.encode(key) +
                "&codeVisiteur=" + Uri.encode(codeVisiteur) +
                "&cleAuthent=" + Uri.encode(cleAuthent);

        // Création d'une nouvelle requête
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Création de l'URL
                    URL urlObj = new URL(urlComplete);

                    // Ouverture de la connexion
                    HttpsURLConnection connection = (HttpsURLConnection) urlObj.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    // Vérification du code de réponse
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        // Lecture de la réponse
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        // Récupération du JSON
                        final String jsonResponse = response.toString();

                        // Retour du résultat sur le thread UI
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // Appel du callback avec la réponse
                                if (callback != null) {
                                    callback.onSuccess(jsonResponse);
                                }
                            }
                        });

                    } else {
                        // Gestion des erreurs
                        final String errorMessage = "Erreur HTTP: " + responseCode;
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onError(errorMessage);
                                }
                            }
                        });
                    }

                    // Fermeture de la connexion
                    connection.disconnect();

                } catch (Exception e) {
                    final String errorMessage = "Erreur: " + e.getMessage();
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onError(errorMessage);
                            }
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
