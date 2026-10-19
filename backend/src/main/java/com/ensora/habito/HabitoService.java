package com.ensora.habito;

import com.ensora.compartilhado.data.Semana;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.habito.dto.HabitoRequest;
import com.ensora.habito.dto.HabitoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HabitoService {

	/**
	 * A data de "hoje" vem do front. O servidor só a recusa se estiver longe do relógio dele
	 * (2 dias cobrem qualquer fuso horário); não é a fonte de "hoje".
	 */
	private static final long TOLERANCIA_DA_DATA_EM_DIAS = 2;

	private final HabitoRepository habitos;
	private final HabitoMetaRepository metas;
	private final Clock relogio;

	public HabitoService(HabitoRepository habitos, HabitoMetaRepository metas, Clock relogio) {
		this.habitos = habitos;
		this.metas = metas;
		this.relogio = relogio;
	}

	// Uma transação só: nunca existe hábito semanal sem a meta da primeira vigência (seção 9).
	@Transactional
	public HabitoResponse criar(Long usuarioId, HabitoRequest requisicao) {
		boolean semanal = requisicao.tipo() == TipoHabito.SEMANAL;
		if (semanal) {
			validarSemanal(requisicao);
		} else {
			validarDiario(requisicao);
		}

		Habito habito = new Habito();
		habito.setUsuarioId(usuarioId);
		habito.setNome(requisicao.nome().trim());
		habito.setTipo(requisicao.tipo());
		habito = habitos.save(habito);

		if (!semanal) {
			return HabitoResponse.de(habito, null);
		}

		HabitoMeta meta = new HabitoMeta();
		meta.setHabitoId(habito.getId());
		meta.setVezesPorSemana(requisicao.vezesPorSemana());
		meta.setVigenteDesde(Semana.segundaFeiraDe(requisicao.data()));
		metas.save(meta);
		return HabitoResponse.de(habito, requisicao.vezesPorSemana());
	}

	@Transactional(readOnly = true)
	public List<HabitoResponse> listar(Long usuarioId) {
		List<Habito> lista = habitos.findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(usuarioId);
		if (lista.isEmpty()) {
			return List.of();
		}

		// Uma consulta só para as metas de todos os hábitos. A lista vem da vigência mais recente para a
		// mais antiga, então a primeira meta vista de cada hábito é a mais recente.
		List<Long> ids = lista.stream().map(Habito::getId).toList();
		Map<Long, Integer> metaMaisRecente = new HashMap<>();
		for (HabitoMeta meta : metas.findByHabitoIdInOrderByVigenteDesdeDesc(ids)) {
			metaMaisRecente.putIfAbsent(meta.getHabitoId(), meta.getVezesPorSemana());
		}

		return lista.stream()
				.map(habito -> HabitoResponse.de(habito, metaMaisRecente.get(habito.getId())))
				.toList();
	}

	private void validarSemanal(HabitoRequest requisicao) {
		if (requisicao.vezesPorSemana() == null) {
			throw new ValidacaoException("vezesPorSemana", "obrigatória para hábito semanal");
		}
		if (requisicao.data() == null) {
			throw new ValidacaoException("data", "obrigatória para hábito semanal");
		}
		long diferenca = Math.abs(ChronoUnit.DAYS.between(LocalDate.now(relogio), requisicao.data()));
		if (diferenca > TOLERANCIA_DA_DATA_EM_DIAS) {
			throw new ValidacaoException("data", "muito distante da data de hoje");
		}
	}

	private void validarDiario(HabitoRequest requisicao) {
		if (requisicao.vezesPorSemana() != null) {
			throw new ValidacaoException("vezesPorSemana", "só vale para hábito semanal");
		}
	}

}
