package br.com.trabalho;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParallelCPU {

    public static int contarPalavra(String texto, String palavra, int numThreads) {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        // Dividir o texto em linhas para evitar cortar palavras ao meio
        String[] linhas = texto.split("\n");
        int totalLinhas = linhas.length;
        int linhasPorThread = (int) Math.ceil((double) totalLinhas / numThreads);
        
        List<Future<Integer>> resultados = new ArrayList<>();
        Pattern p = Pattern.compile("\\b" + Pattern.quote(palavra) + "\\b", Pattern.CASE_INSENSITIVE);

        for (int i = 0; i < numThreads; i++) {
            final int start = i * linhasPorThread;
            final int end = Math.min(start + linhasPorThread, totalLinhas);
            
            if(start >= end) break; // Todas as linhas foram divididas
            
            Callable<Integer> tarefa = () -> {
                int count = 0;
                for (int j = start; j < end; j++) {
                    Matcher m = p.matcher(linhas[j]);
                    while (m.find()) {
                        count++;
                    }
                }
                return count;
            };
            resultados.add(executor.submit(tarefa));
        }

        int totalCount = 0;
        for (Future<Integer> futuro : resultados) {
            try {
                totalCount += futuro.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
        return totalCount;
    }
}
