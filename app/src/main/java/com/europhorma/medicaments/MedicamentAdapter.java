package com.europhorma.medicaments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 * Adaptateur personnalisé permettant d'afficher les objets Medicament dans une ListView.
 * Chaque ligne affiche les informations d'un médicament ainsi que deux boutons d'action.
 */
public class MedicamentAdapter extends ArrayAdapter<Medicament> {

    // Déclaration des écouteurs pour les boutons "Compo" et "Présentation"
    private OnButtonCClickListener onButtonCClickListener;
    private OnButtonPClickListener onButtonPClickListener;

    // Interface utilisée pour gérer les clics sur le bouton "Compo"
    public interface OnButtonCClickListener {
        void onButtonCClick(Medicament medicament);
    }

    // Interface utilisée pour gérer les clics sur le bouton "Présentation"
    public interface OnButtonPClickListener {
        void onButtonPClick(Medicament medicament);
    }

    // Méthode permettant de définir l'écouteur du bouton "Compo"
    public void setOnButtonCClickListener(OnButtonCClickListener listener) {
        this.onButtonCClickListener = listener;
    }

    // Méthode permettant de définir l'écouteur du bouton "Présentation"
    public void setOnButtonPClickListener(OnButtonPClickListener listener) {
        this.onButtonPClickListener = listener;
    }

    // Constructeur de l'adaptateur, appelant le constructeur de la classe parente (ArrayAdapter)
    public MedicamentAdapter(Context context, List<Medicament> medicaments) {
        super(context, 0, medicaments); // 0 car on utilise un layout personnalisé
    }

    /**
     * Méthode appelée automatiquement pour chaque élément de la liste.
     * Elle lie les données de l'objet Medicament à la vue (ligne) correspondante.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Récupère l'objet Medicament à la position actuelle
        Medicament medicament = getItem(position);

        // Si la vue n'existe pas encore, on l'inflète depuis le fichier XML
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_medicament, parent, false);
        }

        // Récupération des composants de l'interface utilisateur de la ligne
        TextView tvCodeCIS = convertView.findViewById(R.id.tvCodeCIS);
        TextView tvDenomination = convertView.findViewById(R.id.tvDenomination);
        TextView tvFormePharmaceutique = convertView.findViewById(R.id.tvFormePharmaceutique);
        TextView tvVoiesAdmin = convertView.findViewById(R.id.tvVoiesAdmin);
        TextView tvTitulaires = convertView.findViewById(R.id.tvTitulaires);
        TextView tvStatutadministratif = convertView.findViewById(R.id.tvStatutAdministratif);
        TextView tvNb_Molecule = convertView.findViewById(R.id.nb_molecule);
        TextView tvDateAutorisation = convertView.findViewById(R.id.tvDateAutorisation);
        Button tvCompo = convertView.findViewById(R.id.tvCompo);
        Button tvPresentation = convertView.findViewById(R.id.tvPresentation);

        // Remplissage des champs TextView avec les informations du médicament
        tvCodeCIS.setText("CIS: " + String.valueOf(medicament.getCodeCIS()));
        tvDenomination.setText("Dénomination : " + medicament.getDenomination());
        tvFormePharmaceutique.setText("Forme pharmaceutique : " + medicament.getFormePharmaceutique());
        tvVoiesAdmin.setText("Voie Administration : " + medicament.getVoiesAdmin());
        tvTitulaires.setText("Fabricant : " + medicament.getTitulaires());
        tvStatutadministratif.setText("Statut Administratif : " + medicament.getStatutAdministratif());
        tvDateAutorisation.setText("Date d'autorisation : " + medicament.getDateAutorisation());

        // Affiche le nombre de molécules avec gestion du pluriel
        tvNb_Molecule.setText("Nombre de molécule(s) : " + medicament.getNb_molecule() + pluriels(Integer.parseInt(medicament.getNb_molecule()), " molécule"));

        // Définition du comportement lorsqu'on clique sur le bouton "Compo"
        tvCompo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Si un écouteur a été défini, on exécute sa méthode
                if (onButtonCClickListener != null && medicament != null) {
                    onButtonCClickListener.onButtonCClick(medicament);
                }
            }
        });

        // Définition du comportement lorsqu'on clique sur le bouton "Présentation"
        tvPresentation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onButtonPClickListener != null && medicament != null) {
                    onButtonPClickListener.onButtonPClick(medicament);
                }
            }
        });

        // Applique une couleur de fond différente selon la position (pair/impair) pour améliorer la lisibilité
        int backgroundColor = (position % 2 == 0)
                ? getContext().getResources().getColor(R.color.colorLight)
                : getContext().getResources().getColor(R.color.colorDark);
        convertView.setBackgroundColor(backgroundColor);

        // Retourne la vue complète avec les données insérées
        return convertView;
    }

    /**
     * Méthode utilitaire pour ajouter un "s" à un mot si le nombre est supérieur à 1.
     * @param nbr Le nombre associé.
     * @param mot Le mot auquel ajouter un "s" si nécessaire.
     * @return Le mot au pluriel si besoin.
     */
    static String pluriels(int nbr, String mot) {
        String un_s = "";
        if (nbr > 1) {
            un_s = "s";
        }
        return (mot + un_s);
    }
}
