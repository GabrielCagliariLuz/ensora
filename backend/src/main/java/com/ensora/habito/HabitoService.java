package com.ensora.habito;

import com.ensora.compartilhado.data.Semana;
import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.habito.dto.HabitoRequest;
import com.ensora.habito.dto.HabitoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class HabitoService {

	/**
	 * A data de "hoje" vem do front. O servidor só a recusa se estiver longe do relógio dele
	 * (2 dias cobrem qualquer fuso horário); não é a fonte de "hoje".
	 */
	private static final long TOLERANCIA_DA_DATA_EM_DIAS = 2;

	private final HabitoRepository habitos;
	private final HabitoMetaRepository metas;
	private final RegistroHabitoRepository registros;
	private final Clock relogio;

	public HabitoService(HabitoRepository habitos, HabitoMetaRepository metas, RegistroHabitoRepository registros,
			Clock relogio) {
		this.habitos = habitos;
		this.metas = metas;
		this.registros = registros;
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

	@Transactional
	public HabitoResponse atualizar(Long usuarioId, Long id, HabitoRequest requisicao) {
		Habito habito = buscarDoUsuario(usuarioId, id);
		if (requisicao.tipo() != habito.getTipo()) {
			throw new ValidacaoException("tipo", "não pode ser alterado");
		}

		boolean semanal = habito.getTipo() == TipoHabito.SEMANAL;
		if (semanal) {
			validarSemanal(requisicao);
		} else {
			validarDiario(requisicao);
		}

		// O hábito foi carregado nesta transação: o JPA grava a mudança do nome sozinho, ao final.
		habito.setNome(requisicao.nome().trim());
		if (!semanal) {
			return HabitoResponse.de(habito, null);
		}

		atualizarMeta(habito.getId(), requisicao);
		return HabitoResponse.de(habito, requisicao.vezesPorSemana());
	}

	/** Exclusão lógica: só preenche excluido_em. O hábito, as metas e os registros ficam para os Insights. */
	@Transactional
	public void excluir(Long usuarioId, Long id) {
		Habito habito = buscarDoUsuario(usuarioId, id);
		habito.setExcluidoEm(relogio.instant());
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

	/**
	 * Os hábitos do usuário na visão da Home de um dia. No semanal, a contagem é da semana da data (segunda a
	 * domingo) e a meta é a que valia na segunda dessa semana, não a mais recente (seção 5.2).
	 * São 3 consultas, qualquer que seja o número de hábitos.
	 */
	@Transactional(readOnly = true)
	public List<HabitoDoDia> listarDoDia(Long usuarioId, LocalDate data) {
		validarProximidadeDeHoje(data);

		List<Habito> lista = habitos.findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(usuarioId);
		if (lista.isEmpty()) {
			return List.of();
		}

		LocalDate segunda = Semana.segundaFeiraDe(data);
		List<Long> ids = lista.stream().map(Habito::getId).toList();

		// Metas da mais recente para a mais antiga: a primeira com vigente_desde <= segunda é a da semana.
		Map<Long, Integer> metaDaSemana = new HashMap<>();
		for (HabitoMeta meta : metas.findByHabitoIdInOrderByVigenteDesdeDesc(ids)) {
			if (!meta.getVigenteDesde().isAfter(segunda)) {
				metaDaSemana.putIfAbsent(meta.getHabitoId(), meta.getVezesPorSemana());
			}
		}

		Map<Long, Integer> feitosNaSemana = new HashMap<>();
		Set<Long> feitosNaData = new HashSet<>();
		for (RegistroHabito registro : registros.findByHabitoIdInAndDataBetween(ids, segunda, segunda.plusDays(6))) {
			feitosNaSemana.merge(registro.getHabitoId(), 1, Integer::sum);
			if (registro.getData().equals(data)) {
				feitosNaData.add(registro.getHabitoId());
			}
		}

		List<HabitoDoDia> resultado = new ArrayList<>();
		for (Habito habito : lista) {
			boolean feito = feitosNaData.contains(habito.getId());
			if (habito.getTipo() == TipoHabito.DIARIO) {
				resultado.add(new HabitoDoDia(habito.getId(), habito.getNome(), habito.getTipo(), feito, null, null));
				continue;
			}
			Integer meta = metaDaSemana.get(habito.getId());
			if (meta == null) {
				continue; // a data é anterior à criação do hábito: ele não existia nessa semana
			}
			resultado.add(new HabitoDoDia(habito.getId(), habito.getNome(), habito.getTipo(), feito,
					feitosNaSemana.getOrDefault(habito.getId(), 0), meta));
		}
		return resultado;
	}

	// Hábito de outro usuário, inexistente ou excluído dão o mesmo 404: não revela se o id existe para outra pessoa.
	private Habito buscarDoUsuario(Long usuarioId, Long id) {
		return habitos.findByIdAndUsuarioIdAndExcluidoEmIsNull(id, usuarioId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Hábito não encontrado"));
	}

	// A meta nova vale da semana seguinte (seção 5.2); a semana em andamento mantém a meta antiga.
	private void atualizarMeta(Long habitoId, HabitoRequest requisicao) {
		LocalDate proximaSegunda = Semana.segundaFeiraDe(requisicao.data()).plusWeeks(1);

		Optional<HabitoMeta> vigenciaFutura = metas.findByHabitoIdAndVigenteDesde(habitoId, proximaSegunda);
		if (vigenciaFutura.isPresent()) {
			HabitoMeta futura = vigenciaFutura.get();
			futura.setVezesPorSemana(requisicao.vezesPorSemana());
			metas.save(futura);
			return;
		}

		Optional<HabitoMeta> vigente = metas
				.findFirstByHabitoIdAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(habitoId, proximaSegunda);
		if (vigente.isPresent() && vigente.get().getVezesPorSemana().equals(requisicao.vezesPorSemana())) {
			return; // a meta não mudou: não cria vigência repetida
		}

		HabitoMeta nova = new HabitoMeta();
		nova.setHabitoId(habitoId);
		nova.setVezesPorSemana(requisicao.vezesPorSemana());
		nova.setVigenteDesde(proximaSegunda);
		metas.save(nova);
	}

	private void validarSemanal(HabitoRequest requisicao) {
		if (requisicao.vezesPorSemana() == null) {
			throw new ValidacaoException("vezesPorSemana", "obrigatória para hábito semanal");
		}
		if (requisicao.data() == null) {
			throw new ValidacaoException("data", "obrigatória para hábito semanal");
		}
		validarProximidadeDeHoje(requisicao.data());
	}

	private void validarProximidadeDeHoje(LocalDate data) {
		long diferenca = Math.abs(ChronoUnit.DAYS.between(LocalDate.now(relogio), data));
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
