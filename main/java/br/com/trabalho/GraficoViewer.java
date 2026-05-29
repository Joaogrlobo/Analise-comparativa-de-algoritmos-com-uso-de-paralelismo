package br.com.trabalho;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GraficoViewer extends JFrame {

    private List<Result> resultados;

    public GraficoViewer(List<Result> resultados) {
        this.resultados = resultados;
        setTitle("Análise Comparativa de Desempenho - Paralelismo");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        add(new DesenhoGrafico(resultados));
    }

    class DesenhoGrafico extends JPanel {
        private List<Result> resultados;

        public DesenhoGrafico(List<Result> resultados) {
            this.resultados = resultados;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (resultados == null || resultados.isEmpty()) return;

            Map<String, List<Result>> agrupadosPorArquivo = resultados.stream()
                    .collect(Collectors.groupingBy(r -> r.arquivo));

            int padding = 50;
            int width = getWidth() - (2 * padding);
            int height = getHeight() - (2 * padding);
            
            // Desenhar eixos
            g2d.drawLine(padding, getHeight() - padding, padding, padding);
            g2d.drawLine(padding, getHeight() - padding, getWidth() - padding, getHeight() - padding);
            
            long maxTempo = resultados.stream().mapToLong(r -> r.tempoMs).max().orElse(1);
            
            int numGrupos = agrupadosPorArquivo.size();
            int larguraGrupo = width / numGrupos;
            
            int coresIndex = 0;
            Color[] paleta = {Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.RED};

            int i = 0;
            for (Map.Entry<String, List<Result>> entry : agrupadosPorArquivo.entrySet()) {
                String arquivo = entry.getKey();
                List<Result> resArquivo = entry.getValue();
                
                int numBarras = resArquivo.size();
                int larguraBarra = (larguraGrupo - 40) / numBarras;
                
                int xInicioGrupo = padding + (i * larguraGrupo) + 20;
                
                // Rotulo do Arquivo
                g2d.setColor(Color.BLACK);
                g2d.drawString(arquivo, xInicioGrupo, getHeight() - padding + 20);

                int j = 0;
                for (Result r : resArquivo) {
                    int alturaBarra = (int) (((double) r.tempoMs / maxTempo) * height);
                    int x = xInicioGrupo + (j * larguraBarra);
                    int y = getHeight() - padding - alturaBarra;
                    
                    g2d.setColor(paleta[j % paleta.length]);
                    g2d.fillRect(x, y, larguraBarra - 2, alturaBarra);
                    
                    // Desenha o tempo em cima da barra
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    g2d.drawString(r.tempoMs + "ms", x, y - 5);
                    
                    // Legenda
                    if (i == 0) {
                        g2d.setColor(paleta[j % paleta.length]);
                        g2d.fillRect(padding + (j * 120), padding - 30, 10, 10);
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(r.metodo, padding + 15 + (j * 120), padding - 20);
                    }
                    j++;
                }
                i++;
            }
            
            // Titulo do Eixo Y
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Tempo (ms)", -getHeight() / 2, padding - 10);
            g2d.rotate(Math.PI / 2);
        }
    }
}
