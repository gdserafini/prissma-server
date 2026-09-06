package br.pucpr.prissma_server.report;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Intervalo fechado [from, to] que recorta o relatório.
 *
 * O intervalo filtra ATIVIDADE (despesas por spentAt, conclusões por
 * actualEndDate/completedAt). Os dados cadastrais da obra, a equipe e os
 * denominadores de etapas e tarefas continuam absolutos — o leitor precisa ver
 * o que andou no período no contexto do todo.
 */
public record ReportPeriod(LocalDate from, LocalDate to) {

    /**
     * Preenche as pontas que o cliente não mandou.
     *
     * Sem 'to', assume hoje. Sem 'from', assume o início planejado da obra; se a
     * obra também não tem data planejada, cai para 30 dias antes do fim, para o
     * relatório nunca sair vazio por falta de cadastro.
     */
    public static ReportPeriod resolve(LocalDate from, LocalDate to,
                                       LocalDate projectPlannedStart, LocalDate today) {
        LocalDate resolvedTo = to != null ? to : today;
        LocalDate resolvedFrom = from != null
                ? from
                : (projectPlannedStart != null ? projectPlannedStart : resolvedTo.minusDays(30));

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data inicial não pode ser posterior à data final");
        }
        return new ReportPeriod(resolvedFrom, resolvedTo);
    }

    /** Intervalo fechado nas duas pontas: uma despesa exatamente em 'from' conta. */
    public boolean contains(LocalDate date) {
        return date != null && !date.isBefore(from) && !date.isAfter(to);
    }

    public boolean contains(Instant instant, ZoneId zone) {
        return instant != null && contains(LocalDate.ofInstant(instant, zone));
    }

    /**
     * Uma etapa entra no recorte se o intervalo dela cruza o período em qualquer
     * ponto. Etapa sem nenhuma das duas datas não se sobrepõe a nada — continua
     * contando nos totais, mas não aparece na tabela do período.
     */
    public boolean overlaps(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return false;
        }
        LocalDate effectiveStart = start != null ? start : end;
        LocalDate effectiveEnd = end != null ? end : start;
        return !effectiveStart.isAfter(to) && !effectiveEnd.isBefore(from);
    }
}
