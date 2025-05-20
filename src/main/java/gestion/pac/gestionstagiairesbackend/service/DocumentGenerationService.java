package gestion.pac.gestionstagiairesbackend.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import gestion.pac.gestionstagiairesbackend.entite.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DocumentGenerationService {

    @Value("${document.output-dir}")
    private String outputDir;

    @Value("${document.reference.format}")
    private String referenceFormat;

    private final ReferenceService referenceService;

    public DocumentGenerationService(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @PostConstruct
    public void init() throws IOException {
        Path outputPath = Paths.get(outputDir).toAbsolutePath();
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            System.out.println("Dossier créé : " + outputPath);
        }
    }

    private String generateReferenceNumber(User user) {
        int nextNumber = referenceService.getNextSequenceNumber();
        int currentYear = LocalDate.now().getYear() % 100;
        return String.format(referenceFormat, nextNumber, currentYear);
    }

    public String generateNoteDeService(User user) throws Exception {
        Path outputPath = Paths.get(outputDir).toAbsolutePath();
        String fileName = "NOTE_SERVICE_" + user.getNom() + "_" + user.getPrenom() + ".pdf";
        Path filePath = outputPath.resolve(fileName);

        System.out.println("Tentative d'écriture dans : " + filePath);

        if (!Files.exists(outputPath)) {
            throw new IOException("Le dossier de sortie n'existe pas: " + outputPath);
        }

        Document document = new Document();
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            PdfWriter.getInstance(document, fos);
            document.open();

            // Style des polices
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            // En-tête du document
            Paragraph portTitle = new Paragraph("PORT AUTONOME DE COTONOU", titleFont);
            portTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(portTitle);

            Paragraph direction = new Paragraph("DIRECTION GENERALE", headerFont);
            direction.setAlignment(Element.ALIGN_CENTER);
            document.add(direction);

            Paragraph daf = new Paragraph("DIRECTION DE L'ADMINISTRATION ET DES FINANCES", headerFont);
            daf.setAlignment(Element.ALIGN_CENTER);
            document.add(daf);

            Paragraph drh = new Paragraph("DEPARTEMENT DES RESSOURCES HUMAINES", headerFont);
            drh.setAlignment(Element.ALIGN_CENTER);
            document.add(drh);

            // Ajout d'espace
            document.add(new Paragraph(" "));

            // Référence de la note
            Paragraph reference = new Paragraph("NOTE DE SERVICE " + generateReferenceNumber(user), normalFont);
            reference.setAlignment(Element.ALIGN_CENTER);
            document.add(reference);

            Paragraph objet = new Paragraph("Portant stage académique de " + user.getCivilite() + " " + user.getNom() + " " + user.getPrenom() + ".", normalFont);
            objet.setAlignment(Element.ALIGN_CENTER);
            document.add(objet);

            // Ajout d'espace
            document.add(new Paragraph(" "));

            // Corps du document
            String stageInfo = String.format(
                    "    %s %s %s, étudiante en %s à %s dans la filière %s, est autorisée à effectuer un stage académique non rémunéré et non renouvelable au Port Autonome de Cotonou du %s au %s.\n",
                    user.getCivilite(),
                    user.getNom(),
                    user.getPrenom(),
                    user.getAnneeAcademique(),
                    user.getNomEtablissement(),
                    user.getFiliere(),
                    user.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    user.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );

            Paragraph content = new Paragraph(stageInfo, normalFont);
            content.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(content);

            // Encadrement et rapport
            Paragraph encadrement = new Paragraph(
                    "    Le Chef du Département des Systèmes d'Information est chargé de l'encadrement de cette stagiaire qui devra produire un rapport à la fin de son stage.\n\n",
                    normalFont
            );
            encadrement.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(encadrement);

            // Signature
            Paragraph dateAndSignature = new Paragraph();

            Phrase firstLine = new Phrase();
            firstLine.add(new Chunk("COTONOU LE   ", normalFont)); // Espacement après la date
            dateAndSignature.add(firstLine);
            dateAndSignature.add(Chunk.NEWLINE);

            dateAndSignature.add("POUR LE DIRECTEUR GENERAL,");
            dateAndSignature.add(Chunk.NEWLINE);

            // Deuxième ligne
            dateAndSignature.add("DU PORT AUTONOME DE COTONOU & P.D.");
            dateAndSignature.add(Chunk.NEWLINE);

            // Troisième ligne
            dateAndSignature.add("LA CHEFFE DU DEPARTEMENT,");
            dateAndSignature.add(Chunk.NEWLINE);

            dateAndSignature.add("DES RESSOURCES HUMAINES,\n\n\n");
            dateAndSignature.add(Chunk.NEWLINE);
            dateAndSignature.add(Chunk.NEWLINE);

            // Signature soulignée
            Chunk signatureName = new Chunk("Arias Frosine ADANHODE GBAGUIDI", normalFont);
            signatureName.setUnderline(0.2f, -2f); // Paramètres pour le soulignement
            dateAndSignature.add(signatureName);

            document.add(dateAndSignature);

            // Ajout d'espace avant les ampliations
            document.add(new Paragraph("\n\n\n"));

            // Ampliations sans tableau (alignées à gauche)
            Paragraph ampliations = new Paragraph();
            ampliations.setAlignment(Element.ALIGN_LEFT);
            ampliations.add(new Paragraph("AMPLIATIONS", normalFont));
            ampliations.add(new Paragraph("Directions 9 Arch 1", normalFont));
            ampliations.add(new Paragraph("Départ 16 Aff 1", normalFont));
            ampliations.add(new Paragraph("Synd 1 Int 1", normalFont));
            ampliations.add(new Paragraph("DP 1 ", normalFont));

            document.add(ampliations);

            // Création d'un tableau avec 1 colonne pour appliquer un fond
            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(100); // Prend toute la largeur
            footerTable.setTotalWidth(document.getPageSize().getWidth()); // S'assure que le tableau couvre toute la page

            // Définir la hauteur de la table pour être en bas
            footerTable.setSpacingBefore(50f); // Décale la table vers le bas, ajustez si nécessaire

            // Texte du contact
            Paragraph contactText = new Paragraph(
                    "Direction Générale du Port Autonome de Cotonou\n" +
                            "BP 927 Boulevard de la Marina, Cotonou - Bénin\n" +
                            "+229 21 31 52 80 contact@pac.bj\n" +
                            "www.portdecotonou.bj",
                    normalFont
            );
            contactText.setAlignment(Element.ALIGN_CENTER);

            // Création de la cellule avec fond bleu
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(new Color(0, 51, 153)); // Bleu foncé
            cell.setPadding(10);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.addElement(contactText);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerTable.addCell(cell);

            // Ajout au document
            document.add(footerTable);

            // Fermeture du document
            document.close();

            return filePath.toString();
        }
    }

    public String generateDemandeStage(User user) throws Exception {
        Path outputPath = Paths.get(outputDir).toAbsolutePath();
        String fileName = "DEMANDE_STAGE_" + user.getNom() + "_" + user.getPrenom() + ".pdf";
        Path filePath = outputPath.resolve(fileName);

        Document document = new Document();
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            PdfWriter.getInstance(document, fos);
            document.open();

            // Style des polices
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            // En-tête du document
            Paragraph portTitle = new Paragraph("PORT AUTONOME DE COTONOU", titleFont);
            portTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(portTitle);

            Paragraph direction = new Paragraph("DIRECTION GENERALE", headerFont);
            direction.setAlignment(Element.ALIGN_CENTER);
            document.add(direction);

            // Date et destinataire
            Paragraph date = new Paragraph("COTONOU, le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n\n", normalFont);
            date.setAlignment(Element.ALIGN_RIGHT);
            document.add(date);

            Paragraph destinataire = new Paragraph();
            destinataire.add(new Chunk("A\n", normalFont));
            destinataire.add(new Chunk(user.getCivilite() + " " + user.getNom() + " " + user.getPrenom() + "\n", boldFont));
            destinataire.add(new Chunk("Tél: " + user.getTelephone() + "\n", normalFont));
            destinataire.add(new Chunk("Email: " + user.getEmail() + "\n", normalFont));
            destinataire.add(new Chunk("COTONOU\n\n", normalFont));
            document.add(destinataire);

            // Objet et référence
            Paragraph objet = new Paragraph("OBJET : Demande de stage.\n\n", boldFont);
            document.add(objet);

            Paragraph reference = new Paragraph("REFERENCE : Votre lettre en date du "
                    + user.getDateSoumission().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".\n\n", normalFont);
            document.add(reference);

            // Corps du document
            Paragraph content = new Paragraph();
            content.add(new Chunk("Mademoiselle,\n\n", normalFont));
            content.add(new Chunk("Faisant suite à votre lettre citée en référence, j'ai l'honneur de vous marquer mon accord pour le stage que vous avez sollicité.\n\n", normalFont));
            content.add(new Chunk("Toutefois, je voudrais vous préciser qu'il s'agit d'un stage académique non rémunéré et non renouvelable qui, en aucun cas, ne donne droit à l'embauche.\n\n", normalFont));
            content.add(new Chunk("Toute absence non justifiée lors du déroulement du présent stage entraînera une annulation pure et simple du présent accord.\n\n", normalFont));
            content.add(new Chunk("Pour tout renseignement complémentaire, vous voudriez bien vous rapprocher du Département des Ressources Humaines.\n\n\n", normalFont));
            document.add(content);

            // Formule de politesse
            Paragraph politesse = new Paragraph();
            politesse.add(new Chunk("Veuillez agréer, ", normalFont));
            politesse.add(new Chunk(user.getCivilite(), boldFont));
            politesse.add(new Chunk(", l'expression de mes sentiments distingués.\n\n\n\n", normalFont));
            document.add(politesse);

            // Signature
            Paragraph signature = new Paragraph();
            signature.add(new Chunk("POUR LE DIRECTEUR GENERAL\n", normalFont));
            signature.add(new Chunk("DU PORT AUTONOME DE COTONOU & P.D.\n", normalFont));
            signature.add(new Chunk("LA CHEFFE DU DEPARTEMENT DES RESSOURCES HUMAINES,\n\n\n", normalFont));

            // Signature soulignée
            Chunk signatureName = new Chunk("Arias Frosine ADANHODE GBAGUIDI", normalFont);
            signatureName.setUnderline(0.2f, -2f);
            signature.add(signatureName);

            signature.setAlignment(Element.ALIGN_RIGHT);
            document.add(signature);

            // Pied de page
            Paragraph footer = new Paragraph("\n\n\n\nDirection Générale du Port Autonome de Cotonou\n"
                    + "BP 927 Boulevard de la Marina, Cotonou - Bénin\n"
                    + "+229 21 31 52 80 contact@pac.bj\n"
                    + "www.portdecotonou.bj",
                    normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return filePath.toString();
        }
    }

}
