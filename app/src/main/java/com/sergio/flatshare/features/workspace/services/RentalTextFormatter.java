package com.sergio.flatshare.features.workspace.services;

public final class RentalTextFormatter {
    private RentalTextFormatter() {
    }

    public static String normalizeDocumentType(String type) {
        if (type == null) return "-";
        return switch (type) {
            case "contrato" -> "Contrato";
            case "factura" -> "Factura";
            case "inventario" -> "Inventario";
            case "foto" -> "Foto";
            case "acta" -> "Acta";
            default -> type;
        };
    }

    public static String eventTypeLabel(String eventType, String eventRentDue, String eventRentOverdue, String eventContractEnding, String eventIncidentPending) {
        if (eventType == null) return "-";
        if (eventType.equals(eventRentDue)) return "Vencimiento de renta";
        if (eventType.equals(eventRentOverdue)) return "Impago";
        if (eventType.equals(eventContractEnding)) return "Fin de contrato";
        if (eventType.equals(eventIncidentPending)) return "Incidencia abierta";
        return eventType;
    }
}
