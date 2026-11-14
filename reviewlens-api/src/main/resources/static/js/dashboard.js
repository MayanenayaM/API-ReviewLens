'use strict';

const URL_ESTATISTICAS = '/api/reviews/stats';
const TEMPO_LIMITE_MS = 10000;
const INTERVALO_ATUALIZACAO_MS = 60000;
const ESPERA_MAXIMA_FONTES_MS = 1500;

const ROTULOS_SENTIMENTO = { POSITIVO: 'Positivo', NEGATIVO: 'Negativo', NEUTRO: 'Neutro' };
const ROTULOS_TOPICO = {
  SABOR: 'Sabor',
  ENTREGA: 'Entrega',
  TEMPERATURA: 'Temperatura',
  EMBALAGEM: 'Embalagem',
  PRECO: 'Preço',
  OUTROS: 'Outros'
};

const COR_SENTIMENTO = { POSITIVO: '#446b16', NEGATIVO: '#e5533d', NEUTRO: '#8f8578' };
const COR_TOPICO = {
  SABOR: '#4b79b4',
  ENTREGA: '#db3215',
  TEMPERATURA: '#699c4c',
  EMBALAGEM: '#ff9100',
  PRECO: '#c79a1d',
  OUTROS: '#a89b8c'
};

const COR_FUNDO_CARD = '#131a2a';

let graficoSentimentos = null;
const linhasTopicos = new Map(); 

/* ---------- Busca de dados ---------- */

async function buscarEstatisticas() {
  const controle = new AbortController();
  const temporizador = setTimeout(() => controle.abort(), TEMPO_LIMITE_MS);
  try {
    const resposta = await fetch(URL_ESTATISTICAS, {
      signal: controle.signal,
      headers: { Accept: 'application/json' }
    });
    if (!resposta.ok) {
      throw new Error('HTTP ' + resposta.status);
    }
    return await resposta.json();
  } finally {
    clearTimeout(temporizador);
  }
}

/* ---------- Utilidades de exibição ---------- */

function formatarPercentual(valor) {
  return valor.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + '%';
}

function formatarMes(anoMes) {
  const [ano, mes] = anoMes.split('-').map(Number);
  return new Date(ano, mes - 1, 1).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
}

function mostrarMensagem(texto, tipo) {
  const caixa = document.getElementById('mensagem');
  if (!texto) {
    caixa.hidden = true;
    caixa.textContent = '';
    return;
  }
  caixa.textContent = texto;
  caixa.className = 'mensagem ' + tipo;
  caixa.hidden = false;
}

// Duplo requestAnimationFrame: garante que a transição de largura comece do valor anterior
function animarLargura(elemento, porcentagem) {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      elemento.style.width = porcentagem + '%';
    });
  });
}

/* ---------- Gráfico de pizza ---------- */

function desenharSentimentos(sentimentos) {
  if (typeof Chart === 'undefined') {
    return;
  }

  const chaves = Object.keys(sentimentos);
  const rotulos = chaves.map((c) => ROTULOS_SENTIMENTO[c] || c);
  const valores = chaves.map((c) => sentimentos[c]);
  const cores = chaves.map((c) => COR_SENTIMENTO[c] || '#999999');

  if (graficoSentimentos) {
    graficoSentimentos.destroy();
  }

  graficoSentimentos = new Chart(document.getElementById('grafico-sentimentos'), {
    type: 'pie',
    data: {
      labels: rotulos,
      datasets: [{
        data: valores,
        backgroundColor: cores,
        borderColor: COR_FUNDO_CARD,
        borderWidth: 3,
        hoverOffset: 8
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: { usePointStyle: true, pointStyle: 'circle', padding: 18 }
        },
        tooltip: {
          callbacks: {
            label: (contexto) => {
              const total = contexto.dataset.data.reduce((soma, v) => soma + v, 0);
              const pct = total ? (contexto.parsed / total) * 100 : 0;
              return ' ' + contexto.label + ': ' + contexto.parsed + ' (' + formatarPercentual(pct) + ')';
            }
          }
        }
      }
    }
  });
}

/* ---------- Barras de progresso ---------- */

function criarLinhaTopico(chave) {
  const rotulo = ROTULOS_TOPICO[chave] || chave;

  const li = document.createElement('li');
  li.className = 'item-topico';

  const linha = document.createElement('div');
  linha.className = 'linha-topico';

  const nome = document.createElement('span');
  nome.className = 'nome-topico';
  const textoNome = document.createElement('span');
  textoNome.textContent = rotulo;
  nome.appendChild(textoNome);

  const valor = document.createElement('span');
  valor.className = 'valor-topico';

  linha.append(nome, valor);

  const trilho = document.createElement('div');
  trilho.className = 'trilho';
  trilho.setAttribute('role', 'progressbar');
  trilho.setAttribute('aria-valuemin', '0');
  trilho.setAttribute('aria-valuemax', '100');

  const preenchimento = document.createElement('div');
  preenchimento.className = 'preenchimento';
  preenchimento.style.setProperty('--cor-topico', COR_TOPICO[chave] || '#ffb347');

  trilho.appendChild(preenchimento);
  li.append(linha, trilho);

  return { li, nome, valor, trilho, preenchimento, etiqueta: null, rotulo };
}

// RN-10: a etiqueta "Crítico" aparece junto com a barra vermelha
function atualizarEtiqueta(linha, critico) {
  if (critico && !linha.etiqueta) {
    const etiqueta = document.createElement('span');
    etiqueta.className = 'etiqueta-critico';
    etiqueta.textContent = 'Crítico';
    linha.nome.appendChild(etiqueta);
    linha.etiqueta = etiqueta;
  } else if (!critico && linha.etiqueta) {
    linha.etiqueta.remove();
    linha.etiqueta = null;
  }
}

function desenharTopicos(topicos, alertaCritico) {
  const lista = document.getElementById('lista-topicos');
  const total = Object.values(topicos).reduce((soma, v) => soma + v, 0);

  const ordenados = Object.entries(topicos).sort((a, b) => b[1] - a[1]);

  ordenados.forEach(([chave, quantidade]) => {
    let linha = linhasTopicos.get(chave);
    if (!linha) {
      linha = criarLinhaTopico(chave);
      linhasTopicos.set(chave, linha);
    }

    const porcentagem = total ? (quantidade / total) * 100 : 0;
    const critico = chave === 'ENTREGA' && alertaCritico;

    linha.valor.textContent = formatarPercentual(porcentagem) + ' das menções (' + quantidade + ')';
    linha.li.classList.toggle('critico', critico);
    linha.li.classList.toggle('vazio', quantidade === 0);
    linha.trilho.setAttribute('aria-valuenow', porcentagem.toFixed(1));
    linha.trilho.setAttribute('aria-label', linha.rotulo + ': ' + formatarPercentual(porcentagem) + ' das menções');
    atualizarEtiqueta(linha, critico);

    lista.appendChild(linha.li); // reposiciona a linha conforme a ordenação
    animarLargura(linha.preenchimento, porcentagem);
  });
}

/* ---------- Cartão e alerta da Entrega (RN-10) ---------- */

function atualizarEntrega(entrega) {
  const cartao = document.getElementById('cartao-entrega');
  const valor = document.getElementById('entrega-percentual');
  const detalhe = document.getElementById('entrega-detalhe');
  const alerta = document.getElementById('alerta');
  const legenda = document.getElementById('legenda-alerta');
  const mes = formatarMes(entrega.mes);

  if (entrega.total === 0) {
    valor.textContent = '—';
    detalhe.textContent = 'Sem avaliações de entrega em ' + mes;
  } else {
    valor.textContent = formatarPercentual(entrega.percentualNegativo);
    detalhe.textContent = entrega.negativas + ' de ' + entrega.total + ' avaliações de entrega em ' + mes;
  }

  cartao.classList.toggle('critico', entrega.alertaCritico);
  legenda.hidden = !entrega.alertaCritico;

  if (entrega.alertaCritico) {
    alerta.textContent = 'Atenção: ' + formatarPercentual(entrega.percentualNegativo) +
      ' das avaliações de Entrega em ' + mes + ' são negativas.';
    alerta.hidden = false;
  } else {
    alerta.hidden = true;
  }
}

/* ---------- Montagem da tela ---------- */

function renderizar(dados) {
  document.getElementById('total').textContent = dados.totalProcessadas.toLocaleString('pt-BR');
  atualizarEntrega(dados.entregaMes);

  if (dados.totalProcessadas === 0) {
    mostrarMensagem('Ainda não há avaliações processadas para exibir.', 'info');
  }

  desenharSentimentos(dados.sentimentos);
  desenharTopicos(dados.topicos, dados.entregaMes.alertaCritico);

  if (typeof Chart === 'undefined') {
    mostrarMensagem(
      'Não foi possível carregar a biblioteca de gráficos (Chart.js). Confira o caminho do arquivo.',
      'erro'
    );
  }

  document.getElementById('atualizado').textContent =
    'Atualizado às ' + new Date().toLocaleTimeString('pt-BR');
}

async function carregar() {
  const botao = document.getElementById('btn-atualizar');
  botao.disabled = true;
  botao.textContent = 'Carregando...';

  try {
    const dados = await buscarEstatisticas();
    mostrarMensagem(null);
    renderizar(dados);
  } catch (erro) {
    console.error('Falha ao carregar as estatísticas:', erro);
    mostrarMensagem(
      'Não foi possível carregar os dados agora. Confira se a API está no ar e tente novamente.',
      'erro'
    );
  } finally {
    botao.disabled = false;
    botao.textContent = 'Atualizar';
  }
}

function iniciar() {
  document.getElementById('btn-atualizar').addEventListener('click', carregar);
  carregar();
  setInterval(carregar, INTERVALO_ATUALIZACAO_MS);
}

/* ---------- Início ---------- */

if (typeof Chart !== 'undefined') {
  // Tema escuro para textos e fontes dentro do canvas
  Chart.defaults.color = '#aab2c5';
  Chart.defaults.font.family = "'Inter', system-ui, sans-serif";
  Chart.defaults.font.size = 13;
}

// Espera as fontes carregarem (no máximo 1,5 s) para o texto do gráfico já sair com a fonte certa
if (document.fonts && document.fonts.ready) {
  Promise.race([
    document.fonts.ready,
    new Promise((resolve) => setTimeout(resolve, ESPERA_MAXIMA_FONTES_MS))
  ]).then(iniciar);
} else {
  iniciar();
}