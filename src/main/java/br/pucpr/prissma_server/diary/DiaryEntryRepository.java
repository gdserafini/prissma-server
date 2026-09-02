package br.pucpr.prissma_server.diary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {

    /**
     * Listagem paginada do diario de uma obra, do mais recente para o mais antigo.
     *
     * A ordenacao vem fixa na query (e nao via Pageable/Sort) porque a ordem faz
     * parte do contrato do endpoint. O desempate por id DESC evita que registros
     * com a mesma entry_date mudem de pagina entre duas requisicoes.
     *
     * O JOIN FETCH do responsavel e do anexo resolve o N+1 que apareceria ao
     * montar a resposta de cada item da pagina; ambos sao ManyToOne opcionais,
     * entao o LEFT e obrigatorio para nao perder registros sem anexo.
     */
    @Query(value = "SELECT d FROM DiaryEntry d "
            + "LEFT JOIN FETCH d.responsibleUser "
            + "LEFT JOIN FETCH d.attachment "
            + "WHERE d.constructionProject.id = :projectId "
            + "ORDER BY d.entryDate DESC, d.id DESC",
            countQuery = "SELECT COUNT(d) FROM DiaryEntry d WHERE d.constructionProject.id = :projectId")
    Page<DiaryEntry> findPageByProject(@Param("projectId") Long projectId, Pageable pageable);

}
