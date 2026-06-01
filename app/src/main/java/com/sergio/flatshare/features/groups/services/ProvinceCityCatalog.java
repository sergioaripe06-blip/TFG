package com.sergio.flatshare.features.groups.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public final class ProvinceCityCatalog {
    private static final LinkedHashMap<String, List<String>> PROVINCE_CITIES = buildProvinceCityMap();

    private ProvinceCityCatalog() {
    }

    public static LinkedHashMap<String, List<String>> provinceCities() {
        return new LinkedHashMap<>(PROVINCE_CITIES);
    }

    public static List<String> provinces() {
        return new ArrayList<>(PROVINCE_CITIES.keySet());
    }

    public static String canonicalProvince(String typedProvince) {
        if (typedProvince == null) return "";
        String value = typedProvince.trim();
        if (value.isEmpty()) return "";
        for (String province : PROVINCE_CITIES.keySet()) {
            if (province.equalsIgnoreCase(value)) {
                return province;
            }
        }
        return value;
    }

    private static LinkedHashMap<String, List<String>> buildProvinceCityMap() {
        LinkedHashMap<String, List<String>> data = new LinkedHashMap<>();
        data.put("A Coruña", Arrays.asList("A Coruña", "Santiago de Compostela", "Ferrol"));
        data.put("Álava", Arrays.asList("Vitoria-Gasteiz", "Llodio", "Amurrio"));
        data.put("Albacete", Arrays.asList("Albacete", "Hellín", "Villarrobledo"));
        data.put("Alicante", Arrays.asList("Alicante", "Elche", "Benidorm"));
        data.put("Almería", Arrays.asList("Almería", "Roquetas de Mar", "El Ejido"));
        data.put("Asturias", Arrays.asList("Oviedo", "Gijón", "Avilés"));
        data.put("Ávila", Arrays.asList("Ávila", "Arévalo", "Cebreros"));
        data.put("Badajoz", Arrays.asList("Badajoz", "Mérida", "Don Benito"));
        data.put("Barcelona", Arrays.asList("Barcelona", "L'Hospitalet de Llobregat", "Badalona"));
        data.put("Burgos", Arrays.asList("Burgos", "Miranda de Ebro", "Aranda de Duero"));
        data.put("Cáceres", Arrays.asList("Cáceres", "Plasencia", "Navalmoral de la Mata"));
        data.put("Cádiz", Arrays.asList("Cádiz", "Jerez de la Frontera", "Algeciras"));
        data.put("Cantabria", Arrays.asList("Santander", "Torrelavega", "Castro-Urdiales"));
        data.put("Castellón", Arrays.asList("Castellón de la Plana", "Vila-real", "Burriana"));
        data.put("Ciudad Real", Arrays.asList("Ciudad Real", "Puertollano", "Tomelloso"));
        data.put("Córdoba", Arrays.asList("Córdoba", "Lucena", "Puente Genil"));
        data.put("Cuenca", Arrays.asList("Cuenca", "Tarancón", "San Clemente"));
        data.put("Girona", Arrays.asList("Girona", "Figueres", "Blanes"));
        data.put("Granada", Arrays.asList("Granada", "Motril", "Armilla"));
        data.put("Guadalajara", Arrays.asList("Guadalajara", "Azuqueca de Henares", "Molina de Aragón"));
        data.put("Guipúzcoa", Arrays.asList("San Sebastián", "Irún", "Eibar"));
        data.put("Huelva", Arrays.asList("Huelva", "Lepe", "Almonte"));
        data.put("Huesca", Arrays.asList("Huesca", "Barbastro", "Jaca"));
        data.put("Illes Balears", Arrays.asList("Palma", "Calvià", "Eivissa"));
        data.put("Jaén", Arrays.asList("Jaén", "Linares", "Andújar"));
        data.put("La Rioja", Arrays.asList("Logroño", "Calahorra", "Arnedo"));
        data.put("Las Palmas", Arrays.asList("Las Palmas de Gran Canaria", "Telde", "Arrecife"));
        data.put("León", Arrays.asList("León", "Ponferrada", "San Andrés del Rabanedo"));
        data.put("Lleida", Arrays.asList("Lleida", "Balaguer", "La Seu d'Urgell"));
        data.put("Lugo", Arrays.asList("Lugo", "Monforte de Lemos", "Viveiro"));
        data.put("Madrid", Arrays.asList("Madrid", "Móstoles", "Alcalá de Henares"));
        data.put("Málaga", Arrays.asList("Málaga", "Marbella", "Fuengirola"));
        data.put("Murcia", Arrays.asList("Murcia", "Cartagena", "Lorca"));
        data.put("Navarra", Arrays.asList("Pamplona", "Tudela", "Estella"));
        data.put("Ourense", Arrays.asList("Ourense", "Verín", "O Barco de Valdeorras"));
        data.put("Palencia", Arrays.asList("Palencia", "Aguilar de Campoo", "Guardo"));
        data.put("Pontevedra", Arrays.asList("Pontevedra", "Vigo", "Vilagarcía de Arousa"));
        data.put("Salamanca", Arrays.asList("Salamanca", "Béjar", "Ciudad Rodrigo"));
        data.put("Santa Cruz de Tenerife", Arrays.asList("Santa Cruz de Tenerife", "San Cristóbal de La Laguna", "Arona"));
        data.put("Segovia", Arrays.asList("Segovia", "Cuéllar", "El Espinar"));
        data.put("Sevilla", Arrays.asList("Sevilla", "Dos Hermanas", "Alcalá de Guadaíra"));
        data.put("Soria", Arrays.asList("Soria", "Almazán", "El Burgo de Osma"));
        data.put("Tarragona", Arrays.asList("Tarragona", "Reus", "Tortosa"));
        data.put("Teruel", Arrays.asList("Teruel", "Alcañiz", "Andorra"));
        data.put("Toledo", Arrays.asList("Toledo", "Talavera de la Reina", "Illescas"));
        data.put("Valencia", Arrays.asList("València", "Torrent", "Gandia"));
        data.put("Valladolid", Arrays.asList("Valladolid", "Medina del Campo", "Laguna de Duero"));
        data.put("Vizcaya", Arrays.asList("Bilbao", "Barakaldo", "Getxo"));
        data.put("Zamora", Arrays.asList("Zamora", "Benavente", "Toro"));
        data.put("Zaragoza", Arrays.asList("Zaragoza", "Calatayud", "Utebo"));
        return data;
    }
}
