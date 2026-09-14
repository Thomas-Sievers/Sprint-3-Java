package br.com.fiap.nous_sight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "ALERTA_GERADO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class GeneratedAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ALERTA")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ID_TRANSCRICAO", nullable = false)
    private TranscriptLine transcriptLine;

    @ManyToOne
    @JoinColumn(name = "ID_TERMO", nullable = false)
    private DictionaryTerm term;

    @Column(name = "OBSERVACAO", length = 200)
    private String note;

    @Column(name = "SCORE_CONFIANCA", nullable = false)
    private Integer confidenceScore;

    @Column(name = "STATUS_VALIDACAO", nullable = false)
    private String validationStatus;
}
