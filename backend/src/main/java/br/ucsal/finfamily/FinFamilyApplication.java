package br.ucsal.finfamily;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FinFamily: Sistema de Gestao Financeira e Controle de Investimentos Familiar.
 * Disciplina: Topicos Avancados de Programacao - UCSAL.
 * Equipe: Matheus Ramos de Oliveira, Alexandre Villarino e Silva, Eduardo Rocha Santos,
 *         Daniel Teixeira Melo, Marcos Vinicius Sousa Batista.
 * Orientador: Prof. Msc. Elton Figueiredo da Silva.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class FinFamilyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinFamilyApplication.class, args);
    }
}
