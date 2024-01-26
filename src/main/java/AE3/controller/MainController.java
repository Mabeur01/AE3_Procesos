package AE3.controller;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import FiltreExtensio.FiltreExtensio;

@RestController
public class MainController {

    private static final File directoriPelis = new File("./pelis");
    
    
    /**
     * Crea un nou usuari a través de l'APIpelis.
     *
     * @param jsonBody Cadena JSON que conté les dades del nou usuari.
     * @return ResponseEntity amb l'estat de la resposta i les capçaleres adequades.
     */
    @PostMapping("APIpelis/nouUsuari")
    public ResponseEntity<String> crearNouUsuari(@RequestBody String jsonBody) {
        JSONObject cosPetició = new JSONObject(jsonBody);

        try {
            String usuari = cosPetició.getString("usuari");
            if (!usuariAutoritzat(usuari)) {
                FileWriter fw = new FileWriter("./autoritzats/autoritzats.txt", true);
                fw.write("\nNom usuari:" + usuari);
                fw.close();
                return ResponseEntity.noContent().header("Content-Length", "0").build();
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).header("Content-Length", "0").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    
    
    /**
     * Cerca en el fitxer si existeix algún usuari amb el nom donat.
     *
     * @param nomUsuari Nom de l'usuari a cercar.
     * @return true si l'usuari està autoritzat, false d'una altra manera.
     */
    private boolean usuariAutoritzat(String nomUsuari) {
        try (BufferedReader br = new BufferedReader(new FileReader("./autoritzats/autoritzats.txt"))) {
            String linia;
            while ((linia = br.readLine()) != null) {
                if (!linia.isBlank()) {
                    String[] descomposicio = linia.split(":");
                    if (nomUsuari.equals(descomposicio[1]))
                        return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Obté informació sobre les pel·lícules a través de l'APIpelis.
     *
     * @param id L'ID de la pel·lícula o "all" per obtenir informació de totes les pel·lícules.
     * @return ResponseEntity amb l'estat de la resposta i les dades de la pel·lícula o llista de pel·lícules.
     */
    @GetMapping("APIpelis/t")
    public ResponseEntity<String> obtenirInfoPellicules(@RequestParam(value = "id") String id) {
        List<Pelicula> pellicules = obtenirPellicules();
        String resposta;

        if ("all".equals(id)) {
            JSONArray titolsArray = new JSONArray();
            for (Pelicula pellicula : pellicules) {
                titolsArray.put(pellicula.toJson());
            }
            resposta = new JSONObject().put("titols", titolsArray).toString();
        } else {
            Pelicula pellicula = trobarPelliculaPerId(id, pellicules);
            if (pellicula == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).header("Content-Length", "0")
                        .body("El recurs no va ser trobat");
            }
            resposta = pellicula.toJson().toString();
        }

        return ResponseEntity.ok(resposta);
    }
    
    
    
    /**
     * Crea una nova pel·lícula a través de l'APIpelis.
     *
     * @param jsonBody Cadena JSON que conté les dades de la nova pel·lícula.
     * @return ResponseEntity amb l'estat de la resposta i les capçaleres adequades.
     */
    @PostMapping("APIpelis/novaPeli")
    public ResponseEntity<String> crearNovaPellicula(@RequestBody String jsonBody) {
        JSONObject cosPetició = new JSONObject(jsonBody);

        try {
            String titol = cosPetició.getString("titol");
            String usuari = cosPetició.getString("usuari");

            if (usuariAutoritzat(usuari)) {
                List<Pelicula> pellicules = obtenirPellicules();
                int id = pellicules.size() + 1;
                Pelicula novaPellicula = new Pelicula(String.valueOf(id), titol, new ArrayList<>());
                pellicules.add(novaPellicula);
                guardarPelliculesEnFitxer(pellicules);
                return ResponseEntity.noContent().header("Content-Length", "0").build();
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Content-Length", "0").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    


    /**
     * Afegeix una nova ressenya a una pel·lícula a través de l'APIpelis.
     *
     * @param jsonBody Cadena JSON que conté les dades de la nova ressenya.
     * @return ResponseEntity amb l'estat de la resposta i les capçaleres adequades.
     */
    @PostMapping("APIpelis/novaRessenya")
    public ResponseEntity<String> afegirNovaResenya(@RequestBody String jsonBody) {
        JSONObject cosPetició = new JSONObject(jsonBody);

        try {
            String usuari = cosPetició.getString("usuari");
            String id = cosPetició.getString("id");
            String ressenya = cosPetició.getString("ressenya");

            List<Pelicula> pellicules = obtenirPellicules();
            Pelicula pellicula = trobarPelliculaPerId(id, pellicules);

            if (pellicula != null) {
                if (usuariAutoritzat(usuari)) {
                    pellicula.getRessenyes().add(usuari + ":" + ressenya);
                    guardarPelliculesEnFitxer(pellicules);
                    return ResponseEntity.noContent().header("Content-Length", "0").build();
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Content-Length", "0").build();
                }
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).header("Content-Length", "0").build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }



    /**
     * Llegeix totes les pel·lícules del directori i les carrega en una llista.
     *
     * @return Llista de pel·lícules.
     */
    private List<Pelicula> obtenirPellicules() {
        List<Pelicula> pellicules = new ArrayList<>();
        File[] arxius = directoriPelis.listFiles(new FiltreExtensio(".txt"));

        if (arxius != null) {
            for (File arxiu : arxius) {
                pellicules.add(llegirPelliculaDesDeFitxer(arxiu));
            }
        }

        return pellicules;
    }

    /**
     * Llegeix una pel·lícula des d'un fitxer.
     *
     * @param arxiu Fitxer de la pel·lícula.
     * @return Pel·lícula llegida.
     */
    private Pelicula llegirPelliculaDesDeFitxer(File arxiu) {
        try (BufferedReader br = new BufferedReader(new FileReader(arxiu))) {
            String id = arxiu.getName().split("\\.")[0];
            String titol = br.readLine().split(":")[1].trim();
            List<String> ressenyes = new ArrayList<>();

            String linia;
            while ((linia = br.readLine()) != null) {
                ressenyes.add(linia.trim());
            }

            return new Pelicula(id, titol, ressenyes);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Guarda la llista de pel·lícules en el fitxer corresponent.
     *
     * @param pellicules Llista de pel·lícules.
     */
    private void guardarPelliculesEnFitxer(List<Pelicula> pellicules) {
        for (Pelicula pellicula : pellicules) {
            String nomArxiu = "./pelis/" + pellicula.getId() + ".txt";
            try (FileWriter fw = new FileWriter(nomArxiu, false);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write("titol:" + pellicula.getTitol());
                for (String ressenya : pellicula.getRessenyes()) {
                    bw.newLine();
                    bw.write(ressenya);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Troba una pel·lícula pel seu ID a la llista de pel·lícules.
     *
     * @param id       ID de la pel·lícula.
     * @param pellicules Llista de pel·lícules.
     * @return Pel·lícula trobada o null si no es troba.
     */
    private Pelicula trobarPelliculaPerId(String id, List<Pelicula> pellicules) {
        for (Pelicula pellicula : pellicules) {
            if (pellicula.getId().equals(id)) {
                return pellicula;
            }
        }
        return null;
    }

}
