package AE3.controller;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Classe per a representar una pel·lícula.
 */
public class Pelicula {
    private final String id;
    private final String titol;
    private final List<String> ressenyes;

    public String getId() {
        return id;
    }

    public String getTitol() {
        return titol;
    }

    public List<String> getRessenyes() {
        return ressenyes;
    }

    public Pelicula(String id, String titol, List<String> ressenyes) {
        this.id = id;
        this.titol = titol;
        this.ressenyes = ressenyes;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("titol", titol);
        json.put("ressenyes", new JSONArray(ressenyes));
        return json;
    }
}
