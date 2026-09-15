/* ==========================================================================
   INIFLEX FULLSTACK + PROTHERA RISK DASHBOARD JAVASCRIPT
   Single Page Application (SPA) Logic, Reactive Store & REST API Integration
   ========================================================================== */

const API_BASE_URL = '/api';

// Reactive Store Pattern (Inspirado no Vue 3 Pinia / Vuex Store)
const store = {
    state: {
        funcionarios: [],
        estatisticas: {},
        filtroAniversariantesAtivo: false,
        filtroFuncao: 'TODAS',
        termoBusca: '',
        ordenacao: 'NOME_ASC',
        theme: localStorage.getItem('iniflex-theme') || 'dark'
    },
    listeners: [],
    subscribe(listener) {
        this.listeners.push(listener);
    },
    notify() {
        this.listeners.forEach(fn => fn(this.state));
    },
    setState(newState) {
        Object.assign(this.state, newState);
        this.notify();
    }
};

// DOM Element References
const elements = {
    tablaBody: document.getElementById('tabla-body'),
    funcoesContainer: document.getElementById('funcoes-container'),
    searchInput: document.getElementById('search-input'),
    filterFuncao: document.getElementById('filter-funcao'),
    sortSelect: document.getElementById('sort-select'),
    btnAumento: document.getElementById('btn-aumento'),
    btnRemoverJoao: document.getElementById('btn-remover-joao'),
    btnFiltroAniv: document.getElementById('btn-filtro-aniv'),
    btnReset: document.getElementById('btn-reset'),
    btnNovoFuncionario: document.getElementById('btn-novo-funcionario'),
    themeToggle: document.getElementById('theme-toggle'),
    modalFuncionario: document.getElementById('modal-funcionario'),
    modalClose: document.getElementById('modal-close'),
    modalCancel: document.getElementById('modal-cancel'),
    formFuncionario: document.getElementById('form-funcionario'),
    
    // Metrics
    metricTotalSalario: document.getElementById('metric-total-salario'),
    metricMediaSalario: document.getElementById('metric-media-salario'),
    metricTotalColab: document.getElementById('metric-total-colab'),
    metricCargosCount: document.getElementById('metric-cargos-count'),
    metricMaisVelhoNome: document.getElementById('metric-mais-velho-nome'),
    metricMaisVelhoIdade: document.getElementById('metric-mais-velho-idade'),
    metricRiscoStatus: document.getElementById('metric-risco-status'),
    metricDisparidade: document.getElementById('metric-disparidade'),

    // Risk Panel
    riscoGlobalDesc: document.getElementById('risco-global-desc'),
    riscoRazao: document.getElementById('risco-razao'),
    riscoPiso: document.getElementById('risco-piso')
};

// Chart Instances
let chartFuncoesInstance = null;
let chartSalariosMinimosInstance = null;
let chartConcentracaoInstance = null;

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
    aplicarTema(store.state.theme);
    configurarEventListeners();
    
    // Inscreve a renderização nas mudanças do Store
    store.subscribe(() => {
        atualizarOpcoesFuncao();
        atualizarMetricas();
        renderizarTabela();
        renderizarAgrupadosPorFuncao();
        renderizarPainelRisco();
    });

    carregarDados();
});

// Setup Event Listeners
function configurarEventListeners() {
    // Theme Switcher
    elements.themeToggle.addEventListener('click', () => {
        const newTheme = store.state.theme === 'dark' ? 'light' : 'dark';
        localStorage.setItem('iniflex-theme', newTheme);
        store.setState({ theme: newTheme });
        aplicarTema(newTheme);
    });

    // Tab Buttons
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const tabId = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            
            btn.classList.add('active');
            document.getElementById(tabId).classList.add('active');

            if (tabId === 'tab-graficos' || tabId === 'tab-risco') {
                renderizarGraficos();
            }
        });
    });

    // Search Input
    elements.searchInput.addEventListener('input', (e) => {
        store.setState({ termoBusca: e.target.value.toLowerCase() });
    });

    // Filter Funcao Select
    elements.filterFuncao.addEventListener('change', (e) => {
        store.setState({ filtroFuncao: e.target.value });
    });

    // Sort Select
    elements.sortSelect.addEventListener('change', (e) => {
        store.setState({ ordenacao: e.target.value });
    });

    // Action 3.4: Aplicar 10% de Aumento
    elements.btnAumento.addEventListener('click', async () => {
        try {
            const res = await fetch(`${API_BASE_URL}/funcionarios/aumento`, { method: 'POST' });
            const data = await res.json();
            showToast(data.mensagem, 'success');
            await carregarDados();
        } catch (err) {
            showToast('Erro ao aplicar aumento.', 'error');
        }
    });

    // Action 3.2: Remover João
    elements.btnRemoverJoao.addEventListener('click', async () => {
        try {
            const res = await fetch(`${API_BASE_URL}/funcionarios/remover-joao`, { method: 'POST' });
            const data = await res.json();
            showToast(data.mensagem, data.sucesso ? 'warning' : 'info');
            await carregarDados();
        } catch (err) {
            showToast('Erro ao remover João.', 'error');
        }
    });

    // Action 3.8: Toggle Filtro Aniversariantes (Meses 10 e 12)
    elements.btnFiltroAniv.addEventListener('click', () => {
        const novoStatus = !store.state.filtroAniversariantesAtivo;
        store.setState({ filtroAniversariantesAtivo: novoStatus });
        
        if (novoStatus) {
            elements.btnFiltroAniv.classList.add('btn-primary');
            elements.btnFiltroAniv.classList.remove('btn-outline');
            showToast('Filtrando aniversariantes dos meses 10 e 12', 'info');
        } else {
            elements.btnFiltroAniv.classList.remove('btn-primary');
            elements.btnFiltroAniv.classList.add('btn-outline');
            showToast('Filtro de aniversariantes desativado', 'info');
        }
    });

    // Action 3.1: Resetar Dados
    elements.btnReset.addEventListener('click', async () => {
        try {
            const res = await fetch(`${API_BASE_URL}/funcionarios/reset`, { method: 'POST' });
            const data = await res.json();
            showToast(data.mensagem, 'info');
            store.setState({ filtroAniversariantesAtivo: false });
            elements.btnFiltroAniv.classList.remove('btn-primary');
            elements.btnFiltroAniv.classList.add('btn-outline');
            await carregarDados();
        } catch (err) {
            showToast('Erro ao restaurar dados.', 'error');
        }
    });

    // Modal Control
    elements.btnNovoFuncionario.addEventListener('click', () => {
        elements.modalFuncionario.classList.add('active');
    });
    elements.modalClose.addEventListener('click', fecharModal);
    elements.modalCancel.addEventListener('click', fecharModal);

    // Form Submit (Novo Funcionário)
    elements.formFuncionario.addEventListener('submit', async (e) => {
        e.preventDefault();
        const nome = document.getElementById('input-nome').value.trim();
        const dataNascimento = document.getElementById('input-data-nasc').value;
        const salario = document.getElementById('input-salario').value;
        const funcao = document.getElementById('input-funcao').value.trim();

        try {
            const res = await fetch(`${API_BASE_URL}/funcionarios`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nome, dataNascimento, salario, funcao })
            });
            const data = await res.json();
            if (data.sucesso) {
                showToast(data.mensagem, 'success');
                fecharModal();
                elements.formFuncionario.reset();
                await carregarDados();
            } else {
                showToast(data.mensagem, 'error');
            }
        } catch (err) {
            showToast('Falha na comunicação com o servidor.', 'error');
        }
    });
}

function fecharModal() {
    elements.modalFuncionario.classList.remove('active');
}

function aplicarTema(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    const icon = elements.themeToggle.querySelector('i');
    if (theme === 'light') {
        icon.className = 'fa-solid fa-sun';
    } else {
        icon.className = 'fa-solid fa-moon';
    }
}

// Sanitizador XSS para prevenção de DOM-based XSS Injection
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// Fetch all data from Java REST API
async function carregarDados() {
    try {
        const [resFunc, resEst] = await Promise.all([
            fetch(`${API_BASE_URL}/funcionarios`),
            fetch(`${API_BASE_URL}/estatisticas`)
        ]);

        const funcionarios = await resFunc.json();
        const estatisticas = await resEst.json();

        store.setState({ funcionarios, estatisticas });
    } catch (err) {
        console.error("Erro ao carregar dados da API:", err);
        showToast("Erro ao conectar com a API REST Java.", "error");
    }
}

// Update Filter Options
function atualizarOpcoesFuncao() {
    const funcoes = [...new Set(store.state.funcionarios.map(f => f.funcao))];
    const valAtual = elements.filterFuncao.value;
    
    elements.filterFuncao.innerHTML = '<option value="TODAS">Todas as Funções (Req 3.5)</option>';
    funcoes.forEach(f => {
        const opt = document.createElement('option');
        opt.value = escapeHtml(f);
        opt.textContent = f;
        elements.filterFuncao.appendChild(opt);
    });
    
    elements.filterFuncao.value = funcoes.includes(valAtual) ? valAtual : 'TODAS';
}

// Update Top Dashboard Cards
function atualizarMetricas() {
    const est = store.state.estatisticas;
    
    elements.metricTotalSalario.textContent = est.totalSalariosFormatado || 'R$ 0,00';
    
    const count = est.quantidadeFuncionarios || 0;
    elements.metricTotalColab.textContent = count;
    
    if (count > 0 && est.totalSalarios) {
        const media = est.totalSalarios / count;
        elements.metricMediaSalario.textContent = `Média: R$ ${formatarMoeda(media)} / colab.`;
    } else {
        elements.metricMediaSalario.textContent = `Média: R$ 0,00 / colab.`;
    }

    const funcoesCount = Object.keys(est.porFuncao || {}).length;
    elements.metricCargosCount.textContent = `${funcoesCount} funções distintas`;

    if (est.funcionarioMaisVelho) {
        elements.metricMaisVelhoNome.textContent = est.funcionarioMaisVelho.nome;
        elements.metricMaisVelhoIdade.textContent = `${est.funcionarioMaisVelho.idade} anos`;
    } else {
        elements.metricMaisVelhoNome.textContent = '--';
        elements.metricMaisVelhoIdade.textContent = '--';
    }

    const risco = est.protheraRisco || {};
    elements.metricRiscoStatus.textContent = risco.nivelRiscoGlobal || 'N/A';
    elements.metricDisparidade.textContent = `Disparidade: ${risco.disparidadeSalarialRazao || 0}x`;
}

// Render Prothera Risk Panel
function renderizarPainelRisco() {
    const risco = store.state.estatisticas.protheraRisco || {};
    
    if (elements.riscoGlobalDesc) {
        elements.riscoGlobalDesc.innerHTML = `
            <strong>Diagnóstico Prothera:</strong> <span style="color: var(--accent-purple); font-weight: 700;">${escapeHtml(risco.nivelRiscoGlobal || 'BAIXO')}</span>. 
            A política salarial foi auditada automaticamente pelo motor de conformidade em Java SQL.
        `;
    }
    if (elements.riscoRazao) elements.riscoRazao.textContent = `${escapeHtml(risco.disparidadeSalarialRazao || '0')}x`;
    if (elements.riscoPiso) elements.riscoPiso.textContent = `${risco.conformidadePisoPercentual || 100}%`;
}

// Render Main Data Table (Requirements 3.3, 3.8, 3.10, 3.12)
function renderizarTabela() {
    let lista = [...store.state.funcionarios];

    // Busca textual
    if (store.state.termoBusca) {
        lista = lista.filter(f => 
            f.nome.toLowerCase().includes(store.state.termoBusca) ||
            f.funcao.toLowerCase().includes(store.state.termoBusca)
        );
    }

    // Filtro Função
    if (store.state.filtroFuncao !== 'TODAS') {
        lista = lista.filter(f => f.funcao === store.state.filtroFuncao);
    }

    // Filtro Aniversariantes (Req 3.8)
    if (store.state.filtroAniversariantesAtivo) {
        lista = lista.filter(f => {
            const parts = f.dataNascimentoFormatada.split('/');
            const mes = parseInt(parts[1], 10);
            return mes === 10 || mes === 12;
        });
    }

    // Ordenação (Req 3.10 e custom)
    lista.sort((a, b) => {
        if (store.state.ordenacao === 'NOME_ASC') return a.nome.localeCompare(b.nome);
        if (store.state.ordenacao === 'SALARIO_DESC') return b.salario - a.salario;
        if (store.state.ordenacao === 'SALARIO_ASC') return a.salario - b.salario;
        if (store.state.ordenacao === 'IDADE_DESC') return b.idade - a.idade;
        return 0;
    });

    elements.tablaBody.innerHTML = '';

    if (lista.length === 0) {
        elements.tablaBody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 32px; color: var(--text-muted);">
                    <i class="fa-solid fa-folder-open" style="font-size: 24px; margin-bottom: 8px; display: block;"></i>
                    Nenhum colaborador encontrado com os filtros selecionados.
                </td>
            </tr>`;
        return;
    }

    lista.forEach(f => {
        const tr = document.createElement('tr');
        const badgeClass = getRoleBadgeClass(f.funcao);
        const inicial = escapeHtml(f.nome.charAt(0).toUpperCase());

        tr.innerHTML = `
            <td>
                <div class="user-cell">
                    <div class="avatar-circle">${inicial}</div>
                    <span>${escapeHtml(f.nome)}</span>
                </div>
            </td>
            <td>${escapeHtml(f.dataNascimentoFormatada)}</td>
            <td>${f.idade} anos</td>
            <td><span class="badge-role ${badgeClass}">${escapeHtml(f.funcao)}</span></td>
            <td class="salario-highlight">${escapeHtml(f.salarioFormatado)}</td>
            <td><span class="min-salario-pill">${f.salariosMinimos} SM</span></td>
            <td class="text-right">
                <button class="btn-sm-delete" onclick="removerFuncionario('${escapeHtml(f.nome)}')" title="Excluir Colaborador">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            </td>
        `;
        elements.tablaBody.appendChild(tr);
    });
}

// Render Function Groups View (Requirement 3.6)
function renderizarAgrupadosPorFuncao() {
    const porFuncao = store.state.estatisticas.porFuncao || {};
    elements.funcoesContainer.innerHTML = '';

    for (const [funcao, lista] of Object.entries(porFuncao)) {
        const card = document.createElement('div');
        card.className = 'funcao-card glass';

        const totalFuncao = lista.reduce((acc, f) => acc + f.salario, 0);

        let membersHtml = '';
        lista.forEach(f => {
            membersHtml += `
                <div class="member-item">
                    <div>
                        <strong>${escapeHtml(f.nome)}</strong>
                        <div style="font-size: 11px; color: var(--text-muted);">${escapeHtml(f.dataNascimentoFormatada)} (${f.idade} anos)</div>
                    </div>
                    <div style="text-align: right;">
                        <span class="salario-highlight" style="font-size: 13px;">${escapeHtml(f.salarioFormatado)}</span>
                        <div style="font-size: 11px; color: var(--text-muted);">${f.salariosMinimos} SM</div>
                    </div>
                </div>
            `;
        });

        card.innerHTML = `
            <div class="funcao-card-header">
                <span class="funcao-title">
                    <span class="badge-role ${getRoleBadgeClass(funcao)}">${escapeHtml(funcao)}</span>
                    <span>(${lista.length})</span>
                </span>
                <span style="font-size: 12px; font-weight: 600; color: var(--accent-green);">R$ ${formatarMoeda(totalFuncao)}</span>
            </div>
            <div class="funcao-members-list">
                ${membersHtml}
            </div>
        `;

        elements.funcoesContainer.appendChild(card);
    }
}

// Render Analytics & Risk Charts (Chart.js)
function renderizarGraficos() {
    const est = store.state.estatisticas;
    const porFuncao = est.porFuncao || {};
    const labelsFuncoes = Object.keys(porFuncao);
    const totalsFuncoes = labelsFuncoes.map(fn => porFuncao[fn].reduce((acc, f) => acc + f.salario, 0));

    // Chart 1: Folha Salarial por Função
    const el1 = document.getElementById('chart-funcoes');
    if (el1) {
        const ctx1 = el1.getContext('2d');
        if (chartFuncoesInstance) chartFuncoesInstance.destroy();

        chartFuncoesInstance = new Chart(ctx1, {
            type: 'bar',
            data: {
                labels: labelsFuncoes,
                datasets: [{
                    label: 'Total Salários (R$)',
                    data: totalsFuncoes,
                    backgroundColor: ['#3b82f6', '#10b981', '#8b5cf6', '#f59e0b', '#ec4899', '#0ea5e9', '#eab308'],
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { ticks: { color: store.state.theme === 'dark' ? '#94a3b8' : '#475569' }, grid: { color: store.state.theme === 'dark' ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' } },
                    x: { ticks: { color: store.state.theme === 'dark' ? '#94a3b8' : '#475569' }, grid: { display: false } }
                }
            }
        });
    }

    // Chart 2: Salários Mínimos por Funcionário
    const el2 = document.getElementById('chart-salarios-minimos');
    if (el2) {
        const ctx2 = el2.getContext('2d');
        if (chartSalariosMinimosInstance) chartSalariosMinimosInstance.destroy();

        const sortedFuncs = [...store.state.funcionarios].sort((a,b) => b.salariosMinimos - a.salariosMinimos);
        const labelsNames = sortedFuncs.map(f => f.nome);
        const dataMinimos = sortedFuncs.map(f => f.salariosMinimos);

        chartSalariosMinimosInstance = new Chart(ctx2, {
            type: 'bar',
            data: {
                labels: labelsNames,
                datasets: [{
                    label: 'Qtde de Salários Mínimos',
                    data: dataMinimos,
                    backgroundColor: 'rgba(16, 185, 129, 0.7)',
                    borderColor: '#10b981',
                    borderWidth: 1,
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                indexAxis: 'y',
                plugins: { legend: { display: false } },
                scales: {
                    x: { ticks: { color: store.state.theme === 'dark' ? '#94a3b8' : '#475569' }, grid: { color: store.state.theme === 'dark' ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' } },
                    y: { ticks: { color: store.state.theme === 'dark' ? '#94a3b8' : '#475569' }, grid: { display: false } }
                }
            }
        });
    }

    // Chart 3: Concentração de Risco por Função
    const el3 = document.getElementById('chart-concentracao-risco');
    if (el3) {
        const ctx3 = el3.getContext('2d');
        if (chartConcentracaoInstance) chartConcentracaoInstance.destroy();

        const concMap = (est.protheraRisco || {}).concentracaoPorFuncao || {};
        const labelsRisco = Object.keys(concMap);
        const dataRisco = Object.values(concMap);

        chartConcentracaoInstance = new Chart(ctx3, {
            type: 'doughnut',
            data: {
                labels: labelsRisco,
                datasets: [{
                    data: dataRisco,
                    backgroundColor: ['#8b5cf6', '#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#0ea5e9', '#eab308']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'right', labels: { color: store.state.theme === 'dark' ? '#94a3b8' : '#475569' } }
                }
            }
        });
    }
}

// Delete Employee Helper
async function removerFuncionario(nome) {
    if (!confirm(`Deseja realmente remover o colaborador "${nome}"?`)) return;
    
    try {
        const res = await fetch(`${API_BASE_URL}/funcionarios/${encodeURIComponent(nome)}`, { method: 'DELETE' });
        const data = await res.json();
        if (data.sucesso) {
            showToast(data.mensagem, 'success');
            await carregarDados();
        } else {
            showToast(data.mensagem, 'error');
        }
    } catch (err) {
        showToast('Erro ao remover funcionário.', 'error');
    }
}

// Role Badge CSS Class Helper
function getRoleBadgeClass(funcao) {
    const map = {
        'operador': 'badge-operador',
        'gerente': 'badge-gerente',
        'diretor': 'badge-diretor',
        'coordenador': 'badge-coordenador',
        'contador': 'badge-contador',
        'recepcionista': 'badge-recepcionista',
        'eletricista': 'badge-eletricista'
    };
    return map[funcao.toLowerCase()] || 'badge-default';
}

// Currency Formatter
function formatarMoeda(val) {
    return val.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// Toast Notifications
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    const icons = {
        success: 'fa-circle-check',
        error: 'fa-circle-xmark',
        warning: 'fa-triangle-exclamation',
        info: 'fa-circle-info'
    };
    
    toast.innerHTML = `<i class="fa-solid ${icons[type] || icons.info}"></i> <span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}
