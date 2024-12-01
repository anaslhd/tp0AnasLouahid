package ma.emsi.anaslouahid.tp0anaslouahid.jsf;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Backing bean pour la page JSF index.xhtml.
 * Portée view pour conserver l'état de la conversation pendant plusieurs requêtes HTTP.
 */
@Named
@ViewScoped
public class Bb implements Serializable {

    /**
     * Rôle "système" que l'on attribuera plus tard à un LLM.
     * Possible d'ajouter de nouveaux rôles dans la méthode getSystemRoles.
     */
    private String systemRole;
    /**
     * Quand le rôle est choisi par l'utilisateur dans la liste déroulante,
     * il n'est plus possible de le modifier (voir code de la page JSF) dans la même session de chat.
     */
    private boolean systemRoleChangeable = true;

    /**
     * Dernière question posée par l'utilisateur.
     */
    private String question;
    /**
     * Dernière réponse de l'API OpenAI.
     */
    private String reponse;
    /**
     * La conversation depuis le début.
     */
    private StringBuilder conversation = new StringBuilder();

    /**
     * Contexte JSF. Utilisé pour qu'un message d'erreur s'affiche dans le formulaire.
     */
    @Inject
    private FacesContext facesContext;

    /**
     * Obligatoire pour un bean CDI (classe gérée par CDI).
     */
    public Bb() {
    }

    public String getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    public boolean isSystemRoleChangeable() {
        return systemRoleChangeable;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    /**
     * setter indispensable pour le textarea.
     *
     * @param reponse la réponse à la question.
     */
    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(conversation);
    }

    /**
     * Envoie la question au serveur.
     * En attendant de l'envoyer à un LLM, le serveur fait un traitement quelconque, juste pour tester :
     * Le traitement consiste à copier la question en minuscules et à l'entourer avec "||". Le rôle système
     * est ajouté au début de la première réponse.
     *
     * @return null pour rester sur la même page.
     */
    public String envoyer() {
        if (question == null || question.isBlank()) {
            // Gestion de l'erreur si le texte est vide
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Texte question vide", "Il manque le texte de la question");
            facesContext.addMessage(null, message);
            return null;
        }

        // Initialisation de la réponse avec les délimiteurs
        this.reponse = "||";

        // Ajouter le rôle système si la conversation commence
        if (this.conversation.isEmpty()) {
            this.reponse += systemRole.toUpperCase(Locale.FRENCH) + "\n";
            this.systemRoleChangeable = false;
        }

        this.reponse += question.toLowerCase(Locale.FRENCH) + "||";

        // Détails ASCII
        StringBuilder asciiDetails = new StringBuilder("\n--- Détails ASCII ---\n");
        int totalAsciiSum = 0;

        String[] words = question.split("\\s+"); // Divise la question en mots
        for (String word : words) {
            int wordAsciiSum = 0;
            asciiDetails.append("Mot : ").append(word).append("\n");

            for (char c : word.toCharArray()) {
                int asciiValue = (int) c;
                wordAsciiSum += asciiValue;
                asciiDetails.append("  Lettre : '").append(c).append("' => Code ASCII : ").append(asciiValue).append("\n");
            }

            asciiDetails.append("  Somme ASCII du mot : ").append(wordAsciiSum).append("\n");
            totalAsciiSum += wordAsciiSum;
        }

        asciiDetails.append("---\n")
                .append("Somme ASCII totale de la phrase : ").append(totalAsciiSum).append("\n");

        // Ajouter les détails ASCII à la réponse
        this.reponse += asciiDetails;

        // Afficher la conversation complète
        afficherConversation();

        return null;
    }




    /**
     * Pour un nouveau chat.
     * Termine la portée view en retournant "index" (la page index.xhtml sera affichée après le traitement
     * effectué pour construire la réponse) et pas null. null aurait indiqué de rester dans la même page (index.xhtml)
     * sans changer de vue.
     * Le fait de changer de vue va faire supprimer l'instance en cours du backing bean par CDI et donc on reprend
     * tout comme au début puisqu'une nouvelle instance du backing va être utilisée par la page index.xhtml.
     * @return "index"
     */
    public String nouveauChat() {
        return "index";
    }

    /**
     * Pour afficher la conversation dans le textArea de la page JSF.
     */
    private void afficherConversation() {
        this.conversation.append("== User:\n").append(question).append("\n== Serveur:\n").append(reponse).append("\n");
    }

    public List<SelectItem> getSystemRoles() {
        List<SelectItem> listeSystemRoles = new ArrayList<>();
        // Ces rôles ne seront utilisés que lorsque la réponse sera données par un LLM.
        String role = """
                You are a helpful assistant. You help the user to find the information they need.
                If the user type a question, you answer it.
                """;
        listeSystemRoles.add(new SelectItem(role, "Assistant"));
        role = """
                You are an interpreter. You translate from English to French and from French to English.
                If the user type a French text, you translate it into English.
                If the user type an English text, you translate it into French.
                If the text contains only one to three words, give some examples of usage of these words in English.
                """;
        // 1er argument : la valeur du rôle, 2ème argument : le libellé du rôle
        listeSystemRoles.add(new SelectItem(role, "Traducteur Anglais-Français"));
        role = """
                Your are a travel guide. If the user type the name of a country or of a town,
                you tell them what are the main places to visit in the country or the town
                are you tell them the average price of a meal.
                """;
        listeSystemRoles.add(new SelectItem(role, "Guide touristique"));
        // Présélectionne le premier rôle de la liste.
                this.systemRole = (String) listeSystemRoles.getFirst().getValue();
        return listeSystemRoles;
    }
}
