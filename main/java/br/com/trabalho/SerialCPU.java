package br.com.trabalho;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SerialCPU {
    public static int contarPalavra(String texto, String palavra) {
        int count = 0;
        // Usa regex para encontrar palavras exatas, ignorando maiúsculas e minúsculas
        Pattern p = Pattern.compile("\\b" + Pattern.quote(palavra) + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        while (m.find()) {
            count++;
        }
        return count;
    }
}
