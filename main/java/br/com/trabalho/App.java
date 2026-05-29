package br.com.trabalho;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class App {

    public static void main(String[] args) {
        String baseDir = System.getProperty("user.dir") + File.separator + "textos";
        File dir = new File(baseDir);
        
        if (!dir.exists() || dir.listFiles() == null || dir.listFiles((d, name) -> name.endsWith(".txt")).length == 0) {
            System.out.println("Por favor, crie uma pasta chamada 'textos' na raiz do projeto e insira os arquivos .txt nela.");
            System.out.println("Caminho esperado: " + baseDir);
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite a palavra que deseja buscar: ");
        String palavraBusca = scanner.nextLine().trim();
        
        int numAmostras = 3;
        List<Result> todosResultados = new ArrayList<>();

        File[] arquivos = dir.listFiles((d, name) -> name.endsWith(".txt"));
        
        System.out.println("\nIniciando testes...\n");

        for (File arquivo : arquivos) {
            System.out.println("=============================================");
            System.out.println("Processando arquivo: " + arquivo.getName());
            
            String texto = lerArquivo(arquivo.getAbsolutePath());
            if (texto.isEmpty()) continue;

            // Variando configurações para CPU Paralela (2, 4 e 8 núcleos/threads)
            int[] coresConfig = {2, 4, 8};

            for (int amostra = 1; amostra <= numAmostras; amostra++) {
                System.out.println("\nAmostra " + amostra + "/3:");
                
                // 1. Serial CPU
                long t0 = System.currentTimeMillis();
                int countSerial = SerialCPU.contarPalavra(texto, palavraBusca);
                long t1 = System.currentTimeMillis();
                long tempoSerial = (t1 - t0);
                System.out.println("SerialCPU: " + countSerial + " ocorrências em " + tempoSerial + " ms");
                todosResultados.add(new Result(arquivo.getName(), "SerialCPU", countSerial, tempoSerial));

                // 2. Parallel CPU (Variando núcleos)
                for (int cores : coresConfig) {
                    long t2 = System.currentTimeMillis();
                    int countParallel = ParallelCPU.contarPalavra(texto, palavraBusca, cores);
                    long t3 = System.currentTimeMillis();
                    long tempoParallel = (t3 - t2);
                    System.out.println("ParallelCPU (" + cores + " threads): " + countParallel + " ocorrências em " + tempoParallel + " ms");
                    todosResultados.add(new Result(arquivo.getName(), "ParallelCPU_" + cores + "T", countParallel, tempoParallel));
                }

                // 3. Parallel GPU (OpenCL)
                try {
                    long t4 = System.currentTimeMillis();
                    int countGPU = ParallelGPU.contarPalavra(texto, palavraBusca);
                    long t5 = System.currentTimeMillis();
                    long tempoGPU = (t5 - t4);
                    System.out.println("ParallelGPU: " + countGPU + " ocorrências em " + tempoGPU + " ms");
                    todosResultados.add(new Result(arquivo.getName(), "ParallelGPU", countGPU, tempoGPU));
                } catch (Exception e) {
                    System.out.println("ParallelGPU: Erro ou OpenCL não suportado na máquina - " + e.getMessage());
                    todosResultados.add(new Result(arquivo.getName(), "ParallelGPU", 0, 0)); // Marca como 0 em caso de erro
                }
            }
        }

        // Processar e tirar as médias das amostras
        List<Result> medias = calcularMedias(todosResultados);
        
        // Exportar para CSV
        exportarCSV(medias, "resultados_medias.csv");
        
        // Gerar Gráfico
        SwingUtilities.invokeLater(() -> {
            GraficoViewer frame = new GraficoViewer(medias);
            frame.setVisible(true);
        });
        
        System.out.println("\nAnálise concluída! Os resultados foram salvos em 'resultados_medias.csv' e o gráfico foi gerado.");
    }

    private static String lerArquivo(String caminho) {
        try {
            return new String(Files.readAllBytes(Paths.get(caminho)), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static List<Result> calcularMedias(List<Result> resultados) {
        Map<String, List<Long>> agrupar = new LinkedHashMap<>();
        Map<String, Integer> contagens = new HashMap<>();

        for (Result r : resultados) {
            String chave = r.arquivo + "|" + r.metodo;
            agrupar.putIfAbsent(chave, new ArrayList<>());
            agrupar.get(chave).add(r.tempoMs);
            contagens.put(chave, r.ocorrencias); // A contagem é a mesma para a mesma chave
        }

        List<Result> medias = new ArrayList<>();
        for (Map.Entry<String, List<Long>> entry : agrupar.entrySet()) {
            String[] partes = entry.getKey().split("\\|");
            long soma = 0;
            for (long t : entry.getValue()) soma += t;
            long media = soma / entry.getValue().size();
            medias.add(new Result(partes[0], partes[1], contagens.get(entry.getKey()), media));
        }
        return medias;
    }

    private static void exportarCSV(List<Result> resultados, String nomeArquivo) {
        try (PrintWriter writer = new PrintWriter(new File(nomeArquivo))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Arquivo,Metodo,Ocorrencias,TempoMedio(ms)\n");
            for (Result r : resultados) {
                sb.append(r.arquivo).append(",")
                  .append(r.metodo).append(",")
                  .append(r.ocorrencias).append(",")
                  .append(r.tempoMs).append("\n");
            }
            writer.write(sb.toString());
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
}
